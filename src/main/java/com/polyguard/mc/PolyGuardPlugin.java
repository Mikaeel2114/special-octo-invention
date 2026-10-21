package com.polyguard.mc;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class PolyGuardPlugin extends JavaPlugin {

    private volatile PolyGuardClient client;
    private BukkitTask refreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupClient();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getCommand("polyguard").setExecutor(new PolyGuardCommand(this));
        getLogger().info("PolyGuardChat enabled (mode: " + getConfig().getString("filter.mode") + ")");
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) refreshTask.cancel();
    }

    public void reloadPlugin() {
        reloadConfig();
        setupClient();
    }

    private void setupClient() {
        if (refreshTask != null) refreshTask.cancel();
        client = new PolyGuardClient(
                getConfig().getString("api.links-url"),
                getConfig().getString("api.api-key", ""),
                getConfig().getInt("api.timeout-ms", 4000));

        long minutes = getConfig().getLong("api.refresh-minutes", 10);
        long period = minutes > 0 ? minutes * 60L * 20L : Long.MAX_VALUE / 2;
        final PolyGuardClient c = client;
        refreshTask = getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    c.refreshBaseUrl();
                    if (isDebug()) getLogger().info("[debug] base_url = " + c.getBaseUrl());
                } catch (Exception e) {
                    getLogger().warning("Could not load base_url from links.json: " + e.getMessage());
                }
            }
        }, 0L, period);
    }

    public PolyGuardClient getClient() {
        return client;
    }

    public boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    public String msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String m = getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', prefix + m);
    }
}
