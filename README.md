# PolyGuardChat

AI anti-swear plugin for Minecraft **1.8.9** (Spigot/Paper 1.8.8 API), built with **JDK 17**.
Every chat message is sent to the PolyGuard API. If it is `TOXIC`, the message is deleted from chat (or censored with `***` in CENSOR mode).

## Build
```
mvn clean package
```
Jar: `target/PolyGuardChat-1.0.0.jar` -> put it in `plugins/`.

GitHub Actions builds on every push (jar under *Actions > Artifacts*).
Push a tag like `v1.0.0` to get an automatic Release with the jar.

## Config (`plugins/PolyGuardChat/config.yml`)
- `api.links-url` : links.json with the live `base_url` (auto re-fetched if the tunnel changes)
- `api.api-key`   : sent as `X-API-Key` if set
- `filter.mode`   : `BLOCK` (delete message) or `CENSOR` (replace bad words)
- `filter.fail-open` : what to do if the API is down / 429

## Commands & permissions
- `/polyguard reload | status | test <text>` (`polyguard.admin`)
- `polyguard.bypass` : not filtered
- `polyguard.notify` : see blocked messages
