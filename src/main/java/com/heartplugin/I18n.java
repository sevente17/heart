package com.heartplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class I18n {

    private static final Map<String, Map<String, String>> MESSAGES = new HashMap<>();
    private static String defaultLocale = "zh_cn";
    private static String languageSetting = "auto";

    private I18n() {
    }

    static void load(JavaPlugin plugin) {
        MESSAGES.clear();
        languageSetting = plugin.getConfig().getString("language", "auto");
        defaultLocale = "zh_cn";

        saveDefaultLang(plugin, "zh_cn");
        saveDefaultLang(plugin, "en_us");

        File langDir = new File(plugin.getDataFolder(), "lang");
        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String locale = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                register(plugin, locale, loadFile(file));
            }
        }
        if (MESSAGES.isEmpty()) {
            plugin.getLogger().warning("没有可用的语言文件，无法加载消息。");
        }
    }

    private static void saveDefaultLang(JavaPlugin plugin, String locale) {
        File file = new File(plugin.getDataFolder(), "lang" + File.separator + locale + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + locale + ".yml", false);
        }
    }

    private static YamlConfiguration loadFile(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            return new YamlConfiguration();
        }
    }

    private static void register(JavaPlugin plugin, String locale, YamlConfiguration yaml) {
        Map<String, String> map = MESSAGES.computeIfAbsent(locale, k -> new HashMap<>());
        for (Map.Entry<String, Object> entry : yaml.getValues(false).entrySet()) {
            map.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private static String resolveLocale(Player player) {
        if (!"auto".equalsIgnoreCase(languageSetting)) {
            return MESSAGES.containsKey(languageSetting) ? languageSetting : defaultLocale;
        }
        if (player != null) {
            String locale = player.getLocale() == null ? "" : player.getLocale().toLowerCase(Locale.ROOT).replace('-', '_');
            if (MESSAGES.containsKey(locale)) {
                return locale;
            }
            String prefix = locale.contains("_") ? locale.substring(0, locale.indexOf('_')) : locale;
            for (String key : MESSAGES.keySet()) {
                if (key.startsWith(prefix)) {
                    return key;
                }
            }
        }
        return defaultLocale;
    }

    static String raw(String locale, String key) {
        Map<String, String> map = MESSAGES.get(locale);
        if (map == null || !map.containsKey(key)) {
            map = MESSAGES.get(defaultLocale);
        }
        if (map == null || !map.containsKey(key)) {
            return key;
        }
        return map.get(key);
    }

    private static String translate(String locale, String key, Object... args) {
        String text = raw(locale, key);
        for (int i = 0; i + 1 < args.length; i += 2) {
            text = text.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
        }
        return text;
    }

    static Component component(Player player, String key, Object... args) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(translate(resolveLocale(player), key, args));
    }

    static String text(String locale, String key, Object... args) {
        return translate(locale, key, args);
    }

    static String itemName() {
        return translate(defaultLocale, "item-name");
    }

    public static String defaultLocale() {
        return defaultLocale;
    }
}