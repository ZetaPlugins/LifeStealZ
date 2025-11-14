package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static com.zetaplugins.lifestealz.util.customitems.CustomItemManager.DESPAWNABLE_KEY;
import static com.zetaplugins.lifestealz.util.customitems.CustomItemManager.INVULNERABLE_KEY;

@AutoRegisterListener( name = "HahahaItemSpawnListenerLol")
public class ItemSpawnListener implements Listener {
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();

        PersistentDataContainer container = item.getItemStack().getItemMeta().getPersistentDataContainer();
        final boolean shouldHaveUnlimitedLifetime = container.has(DESPAWNABLE_KEY)
                && !Boolean.TRUE.equals(container.get(DESPAWNABLE_KEY, PersistentDataType.BOOLEAN));
        final boolean shouldBeInvulnerable = container.has(INVULNERABLE_KEY)
                && Boolean.TRUE.equals(container.get(INVULNERABLE_KEY, PersistentDataType.BOOLEAN));
        if (shouldHaveUnlimitedLifetime) item.setUnlimitedLifetime(true);
        if (shouldBeInvulnerable) item.setInvulnerable(true);
    }
}
