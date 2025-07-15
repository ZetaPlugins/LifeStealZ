package org.strassburger.lifestealz.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.strassburger.lifestealz.LifeStealZ;
import org.strassburger.lifestealz.storage.PlayerData;
import org.strassburger.lifestealz.util.*;
import org.strassburger.lifestealz.util.customitems.CustomItemManager;
import org.strassburger.lifestealz.util.worldguard.WorldGuardManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

import static org.strassburger.lifestealz.util.MaxHeartsManager.getMaxHearts;

public final class PlayerDeathListener implements Listener {

    private final LifeStealZ plugin;

    public PlayerDeathListener(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final Player killer = player.getKiller();

        // WorldGuard check
        if (plugin.hasWorldGuard() && !WorldGuardManager.checkHeartLossFlag(player)) return;

        UUID playerUUID = player.getUniqueId();
        if (player.hasMetadata("combat_log_npc")) {
            // If the player is a combat log NPC, get the original player's UUID
            playerUUID = (UUID) player.getMetadata("combat_log_npc").get(0).value();
        }
        final PlayerData playerData = plugin.getStorage().load(playerUUID);

        final boolean isDeathByPlayer = killer != null && !killer.getUniqueId().equals(playerUUID);

        if (handleAntiAltLogic(player, killer)) return;

        boolean looseHeartsToNature = plugin.getConfig().getBoolean("looseHeartsToNature") || plugin.getConfig().getInt("heartsPerKill") <= 0;
        boolean looseHeartsToPlayer = plugin.getConfig().getBoolean("looseHeartsToPlayer") || plugin.getConfig().getInt("heartsPerNaturalDeath") <= 0;

        // Natural death or death by player
        if ((!isDeathByPlayer && looseHeartsToNature) || (isDeathByPlayer && looseHeartsToPlayer)) {
            handleHeartLoss(event, player, killer, playerData, isDeathByPlayer);
        }
    }

