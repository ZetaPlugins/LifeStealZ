package com.zetaplugins.lifestealz.listeners.revivebeacon;

import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.revive.ReviveTask;
import com.zetaplugins.lifestealz.util.customblocks.CustomBlock;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;
import com.zetaplugins.lifestealz.util.customitems.customitemdata.CustomReviveBeaconItemData;

@AutoRegisterListener
public final class ReviveBeaconBreakListener implements Listener {
    private final LifeStealZ plugin;

    public ReviveBeaconBreakListener(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onReviveBeaconBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!CustomBlock.REVIVE_BEACON.is(block)) return;

        Player player = event.getPlayer();
        Location location = block.getLocation();

        ReviveTask reviveTask = plugin.getReviveTaskManager().getReviveTask(location);
        if (reviveTask != null) {
            CustomReviveBeaconItemData itemData = new CustomReviveBeaconItemData(CustomBlock.REVIVE_BEACON.getCustomItemId(block));
            if (!itemData.isAllowBreakingBeaconWhileReviving()) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.getAndFormatMsg(
                        false,
                        "noReviveBeaconBreak",
                        "&cYou cannot break a revive beacon while it is in use!"
                ));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (!reviveTask.task().isCancelled()) reviveTask.task().cancel();
            plugin.getReviveTaskManager().removeReviveTask(location);
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
            Player reviver = Bukkit.getPlayer(reviveTask.reviver());
            if (reviver != null && reviver.isOnline()) {
                reviver.sendMessage(MessageUtils.getAndFormatMsg(
                        true,
                        "reviveBeaconBreak",
                        "&7Your revive beacon has been broken by &c%breaker%&7, the revive process has been cancelled.",
                        new MessageUtils.Replaceable("%breaker%", player.getName())
                ));
            }
        }

        plugin.getReviveBeaconEffectManager().clearAllEffects(location);

        event.setDropItems(false);
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;
        String customID = CustomBlock.REVIVE_BEACON.getCustomItemId(block);
        if (customID == null) return;
        ItemStack item = CustomItemManager.createCustomItem(customID);
        block.getWorld().dropItemNaturally(location.add(0.5, 0.5, 0.5), item);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(CustomBlock.REVIVE_BEACON::is);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(CustomBlock.REVIVE_BEACON::is);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (CustomBlock.REVIVE_BEACON.is(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (CustomBlock.REVIVE_BEACON.is(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (CustomBlock.REVIVE_BEACON.is(event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
