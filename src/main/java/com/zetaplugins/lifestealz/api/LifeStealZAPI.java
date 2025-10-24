package com.zetaplugins.lifestealz.api;

import org.bukkit.inventory.ItemStack;
import com.zetaplugins.lifestealz.util.customitems.customitemdata.CustomItemData;
import com.zetaplugins.lifestealz.storage.PlayerData;

import java.util.Set;
import java.util.UUID;

public interface LifeStealZAPI {
    /**
     * Get the current version of the LifeStealZ plugin.
     *
     * @return The current version of the LifeStealZ plugin.
     */
    String getVersion();

    /**
     * Get the player data for a player.
     * @param uuid The UUID of the player to get the data for.
     * @return The player data for the player.
     */
    PlayerData getPlayerData(UUID uuid);

    /**
     * Save the player data for a player.
     * @param playerData The player data to save.
     */
    void savePlayerData(PlayerData playerData);

    /**
     * Get if a player is eliminated.
     * @param uuid The UUID of the player to check.
     * @return True if the player is eliminated, false otherwise.
     */
    boolean isEliminated(UUID uuid);

    /**
     * Eliminate a player.
     * @param uuid The UUID of the player to eliminate.
     * @return True if the player was successfully eliminated, false otherwise.
     */
    boolean eliminate(UUID uuid);

    /**
     * Revive a player.
     * @param uuid The UUID of the player to revive.
     * @return True if the player was successfully revived, false otherwise.
     */
    boolean revive(UUID uuid);

    /**
     * Get a custom LifeStealZ item by its ID.
     * @return The custom LifeStealZ item with the given ID.
     */
    ItemStack getCustomItem(String customItemID);

    /**
     * Get the custom data for a custom LifeStealZ item.
     * @return The custom data for the custom LifeStealZ item.
     */
    CustomItemData getCustomItemData(String customItemID);

    /**
     * Get a list of all custom LifeStealZ item IDs.
     * @return A list of all custom LifeStealZ item IDs.
     */
    Set<String> getCustomItemIDs();

    /**
     * Get the default heart item.
     * @return The default heart item.
     */
    ItemStack getDefaultHeart();

    /**
     * Get the custom item ID of an item.
     * @param item The item to get the ID from.
     * @return The custom item ID of the item.
     */
    String getCustomItemID(ItemStack item);

    /**
     * Returns whether the bypass permission is active for the player.
     * When active, deaths will not cause heart loss, heart drops, or killer rewards.
     */
    boolean isBypassActive(org.bukkit.entity.Player player);
}
