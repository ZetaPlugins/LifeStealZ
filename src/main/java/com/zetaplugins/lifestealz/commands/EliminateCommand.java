package com.zetaplugins.lifestealz.commands;

import com.zetaplugins.zetacore.annotations.AutoRegisterCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.WebHookManager;
import com.zetaplugins.lifestealz.storage.PlayerData;

import java.util.List;

@AutoRegisterCommand(command = "eliminate")
public final class EliminateCommand implements CommandExecutor, TabCompleter {
    private final LifeStealZ plugin;

    public EliminateCommand(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String targetPlayerName = (args != null && args.length > 0) ? args[0] : null;
        if (targetPlayerName == null) {
            throwUsageError(sender);
            return false;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            throwUsageError(sender);
            return false;
        }

        eliminatePlayer(sender, targetPlayer);
        return true;
    }


    private void eliminatePlayer(CommandSender sender, Player targetPlayer) {
        PlayerData playerData = plugin.getStorage().load(targetPlayer.getUniqueId());
        playerData.setMaxHealth(0.0);
        plugin.getStorage().save(playerData);

        dropPlayerInventory(targetPlayer);

        Component kickmsg = MessageUtils.getAndFormatMsg(false, "eliminatedjoin", "&cYou don't have any hearts left!");
        targetPlayer.kick(kickmsg, PlayerKickEvent.Cause.BANNED);

        sender.sendMessage(MessageUtils.getAndFormatMsg(true, "eliminateSuc", "&7You successfully eliminated &c%player%&7!",
                new MessageUtils.Replaceable("%player%", targetPlayer.getName())));

        if (plugin.getConfig().getBoolean("announceElimination")) {
            Component elimAnnouncementMsg = MessageUtils.getAndFormatMsg(true, "eliminateionAnnouncementNature", "&c%player% &7has been eliminated!",
                    new MessageUtils.Replaceable("%player%", targetPlayer.getName()));
            Bukkit.broadcast(elimAnnouncementMsg);
        }

        plugin.getEliminatedPlayersCache().addEliminatedPlayer(targetPlayer.getName());

        plugin.getWebHookManager().sendWebhookMessage(WebHookManager.WebHookType.ELIMINATION, targetPlayer.getName(), sender.getName());
    }

    private void dropPlayerInventory(Player targetPlayer) {
        for (ItemStack item : targetPlayer.getInventory().getContents()) {
            if (item != null) targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), item);
        }
        targetPlayer.getInventory().clear();
    }

    private void throwUsageError(CommandSender sender) {
        Component usageMessage = MessageUtils.getAndFormatMsg(false, "usageError", "&cUsage: %usage%", new MessageUtils.Replaceable("%usage%", "/eliminate <player>"));
        sender.sendMessage(usageMessage);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return null;
    }
}