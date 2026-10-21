package com.polyguard.mc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final PolyGuardPlugin plugin;

    public ChatListener(PolyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    // LOWEST = we run first, so other chat plugins never see a toxic message
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        final Player player = e.getPlayer();
        if (player.hasPermission("polyguard.bypass")) return;

        final String msg = e.getMessage();
        if (msg == null || msg.trim().length() < plugin.getConfig().getInt("filter.min-length", 2)) return;

        PolyGuardClient client = plugin.getClient();
        String mode = plugin.getConfig().getString("filter.mode", "BLOCK").toUpperCase();

        // AsyncPlayerChatEvent runs off the main thread, so a blocking HTTP call is safe here.
        try {
            if (mode.equals("CENSOR")) {
                String clean = client.censor(msg);
                if (!clean.equals(msg)) {
                    e.setMessage(clean);
                    send(player, plugin.msg("censored"));
                    notifyStaff(player, msg);
                }
            } else {
                String decision = client.check(msg);
                if ("TOXIC".equalsIgnoreCase(decision)) {
                    e.setCancelled(true); // deleted from chat
                    send(player, plugin.msg("blocked"));
                    notifyStaff(player, msg);
                }
            }
            if (plugin.isDebug()) plugin.getLogger().info("[debug] checked '" + msg + "' from " + player.getName());
        } catch (Exception ex) {
            if (!plugin.getConfig().getBoolean("filter.fail-open", true)) {
                e.setCancelled(true);
                send(player, plugin.msg("blocked"));
            }
            plugin.getLogger().warning("PolyGuard API error: " + ex.getMessage());
        }
    }

    private void send(final Player p, final String text) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (p.isOnline()) p.sendMessage(text);
            }
        });
    }

    private void notifyStaff(final Player offender, final String original) {
        final String line = plugin.msg("notify")
                .replace("{player}", offender.getName())
                .replace("{message}", original);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getLogger().info(offender.getName() + " blocked: " + original);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p != offender && p.hasPermission("polyguard.notify")) p.sendMessage(line);
                }
            }
        });
    }
}
