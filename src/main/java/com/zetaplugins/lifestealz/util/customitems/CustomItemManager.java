package com.zetaplugins.lifestealz.util.customitems;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.util.MessageUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import com.zetaplugins.lifestealz.util.customitems.customitemdata.CustomItemData;

import java.util.*;
import java.util.logging.Level;

public final class CustomItemManager {
    public static final NamespacedKey CUSTOM_ITEM_ID_KEY = new NamespacedKey(LifeStealZ.getInstance(), "customitemid");
    public static final NamespacedKey CUSTOM_ITEM_TYPE_KEY = new NamespacedKey(LifeStealZ.getInstance(), "customitemtype");
    public static final NamespacedKey CUSTOM_HEART_VALUE_KEY = new NamespacedKey(LifeStealZ.getInstance(), "customheartvalue");
    public static final NamespacedKey REVIVE_PAGE_KEY = new NamespacedKey(LifeStealZ.getInstance(), "revivepage");
    public static final NamespacedKey DESPAWNABLE_KEY = new NamespacedKey(LifeStealZ.getInstance(), "despawnable");
    public static final NamespacedKey INVULNERABLE_KEY = new NamespacedKey(LifeStealZ.getInstance(), "invulnerable");

    private CustomItemManager() {}

    /**
     * Creates a custom item
     *
     * @param itemId The id of the item
     * @return The custom item
     */
    public static ItemStack createCustomItem(String itemId) {
        try {
            FileConfiguration config = LifeStealZ.getInstance().getConfigManager().getCustomItemConfig();
            String customItemType = config.getString(itemId + ".customItemType", "heart");

            String materialString = config.getString(itemId + ".material", "NETHER_STAR");
            if (customItemType.equalsIgnoreCase("revivebeacon")) materialString = "BEACON";
            Material material = Material.valueOf(materialString);

            CustomItem ci = new CustomItem(material)
                    .setName(config.getString(itemId + ".name"))
                    .setLore(config.getStringList(itemId + ".lore"))
                    .setEnchanted(config.getBoolean(itemId + ".enchanted"))
                    .setInvulnerable(config.getBoolean(itemId + ".invulnerable"))
                    .setDespawnable(config.getBoolean(itemId + ".despawnable"))
                    .setId("lifestealz_" + itemId, config.getInt(itemId + ".customModelId", 0))
                    .addFlag(ItemFlag.HIDE_ATTRIBUTES);

            ItemMeta itemMeta = ci.getItemStack().getItemMeta();

            itemMeta.getPersistentDataContainer().set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);

            itemMeta.getPersistentDataContainer().set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, customItemType);

            if (customItemType.equalsIgnoreCase("heart")) itemMeta.getPersistentDataContainer().set(CUSTOM_HEART_VALUE_KEY, PersistentDataType.INTEGER, config.getInt( itemId + ".customHeartValue"));

            ci.getItemStack().setItemMeta(itemMeta);

