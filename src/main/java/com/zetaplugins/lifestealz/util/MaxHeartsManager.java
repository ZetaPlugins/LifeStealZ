package com.zetaplugins.lifestealz.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 * Utility class to manage the maximum number of hearts a player can have.
 */
public final class MaxHeartsManager {
    private MaxHeartsManager() {}

    /**
     * Returns the maximum number of hearts a player can have.
     * @param player the player to check
     * @param config the LifeStealZ main configuration
     * @return the maximum number of hearts the player can have
     */
    public static double getMaxHearts(Player player, FileConfiguration config) {
        final int configMaxHearts = config.getInt("maxHearts");

        int highestHeartLimit = -1;

        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String perm = permInfo.getPermission();
            if (perm.startsWith("lifestealz.maxhearts.")) {
                try {
                    String numberPart = perm.substring("lifestealz.maxhearts.".length());
                    int hearts = Integer.parseInt(numberPart);
                    if (hearts > highestHeartLimit) highestHeartLimit = hearts;
                } catch (NumberFormatException ignored) {}
            }
        }

        return highestHeartLimit == -1 ? configMaxHearts : highestHeartLimit;
    }
}
