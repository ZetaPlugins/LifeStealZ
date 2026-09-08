package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.GracePeriodManager;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.geysermc.GeyserManager;
import com.zetaplugins.lifestealz.util.geysermc.GeyserPlayerFile;
import com.zetaplugins.lifestealz.storage.PlayerData;
import com.zetaplugins.lifestealz.storage.Storage;

@AutoRegisterListener
public final class PlayerJoinListener implements Listener {

    private final LifeStealZ plugin;

    private final GeyserManager geyserManager;
    private final GeyserPlayerFile geyserPlayerFile;
    private final GracePeriodManager gracePeriodManager;

    public PlayerJoinListener(LifeStealZ plugin) {
        this.plugin = plugin;
        this.geyserManager = plugin.getGeyserManager();
        this.geyserPlayerFile = plugin.getGeyserPlayerFile();
        this.gracePeriodManager = plugin.getGracePeriodManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (gracePeriodManager.isEnabled()) {
            // Removing all tags if the player was reset
            if (gracePeriodManager.wasReset(player)) {
                gracePeriodManager.removeResetMask(player);
                player.getPersistentDataContainer().remove(GracePeriodManager.GRACE_ENDED);
            }
            
            if (!gracePeriodManager.hasEndedTag(player)) {
                // Returns 0 only when the timer has run out or if the grace period has been skipped
                int remaining = gracePeriodManager.getGracePeriodRemaining(player).orElse(0);

                // 0 * 20 = 0 so it's still immediate notification
                gracePeriodManager.endGraceLater(player, remaining * 20);
            }
        }

        Storage storage = plugin.getStorage();

        if(plugin.hasGeyser()) {
            if(geyserManager.isBedrockPlayer(player)) {
                geyserPlayerFile.savePlayer(player.getUniqueId(), player.getName());
            }
        }

        PlayerData playerData = loadOrCreatePlayerData(player, storage, plugin.getConfig().getInt("startHearts", 10));
        LifeStealZ.setMaxHealth(player, playerData.getMaxHealth());

        notifyOpAboutUpdate(player);
    }

    private PlayerData loadOrCreatePlayerData(Player player, Storage storage, int startHearts) {
        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
        if (playerData == null) {
            playerData = new PlayerData(player.getName(), player.getUniqueId());
            playerData.setMaxHealth(startHearts * 2.0);
            storage.save(playerData);
            plugin.getGracePeriodManager().startGracePeriod(player);
            plugin.getOfflinePlayerCache().addItem(player.getName());
        }
        return playerData;
    }

    private void notifyOpAboutUpdate(Player player) {
        if (player.isOp() && plugin.getConfig().getBoolean("checkForUpdates") && plugin.getVersionChecker().isNewVersionAvailable()) {
            player.sendMessage(MessageUtils.getAndFormatMsg(true, "newVersionAvailable", "&7A new version of LifeStealZ is available!\\n&c<click:OPEN_URL:https://modrinth.com/plugin/lifestealz/versions>https://modrinth.com/plugin/lifestealz/versions</click>"));
        }
    }
}