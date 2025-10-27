package com.zetaplugins.lifestealz.util.customblocks;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class BlockTagManager {
    private BlockTagManager() {}

    /**
     * Tags a block with a persistent data value.
     * [!!!] This method only works for blocks that have a TileState (like chests, furnaces, etc.).
     * @param block the block to tag
     * @param key the NamespacedKey to use for the tag
     * @param type the PersistentDataType to use for the tag
     * @param value the value to set for the tag
     * @param <P> the type of the persistent data value
     * @param <C> the type of the value to set
     */
    public static <P, C> void tagBlock(Block block, NamespacedKey key, PersistentDataType<P, C> type, C value) {
        BlockState state = block.getState();

        if (state instanceof TileState tileState) {
            PersistentDataContainer container = tileState.getPersistentDataContainer();

            container.set(key, type, value);
            tileState.update();
        }
    }

    /**
     * Gets a persistent data value from a block.
     * [!!!] This method only works for blocks that have a TileState (like chests, furnaces, etc.).
     * @param block the block to get the tag from
     * @param key the NamespacedKey to use for the tag
     * @param type the PersistentDataType to use for the tag
     * @param <P> the type of the persistent data value
     * @param <C> the type of the value to get
     * @return the value of the tag, or null if not found
     */
    public static <P, C> C getBlockTag(Block block, NamespacedKey key, PersistentDataType<P, C> type) {
        BlockState state = block.getState();

        if (state instanceof TileState tileState) {
            PersistentDataContainer container = tileState.getPersistentDataContainer();

            return container.get(key, type);
        }
        return null;
    }
}
