package com.zetaplugins.lifestealz.commands.MainCommand;

import com.zetaplugins.zetacore.annotations.AutoRegisterTabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.zetaplugins.lifestealz.LifeStealZ;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.zetaplugins.lifestealz.util.commands.CommandUtils.*;

@AutoRegisterTabCompleter(command = "lifestealz")
public final class MainTabCompleter implements TabCompleter {
    private final LifeStealZ plugin;

    public MainTabCompleter(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) return getFirstArgOptions(sender, args);
        if (args.length == 2) return getSecondArgOptions(sender, args);
        if (args.length == 3) return getThirdArgOptions(sender, args);
        if (args.length == 4) return getFourthArgOptions(args);
        if (args.length == 5) return getFifthArgOptions(args);
        return List.of();
    }

    private List<String> getFirstArgOptions(CommandSender sender, String[] args) {
        String input = args[0].toLowerCase();

        List<String> options = new ArrayList<>();
        if (sender.hasPermission("lifestealz.admin.reload")) options.add("reload");
        if (sender.hasPermission("lifestealz.admin.debug")) options.add("debug");
        if (sender.hasPermission("lifestealz.admin.setlife")) options.add("hearts");
        if (sender.hasPermission("lifestealz.admin.giveitem")) options.add("giveItem");
        if (sender.hasPermission("lifestealz.admin.bypasscheck")) options.add("checkbypass");
        if (sender.hasPermission("lifestealz.viewrecipes")) options.add("recipe");
        if (sender.hasPermission("lifestealz.help")) options.add("help");
        if (sender.hasPermission("lifestealz.managedata")) options.add("data");
        if (sender.hasPermission("lifestealz.graceperiod")) options.add("graceperiod");

        return getDisplayOptions(options, input);
    }

    private List<String> getSecondArgOptions(CommandSender sender, String[] args) {
        String input = args[1].toLowerCase();
        switch (args[0]) {
            case "hearts":
                return getDisplayOptions(List.of("add", "set", "remove", "get"), input);
            case "giveItem":
                return getDisplayOptions(getPlayersTabCompletion(true, plugin), input);
            case "graceperiod":
                return getDisplayOptions(getOfflinePlayersTabCompletion(true, true, plugin), input);
            case "recipe":
                return getDisplayOptions(plugin.getRecipeManager().getItemIds(), input);
            case "data":
                if (sender.hasPermission("lifestealz.managedata")) return getDisplayOptions(List.of("import", "export"), input);
                break;
            case "checkbypass":
                return getDisplayOptions(getPlayersTabCompletion(false, plugin), input);
            case "debug":
                return getDisplayOptions(List.of("upload", "generate"), input);
            case "dev":
                return getDisplayOptions(List.of("giveForbiddenitem", "isInGracePeriod", "setFirstJoinDate", "refreshCaches", "crash", "cleardatabase", "giveAnimationTotem", "getEffectivePerms"), input);
        }
        return List.of();
    }

    private List<String> getThirdArgOptions(CommandSender sender, String[] args) {
        String input = args[2].toLowerCase();
        switch (args[0]) {
            case "hearts":
                if ("get".equals(args[1])) return getDisplayOptions(getOfflinePlayersTabCompletion(false, true, plugin), input);
                return getDisplayOptions(getOfflinePlayersTabCompletion(true, true, plugin), input);
            case "graceperiod":
                return getDisplayOptions(List.of("skip", "reset"), input);
            case "giveItem":
                return getDisplayOptions(plugin.getRecipeManager().getItemIds(), input);
            case "recipe":
                return getDisplayOptions(plugin.getRecipeManager().getRecipeIds(args[1]), input);
            case "data":
                if ("import".equals(args[1]) && sender.hasPermission("lifestealz.managedata")) {
                    return getDisplayOptions(getCSVFiles(), input);
                }
        }
        return List.of();
    }

    private List<String> getFourthArgOptions(String[] args) {
        if ("hearts".equals(args[0]) || "giveItem".equals(args[0])) {
            return List.of("1", "32", "64");
        }
        return List.of();
    }

    private List<String> getFifthArgOptions(String[] args) {
        String input = args[4].toLowerCase();
        if ("giveItem".equals(args[0])) {
            return getDisplayOptions(List.of("silent"), input);
        }
        return List.of("");
    }



    private List<String> getCSVFiles() {
        List<String> csvFiles = new ArrayList<>();
        File pluginFolder = plugin.getDataFolder();
        File[] files = pluginFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (files != null) {
            for (File file : files) {
                csvFiles.add(file.getName());
            }
        }
        return csvFiles;
    }
}
