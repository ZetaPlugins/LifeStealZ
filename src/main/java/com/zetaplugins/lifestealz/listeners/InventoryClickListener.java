package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.lifestealz.util.GuiManager;
import com.zetaplugins.lifestealz.util.MessageUtils;
import com.zetaplugins.lifestealz.util.revive.ReviveTask;
import com.zetaplugins.lifestealz.util.WebHookManager;
import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.commands.CommandUtils;
import com.zetaplugins.lifestealz.util.customblocks.CustomBlock;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;
import com.zetaplugins.lifestealz.storage.PlayerData;
import com.zetaplugins.lifestealz.util.customitems.customitemdata.CustomReviveBeaconItemData;

import java.util.List;
import java.util.UUID;

@AutoRegisterListener
public final class InventoryClickListener implements Listener {
    private final LifeStealZ plugin;

    public InventoryClickListener(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory openInventory = player.getOpenInventory().getTopInventory();

        if (handleRecipeGuiClick(event, player, openInventory)) return;
        if (handleReviveGuiClick(event, player, openInventory)) return;
        if (handleBeaconReviveGuiClick(event, player, openInventory)) return;

        if (event.getCurrentItem() != null && CustomItemManager.isForbiddenItem(event.getCurrentItem())) {
            event.setCancelled(true);
            event.setCurrentItem(new ItemStack(Material.AIR));
        }
    }

    /**
     * Handles clicks in the recipe GUI.
     * @param event The InventoryClickEvent to handle.
     * @param player The player who clicked in the inventory.
     * @param openInventory The inventory that is currently open for the player.
     * @return true if the event was handled, false otherwise.
     */
    private boolean handleRecipeGuiClick(InventoryClickEvent event, Player player, Inventory openInventory) {
        if (!openInventory.equals(GuiManager.RECIPE_GUI_MAP.get(player.getUniqueId()))) return false;

        event.setCancelled(true);
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null && currentItem.getType() == Material.BARRIER) {
            player.closeInventory();
        }

