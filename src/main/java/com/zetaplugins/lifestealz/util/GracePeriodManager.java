package com.zetaplugins.lifestealz.util;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.storage.PlayerData;

import java.util.List;
import java.util.Optional;

public final class GracePeriodManager {
    // A tag attached to the player when their grace period is ended
    public static final NamespacedKey GRACE_ENDED = new NamespacedKey("lifestealz", "grace_ended");
    private static final int skippedBitMask = 0b10000000000;
    private static final int resetBitMask = 0b100000000000;

    private final LifeStealZ plugin;

    public GracePeriodManager(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    public GracePeriodConfig getConfig() {
        return new GracePeriodConfig(plugin);
    }

    public boolean isEnabled() {
        return getConfig().isEnabled();
    }

    /**
     * Checks if the player is in the grace period.
     * @param player The player to check.
     * @return True if the player is in the grace period, false otherwise.
     */
    public boolean isInGracePeriod(OfflinePlayer player) {
        if (!isEnabled()) return false;

        Optional<Integer> remaining = getGracePeriodRemaining(player);

        if (!remaining.isPresent()) return false;
        return remaining.get() != 0;
    }

    /**
     * Gets the remaining time of the grace period in seconds.
     * @param player The player to get the grace period remaining time for.
     * @return The remaining time of the grace period in seconds.
     */
    public Optional<Integer> getGracePeriodRemaining(OfflinePlayer player) {
        if (!isEnabled()) return Optional.empty();
        if (!wasReset(player)) {
            if (player.getPersistentDataContainer().has(GRACE_ENDED)) return Optional.empty();
            if (wasSkipped(player)) return Optional.empty();
        }

        final long gracePeriodDuration = (long) getConfig().getDuration() * 1000;

        long offset = 0;
        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
        if (playerData != null) {
            offset = playerData.getGraceOffset();
        }

        long elapsed;
        if (getConfig().shouldRunOffline()) {
            elapsed = (System.currentTimeMillis() - player.getFirstPlayed());
        } else {
            elapsed = player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50;
        }

        long remaining = gracePeriodDuration + offset - elapsed;

        return remaining < 0 ? Optional.empty() : Optional.of((int) (remaining / 1000));
    }

    public boolean hasEndedTag(OfflinePlayer player) {
        return player.getPersistentDataContainer().has(GRACE_ENDED);
    }

    public boolean wasSkipped(OfflinePlayer player) {
        return (player.getStatistic(Statistic.ENTITY_KILLED_BY, EntityType.ILLUSIONER) & skippedBitMask) != 0;
    }

    public boolean wasReset(OfflinePlayer player) {
        return (player.getStatistic(Statistic.ENTITY_KILLED_BY, EntityType.ILLUSIONER) & resetBitMask) != 0;
    }

    public void applySkipMask(OfflinePlayer player) {
        if (wasSkipped(player)) return;
        
        player.incrementStatistic(Statistic.ENTITY_KILLED_BY, EntityType.ILLUSIONER, skippedBitMask);
    }

    public void applyResetMask(OfflinePlayer player) {
        if (wasReset(player)) return;
        
        player.incrementStatistic(Statistic.ENTITY_KILLED_BY, EntityType.ILLUSIONER, resetBitMask);
    }

    public void removeSkipMask(OfflinePlayer player) {
        if (!wasSkipped(player)) return;
        
        player.decrementStatistic(Statistic.ENTITY_KILLED_BY, EntityType.ILLUSIONER, skippedBitMask);
    }

    public void removeResetMask(OfflinePlayer player) {
        if (!wasReset(player)) return;
        
        player.decrementStatistic(Statistic.ENTITY_KILLED_BY, EntityType.ILLUSIONER, resetBitMask);
    }

    /**
     * Sends the player a message and executes commands when the grace period starts.
     * @param player The player to start the grace period for.
     */
    public void startGracePeriod(Player player) {
        if (!isEnabled()) return;

        for (String command : getConfig().getStartCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    command.replace("&player&", player.getName()));
        }

        // Duration in ticks: 20 ticks = 1 second
        final long gracePeriodDuration = (long) getConfig().getDuration() * 20;

        endGraceLater(player, gracePeriodDuration);
    }

