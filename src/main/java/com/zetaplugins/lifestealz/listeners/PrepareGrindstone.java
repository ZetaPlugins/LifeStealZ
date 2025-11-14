package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;

@AutoRegisterListener
public final class PrepareGrindstone implements Listener {
    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        GrindstoneInventory inventory = event.getInventory();
        ItemStack[] inputs = inventory.getContents();

        if (hasCustomItem(inputs)) {
            inventory.setResult(null);
            event.setResult(null);
        }
    }

    private boolean hasCustomItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.getItemMeta() == null) continue;
            if (CustomItemManager.isCustomItem(item)) return true;
        }
        return false;
    }
}