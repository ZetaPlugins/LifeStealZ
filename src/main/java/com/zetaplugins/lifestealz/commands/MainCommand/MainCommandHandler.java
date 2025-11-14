package com.zetaplugins.lifestealz.commands.MainCommand;

import com.zetaplugins.lifestealz.commands.MainCommand.subcommands.*;
import com.zetaplugins.zetacore.annotations.AutoRegisterCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.commands.SubCommand;
import com.zetaplugins.lifestealz.util.MessageUtils;

import java.util.HashMap;
import java.util.Map;

@AutoRegisterCommand(command = "lifestealz")
public final class MainCommandHandler implements CommandExecutor {
    private final LifeStealZ plugin;
    private final Map<String, SubCommand> commands = new HashMap<>();

    public MainCommandHandler(LifeStealZ plugin) {
        this.plugin = plugin;

        commands.put("reload", new ReloadSubCommand(plugin));
        commands.put("help", new HelpSubCommand(plugin));
        commands.put("recipe", new RecipeSubCommand(plugin));
        commands.put("hearts", new HeartsSubCommand(plugin));
        commands.put("giveItem", new GiveItemSubCommand(plugin));
        commands.put("data", new DataSubCommand(plugin));
        commands.put("graceperiod", new GracePeriodSubcommand(plugin));
        commands.put("checkbypass", new BypassStatusSubCommand(plugin));
        commands.put("dev", new DevSubCommand(plugin));
        commands.put("debug", new DebugSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendVersionMessage(sender);
            return true;
        }

        SubCommand subCommand = commands.get(args[0]);

        if (subCommand == null) {
            sendVersionMessage(sender);
            return true;
        }

        return subCommand.execute(sender, args);
    }

    private void sendVersionMessage(CommandSender sender) {
        sender.sendMessage(MessageUtils.getAndFormatMsg(
                false,
                "newVersionMsg",
                "\n&c<b><grey>></grey> LifeStealZ</b> <grey>v%version%</grey>\n\n&c <u><click:open_url:'https://wiki.lifestealz.com/'>Documentation</click></u>  &c<u><click:open_url:'https://strassburger.org/discord'>Support Discord</click></u>\n",
                new MessageUtils.Replaceable("%version%", plugin.getDescription().getVersion())
        ));
    }
}