    private void handleHeartLoss(PlayerDeathEvent event, Player player, Player killer, PlayerData playerData, boolean isDeathByPlayer) {
        final double minHearts = plugin.getConfig().getInt("minHearts") * 2;

        double healthPerKill = plugin.getConfig().getInt("heartsPerKill") * 2;
        double healthPerNaturalDeath = plugin.getConfig().getInt("heartsPerNaturalDeath") * 2;
        double healthToLoose = isDeathByPlayer ? healthPerKill : healthPerNaturalDeath;

        // Drop hearts or handle heart gain for the killer (if applicable)
        if (restrictedHeartLossByGracePeriod(player) && isDeathByPlayer) {
            killer.sendMessage(MessageUtils.getAndFormatMsg(false, "noHeartGainFromPlayersInGracePeriod", "&cYou can't gain hearts from players during their grace period!"));
        } else if (isDeathByPlayer && restrictedHeartGainByGracePeriod(killer)) {
            killer.sendMessage(MessageUtils.getAndFormatMsg(false, "noHeartGainInGracePeriod", "&cYou can't gain hearts during the grace period!"));
        } else if (isDeathByPlayer && plugin.getConfig().getBoolean("dropHeartsPlayer")) {
            dropHeartsNaturally(player.getLocation(), (int) (healthToLoose / 2), CustomItemManager.createKillHeart());
        } else if (isDeathByPlayer) {
            handleKillerHeartGain(playerData, killer, healthToLoose);
        } else if (plugin.getConfig().getBoolean("dropHeartsNatural")) {
            dropHeartsNaturally(player.getLocation(), (int) (healthToLoose / 2), CustomItemManager.createNaturalDeathHeart());
        }

        if (restrictedHeartLossByGracePeriod(player)) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "noHeartLossInGracePeriod",
                    "&cYou can't lose hearts during the grace period!"
            ));
            return;
        }

        if (isDeathByPlayer && restrictedHeartGainByGracePeriod(killer)) return;

        // Check for elimination
        if (playerData.getMaxHealth() - healthToLoose <= minHearts) {
            handleElimination(event, player, playerData, killer, isDeathByPlayer);
            return;
        }

        // Reduce the victim's hearts
        playerData.setMaxHealth(playerData.getMaxHealth() - healthToLoose);
        plugin.getStorage().save(playerData);
        LifeStealZ.setMaxHealth(player, playerData.getMaxHealth());
    }

    private void handleElimination(PlayerDeathEvent event, Player player, PlayerData playerData, Player killer, boolean isDeathByPlayer) {
        final List<String> elimCommands = plugin.getConfig().getStringList("eliminationCommands");
        final boolean disableBanOnElimination = plugin.getConfig().getBoolean("disablePlayerBanOnElimination");
        final boolean announceElimination = plugin.getConfig().getBoolean("announceElimination");

        SchedulerUtils.scheduleSyncDelayedTask(plugin, () -> {
            for (String command : elimCommands) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.replace("&player&", player.getName()));
            }
        }, 1L);

        if (disableBanOnElimination) {
            double respawnHP = plugin.getConfig().getInt("reviveHearts") * 2;
            playerData.setMaxHealth(respawnHP);
            plugin.getStorage().save(playerData);
            LifeStealZ.setMaxHealth(player, respawnHP);
            return;
        }

        SchedulerUtils.scheduleSyncDelayedTask(plugin, () -> {
            Component kickMessage = MessageUtils.getAndFormatMsg(
                    false,
                    "eliminatedJoin",
                    "&cYou don't have any hearts left!");
            if (player.isOnline()) { // Avoids trying to kick NPCs since they are not online
                player.kick(kickMessage);
            }
        }, 1L);

        if (announceElimination) {
            String messageKey = isDeathByPlayer ? "eliminationAnnouncement" : "eliminateionAnnouncementNature";
            Bukkit.broadcast(MessageUtils.getAndFormatMsg(
                    false,
                    messageKey,
                    isDeathByPlayer ? "&c%player% &7has been eliminated by &c%killer%&7!" : "&c%player% &7has been eliminated!",
                    new MessageUtils.Replaceable("%player%", player.getName()),
                    new MessageUtils.Replaceable("%killer%", killer != null ? killer.getName() : "")
            ));
            event.setDeathMessage(null);
        }

        plugin.getWebHookManager().sendWebhookMessage(WebHookManager.WebHookType.ELIMINATION, player.getName(), killer != null ? killer.getName() : "");

        playerData.setMaxHealth(0.0);
        plugin.getStorage().save(playerData);
        plugin.getEliminatedPlayersCache().addEliminatedPlayer(player.getName());
    }

    private void handleKillerHeartGain(PlayerData playerData, Player killer, double healthGain) {
        final boolean heartGainCooldownEnabled = plugin.getConfig().getBoolean("heartGainCooldown.enabled");
        final long heartGainCooldown = plugin.getConfig().getLong("heartGainCooldown.cooldown");
        final boolean heartGainCooldownDropOnCooldown = plugin.getConfig().getBoolean("heartGainCooldown.dropOnCooldown");
        final double maxHearts = getMaxHearts(killer, plugin.getConfig());
        final double minHearts = plugin.getConfig().getInt("minHearts") * 2;
        final boolean heartRewardOnElimination = plugin.getConfig().getBoolean("heartRewardOnElimination");
        final boolean dropHeartsIfMax = plugin.getConfig().getBoolean("dropHeartsIfMax");

        PlayerData killerPlayerData = plugin.getStorage().load(killer.getUniqueId());

        if (heartGainCooldownEnabled
                && CooldownManager.lastHeartGain.get(killer.getUniqueId()) != null
                && CooldownManager.lastHeartGain.get(killer.getUniqueId()) + heartGainCooldown > System.currentTimeMillis()) {
            long timeLeft = (CooldownManager.lastHeartGain.get(killer.getUniqueId()) + heartGainCooldown - System.currentTimeMillis()) / 1000;
            killer.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "heartGainCooldown",
                    "&cYou have to wait before gaining another heart!", new MessageUtils.Replaceable("%time%", MessageUtils.formatTime(timeLeft))
            ));
            if (heartGainCooldownDropOnCooldown) {
                dropHeartsNaturally(killer.getLocation(), (int) (healthGain / 2), CustomItemManager.createHeartGainCooldownHeart());
            }
        } else if (playerData.getMaxHealth() - healthGain > minHearts || (playerData.getMaxHealth() - healthGain <= minHearts && heartRewardOnElimination)) {
            if (killerPlayerData.getMaxHealth() + healthGain > maxHearts) {
                if (dropHeartsIfMax) {
                    dropHeartsNaturally(killer.getLocation(), (int) (healthGain / 2), CustomItemManager.createMaxHealthHeart());
                } else {
                    killer.sendMessage(MessageUtils.getAndFormatMsg(
                            false, "maxHeartLimitReached",
                            "&cYou already reached the limit of %limit% hearts!",
                            new MessageUtils.Replaceable("%limit%", (int) maxHearts / 2 + "")
                    ));
                }
            } else {
                killerPlayerData.setMaxHealth(killerPlayerData.getMaxHealth() + healthGain);
                plugin.getStorage().save(killerPlayerData);
                LifeStealZ.setMaxHealth(killer, killerPlayerData.getMaxHealth());
                killer.setHealth(Math.min(killer.getHealth() + healthGain, killerPlayerData.getMaxHealth()));
                CooldownManager.lastHeartGain.put(killer.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    private boolean handleAntiAltLogic(Player player, @Nullable Player killer) {
        if (killer == null || player.getUniqueId().equals(killer.getUniqueId())) return false;

        final String victimIP = getPlayerIP(player);
        final String killerIP = getPlayerIP(killer);

        if (victimIP != null && victimIP.equals(killerIP) && plugin.getConfig().getBoolean("antiAlt.enabled")) {
            if (plugin.getConfig().getBoolean("antiAlt.logAttempt")) {
                plugin.getLogger().info("[ALT WARNING] Player " + killer.getName() + " tried to kill " + player.getName() + " with the same IP address! (Probably an alt account)");
            }
            if (plugin.getConfig().getBoolean("antiAlt.sendMessage")) {
                killer.sendMessage(MessageUtils.getAndFormatMsg(false, "altKill", "&cPlease don't kill alts! This attempt has been logged!"));
            }
            for (String command : plugin.getConfig().getStringList("antiAlt.commands")) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.replace("&player&", killer.getName()));
            }
            return plugin.getConfig().getBoolean("antiAlt.preventKill");
        }
        return false;
    }

    private String getPlayerIP(Player player) {
        InetSocketAddress inetSocketAddress = player.getAddress();
        if (inetSocketAddress == null) return null;
        InetAddress address = inetSocketAddress.getAddress();
        return address.getHostAddress();
    }

    private void dropHeartsNaturally(Location location, int amount, ItemStack itemStack) {
        World world = location.getWorld();
        for (int i = 0; i < amount; i++) {
            world.dropItemNaturally(location, itemStack);
        }
    }

    private boolean restrictedHeartLossByGracePeriod(Player player) {
        GracePeriodManager gracePeriodManager = plugin.getGracePeriodManager();
        return gracePeriodManager.isInGracePeriod(player) && !gracePeriodManager.getConfig().looseHearts();
    }

    private boolean restrictedHeartGainByGracePeriod(Player player) {
        GracePeriodManager gracePeriodManager = plugin.getGracePeriodManager();
        return gracePeriodManager.isInGracePeriod(player) && !gracePeriodManager.getConfig().gainHearts();
    }
}

