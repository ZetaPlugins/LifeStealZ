package com.zetaplugins.lifestealz.commands;

import com.zetaplugins.zetacore.annotations.AutoRegisterCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.WebHookManager;
import com.zetaplugins.lifestealz.storage.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoRegisterCommand(command = "revive")
public final class ReviveCommand implements CommandExecutor, TabCompleter {
    private final LifeStealZ plugin;

    public ReviveCommand(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    private static final String BYPASS_OPTION = "bypass";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            throwUsageError(sender);
            return false;
        }

        String targetPlayersArg = args[0];

        String bypassOption = args.length > 1 ? args[1] : null;

        return targetPlayersArg.equals("*") ?
                handleReviveAll(sender, bypassOption) :
                handleReviveOne(sender, targetPlayersArg, bypassOption);
    }

    private boolean handleReviveAll(CommandSender sender, String bypassOption) {
        try {
            int revivedPlayers = plugin.getStorage().reviveAllPlayers(
                    plugin.getConfig().getInt("minHearts"),
                    plugin.getConfig().getInt("reviveHearts"),
                    plugin.getConfig().getInt("maxRevives"),
                    BYPASS_OPTION.equals(bypassOption) && sender.hasPermission("lifestealz.bypassrevivelimit")
            );
            plugin.getEliminatedPlayersCache().reloadCache();
            sender.sendMessage(MessageUtils.getAndFormatMsg(true, "massReviveSuccess",
                    "&7You successfully revived &c%amount% &7player(s)!",
                    new MessageUtils.Replaceable("%amount%", Integer.toString(revivedPlayers))
            ));
            return true;
        } catch (Exception e) {
            sender.sendMessage(MessageUtils.getAndFormatMsg(false, "massReviveError",
                    "&cAn error occurred while reviving all players: %error%",
                    new MessageUtils.Replaceable("%error%", e.getMessage())
            ));
            plugin.getLogger().severe("An error occurred while reviving all players: " + e.getMessage());
            return false;
        }
    }

    private boolean handleReviveOne(CommandSender sender, String targetPlayerName, String bypassOption) {
        OfflinePlayer targetPlayer = plugin.getServer().getOfflinePlayer(targetPlayerName);
        PlayerData playerData = plugin.getStorage().load(targetPlayer.getUniqueId());

        if (playerData == null) {
            sender.sendMessage(MessageUtils.getAndFormatMsg(false, "noPlayerData", "&cThis player has not played on this server yet!"));
            return false;
        }

        playerData = plugin.getStorage().load(targetPlayer.getUniqueId());

        // Check if the player has reached the revive limit or has the bypass permission
        if (!canRevive(sender, playerData, bypassOption)) {
            sender.sendMessage(MessageUtils.getAndFormatMsg(false, "reviveMaxReached",
                    "&cThis player has already been revived %amount% times!",
                    new MessageUtils.Replaceable("%amount%", Integer.toString(playerData.getHasBeenRevived()))));
            return false;
        }

        // Check if the player is eliminated
        if (!isEligibleForRevive(sender, playerData)) {
            sender.sendMessage(MessageUtils.getAndFormatMsg(false, "onlyReviveElimPlayers","&cYou can only revive eliminated players!"));
            return false;
        }

        revivePlayer(sender, targetPlayer.getName(), playerData);

        sender.sendMessage(MessageUtils.getAndFormatMsg(true, "reviveSuccess",
                "&7You successfully revived &c%player%&7!",
                new MessageUtils.Replaceable("%player%", targetPlayer.getName())));

        return true;
    }

    /**
     * Checks if the player can be revived
     * @param sender The command sender
     * @param playerData The player data of the player to be revived
     * @param bypassOption The bypass option
     * @return True if the player can be revived, false otherwise
     */
    private boolean canRevive(CommandSender sender, PlayerData playerData, String bypassOption) {
        int maxRevives = plugin.getConfig().getInt("maxRevives");
        boolean hasBypassPermission = sender.hasPermission("lifestealz.bypassrevivelimit");

        return maxRevives == -1 || playerData.getHasBeenRevived() < maxRevives ||
                (BYPASS_OPTION.equals(bypassOption) && hasBypassPermission);
    }

    /**
     * Checks if the player is eligible for a revive (e.g. has been eliminated)
     * @param sender The command sender
     * @param playerData The player data of the player to be revived
     * @return True if the player is eligible for a revive, false otherwise
     */
    private boolean isEligibleForRevive(CommandSender sender, PlayerData playerData) {
        int minHearts = plugin.getConfig().getInt("minHearts");

        return !(playerData.getMaxHealth() > minHearts * 2);
    }

    /**
     * Revives the player
     * @param sender The command sender
     * @param targetPlayerName The name of the player to be revived
     * @param playerData The player data of the player to be revived
     */
    private void revivePlayer(CommandSender sender, String targetPlayerName, PlayerData playerData) {
        playerData.setMaxHealth(plugin.getConfig().getDouble("reviveHearts") * 2);
        playerData.setHasBeenRevived(playerData.getHasBeenRevived() + 1);
        plugin.getStorage().save(playerData);
        plugin.getEliminatedPlayersCache().removeEliminatedPlayer(targetPlayerName);

        plugin.getWebHookManager().sendWebhookMessage(WebHookManager.WebHookType.REVIVE, targetPlayerName, sender.getName());
    }

    /**
     * Throws a usage error message
     * @param sender The command sender
     */
    private void throwUsageError(CommandSender sender) {
        Component usageMessage = MessageUtils.getAndFormatMsg(false, "usageError",
                "&cUsage: %usage%", new MessageUtils.Replaceable("%usage%", "/revive <player>"));
        sender.sendMessage(usageMessage);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            Set<String> eliminatedPlayers = plugin.getEliminatedPlayersCache().getEliminatedPlayers();
            eliminatedPlayers.add("*");
            return new ArrayList<>(eliminatedPlayers);
        }
        if (args.length == 2 && sender.hasPermission("lifestealz.bypassrevivelimit")) {
            return List.of(BYPASS_OPTION);
        }
        return null;
    }
}