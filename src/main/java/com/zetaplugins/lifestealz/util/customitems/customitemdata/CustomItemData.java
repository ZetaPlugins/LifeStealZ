package com.zetaplugins.lifestealz.util.customitems.customitemdata;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.customitems.CustomItemType;

import java.util.ArrayList;
import java.util.List;

public class CustomItemData {
    private final String itemId;
    private final String name;
    private final List<String> lore;
    private final Material material;
    private final boolean enchanted;
    private final CustomItemType customItemType;
    private final boolean craftable;
    private final boolean requirePermission;
    private final boolean invulnerable;
    private final boolean despawnable;
    private final int customModelID;
    private final List<String> whitelistedWorlds;

    /**
     * Creates a new CustomItemData object for the given item ID.
     * @param itemId the ID of the custom item
     * @throws IllegalArgumentException if the custom item with the given ID does not exist
     */
    public CustomItemData(String itemId) throws IllegalArgumentException {
        this.itemId = itemId;
        ConfigurationSection section = getConfigurationSection();
        if (section == null) throw new IllegalArgumentException("Custom item with id " + itemId + " does not exist!");

        this.name = section.getString("name", "&7Fallback Name");
        this.lore = section.getStringList("lore");
        this.material = Material.valueOf(section.getString("material", "STONE"));
        this.enchanted = section.getBoolean("enchanted", false);
        this.customItemType = CustomItemType.fromString(section.getString("customItemType", "none"));
        this.craftable = section.getBoolean("craftable", true);
        this.requirePermission = section.getBoolean("requirePermission", false);
        this.invulnerable = section.getBoolean("invulnerable", false);
        this.despawnable = section.getBoolean("despawnable", true);
        this.whitelistedWorlds = section.getStringList("whitelistedWorlds");
        this.customModelID = section.getInt("customModelId", 0);
    }

    /**
     * Returns the configuration section for this custom item.
     * @return the configuration section for this custom item
     */
    protected ConfigurationSection getConfigurationSection() {
        FileConfiguration config = LifeStealZ.getInstance().getConfigManager().getCustomItemConfig();
        return config.getConfigurationSection(itemId);
    }

    public CustomItemSoundData getSound() {
        return new CustomItemSoundData(itemId);
    }

    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    public CustomItemType getCustomItemType() {
        return customItemType;
    }

    public boolean isCraftable() {
        return craftable;
    }

    public boolean requiresPermission() {
        return requirePermission;
    }

    public String getPermission() {
        return "lifestealz.item." + itemId;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isDespawnable() {
        return despawnable;
    }

    public int getCustomModelID() {
        return customModelID;
    }

    public List<String> getWhitelistedWorlds() {
        return new ArrayList<>(whitelistedWorlds);
    }

    /**
     * Checks if the custom item is allowed in the given world.
     * @param worldName the name of the world to check
     * @return true if the item is allowed in the world, false otherwise
     */
    public boolean isAllowedInWorld(String worldName) {
        return whitelistedWorlds.isEmpty() || whitelistedWorlds.contains(worldName);
    }

    public static class CustomItemSoundData {
        private final boolean enabled;
        private final Sound sound;
        private final double volume;
        private final double pitch;

        private CustomItemSoundData(String itemId) {
            FileConfiguration config = LifeStealZ.getInstance().getConfigManager().getCustomItemConfig();
            enabled = config.getBoolean(itemId + ".sound.enabled");
            sound = Sound.valueOf(config.getString(itemId + ".sound.sound"));
            volume = config.getDouble(itemId + ".sound.volume");
            pitch = config.getDouble(itemId + ".sound.pitch");
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Sound getSound() {
            return sound;
        }

        public double getVolume() {
            return volume;
        }

        public double getPitch() {
            return pitch;
        }
    }
}