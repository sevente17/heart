package com.heartplugin;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class HeartCommand implements CommandExecutor {

    private final HeartPlugin plugin;

    public HeartCommand(HeartPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            handleGive(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "get":
            case "give":
                handleGive(sender);
                return true;
            case "opgive":
                handleOpGive(sender);
                return true;
            case "reload":
                handleReload(sender);
                return true;
            default:
                sender.sendMessage(I18n.component(null, "command-usage"));
                return true;
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("heartplugin.admin")) {
            sender.sendMessage(I18n.component(null, "no-permission"));
            return;
        }
        plugin.reloadSettings();
        sender.sendMessage(I18n.component(null, "config-reloaded"));
    }

    private void handleGive(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.component(null, "only-player"));
            return;
        }

        if (plugin.onCooldown(player)) {
            player.sendMessage(I18n.component(player, "cooldown", "seconds", plugin.cooldownSeconds()));
            return;
        }

        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        double currentMax = attribute.getValue();
        if (currentMax <= plugin.minHearts() * plugin.heartHp()) {
            player.sendMessage(I18n.component(player, "too-few-hearts", "min", plugin.minHearts()));
            return;
        }

        attribute.addModifier(new AttributeModifier(
                new NamespacedKey(plugin, "heart_loss_" + UUID.randomUUID()),
                -plugin.heartHp(),
                AttributeModifier.Operation.ADD_NUMBER));

        if (player.getHealth() > attribute.getValue()) {
            player.setHealth(attribute.getValue());
        }

        giveApple(player);
        player.sendMessage(I18n.component(player, "heart-given", "item", I18n.itemName()));
    }

    private void handleOpGive(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.component(null, "only-player"));
            return;
        }
        if (!sender.hasPermission("heartplugin.admin")) {
            sender.sendMessage(I18n.component(player, "no-permission"));
            return;
        }
        giveApple(player);
        player.sendMessage(I18n.component(player, "admin-heart-given", "item", I18n.itemName()));
    }

    private void giveApple(Player player) {
        ItemStack heartApple = plugin.makeHeartApple();
        for (ItemStack rest : player.getInventory().addItem(heartApple).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }
}