package com.zetaplugins.lifestealz.listeners;

import com.zetaplugins.lifestealz.util.*;
import com.zetaplugins.zetacore.annotations.AutoRegisterListener;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.customitems.CustomItemType;
import com.zetaplugins.lifestealz.util.customitems.customitemdata.CustomHeartItemData;
import com.zetaplugins.lifestealz.util.customitems.customitemdata.CustomItemData;
import com.zetaplugins.lifestealz.util.customitems.CustomItemManager;
import com.zetaplugins.lifestealz.storage.PlayerData;

import java.util.List;

@AutoRegisterListener
public final class InteractionListener implements Listener {
    private final LifeStealZ plugin;

    public InteractionListener(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteraction(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand(); // Track which hand is being used

        if (shouldCancelRespawnAnchorUsage(event) || shouldCancelBedInteraction(event)) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "interactionNotAllowed",
                    "&cYou are not allowed to interact with this!"
            ));
            event.setCancelled(true);
            return;
        }

        if (event.getAction().isRightClick() && item != null) {
            if (CustomItemManager.isForbiddenItem(item)) {
                event.setCancelled(true);

                if (hand == null) return;

                switch (hand) {
                    case HAND:
                        player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.AIR));
                        break;
                    case OFF_HAND:
                        player.getInventory().setItem(40, new ItemStack(Material.AIR));
                        break;
                }
            }

            if (CustomItemType.NONUSABLE.is(item)) {
                event.setCancelled(true);
                return;
            }

            if (CustomItemType.HEART.is(item)) {
                handleHeartItem(item, player, hand, event);
            }

            if (CustomItemType.REVIVE.is(item)) {
                handleReviveItem(item, player, hand, event);
            }
        }
    }

    /**
     * Checks if the event should be cancelled when a player interacts with a respawn anchor
     * @param event PlayerInteractEvent
     * @return wether the event needs to be cancelled
     */
    private boolean shouldCancelRespawnAnchorUsage(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !plugin.getConfig().getBoolean("preventRespawnAnchors")) {
            return false;
        }

        Block block = event.getClickedBlock();
        List<World.Environment> disabledEnvironments = List.of(World.Environment.NORMAL, World.Environment.THE_END);

        return block != null && block.getType() == Material.RESPAWN_ANCHOR &&
                disabledEnvironments.contains(event.getPlayer().getWorld().getEnvironment());
    }

    private boolean shouldCancelBedInteraction(PlayerInteractEvent event) {
        List<World.Environment> disabledEnvironments = List.of(World.Environment.NETHER, World.Environment.THE_END);
        List<Material> disabledMaterials = List.of(Material.BLACK_BED, Material.BLUE_BED, Material.BROWN_BED, Material.CYAN_BED, Material.GRAY_BED, Material.GREEN_BED, Material.LIGHT_BLUE_BED, Material.LIGHT_GRAY_BED, Material.LIME_BED, Material.MAGENTA_BED, Material.ORANGE_BED, Material.PINK_BED, Material.PURPLE_BED, Material.RED_BED, Material.WHITE_BED, Material.YELLOW_BED);
        Block block = event.getClickedBlock();
        if (block == null || !plugin.getConfig().getBoolean("preventBeds")) return false;
        return event.getAction() == Action.RIGHT_CLICK_BLOCK
                && disabledEnvironments.contains(event.getPlayer().getWorld().getEnvironment())
                && disabledMaterials.contains(block.getType());
    }

    private void handleHeartItem(ItemStack item, Player player, EquipmentSlot hand, PlayerInteractEvent event) {
        CustomHeartItemData customItemData;
        String customItemId = CustomItemManager.getCustomItemId(item);

        try {
            customItemData = new CustomHeartItemData(customItemId);
        } catch (IllegalArgumentException e) {
            return;
        }

        event.setCancelled(true);

        String world = player.getWorld().getName();
        if (!customItemData.isAllowedInWorld(world)) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "noItemUseInWorld",
                    "&cYou cannot use this item in this world!"
            ));
            return;
        }

        if (customItemData.requiresPermission() && !player.hasPermission(customItemData.getPermission()) && !player.isOp() && !player.hasPermission("lifestealz.item.*")) {
            player.sendMessage(MessageUtils.getAndFormatMsg(false, "noPermissionError", "&cYou don't have permission to use this!"));
            return;
        }

        if (restrictedHeartUseByBypass(player)) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "noHeartUseWithBypass",
                    "&cYou can't use hearts with bypass permission!"
            ));
            return;
        }

        if (restrictedHeartByGracePeriod(player)) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "noHeartUseInGracePeriod",
                    "&cYou can't use hearts during the grace period!"
            ));
            return;
        }

        long heartCooldown = plugin.getConfig().getLong("heartCooldown");
        if (CooldownManager.lastHeartUse.get(player.getUniqueId()) != null && CooldownManager.lastHeartUse.get(player.getUniqueId()) + heartCooldown > System.currentTimeMillis()) {
            player.sendMessage(MessageUtils.getAndFormatMsg(false, "heartconsumeCooldown", "&cYou have to wait before using another heart!"));
            return;
        }

        PlayerData playerData = plugin.getStorage().load(player.getUniqueId());

        Integer savedHeartAmountInteger = item.getItemMeta().getPersistentDataContainer().has(CustomItemManager.CUSTOM_HEART_VALUE_KEY, PersistentDataType.INTEGER) ? item.getItemMeta().getPersistentDataContainer().get(CustomItemManager.CUSTOM_HEART_VALUE_KEY, PersistentDataType.INTEGER) : 1;
        int savedHeartAmount = savedHeartAmountInteger != null ? savedHeartAmountInteger : 1;
        double heartsToAdd = savedHeartAmount * 2;
        double newHearts = playerData.getMaxHealth() + heartsToAdd;

        final double maxHearts = MaxHeartsManager.getMaxHearts(player, plugin.getConfig());

        if (newHearts > maxHearts) {
            player.sendMessage(MessageUtils.getAndFormatMsg(false, "maxHeartLimitReached", "&cYou already reached the limit of %limit% hearts!", new MessageUtils.Replaceable("%limit%", Integer.toString((int) maxHearts / 2))));
            return;
        }

        if (playerData.getMaxHealth() < customItemData.getMinHearts() * 2 && customItemData.getMinHearts() != -1) {
            player.sendMessage(MessageUtils.getAndFormatMsg(false, "itemMinHearts", "&cYou need at least %amount% hearts to use this item!", new MessageUtils.Replaceable("%amount%", Integer.toString(customItemData.getMinHearts()))));
            return;
        }

        if (playerData.getMaxHealth() >= customItemData.getMaxHearts() * 2 && customItemData.getMaxHearts() != -1) {
            player.sendMessage(MessageUtils.getAndFormatMsg(false, "itemMaxHearts", "&cYou can't use this item with more than %amount% hearts!", new MessageUtils.Replaceable("%amount%", Integer.toString(customItemData.getMaxHearts()))));
            return;
        }

        if (hand == EquipmentSlot.HAND) {
            updateItemInHand(player, item, player.getInventory().getHeldItemSlot());
        } else if (hand == EquipmentSlot.OFF_HAND) {
            updateItemInHand(player, item, 40);
        }

        playerData.setMaxHealth(newHearts);
        plugin.getStorage().save(playerData);
        LifeStealZ.setMaxHealth(player, newHearts);
        if (plugin.getConfig().getBoolean("healOnHeartUse")) player.setHealth(Math.min(player.getHealth() + heartsToAdd, newHearts));

        String customItemID = CustomItemManager.getCustomItemId(item);
        if (customItemID != null) {
            CustomItemData.CustomItemSoundData sound = CustomItemManager.getCustomItemData(customItemID).getSound();
            if (sound.isEnabled()) player.playSound(player.getLocation(), sound.getSound(), (float) sound.getVolume(), (float) sound.getPitch());
        }

        List<String> heartuseCommands = plugin.getConfig().getStringList("heartuseCommands");
        for (String command : heartuseCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("&player&", player.getName()));
        }

        // NOW USES CUSTOM MODEL
        if (plugin.getConfig().getBoolean("playTotemEffect")) playHeartAnimation(player);

        player.sendMessage(MessageUtils.getAndFormatMsg(true, "heartconsume", "&7Consumed a heart and got &c%amount% &7hearts!", new MessageUtils.Replaceable("%amount%", savedHeartAmount + "")));
        CooldownManager.lastHeartUse.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void handleReviveItem(ItemStack item, Player player, EquipmentSlot hand, PlayerInteractEvent event) {
        CustomItemData customItemData;
        String customItemId = CustomItemManager.getCustomItemId(item);

        try {
            customItemData = new CustomItemData(customItemId);
        } catch (IllegalArgumentException e) {
            return;
        }

        event.setCancelled(true);

        String world = player.getWorld().getName();
        if (!customItemData.isAllowedInWorld(world)) {
            player.sendMessage(MessageUtils.getAndFormatMsg(
                    false,
                    "noItemUseInWorld",
                    "&cYou cannot use this item in this world!"
            ));
            return;
        }

        GuiManager.openReviveGui(player, 1);
    }

    private void playHeartAnimation(Player player) {
        // Store the original off-hand item
        ItemStack originalOffHandItem = player.getInventory().getItemInOffHand();

        // Create a fake totem item (will be shown for a very brief moment before the animation)
        ItemStack fakeTotem = CustomItemManager.createHeartAnimationTotem();

        player.getInventory().setItemInOffHand(fakeTotem);
        // if you dont do a delay, it appears to use default texture
        new BukkitRunnable() {
            @Override
            public void run() {
                // Play the totem animation
                player.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
                player.getInventory().setItemInOffHand(originalOffHandItem);
            }
        }.runTaskLater(plugin, 3L);

    }

    private void updateItemInHand(Player player, ItemStack item, int slot) {
        ItemStack updatedItem = item.clone();
        updatedItem.setAmount(item.getAmount() - 1);

        if (updatedItem.getAmount() > 0) updatedItem.setItemMeta(item.getItemMeta());

        player.getInventory().setItem(slot, updatedItem);
    }

    private boolean restrictedHeartByGracePeriod(Player player) {
        GracePeriodManager gracePeriodManager = plugin.getGracePeriodManager();
        return gracePeriodManager.isInGracePeriod(player) && !gracePeriodManager.getConfig().useHearts();
    }

    private boolean restrictedHeartUseByBypass(Player player) {
        return plugin.getBypassManager().hasBypass(player) && !plugin.getBypassManager().getConfig().useHearts();
    }
}