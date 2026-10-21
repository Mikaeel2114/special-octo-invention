package com.polyguard.mc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PolyGuardCommand implements CommandExecutor {

    private final PolyGuardPlugin plugin;

    public PolyGuardCommand(PolyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("polyguard.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload | status | test <text>");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.msg("reloaded"));
        } else if (sub.equals("status")) {
            String base = plugin.getClient().getBaseUrl();
            sender.sendMessage(ChatColor.GRAY + "base_url: " + ChatColor.WHITE + (base == null ? "not loaded" : base));
            sender.sendMessage(ChatColor.GRAY + "mode: " + ChatColor.WHITE + plugin.getConfig().getString("filter.mode"));
        } else if (sub.equals("test") && args.length > 1) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) sb.append(i > 1 ? " " : "").append(args[i]);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    String result;
                    try {
                        result = ChatColor.GRAY + "decision: " + ChatColor.WHITE + plugin.getClient().check(sb.toString())
                                + ChatColor.GRAY + " | censor: " + ChatColor.WHITE + plugin.getClient().censor(sb.toString());
                    } catch (Exception ex) {
                        result = ChatColor.RED + "Error: " + ex.getMessage();
                    }
                    final String r = result;
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(r);
                        }
                    });
                }
            });
        } else {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload | status | test <text>");
        }
        return true;
    }
}