        return true;
    }

    /**
     * Handles clicks in the revive GUI.
     * @param event The InventoryClickEvent to handle.
     * @param player The player who clicked in the inventory.
     * @param openInventory The inventory that is currently open for the player.
     * @return true if the event was handled, false otherwise.
     */
    private boolean handleReviveGuiClick(InventoryClickEvent event, Player player, Inventory openInventory) {
        if (!openInventory.equals(GuiManager.REVIVE_GUI_MAP.get(player.getUniqueId()))) return false;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return true;

        switch (item.getType()) {
            case BARRIER -> player.closeInventory();
            case ARROW -> GuiManager.openReviveGui(player, getPageFromItem(item));
            case PLAYER_HEAD -> handleReviveClick(item, player, false);
            case SKELETON_SKULL -> handleReviveClick(item, player, true);
        }

        return true;
    }

    /**
     * Handles clicks in the beacon revive GUI.
     * @param event The InventoryClickEvent to handle.
     * @param player The player who clicked in the inventory.
     * @param openInventory The inventory that is currently open for the player.
     * @return true if the event was handled, false otherwise.
     */
    private boolean handleBeaconReviveGuiClick(InventoryClickEvent event, Player player, Inventory openInventory) {
        if (!openInventory.equals(GuiManager.REVIVE_BEACON_GUI_MAP.get(player.getUniqueId()))) return false;

        Location beaconLocation = GuiManager.REVIVE_BEACON_INVENTORY_LOCATIONS.get(player.getUniqueId());

        if (beaconLocation == null) {
            player.sendMessage(Component.text("§cAn error occurred while fetching the beacon location! Please try again."));
            return false;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return true;

        switch (item.getType()) {
            case BARRIER -> player.closeInventory();
            case ARROW -> GuiManager.openReviveBeaconGui(player, getPageFromItem(item), plugin, beaconLocation);
            case PLAYER_HEAD -> handleBeaconReviveClick(item, player, false, beaconLocation);
            case SKELETON_SKULL -> handleBeaconReviveClick(item, player, true, beaconLocation);
        }

        return true;
    }

    /**
     * Handles the click on a revive item in the inventory.
     * @param item The ItemStack that was clicked, representing a revive item.
     * @param player The player who clicked the item.
     * @param bedrock Whether the item is for Bedrock edition.
     */
    private void handleReviveClick(ItemStack item, Player player, boolean bedrock) {
        if (!player.hasPermission("lifestealz.revive")) {
            throwPermissionError(player);
            return;
        }

        String uuidString = getLastLineOfLore(item, bedrock);
        if (uuidString == null) return;

        UUID uuid = UUID.fromString(uuidString);
        OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(uuid);

        if (target.getName() == null) {
            player.sendMessage(Component.text("§cAn error occurred while fetching playerdata! Are you sure this is a real player?"));
            return;
        }

        if (!hasReviveCrystal(player)) {
            plugin.getLogger().warning("Player " + player.getName() + " tried to revive " + target.getName() + " without a revive crystal!");
            return;
        }

        revivePlayer(player, target, bedrock);
    }

    /**
     * Handles the click on a revive beacon item in the inventory.
     * @param item The ItemStack that was clicked, representing a revive beacon item.
     * @param player The player who clicked the item.
     * @param bedrock Whether the item is for Bedrock edition.
     * @param beaconLocation The location of the beacon where the revive is being initiated.
     */
    private void handleBeaconReviveClick(ItemStack item, Player player, boolean bedrock, Location beaconLocation) {
        if (!player.hasPermission("lifestealz.revive")) {
            throwPermissionError(player);
            return;
        }

        if (plugin.getReviveTaskManager().isReviving(beaconLocation)) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "reviveBeaconAlreadyInUse",
                    "&cThis revive beacon is already in use! Please wait until the current revive is finished."
            ));
            return;
        }

        String uuidString = getLastLineOfLore(item, bedrock);
        if (uuidString == null) return;

        UUID uuid = UUID.fromString(uuidString);
        OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(uuid);

        if (target.getName() == null) {
            player.sendMessage(Component.text("§cAn error occurred while fetching playerdata! Are you sure this is a real player?"));
            return;
        }

        boolean targetReviving = plugin.getReviveTaskManager().isRevivingTarget(target.getUniqueId());

        if (targetReviving) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "alreadyRevivingPlayer",
                    "&cThis player is already being revived by another beacon! Please wait until the current revive is finished."
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        beaconRevivePlayer(player, target, bedrock, beaconLocation);
    }

    /**
     * Checks if the player can revive the target player.
     * @param reviver The player who is trying to revive another player.
     * @param target The player who is being revived (OfflinePlayer).
     * @param data The PlayerData of the target player.
     * @return true if the player can revive the target, false otherwise.
     */
    private boolean canRevivePlayer(Player reviver, OfflinePlayer target, PlayerData data) {
        int reviveMaximum = plugin.getConfig().getInt("maxRevives");
        int minHearts = plugin.getConfig().getInt("minHearts");

        if (reviveMaximum != -1 && data.getHasBeenRevived() >= reviveMaximum) {
            reviver.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "reviveMaxReached",
                    "&cThis player has already been revived %amount% times!",
                    new MessageUtils.Replaceable("%amount%", String.valueOf(data.getHasBeenRevived()))
            ));
            return false;
        }

        if (data.getMaxHealth() > minHearts * 2) {
            reviver.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "onlyReviveElimPlayers",
                    "&cYou can only revive eliminated players!"
            ));
            return false;
        }

        return true;
    }

    /**
     * Modifes the data of the player being revived.
     * @param data The PlayerData of the player being revived.
     */
    private void applyReviveData(PlayerData data) {
        data.setMaxHealth(plugin.getConfig().getInt("reviveHearts") * 2);
        data.setHasBeenRevived(data.getHasBeenRevived() + 1);
        plugin.getStorage().save(data);
    }

    /**
     * Executes common actions after a player has been revived.
     * @param reviver The player who revived the target.
     * @param target The player who was revived (OfflinePlayer).
     * @param location The location where the revive took place as a String Array containing the X, Y and Z value, or "null" if not applicable.
     */
    private void executeReviveActions(Player reviver, OfflinePlayer target, String[] location) {
        plugin.getEliminatedPlayersCache().removeEliminatedPlayer(target.getName());

        reviver.sendMessage(MessageUtils.getAndFormatMsg(
                true,
                "reviveSuccess",
                "&7You successfully revived &c%player%&7!",
                new MessageUtils.Replaceable("%player%", target.getName())
        ));
        reviver.playSound(reviver.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 500.0f, 1.0f);

        for (String command : plugin.getConfig().getStringList("reviveuseCommands")) {
            String finalCommand = command
                    .replace("&player&", reviver.getName())
                    .replace("&target&", target.getName())
                    .replace("&location&", location[0] + ", " + location[1] + ", " + location[2])
                    .replace("&locationX&", location[0])
                    .replace("&locationY&", location[1])
                    .replace("&locationZ&", location[2]);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }

        plugin.getWebHookManager().sendWebhookMessage(WebHookManager.WebHookType.REVIVE, target.getName(), reviver.getName());
    }

    /**
     * Revives a player from the normal revive GUI.
     * @param reviver The player who is reviving another player.
     * @param target The player who is being revived (OfflinePlayer).
     * @param isBedrock Whether the revive is for Bedrock edition.
     */
    private void revivePlayer(Player reviver, OfflinePlayer target, boolean isBedrock) {
        PlayerData data = plugin.getStorage().load(target.getUniqueId());

        if (!canRevivePlayer(reviver, target, data)) return;

        String[] locationNull = {"null", "null", "null"};
        applyReviveData(data);
        executeReviveActions(reviver, target, locationNull);
        removeReviveCrystal(reviver);
        reviver.closeInventory();
    }

    /**
     * Revives a player using a revive beacon.
     * @param reviver The player who is reviving another player.
     * @param target The player who is being revived (OfflinePlayer).
     * @param isBedrock Whether the revive is for Bedrock edition.
     * @param beaconLocation The location of the beacon where the revive is being initiated.
     */
    private void beaconRevivePlayer(Player reviver, OfflinePlayer target, boolean isBedrock, Location beaconLocation) {
        PlayerData data = plugin.getStorage().load(target.getUniqueId());

        String customItemId = CustomBlock.REVIVE_BEACON.getCustomItemId(beaconLocation.getBlock());
        CustomReviveBeaconItemData itemData;

        try {
            itemData = new CustomReviveBeaconItemData(customItemId);
        } catch (IllegalArgumentException e) {
            return;
        }

        if (!canRevivePlayer(reviver, target, data)) return;

        String[] location = {String.valueOf(beaconLocation.getBlockX()), String.valueOf(beaconLocation.getBlockY()), String.valueOf(beaconLocation.getBlockZ())};

        reviver.sendMessage(MessageUtils.getAndFormatMsg(
                true,
                "reviveBeaconStart",
                "&c%player% &7will be revived in &c%seconds% seconds&7! Please wait...",
                new MessageUtils.Replaceable("%player%", target.getName()),
                new MessageUtils.Replaceable("%seconds%", String.valueOf(itemData.getReviveTime()))
        ));

        reviver.closeInventory();

        for (String command : plugin.getConfig().getStringList("reviveStartCommands")) {
            String finalCommand = command
                    .replace("&player&", reviver.getName())
                    .replace("&target&", target.getName())
                    .replace("&location&", location[0] + ", " + location[1] + ", " + location[2])
                    .replace("&locationX&", location[0])
                    .replace("&locationY&", location[1])
                    .replace("&locationZ&", location[2]);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }

        plugin.getReviveBeaconEffectManager().startRevivingEffects(
                beaconLocation,
                target.getName(),
                itemData.shouldShowLaser(),
                itemData.shouldShowParticleRing(),
                itemData.getParticleColor(),
                itemData.getInnerLaser(),
                itemData.getOuterLaser(),
                itemData.getReviveTime()
        );

        BukkitTask reviveTask = new BukkitRunnable() {
            @Override
            public void run() {
                applyReviveData(data);
                executeReviveActions(reviver, target, location);

                plugin.getReviveTaskManager().removeReviveTask(beaconLocation);

                plugin.getReviveBeaconEffectManager().clearAllEffects(beaconLocation);
                beaconLocation.getBlock().setType(Material.AIR);
                beaconLocation.getWorld().playSound(beaconLocation, Sound.ENTITY_PLAYER_LEVELUP, 500.0f, 1.0f);
            }
        }.runTaskLater(plugin, itemData.getReviveTime() * 20L);

        plugin.getReviveTaskManager().addReviveTask(beaconLocation, new ReviveTask(
                beaconLocation,
                reviveTask,
                reviver.getUniqueId(),
                target.getUniqueId(),
                System.currentTimeMillis() / 1000L,
                itemData.getReviveTime()
        ));
    }

    /**
     * Retrieves the page number from the item's persistent data container.
     * If the page number is not set, it defaults to 1.
     *
     * @param item The ItemStack from which to retrieve the page number.
     * @return The page number, or 1 if not set.
     */
    private int getPageFromItem(ItemStack item) {
        Integer pageInt = item.getItemMeta().getPersistentDataContainer()
                .get(CustomItemManager.REVIVE_PAGE_KEY, PersistentDataType.INTEGER);
        return pageInt != null ? pageInt : 1;
    }

    /**
     * Throws a permission error message to the player.
     * @param player The player who lacks the required permission.
     */
    private void throwPermissionError(HumanEntity player) {
        CommandUtils.throwPermissionError(player);
    }

    /**
     * Retrieves the last line of lore from an item.
     * @param item The ItemStack from which to retrieve the lore.
     * @param bedrock Whether the item is for Bedrock edition.
     * @return The last line of lore as a String, or null if the lore is not available or too short.
     */
    private String getLastLineOfLore(ItemStack item, boolean bedrock) {
        PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        List<Component> lore = item.lore();
        if (lore == null || lore.size() < (bedrock ? 2 : 1)) return null;
        int line = bedrock ? 2 : 1;
        Component lastLore = lore.get(lore.size() - line);
        return plainSerializer.serialize(lastLore);
    }

    /**
     * Checks if the player has a revive crystal in their inventory.
     * @param player The player to check.
     * @return true if the player has a revive crystal, false otherwise.
     */
    private boolean hasReviveCrystal(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && CustomItemManager.isReviveItem(item)) return true;
        }
        return false;
    }

    /**
     * Removes one revive crystal from the player's inventory.
     * If the player has multiple, it only removes one.
     * If the player has no revive crystals, it does nothing.
     *
     * @param player The player from whose inventory to remove the revive crystal.
     */
    private void removeReviveCrystal(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && CustomItemManager.isReviveItem(item)) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }
}
