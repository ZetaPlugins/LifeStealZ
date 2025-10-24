package com.zetaplugins.lifestealz.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.addon.LifeStealZAddon;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class ConfigManager {
    private final LifeStealZ plugin;
    private final Map<String, FileConfiguration> addonConfigs = new HashMap<>();

    public ConfigManager(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration getStorageConfig() {
        return getCustomConfig("storage");
    }

    public FileConfiguration getCustomItemConfig() {
        return getCustomConfig("items");
    }

    public FileConfiguration getCustomConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(fileName + ".yml", false);
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getAddonConfig(LifeStealZAddon addon) {
        return getAddonConfig(addon.getMetadata().getConfigName(), addon.getClass());
    }

    /**
     * Gets all configs as a map of file name to file content used for debug command
     * @return Map of file name to file content
     */
    public Map<String, String> getConfigsMap() {
        Map<String, String> configs = new HashMap<>();
        configs.put("config.yml", plugin.getConfig().saveToString());
        configs.put("storage.yml", getStorageConfig().saveToString());
        configs.put("items.yml", getCustomItemConfig().saveToString());
        return configs;
    }

    public FileConfiguration getAddonConfig(String configName, Class<? extends LifeStealZAddon> addonClass) {
        if (addonConfigs.containsKey(configName)) {
            return addonConfigs.get(configName);
        }

        // Create addons directory if it doesn't exist
        File addonsDir = new File(plugin.getDataFolder(), "addons");
        if (!addonsDir.exists()) {
            addonsDir.mkdirs();
        }

        File configFile = new File(addonsDir, configName + ".yml");

        // If config doesn't exist, try to copy from addon resources
        if (!configFile.exists()) {
            try {
                // Try to get config.yml from the addon's jar resources
                InputStream defaultConfigStream = addonClass.getResourceAsStream("/config.yml");
                if (defaultConfigStream != null) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(defaultConfigStream)
                    );
                    defaultConfig.save(configFile);
                } else {
                    // Create empty config if no default exists
                    configFile.createNewFile();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not create config for addon: " + configName);
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        addonConfigs.put(configName, config);
        return config;
    }

    public void reloadAddonConfig(String configName) {
        addonConfigs.remove(configName);
    }

    public void saveAddonConfig(String configName) {
        FileConfiguration config = addonConfigs.get(configName);
        if (config != null) {
            try {
                File configFile = new File(new File(plugin.getDataFolder(), "addons"), configName + ".yml");
                config.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not save addon config: " + configName);
                e.printStackTrace();
            }
        }
    }
}