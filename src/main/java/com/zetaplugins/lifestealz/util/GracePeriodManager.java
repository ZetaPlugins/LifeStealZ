package com.zetaplugins.lifestealz.util;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.storage.PlayerData;

import java.util.List;
import java.util.Optional;

public final class GracePeriodManager {
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

        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());

        if (playerData == null) return false;

        long firstJoin = playerData.getFirstJoin();
        final long now = System.currentTimeMillis();
        final long gracePeriodDuration = (long) getConfig().getDuration() * 1000;

        return now - firstJoin < gracePeriodDuration;
    }

    /**
     * Gets the remaining time of the grace period in seconds.
     * @param player The player to get the grace period remaining time for.
     * @return The remaining time of the grace period in seconds.
     */
    public Optional<Integer> getGracePeriodRemaining(Player player) {
        if (!isEnabled()) return Optional.empty();

        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());

        if (playerData == null) return Optional.empty();

        long firstJoin = playerData.getFirstJoin();
        final long now = System.currentTimeMillis();
        final long gracePeriodDuration = (long) getConfig().getDuration() * 1000;

        long remaining = gracePeriodDuration - (now - firstJoin);

        return remaining < 0 ? Optional.empty() : Optional.of((int) (remaining));
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

        new BukkitRunnable() {
            @Override
            public void run() {
                endGracePeriod(player);
            }
        }.runTaskLater(plugin, gracePeriodDuration);
    }

    /**
     * Sends the player a message and executes commands when the grace period ends.
     * @param player The player to end the grace period for.
     */
    public void endGracePeriod(Player player) {
        if (!isEnabled()) return;

        if (getConfig().shouldAnnounce()) {
            Component endMessage = MessageUtils.getAndFormatMsg(
                    true,
                    "gracePeriodEnd",
                    "&7The grace period has ended!"
            );
            player.sendMessage(endMessage);
        }

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

        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
        if (playerData == null) return false;

        playerData.setFirstJoin(System.currentTimeMillis() - getConfig().getDuration() * 1000L);// Subtract the duration of the grace period
        plugin.getStorage().save(playerData);

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

        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
        if (playerData == null) return false;

        playerData.setFirstJoin(System.currentTimeMillis());
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

        public boolean loseHearts() {
            return plugin.getConfig().getBoolean("gracePeriod.loseHearts");
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
