package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.lifestealz.events.death.*;
import com.zetaplugins.lifestealz.util.BypassManager;
import com.zetaplugins.lifestealz.util.CooldownManager;
import com.zetaplugins.lifestealz.util.GracePeriodManager;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.WebHookManager;
import com.zetaplugins.lifestealz.events.*;
import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;
import com.zetaplugins.lifestealz.storage.PlayerData;
import com.zetaplugins.lifestealz.util.worldguard.WorldGuardManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

import static com.zetaplugins.lifestealz.util.MaxHeartsManager.getMaxHearts;
import static com.zetaplugins.lifestealz.util.customitems.CustomItemManager.DESPAWNABLE_KEY;
import static com.zetaplugins.lifestealz.util.customitems.CustomItemManager.INVULNERABLE_KEY;

@AutoRegisterListener
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

        // Handle anti-alt logic first
        if (handleAntiAltLogic(event, player, killer)) return;

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

        // Check bypass first (takes priority over grace period)
        boolean victimHasBypass = restrictedHeartLossByBypass(player);
        boolean killerHasBypass = isDeathByPlayer && killer != null && restrictedHeartGainByBypass(killer);

        if (victimHasBypass || killerHasBypass) {
            ZPlayerBypassDeathEvent bypassEvent = 
                    new ZPlayerBypassDeathEvent(event, killer, victimHasBypass, killerHasBypass);
            Bukkit.getPluginManager().callEvent(bypassEvent);

            if (!bypassEvent.isCancelled()) {
                if (victimHasBypass) player.sendMessage(bypassEvent.getMessageToVictim());
                if (killerHasBypass && killer != null) killer.sendMessage(bypassEvent.getMessageToKiller());
                return; // Fully blocks heart loss/gain
            }
        }

        // Check grace period (only if bypass doesn't apply)
        boolean victimInGracePeriod = restrictedHeartLossByGracePeriod(player);
        boolean killerInGracePeriod = isDeathByPlayer && restrictedHeartGainByGracePeriod(killer);

        if (victimInGracePeriod || killerInGracePeriod) {
            ZPlayerGracePeriodDeathEvent graceEvent =
                    new ZPlayerGracePeriodDeathEvent(event, killer, victimInGracePeriod, killerInGracePeriod);
            Bukkit.getPluginManager().callEvent(graceEvent);

            if (!graceEvent.isCancelled()) {
                if (victimInGracePeriod) player.sendMessage(graceEvent.getMessageToVictim());
                if (killerInGracePeriod && killer != null) killer.sendMessage(graceEvent.getMessageToKiller());
                if (victimInGracePeriod) return; // Only victim in grace fully blocks heart loss
            }
        }


        boolean preventKillerGain = false;
        boolean droppedAtKiller = false;

        if (isDeathByPlayer && !killerInGracePeriod && !killerHasBypass) {
            
            if (handleHeartGainCooldown(event, player, killer, healthToLoose)) {
                preventKillerGain = true;
                if (plugin.getConfig().getBoolean("heartGainCooldown.dropOnCooldown")) {
                    droppedAtKiller = true;
                }
            }
            if (handleMaxHeartsLimit(event, player, killer, healthToLoose)) {
                preventKillerGain = true;
                if (plugin.getConfig().getBoolean("dropHeartsIfMax")) {
                    droppedAtKiller = true;
                }
            }
        }

        if (playerData.getMaxHealth() - healthToLoose <= minHearts) {
            handleElimination(event, player, playerData, killer, isDeathByPlayer, healthToLoose, preventKillerGain, droppedAtKiller);
            return;
        }

        if (isDeathByPlayer) {
            handlePvPDeath(event, player, killer, playerData, healthToLoose, preventKillerGain, droppedAtKiller);
        } else {
            handleNaturalDeath(event, player, playerData, healthToLoose);
        }
    }

    private boolean handleHeartGainCooldown(PlayerDeathEvent event, Player player, Player killer, double healthGain) {
        final boolean heartGainCooldownEnabled = plugin.getConfig().getBoolean("heartGainCooldown.enabled");
        final long heartGainCooldown = plugin.getConfig().getLong("heartGainCooldown.cooldown");

        if (heartGainCooldownEnabled
                && CooldownManager.lastHeartGain.get(killer.getUniqueId()) != null
                && CooldownManager.lastHeartGain.get(killer.getUniqueId()) + heartGainCooldown > System.currentTimeMillis()) {

            long timeLeft = (CooldownManager.lastHeartGain.get(killer.getUniqueId()) + heartGainCooldown - System.currentTimeMillis()) / 1000;

            ZPlayerHeartGainCooldownEvent cooldownEvent =
                    new ZPlayerHeartGainCooldownEvent(event, killer, timeLeft);
            cooldownEvent.setShouldDropHeartsInstead(plugin.getConfig().getBoolean("heartGainCooldown.dropOnCooldown"));
            Bukkit.getPluginManager().callEvent(cooldownEvent);

            if (!cooldownEvent.isCancelled()) {
                killer.sendMessage(cooldownEvent.getCooldownMessage());

                if (cooldownEvent.isShouldDropHeartsInstead()) {
                    dropHeartsNaturally(killer.getLocation(), (int) (healthGain / 2), CustomItemManager.createHeartGainCooldownHeart());
                }
                return true; // Prevent normal heart gain
            }
        }
        return false;
    }

    private boolean handleMaxHeartsLimit(PlayerDeathEvent event, Player player, Player killer, double healthGain) {
        final double maxHearts = getMaxHearts(killer, plugin.getConfig());
        PlayerData killerPlayerData = plugin.getStorage().load(killer.getUniqueId());

        if (killerPlayerData.getMaxHealth() + healthGain > maxHearts) {
            ZPlayerMaxHeartsReachedEvent maxHeartsEvent =
                    new ZPlayerMaxHeartsReachedEvent(event, killer, maxHearts);
            maxHeartsEvent.setShouldDropHeartsInstead(plugin.getConfig().getBoolean("dropHeartsIfMax"));
            Bukkit.getPluginManager().callEvent(maxHeartsEvent);

            if (!maxHeartsEvent.isCancelled()) {
                if (maxHeartsEvent.isShouldDropHeartsInstead()) {
                    dropHeartsNaturally(killer.getLocation(), (int) (healthGain / 2), CustomItemManager.createMaxHealthHeart());
                } else {
                    killer.sendMessage(maxHeartsEvent.getMaxHeartsMessage());
                }
                return true; // Prevent normal heart gain
            }
        }
        return false;
    }

    private void handlePvPDeath(PlayerDeathEvent event, Player player, Player killer, PlayerData playerData, double healthToLoose, boolean preventKillerGain, boolean droppedAtKiller) {
        double healthGain = healthToLoose;

        ZPlayerPvPDeathEvent pvpEvent = new ZPlayerPvPDeathEvent(event, killer, healthToLoose, healthGain);

        boolean shouldDropFromPvP = plugin.getConfig().getBoolean("dropHeartsPlayer") && !droppedAtKiller;
        pvpEvent.setShouldDropHearts(shouldDropFromPvP);

        pvpEvent.setKillerShouldGainHearts(!preventKillerGain);

        Bukkit.getPluginManager().callEvent(pvpEvent);

        if (!pvpEvent.isCancelled()) {
            // Victim loses hearts
            if (pvpEvent.getHeartsToLose() > 0) {
                playerData.setMaxHealth(playerData.getMaxHealth() - pvpEvent.getHeartsToLose());
                plugin.getStorage().save(playerData);
                LifeStealZ.setMaxHealth(player, playerData.getMaxHealth());
            }

            // Killer gain or drops
            if (pvpEvent.isShouldDropHearts()) {
                dropHeartsNaturally(player.getLocation(), (int) (pvpEvent.getHeartsToLose() / 2), CustomItemManager.createKillHeart());
            } else if (pvpEvent.isKillerShouldGainHearts() && pvpEvent.getHeartsKillerGains() > 0) {
                handleKillerHeartGainDirect(killer, pvpEvent.getHeartsKillerGains());
            }

            if (!pvpEvent.getDeathMessage().equals(event.getDeathMessage())) {
                event.setDeathMessage(pvpEvent.getDeathMessage());
            }
        }
    }

    private void handleNaturalDeath(PlayerDeathEvent event, Player player, PlayerData playerData, double healthToLoose) {
        ZPlayerNaturalDeathEvent naturalEvent =
                new ZPlayerNaturalDeathEvent(event, healthToLoose);
        naturalEvent.setShouldDropHearts(plugin.getConfig().getBoolean("dropHeartsNatural"));
        Bukkit.getPluginManager().callEvent(naturalEvent);

        if (!naturalEvent.isCancelled()) {
            // Apply heart loss
            if (naturalEvent.getHeartsToLose() > 0) {
                playerData.setMaxHealth(playerData.getMaxHealth() - naturalEvent.getHeartsToLose());
                plugin.getStorage().save(playerData);
                LifeStealZ.setMaxHealth(player, playerData.getMaxHealth());
            }

            // Handle heart drops
            if (naturalEvent.isShouldDropHearts()) {
                dropHeartsNaturally(player.getLocation(), (int) (naturalEvent.getHeartsToLose() / 2), CustomItemManager.createNaturalDeathHeart());
            }

            // Update death message if changed
            if (!naturalEvent.getDeathMessage().equals(event.getDeathMessage())) {
                event.setDeathMessage(naturalEvent.getDeathMessage());
            }
        }
    }

    private void handleElimination(PlayerDeathEvent event, Player player, PlayerData playerData, Player killer, boolean isDeathByPlayer, double healthToLoose, boolean preventKillerGain, boolean droppedAtKiller) {
        ZPlayerEliminationEvent eliminationEvent =
                new ZPlayerEliminationEvent(event, killer);
        eliminationEvent.setShouldBanPlayer(!plugin.getConfig().getBoolean("disablePlayerBanOnElimination"));
        eliminationEvent.setShouldAnnounceElimination(plugin.getConfig().getBoolean("announceElimination"));

        String messageKey = isDeathByPlayer ? "eliminationAnnouncement" : "eliminateionAnnouncementNature";
        String defaultMessage = isDeathByPlayer ? "&c%player% &7has been eliminated by &c%killer%&7!" : "&c%player% &7has been eliminated!";
        eliminationEvent.setEliminationMessage(MessageUtils.getAndFormatMsg(
                false,
                messageKey,
                defaultMessage,
                new MessageUtils.Replaceable("%player%", player.getName()),
                new MessageUtils.Replaceable("%killer%", killer != null ? killer.getName() : "")
        ));

        eliminationEvent.setKickMessage(MessageUtils.getAndFormatMsg(
                false,
                "eliminatedJoin",
                "&cYou don't have any hearts left!"
        ));

        Bukkit.getPluginManager().callEvent(eliminationEvent);

        if (!eliminationEvent.isCancelled()) {
            // Execute elimination commands
            final List<String> elimCommands = plugin.getConfig().getStringList("eliminationCommands");
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                for (String command : elimCommands) {
                    plugin.getServer().dispatchCommand(
                            plugin.getServer().getConsoleSender(),
                            command.replace("&player&", player.getName())
                    );
                }
            }, 1L);

            boolean heartRewardOnElimination = plugin.getConfig().getBoolean("heartRewardOnElimination", true);
            if (heartRewardOnElimination) {
                // Reward killer with hearts on elimination
                if (isDeathByPlayer && killer != null) {
                    boolean dropHeartsPlayer = plugin.getConfig().getBoolean("dropHeartsPlayer", true);
                    if (!preventKillerGain && !droppedAtKiller && !dropHeartsPlayer) {
                        boolean preventedByCooldown = handleHeartGainCooldown(event, player, killer, healthToLoose);
                        boolean preventedByMax = false;
                        if (!preventedByCooldown) {
                            preventedByMax = handleMaxHeartsLimit(event, player, killer, healthToLoose);
                        }
                        if (!preventedByCooldown && !preventedByMax) {
                            handleKillerHeartGainDirect(killer, healthToLoose);
                        }
                    } else {
                        if (droppedAtKiller || dropHeartsPlayer) {
                            dropHeartsNaturally(killer.getLocation(), (int) (healthToLoose / 2), CustomItemManager.createKillHeart());
                        }
                    }
                } else if (!isDeathByPlayer) {
                    // Natural death eliminations also drop hearts if enabled
                    boolean dropHeartsNatural = plugin.getConfig().getBoolean("dropHeartsNatural", true);
                    if (dropHeartsNatural) {
                        dropHeartsNaturally(player.getLocation(), (int) (healthToLoose / 2), CustomItemManager.createNaturalDeathHeart());
                    }
                }
            }

            if (!eliminationEvent.isShouldBanPlayer()) {
                // Respawn with revive hearts instead of elimination
                double respawnHP = plugin.getConfig().getInt("reviveHearts") * 2;
                playerData.setMaxHealth(respawnHP);
                plugin.getStorage().save(playerData);
                LifeStealZ.setMaxHealth(player, respawnHP);
                return;
            }

            // Kick the player
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline()) { // Avoids trying to kick NPCs since they are not online
                    player.kick(eliminationEvent.getKickMessage());
                }
            }, 1L);

            // Announce elimination
            if (eliminationEvent.isShouldAnnounceElimination()) {
                Bukkit.broadcast(eliminationEvent.getEliminationMessage());
                event.setDeathMessage(null);
            }

            // Send webhook
            plugin.getWebHookManager().sendWebhookMessage(WebHookManager.WebHookType.ELIMINATION, player.getName(), killer != null ? killer.getName() : "");

            // Set player data to eliminated
            playerData.setMaxHealth(0.0);
            plugin.getStorage().save(playerData);
            plugin.getEliminatedPlayersCache().addEliminatedPlayer(player.getName());
        }
    }

    private void handleKillerHeartGainDirect(Player killer, double healthGain) {
        PlayerData killerPlayerData = plugin.getStorage().load(killer.getUniqueId());
        killerPlayerData.setMaxHealth(killerPlayerData.getMaxHealth() + healthGain);
        plugin.getStorage().save(killerPlayerData);
        LifeStealZ.setMaxHealth(killer, killerPlayerData.getMaxHealth());
        killer.setHealth(Math.min(killer.getHealth() + healthGain, killerPlayerData.getMaxHealth()));
        CooldownManager.lastHeartGain.put(killer.getUniqueId(), System.currentTimeMillis());
    }

    private boolean handleAntiAltLogic(PlayerDeathEvent event, Player player, @Nullable Player killer) {
        if (killer == null || player.getUniqueId().equals(killer.getUniqueId())) return false;

        final String victimIP = getPlayerIP(player);
        final String killerIP = getPlayerIP(killer);

        if (victimIP != null && victimIP.equals(killerIP) && plugin.getConfig().getBoolean("antiAlt.enabled")) {
            ZPlayerAltKillEvent altEvent =
                    new ZPlayerAltKillEvent(event, killer, victimIP);
            altEvent.setShouldPreventKill(plugin.getConfig().getBoolean("antiAlt.preventKill"));
            altEvent.setShouldLogAttempt(plugin.getConfig().getBoolean("antiAlt.logAttempt"));
            altEvent.setShouldSendMessage(plugin.getConfig().getBoolean("antiAlt.sendMessage"));
            Bukkit.getPluginManager().callEvent(altEvent);

            if (!altEvent.isCancelled()) {
                if (altEvent.isShouldLogAttempt()) {
                    plugin.getLogger().info("[ALT WARNING] Player " + killer.getName() + " tried to kill "
                            + player.getName() + " with the same IP address! (Probably an alt account)");
                }
                if (altEvent.isShouldSendMessage()) {
                    killer.sendMessage(altEvent.getWarningMessage());
                }
                for (String command : plugin.getConfig().getStringList("antiAlt.commands")) {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            command.replace("&player&", killer.getName()));
                }
                return altEvent.isShouldPreventKill();
            }
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
        PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
        final boolean shouldHaveUnlimitedLifetime = container.has(DESPAWNABLE_KEY)
                && !Boolean.TRUE.equals(container.get(DESPAWNABLE_KEY, PersistentDataType.BOOLEAN));
        final boolean shouldBeInvulnerable = container.has(INVULNERABLE_KEY)
                && Boolean.TRUE.equals(container.get(INVULNERABLE_KEY, PersistentDataType.BOOLEAN));

        for (int i = 0; i < amount; i++) {
            Item item =  location.getWorld().dropItemNaturally(location, itemStack);
            if (shouldHaveUnlimitedLifetime) item.setUnlimitedLifetime(true);
            if (shouldBeInvulnerable) item.setInvulnerable(true);
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

    private boolean restrictedHeartLossByBypass(Player player) {
        BypassManager bypassManager = plugin.getBypassManager();
        return bypassManager.hasBypass(player) && !bypassManager.getConfig().looseHearts();
    }

    private boolean restrictedHeartGainByBypass(Player player) {
        BypassManager bypassManager = plugin.getBypassManager();
        return bypassManager.hasBypass(player) && !bypassManager.getConfig().gainHearts();
    }
}
