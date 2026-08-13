package com.heartplugin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeartPlugin extends JavaPlugin {

    static final String HEART_APPLE_TAG = "heart_apple";

    private final Map<UUID, Long> lastHeartUse = new ConcurrentHashMap<>();

    private NamespacedKey heartAppleKey;
    private double heartHp = 2.0;
    private int minHearts = 1;
    private int maxHearts = -1;
    private long cooldownSeconds = 0;
    private boolean deathDropEnabled = true;
    private int deathDropCount = 1;

    @Override
    public void onEnable() {
        reloadSettings();
        heartAppleKey = new NamespacedKey(this, HEART_APPLE_TAG);
        getCommand("heart").setExecutor(new HeartCommand(this));
        getCommand("heart").setTabCompleter(new HeartTabCompleter());
        getServer().getPluginManager().registerEvents(new HeartListener(this), this);
        getLogger().info("HeartPlugin 已启用");
    }

    @Override
    public void onDisable() {
        lastHeartUse.clear();
    }

    void reloadSettings() {
        saveDefaultConfig();
        reloadConfig();
        heartHp = getConfig().getDouble("heart-hp", 2.0);
        minHearts = Math.max(1, getConfig().getInt("min-hearts", 1));
        maxHearts = getConfig().getInt("max-hearts", -1);
        cooldownSeconds = Math.max(0, getConfig().getLong("heart-cooldown", 0));
        deathDropEnabled = getConfig().getBoolean("death-heart-drop.enabled", true);
        deathDropCount = Math.max(1, getConfig().getInt("death-heart-drop.heart-count", 1));
        I18n.load(this);
    }

    NamespacedKey heartAppleKey() {
        return heartAppleKey;
    }

    double heartHp() {
        return heartHp;
    }

    int minHearts() {
        return minHearts;
    }

    int maxHearts() {
        return maxHearts;
    }

    long cooldownSeconds() {
        return cooldownSeconds;
    }

    boolean deathDropEnabled() {
        return deathDropEnabled;
    }

    int deathDropCount() {
        return deathDropCount;
    }

    boolean onCooldown(Player player) {
        if (cooldownSeconds <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = lastHeartUse.get(player.getUniqueId());
        if (last != null && now - last < cooldownSeconds * 1000L) {
            return true;
        }
        lastHeartUse.put(player.getUniqueId(), now);
        return false;
    }

    ItemStack makeHeartApple() {
        ItemStack item = new ItemStack(Material.APPLE);
        if (!item.hasData(DataComponentTypes.CONSUMABLE)) {
            item.setData(DataComponentTypes.CONSUMABLE, Material.APPLE.getDefaultData(DataComponentTypes.CONSUMABLE));
        }
        if (!item.hasData(DataComponentTypes.FOOD)) {
            item.setData(DataComponentTypes.FOOD, Material.APPLE.getDefaultData(DataComponentTypes.FOOD));
        }
        ItemMeta meta = item.getItemMeta();
        FoodComponent food = meta.getFood();
        food.setCanAlwaysEat(true);
        meta.setFood(food);
        meta.customName(Component.text(I18n.itemName()).color(NamedTextColor.RED));
        meta.lore(List.of(
                Component.text(I18n.raw(I18n.defaultLocale(), "item-lore-1")).color(NamedTextColor.GRAY),
                Component.text(I18n.raw(I18n.defaultLocale(), "item-lore-2")).color(NamedTextColor.GRAY)));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(heartAppleKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    boolean isHeartApple(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        return item.getPersistentDataContainer().has(heartAppleKey, PersistentDataType.BOOLEAN);
    }

    void grantHeart(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        attribute.addModifier(new AttributeModifier(
                new NamespacedKey(this, "heart_gain_" + UUID.randomUUID()),
                heartHp,
                AttributeModifier.Operation.ADD_NUMBER));
        player.setHealth(Math.min(player.getHealth() + heartHp, attribute.getValue()));
        player.sendMessage(I18n.component(player, "heart-gained"));
    }
}