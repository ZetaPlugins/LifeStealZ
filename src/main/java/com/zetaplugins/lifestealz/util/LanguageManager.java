package com.zetaplugins.lifestealz.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.zetaplugins.lifestealz.LifeStealZ;

import java.io.File;
import java.util.List;

public final class LanguageManager {
    private final JavaPlugin plugin;
    public static final List<String> defaultLangs = List.of("cs-CZ", "de-DE", "en-US", "es-ES", "fr-FR", "pl-PL", "vi-VN", "zh-CN", "nl-NL", "ru-RU", "pt-BR", "ja-JP", "hu-HU", "it-IT");

    private FileConfiguration langConfig;

    public LanguageManager(LifeStealZ plugin) {
        this.plugin = plugin;
        loadLanguageConfig();
    }

    public void reload() {
        loadLanguageConfig();
    }

    /**
     * Load the language file from the plugin data folder
     */
    private void loadLanguageConfig() {
        File languageDirectory = new File(plugin.getDataFolder(), "lang/");
        if (!languageDirectory.exists() || !languageDirectory.isDirectory()) languageDirectory.mkdir();

        for (String langString : defaultLangs) {
            File langFile = new File("lang/", langString + ".yml");
            if (!new File(languageDirectory, langString + ".yml").exists()) {
                plugin.getLogger().info("Saving file " + langFile.getPath());
                plugin.saveResource(langFile.getPath(), false);
            }
        }

        String langOption = plugin.getConfig().getString("lang") != null ? plugin.getConfig().getString("lang") : "en-US";
        File selectedLangFile = new File(languageDirectory, langOption + ".yml");

        if (!selectedLangFile.exists()) {
            selectedLangFile = new File(languageDirectory, "en-US.yml");
            plugin.getLogger().warning("Language file " + langOption + ".yml (" + selectedLangFile.getPath() + ") not found! Using fallback en-US.yml.");
        }

        plugin.getLogger().info("Using language file: " + selectedLangFile.getPath());
        langConfig = YamlConfiguration.loadConfiguration(selectedLangFile);
    }

    /**
     * Get a string from the language file
     * @param key The key to get the string for
     * @return The string from the language file
     */
    public String getString(String key) {
        return langConfig.getString(key);
    }

    /**
     * Get a string from the language file with a fallback
     * @param key The key to get the string for
     * @param fallback The fallback string
     * @return The string from the language file or the fallback
     */
    public String getString(String key, String fallback) {
        return langConfig.getString(key) != null ? langConfig.getString(key) : fallback;
    }
}
