package com.zetaplugins.lifestealz.listeners.revivebeacon;

import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.GuiManager;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.revive.ReviveTask;
import com.zetaplugins.lifestealz.util.customblocks.CustomBlock;

@AutoRegisterListener
public final class ReviveBeaconInteractListener implements Listener {
    private final LifeStealZ plugin;

    public ReviveBeaconInteractListener(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onReviveBeaconInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        if (!action.equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!CustomBlock.REVIVE_BEACON.is(block)) return;
        Player player = event.getPlayer();
        event.setCancelled(true);

        ReviveTask reviveTask = plugin.getReviveTaskManager().getReviveTask(block.getLocation());
        if (reviveTask != null) {
            long nowSeconds = System.currentTimeMillis() / 1000L;
            int secondsLeft = (int) (reviveTask.start() + reviveTask.durationSeconds() - nowSeconds);
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    true,
                    "beaconInUseInteract",
                    "&7There is already a revive process in progress. Time left: &c%timeLeft% seconds&7.",
                    new MessageUtils.Replaceable("%timeLeft%", String.valueOf(secondsLeft))
            ));
            return;
        }

        GuiManager.openReviveBeaconGui(player, 1, plugin, block.getLocation());

        //plugin.getReviveBeaconEffectManager().startRevivingEffects(block.getLocation());
    }
}
