package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;

@AutoRegisterListener
public final class PrepareItemCraft implements Listener {

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        ItemStack[] matrix = inventory.getMatrix();

        if (result == null) return;
        if (result.getItemMeta() == null) return;

        if (CustomItemManager.isCustomItem(result)) return;
        if (!matrixHasCustomItem(matrix)) return;

        inventory.setResult(null);
    }

    private boolean matrixHasCustomItem(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item == null) continue;
            if (item.getItemMeta() == null) continue;
            if (CustomItemManager.isCustomItem(item)) return true;
        }
        return false;
    }
}