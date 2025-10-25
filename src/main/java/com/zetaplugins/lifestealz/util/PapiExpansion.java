package com.zetaplugins.lifestealz.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.storage.PlayerData;

public final class PapiExpansion extends PlaceholderExpansion {
    private final LifeStealZ plugin;

    public PapiExpansion(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Kartoffelchipss";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lifestealz";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null || player.getPlayer() == null) return "PlayerNotFound";

        switch (identifier) {
            case "name": {
                return player.getName();
            }
            case "hearts": {
                Player onlinePlayer = player.getPlayer();

                // Try to get max health attribute from player as LSZ always updates it and it's faster
                if (onlinePlayer != null) {
                    AttributeInstance attribute = onlinePlayer.getAttribute(Attribute.MAX_HEALTH);
                    if (attribute != null) {
                        return String.valueOf((int) (attribute.getBaseValue() / 2));
                    }
                }

                // Fallback to stored data
                PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
                return String.valueOf((int) (playerData.getMaxHealth() / 2));
            }
            case "revived": {
                PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
                return String.valueOf(playerData.getHasBeenRevived());
            }
            case "health": {
                return String.valueOf((int) (player.getPlayer().getHealth() / 2));
            }
            case "maxhearts": {
                Player onlinePlayer = player.getPlayer();

                // Default to max hearts from config if player is not online
                if (onlinePlayer == null) return String.valueOf(plugin.getConfig().getInt("maxHearts"));

                return String.valueOf((int) (MaxHeartsManager.getMaxHearts(onlinePlayer, plugin.getConfig()) / 2));
            }
            case "maxrevives": {
                return String.valueOf(plugin.getConfig().getInt("maxRevives"));
            }
            case "craftedhearts": {
                PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
                return String.valueOf(playerData.getCraftedHearts());
            }
            case "craftedrevives": {
                PlayerData playerData = plugin.getStorage().load(player.getUniqueId());
                return String.valueOf(playerData.getCraftedRevives());
            }
            case "isInGracePeriod": {
                GracePeriodManager gracePeriodManager = plugin.getGracePeriodManager();
                if (!gracePeriodManager.isEnabled()) return "false";
                return String.valueOf(gracePeriodManager.isInGracePeriod(player.getPlayer()));
            }
            case "gracePeriodRemaining": {
                GracePeriodManager gracePeriodManager = plugin.getGracePeriodManager();
                if (!gracePeriodManager.isEnabled()) return "-1";
                return TimeFormatter.formatDuration(
                        gracePeriodManager.getGracePeriodRemaining(player.getPlayer()).orElse(0)
                );
            }
            case "hasBypass": {
                return String.valueOf(plugin.getBypassManager().hasBypass(player.getPlayer()));
            }
            case "heartCooldown": {
                long heartCooldownTime = plugin.getConfig().getLong("heartCooldown");
                final long now = System.currentTimeMillis();
                long lastHeartUse = CooldownManager.lastHeartUse.getOrDefault(player.getUniqueId(), 0L);
                long timeLeft = lastHeartUse + heartCooldownTime - now;
                return TimeFormatter.formatDuration(timeLeft);
            }
        }

        return "InvalidPlaceholder";
    }
}
