package com.polyguard.mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Java port of the Python guard() helper. */
public class PolyGuardClient {

    private final String linksUrl;
    private final String apiKey;
    private final int timeoutMs;
    private volatile String baseUrl;

    public PolyGuardClient(String linksUrl, String apiKey, int timeoutMs) {
        this.linksUrl = linksUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeoutMs = timeoutMs;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /** BASE = requests.get(PAGES).json()["base_url"] */
    public synchronized void refreshBaseUrl() throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(linksUrl).openConnection();
        con.setConnectTimeout(timeoutMs);
        con.setReadTimeout(timeoutMs);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "PolyGuardChat");
        // bust GitHub Pages cache so a new tunnel link is picked up fast
        con.setUseCaches(false);
        int code = con.getResponseCode();
        String body = read(code >= 400 ? con.getErrorStream() : con.getInputStream());
        if (code >= 400) throw new IOException("links.json HTTP " + code);
        JsonObject obj = new JsonParser().parse(body).getAsJsonObject();
        if (!obj.has("base_url")) throw new IOException("links.json has no base_url");
        String b = obj.get("base_url").getAsString().trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        baseUrl = b;
    }

    /** guard(path, **body). Retries once with a fresh base_url if the tunnel died. */
    public JsonObject guard(String path, JsonObject body) throws IOException {
        if (baseUrl == null) refreshBaseUrl();
        try {
            return post(path, body);
        } catch (RateLimitedException e) {
            throw e;
        } catch (IOException e) {
            refreshBaseUrl();
            return post(path, body);
        }
    }

    private JsonObject post(String path, JsonObject body) throws IOException {
        URL url = new URL(baseUrl + "/api/v1/" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(timeoutMs);
        con.setReadTimeout(timeoutMs);
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "PolyGuardChat");
        if (!apiKey.isEmpty()) con.setRequestProperty("X-API-Key", apiKey);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream os = con.getOutputStream();
        try {
            os.write(payload);
        } finally {
            os.close();
        }

        int code = con.getResponseCode();
        if (code == 429) {
            String ra = con.getHeaderField("Retry-After");
            throw new RateLimitedException(ra == null ? "?" : ra);
        }
        String resp = read(code >= 400 ? con.getErrorStream() : con.getInputStream());
        if (code >= 400) throw new IOException("PolyGuard HTTP " + code + ": " + resp);
        return new JsonParser().parse(resp).getAsJsonObject();
    }

    // ---- helpers matching the Python examples ----

    /** guard("check", text=...)["decision"] -> "TOXIC" / ... */
    public String check(String text) throws IOException {
        JsonObject r = guard("check", textBody(text));
        JsonElement d = r.get("decision");
        return d == null || d.isJsonNull() ? "" : d.getAsString();
    }

    /** guard("verdict", text=...)["flagged"] */
    public boolean verdict(String text) throws IOException {
        JsonObject r = guard("verdict", textBody(text));
        JsonElement f = r.get("flagged");
        return f != null && !f.isJsonNull() && f.getAsBoolean();
    }

    /** guard("censor", text=...)["text"] */
    public String censor(String text) throws IOException {
        JsonObject r = guard("censor", textBody(text));
        JsonElement t = r.get("text");
        return t == null || t.isJsonNull() ? text : t.getAsString();
    }

    /** guard("check/batch", texts=[...])["summary"] */
    public JsonElement checkBatch(List<String> texts) throws IOException {
        JsonObject b = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String s : texts) arr.add(new com.google.gson.JsonPrimitive(s));
        b.add("texts", arr);
        return guard("check/batch", b).get("summary");
    }

    private static JsonObject textBody(String text) {
        JsonObject o = new JsonObject();
        o.addProperty("text", text);
        return o;
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
