package com.zetaplugins.lifestealz.listeners;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;

public class PlayerDropItemListener implements Listener {
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        if (CustomItemManager.isForbiddenItem(item.getItemStack())) item.remove();
    }
}
