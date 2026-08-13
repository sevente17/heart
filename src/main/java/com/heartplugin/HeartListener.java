package com.heartplugin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;

import java.util.UUID;

public final class HeartListener implements Listener {

    private final HeartPlugin plugin;

    public HeartListener(HeartPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (!plugin.isHeartApple(item)) {
            return;
        }

        int maxHearts = plugin.maxHearts();
        if (maxHearts >= 0) {
            AttributeInstance attribute = event.getPlayer().getAttribute(Attribute.MAX_HEALTH);
            double limit = maxHearts * plugin.heartHp();
            if (attribute != null && attribute.getValue() >= limit) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(I18n.component(event.getPlayer(), "max-hearts-reached"));
                return;
            }
        }

        plugin.grantHeart(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.deathDropEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        double minHp = plugin.minHearts() * plugin.heartHp();
        int removable = (int) ((attribute.getValue() - minHp) / plugin.heartHp());
        if (removable <= 0) {
            return;
        }

        int count = Math.min(plugin.deathDropCount(), removable);
        attribute.addModifier(new AttributeModifier(
                new NamespacedKey(plugin, "heart_death_" + UUID.randomUUID()),
                -plugin.heartHp() * count,
                AttributeModifier.Operation.ADD_NUMBER));

        for (int i = 0; i < count; i++) {
            player.getWorld().dropItemNaturally(player.getLocation(), plugin.makeHeartApple());
        }

        player.sendMessage(I18n.component(player, "death-lost-hearts", "count", count));
    }
}