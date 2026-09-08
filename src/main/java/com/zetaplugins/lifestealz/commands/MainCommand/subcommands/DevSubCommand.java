package com.zetaplugins.lifestealz.commands.MainCommand.subcommands;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.commands.SubCommand;
import com.zetaplugins.lifestealz.util.GracePeriodManager;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.commands.CommandUtils;
import com.zetaplugins.lifestealz.util.customitems.CustomItem;
import com.zetaplugins.lifestealz.storage.PlayerData;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;

import static com.zetaplugins.lifestealz.util.commands.CommandUtils.throwUsageError;

public final class DevSubCommand implements SubCommand {
    private final LifeStealZ plugin;

    public DevSubCommand(LifeStealZ plugin) {
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

        String optionTwo = args[1];

        if (optionTwo.equals("giveForbiddenitem")) {
            if (!(sender instanceof Player)) return false;

            Player player = (Player) sender;

            player.getInventory().addItem(new CustomItem(Material.BARRIER).makeForbidden().getItemStack());

            return true;
        }

        if (optionTwo.equals("isInGracePeriod")) {
            if (args.length < 3 && !(sender instanceof Player)) {
                throwUsageError(sender, "/lifestealz dev isInGracePeriod [player]");
                return false;
            }

            Player player = args.length > 2 ? plugin.getServer().getPlayer(args[2]) : (Player) sender;
            if (player == null) {
                throwUsageError(sender, "/lifestealz dev isInGracePeriod [player]");
                return false;
            }

            GracePeriodManager gracePeriodManager = plugin.getGracePeriodManager();

            String gracePeriodColor = gracePeriodManager.isInGracePeriod(player) ? "&a" : "&c";
            String gracePeriodEnabledColor = gracePeriodManager.isEnabled() ? "&a" : "&c";

            sender.sendMessage(MessageUtils.formatMsg(
                    "&7Is &c" + player.getName() + " &7in grace period? "
                            + gracePeriodColor + gracePeriodManager.isInGracePeriod(player)
                            + (gracePeriodManager.isInGracePeriod(player) ? " &7(" + gracePeriodManager.getGracePeriodRemaining(player).orElse(0) + "s remaining)" : "")
                            + "\n&7Grace period enabled: " + gracePeriodEnabledColor + gracePeriodManager.isEnabled() + "&7"
            ));
        }

        if (optionTwo.equals("setGraceOffset")) {
            if (args.length < 3 && !(sender instanceof Player)) {
                throwUsageError(sender, "/lifestealz dev setGraceOffset [player]");
                return false;
            }

            Player player = args.length > 2 ? plugin.getServer().getPlayer(args[2]) : (Player) sender;
            if (player == null) {
                throwUsageError(sender, "/lifestealz dev setGraceOffset [player]");
                return false;
            }

            PlayerData playerData = plugin.getStorage().load(player.getUniqueId());

            long elapsed;
            if (plugin.getGracePeriodManager().getConfig().shouldRunOffline()) {
                elapsed = (System.currentTimeMillis() - player.getFirstPlayed());
            } else {
                elapsed = player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50;
            }
            
            playerData.setGraceOffset(elapsed);
            plugin.getStorage().save(playerData);
            plugin.getGracePeriodManager().startGracePeriod(player);
        }

        if (optionTwo.equals("refreshCaches")) {
            plugin.getEliminatedPlayersCache().reloadCache();
            sender.sendMessage(MessageUtils.formatMsg("&7Caches reloaded!"));
        }

        if (optionTwo.equals("crash")) {
            throw new RuntimeException("Crash test");
        }

        if (optionTwo.equals("cleardatabase")) {
            plugin.getStorage().clearDatabase();
            sender.sendMessage(MessageUtils.formatMsg("&7Database cleared!"));
        }

        if (optionTwo.equals("giveAnimationTotem")) {
            if (!(sender instanceof Player)) return false;

            Player player = (Player) sender;

            player.getInventory().addItem(CustomItemManager.createHeartAnimationTotem());

            return true;
        }

        if (optionTwo.equals("getEffectivePerms")) {
            if (!(sender instanceof Player)) return false;

            Player player = (Player) sender;

            String effectivePerms = player.getEffectivePermissions()
                    .stream()
                    .map(permission -> permission.getPermission() + (permission.getValue() ? "" : " (denied)"))
                    .reduce((perm1, perm2) -> perm1 + ", " + perm2)
                    .orElse("No permissions found");
            sender.sendMessage(MessageUtils.formatMsg("&7Effective permissions for " + player.getName() + ": " + effectivePerms));
            return true;
        }

        return false;
    }

    @Override
    public String getUsage() {
        return "/lifestealz dev <giveForbiddenitem | isInGracePeriod | setGraceOffset | refreshCaches | crash | cleardatabase | giveAnimationTotem | getEffectivePerms>";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.isOp();
    }
}
