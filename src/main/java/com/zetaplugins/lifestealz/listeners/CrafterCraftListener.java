package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;
import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@AutoRegisterListener
public class CrafterCraftListener implements Listener {
    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (event.getBlock().getState(false) instanceof final Crafter crafter) {
            if (invHasCustomItem(crafter.getInventory())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean invHasCustomItem(Inventory inventory) {
        for (ItemStack item : inventory) {
            if (item == null) continue;
            if (item.getItemMeta() == null) continue;
            if (CustomItemManager.isCustomItem(item)) return true;
        }
        return false;
    }
}