            return ci.getItemStack();
        } catch (IllegalArgumentException ex) {
            LifeStealZ.getInstance().getLogger().log(Level.SEVERE, "Could not create custom item with id '" + itemId + "': " + ex.getMessage(), ex);
            return new CustomItem(Material.BARRIER).setName("&cInvalid Item").setLore(new ArrayList<>(List.of("&7This item is invalid or does not exist.", "&cCheck the server console for more information!"))).makeForbidden().getItemStack();
        }
    }

    /**
     * Creates a custom item with a specific amount
     *
     * @param itemId The id of the item
     * @param amount The amount of the item
     * @return The custom item
     */
    public static ItemStack createCustomItem(String itemId, int amount) {
        return new CustomItem(createCustomItem(itemId)).setAmount(amount).getItemStack();
    }

    /**
     * Creates the default heart item
     *
     * @return The default heart item
     */
    public static ItemStack createHeart() {
        return createCustomItem(LifeStealZ.getInstance().getConfig().getString("heartItem.default", "defaultheart"));
    }

    /**
     * Creates the withdraw heart item
     *
     * @return The withdraw heart item
     */
    public static ItemStack createWithdrawHeart() {
        return createCustomItem(LifeStealZ.getInstance().getConfig().getString("heartItem.withdraw", "defaultheart"));
    }

    /**
     * Creates the kill heart item
     *
     * @return The kill heart item
     */
    public static ItemStack createKillHeart() {
        return createCustomItem(LifeStealZ.getInstance().getConfig().getString("heartItem.kill", "defaultheart"));
    }

    /**
     * Creates the natural death heart item
     *
     * @return The natural death heart item
     */
    public static ItemStack createNaturalDeathHeart() {
        return createCustomItem(LifeStealZ.getInstance().getConfig().getString("heartItem.naturalDeath", "defaultheart"));
    }

    /**
     * Creates the revive heart item
     *
     * @return The revive heart item
     */
    public static ItemStack createHeartGainCooldownHeart() {
        return createCustomItem(LifeStealZ.getInstance().getConfig().getString("heartItem.heartGainCooldown", "defaultheart"));
    }

    /**
     * Creates the max hearts heart item
     *
     * @return The max hearts heart item
     */
    public static ItemStack createMaxHealthHeart() {
        return createCustomItem(LifeStealZ.getInstance().getConfig().getString("heartItem.maxHearts", "defaultheart"));
    }

    /**
     * Creates a close item for GUIs
     *
     * @return The close item
     */
    public static ItemStack createCloseItem() {
        CustomItem ci = new CustomItem(Material.BARRIER)
                .setName(MessageUtils.getAndFormatMsg(false, "closeBtn", "&cClose"))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                .makeForbidden();

        ci.getItemStack().setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addString("lifestealz_close").build()
        );

        return ci.getItemStack();
    }

    /**
     * Creates a back item for paginated GUIs
     *
     * @param page The page to go back to
     * @return The back item
     */
    public static ItemStack createBackItem(int page) {
        CustomItem ci = new CustomItem(Material.ARROW)
                .setName(MessageUtils.getAndFormatMsg(false, "backBtn", "&cBack"))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                .makeForbidden();

        ci.getItemStack().setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addString("lifestealz_back").build()
        );

        ItemMeta itemMeta = ci.getItemStack().getItemMeta();
        itemMeta.getPersistentDataContainer().set(REVIVE_PAGE_KEY, PersistentDataType.INTEGER, page);
        ci.getItemStack().setItemMeta(itemMeta);

        return ci.getItemStack();
    }

    /**
     * Creates a next item for paginated GUIs
     *
     * @param page The page to go to
     * @return The next item
     */
    public static ItemStack createNextItem(int page) {
        CustomItem ci = new CustomItem(Material.ARROW)
                .setName(MessageUtils.getAndFormatMsg(false, "nextBtn", "&cNext"))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                .makeForbidden();

        ci.getItemStack().setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addString("lifestealz_next").build()
        );

        ItemMeta itemMeta = ci.getItemStack().getItemMeta();
        itemMeta.getPersistentDataContainer().set(REVIVE_PAGE_KEY, PersistentDataType.INTEGER, page);
        ci.getItemStack().setItemMeta(itemMeta);

        return ci.getItemStack();
    }

    /**
     * Creates the totem used for the heart animation
     * @return The heart animation totem
     */
    public static ItemStack createHeartAnimationTotem() {
        CustomItem ci = new CustomItem(Material.TOTEM_OF_UNDYING)
                .setName(MessageUtils.getAndFormatMsg(false, "heartAnimationTotem", "&cHeart Animation"))
                .setLore(new ArrayList<>(List.of("&7This item is used for heart animations.")))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                .makeForbidden();

        ci.getItemStack().setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addString("lifestealz_totem").build()
        );

        return ci.getItemStack();
    }

    /**
     * Checks if an item is a heart item
     *
     * @param item The item to check
     * @return If the item is a heart item
     * @deprecated Use {@link CustomItemType#HEART} instead
     */
    @Deprecated
    public static boolean isHeartItem(ItemStack item) {
        return CustomItemType.HEART.is(item);
    }

    /**
     * Checks if an item is a revive item
     *
     * @param item The item to check
     * @return If the item is a revive item
     * @deprecated Use {@link CustomItemType#REVIVE} instead
     */
    @Deprecated
    public static boolean isReviveItem(ItemStack item) {
        return CustomItemType.REVIVE.is(item);
    }

    /**
     * Checks if an item is a forbidden item
     * @param item The item to check
     * @return If the item is a forbidden item
     */
    public static boolean isForbiddenItem(ItemStack item) {
        return getCustomItemId(item) != null && getCustomItemId(item).equals("forbidden");
    }

    /**
     * Checks if an item is a custom item (checks if it has a custom item id)
     * @param item The item to check
     * @return If the item is a custom item
     */
    public static boolean isCustomItem(ItemStack item) {
        return getCustomItemId(item) != null;
    }

    /**
     * Checks if an item is despawnable
     * @param item The item to check
     * @return If the item is despawnable
     */
    public static boolean isDespawnable(ItemStack item) {
        return item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(DESPAWNABLE_KEY, PersistentDataType.BOOLEAN)
                && item.getItemMeta().getPersistentDataContainer().get(DESPAWNABLE_KEY, PersistentDataType.BOOLEAN);
    }

    /**
     * Checks if an item is invulnerable
     * @param item The item to check
     * @return If the item is invulnerable
     */
    public static boolean isInvulnerable(ItemStack item) {
        return item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(INVULNERABLE_KEY, PersistentDataType.BOOLEAN)
                && item.getItemMeta().getPersistentDataContainer().get(INVULNERABLE_KEY, PersistentDataType.BOOLEAN);
    }

    /**
     * Gets the custom item id of an item
     *
     * @param item The item to get the id from
     * @return The custom item id
     */
    @Nullable
    public static String getCustomItemId(ItemStack item) {
        if (item.getItemMeta() == null || !item.getItemMeta().getPersistentDataContainer().has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING)) return null;
        else return item.getItemMeta().getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Gets the head of an offline player
     *
     * @param offlinePlayer The offline player
     * @return The head of the player
     */
    public static ItemStack getPlayerHead(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null || offlinePlayer.getName() == null) return new CustomItem(Material.SKELETON_SKULL).setName("&dUnknown").setLore(new ArrayList<>(List.of("&8" + UUID.randomUUID()))).getItemStack();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        skullMeta.displayName(Component.text("§d" + offlinePlayer.getName()));

        List<Component> lines = new ArrayList<>();
        lines.add(MessageUtils.getAndFormatMsg(false, "revivePlayerDesc", "&7Click to revive this player"));
        lines.add(MessageUtils.formatMsg("<dark_gray>" + offlinePlayer.getUniqueId()));

        skullMeta.lore(lines);
        skullMeta.setOwningPlayer(offlinePlayer);

        head.setItemMeta(skullMeta);
        return head;
    }

    /**
     * Gets a skeleton skull instead of a head
     *
     * @param uuid The uuid of the bedrock player
     * @return A skeleton skull
     */
    public static ItemStack getBedrockPlayerHead(UUID uuid) {
        ItemStack head = new ItemStack(Material.SKELETON_SKULL);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        skullMeta.displayName(Component.text("§d" + LifeStealZ.getInstance().getGeyserManager().getOfflineBedrockPlayerName(uuid)));

        List<Component> lines = new ArrayList<>();
        lines.add(MessageUtils.getAndFormatMsg(false, "revivePlayerDesc", "&7Click to revive this player"));
        lines.add(MessageUtils.formatMsg("<dark_gray>" + uuid));
        lines.add(MessageUtils.formatMsg("<dark_gray><i>This player is using the Bedrock Edition of Minecraft.</i>"));

        skullMeta.lore(lines);

        head.setItemMeta(skullMeta);
        return head;
    }

    /**
     * Gets the custom item data of an item
     *
     * @param itemId The id of the item
     * @return The custom item data
     */
    public static CustomItemData getCustomItemData(String itemId) {
        try {
            return new CustomItemData(itemId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}