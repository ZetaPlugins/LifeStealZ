package com.zetaplugins.lifestealz.commands.MainCommand.subcommands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.commands.SubCommand;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.commands.CommandUtils;

import java.util.List;

import static com.zetaplugins.lifestealz.util.commands.CommandUtils.*;

public final class BypassStatusSubCommand implements SubCommand {
    private final LifeStealZ plugin;

    public BypassStatusSubCommand(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!hasPermission(sender)) {
            CommandUtils.throwPermissionError(sender);
            return false;
        }

        if (args.length < 2) {
            throwUsageError(sender, getUsage());
            return false;
        }

        List<OfflinePlayer> targetPlayers = parseOfflinePlayer(args[1], true, true, plugin);

        if (targetPlayers.isEmpty()) {
            sender.sendMessage(MessageUtils.getAndFormatMsg(false, "playerNotFound", "&cPlayer not found!"));
            return false;
        }

        OfflinePlayer targetPlayer = targetPlayers.get(0);
        boolean hasBypass = plugin.getBypassManager().hasBypass(targetPlayer.getPlayer());
        
        if (hasBypass) {
            sender.sendMessage(
                MessageUtils.getAndFormatMsg(
                    true,
                    "bypassStatusActive",
                    "&7Bypass permission is &aactive &7for %player%",
                    new MessageUtils.Replaceable("%player%", targetPlayer.getName())
                )
            );
        } else {
            sender.sendMessage(
                MessageUtils.getAndFormatMsg(
                    true,
                    "bypassStatusInactive",
                    "&7Bypass permission is &cinactive &7for %player%.",
                    new MessageUtils.Replaceable("%player%", targetPlayer.getName())
                )
            );
        }

        return true;
    }

    @Override
    public String getUsage() {
        return "/lifestealz checkbypass <player>";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("lifestealz.admin.bypasscheck");
    }
}
