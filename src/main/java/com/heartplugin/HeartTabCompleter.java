package com.heartplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public final class HeartTabCompleter implements TabCompleter {

    private static final String ADMIN_PERMISSION = "heartplugin.admin";
    private static final List<String> PUBLIC_SUBCOMMANDS = List.of("get", "give");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("opgive", "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> available = new ArrayList<>(PUBLIC_SUBCOMMANDS);
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                available.addAll(ADMIN_SUBCOMMANDS);
            }
            String prefix = args[0].toLowerCase();
            return available.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}