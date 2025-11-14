package com.zetaplugins.lifestealz.commands;

import com.zetaplugins.zetacore.annotations.AutoRegisterCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.storage.PlayerData;

import java.util.List;

@AutoRegisterCommand(command = "hearts")
public final class HeartCommand implements CommandExecutor, TabCompleter {
    private final LifeStealZ plugin;

    public HeartCommand(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String targetName = (args != null && args.length > 0) ? args[0] : null;

        if (targetName == null) {
            return handleSelfHeartCheck(sender);
        } else {
            return handleOtherHeartCheck(sender, targetName);
        }
    }

    /**
     * Handles the heart check for the player who executed the command
     * @param sender The command sender
     * @return Whether the command was executed successfully
     */
    private boolean handleSelfHeartCheck(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "specifyPlayerOrBePlayer",
                    "&cYou need to either specify a player or be a player yourself!"
            ));
            return false;
        }

        Player player = (Player) sender;
        PlayerData playerdata = plugin.getStorage().load(player.getUniqueId());
        int heartCount = (int) Math.floor(playerdata.getMaxHealth() / 2);
        sender.sendMessage(MessageUtils.getAndFormatMsg(
                true,
                "viewheartsYou",
                "&7You have &c%amount% &7hearts!",
                new MessageUtils.Replaceable("%amount%", Integer.toString(heartCount))));
        return true;
    }

    /**
     * Handles the heart check for another player
     * @param sender The command sender
     * @param targetName The name of the player to check
     * @return Whether the command was executed successfully
     */
    private boolean handleOtherHeartCheck(CommandSender sender, String targetName) {

        PlayerData playerdata;

        if(plugin.hasGeyser() && plugin.getGeyserPlayerFile().isPlayerStored(targetName)) {

            playerdata = plugin.getStorage().load(plugin.getGeyserManager().getOfflineBedrockPlayerUniqueId(targetName));

        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target.getName() == null) {
                sender.sendMessage(MessageUtils.getAndFormatMsg(
                        false,
                        "playerNotFound",
                        "&cPlayer not found!"
                ));
                return false;
            }

            playerdata = plugin.getStorage().load(target.getUniqueId());
        }

        if (playerdata == null) {
            sender.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "playerNotFound",
                    "&cPlayer not found!"
            ));
            return false;
        }

        int heartCount = (int) Math.floor(playerdata.getMaxHealth() / 2);
        sender.sendMessage(MessageUtils.getAndFormatMsg(
                true,
                "viewheartsOther",
                "&c%player% &7currently has &c%amount% &7hearts!",
                new MessageUtils.Replaceable("%amount%", Integer.toString(heartCount)),
                new MessageUtils.Replaceable("%player%", targetName)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return null;
    }
}