    public void endGraceLater(Player player, long ticks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                endGracePeriod(player);
            }
        }.runTaskLater(plugin, ticks);
    }

    /**
     * Sends the player a message and executes commands when the grace period ends.
     * @param player The player to end the grace period for.
     */
    public void endGracePeriod(Player player) {
        if (!isEnabled()) return;
        if (player.getPersistentDataContainer().has(GRACE_ENDED)) return;

        removeSkipMask(player);

        if (getConfig().shouldAnnounce()) {
            Component endMessage = MessageUtils.getAndFormatMsg(
                    true,
                    "gracePeriodEnd",
                    "&7The grace period has ended!"
            );
            player.sendMessage(endMessage);
        }

        player.getPersistentDataContainer().set(GRACE_ENDED, PersistentDataType.BOOLEAN, true);

        if (getConfig().shouldPlaySound()) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 500.0f, 1.0f);
        }

        for (String command : getConfig().getEndCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    command.replace("&player&", player.getName()));
        }
    }

    /**
     * Skips the grace period for the player.
     * @param player The player to skip the grace period for.
     * @return True if the grace period was skipped, false otherwise.
     */
    public boolean skipGracePeriod(OfflinePlayer player) {
        if (!isEnabled()) return false;
        if (!isInGracePeriod(player)) return false;
        
        // Removing the reset mask if it was set
        removeResetMask(player);

        if (player.isOnline()) {
            endGracePeriod(player.getPlayer());
        } else {
            applySkipMask(player);
        }

        for (String command : getConfig().getEndCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    command.replace("&player&", player.getName()));
        }

        return true;
    }

    /**
     * Resets the grace period for the player.
     * @param player The player to reset the grace period for.
     * @return True if the grace period was reset, false otherwise.
     */
    public boolean resetGracePeriod(OfflinePlayer player) {
        if (!isEnabled()) return false;

        removeSkipMask(player);

        if (player.isOnline()) {
            player.getPlayer().getPersistentDataContainer().remove(GRACE_ENDED);
        } else {
            applyResetMask(player);
        }


        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
        if (playerData == null) return false;

        playerData.setGraceOffset(System.currentTimeMillis());
        plugin.getStorage().save(playerData);

        for (String command : getConfig().getStartCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    command.replace("&player&", player.getName()));
        }

        return true;
    }

    public static class GracePeriodConfig {
        private final LifeStealZ plugin;

        public GracePeriodConfig(LifeStealZ plugin) {
            this.plugin = plugin;
        }

        public boolean isEnabled() {
            return plugin.getConfig().getBoolean("gracePeriod.enabled");
        }

        public boolean shouldRunOffline() {
            return plugin.getConfig().getBoolean("gracePerion.runOffline");
        }

        public int getDuration() {
            return plugin.getConfig().getInt("gracePeriod.duration");
        }

        public boolean shouldAnnounce() {
            return plugin.getConfig().getBoolean("gracePeriod.announce");
        }

        public boolean shouldPlaySound() {
            return plugin.getConfig().getBoolean("gracePeriod.playSound");
        }

        public boolean damageFromPlayers() {
            return plugin.getConfig().getBoolean("gracePeriod.damageFromPlayers");
        }

        public boolean damageToPlayers() {
            return plugin.getConfig().getBoolean("gracePeriod.damageToPlayers");
        }

        public boolean useHearts() {
            return plugin.getConfig().getBoolean("gracePeriod.useHearts");
        }

        public boolean looseHearts() {
            return plugin.getConfig().getBoolean("gracePeriod.looseHearts");
        }

        public boolean gainHearts() {
            return plugin.getConfig().getBoolean("gracePeriod.gainHearts");
        }

        public List<String> getStartCommands() {
            return plugin.getConfig().getStringList("gracePeriod.startCommands");
        }

        public List<String> getEndCommands() {
            return plugin.getConfig().getStringList("gracePeriod.endCommands");
        }
    }
}
