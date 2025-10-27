package com.zetaplugins.lifestealz.util.customitems;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import com.zetaplugins.lifestealz.util.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.zetaplugins.lifestealz.util.customitems.CustomItemManager.*;

public final class CustomItem {
    private final ItemStack itemStack;

    public CustomItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public CustomItem(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public CustomItem() {
        this.itemStack = new ItemStack(Material.AIR);
    }

    public CustomItem setMaterial(Material material) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        int amount = itemStack.getAmount();
        ItemStack newItemStack = new ItemStack(material, amount);
        newItemStack.setItemMeta(itemMeta);
        return this;
    }

    public CustomItem setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public CustomItem setName(String name) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(MessageUtils.formatMsg(name));
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem setName(Component name) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(name);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem setCustomModelID(int customModelID) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(customModelID);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem setUnbreakable(boolean unbreakable) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setUnbreakable(unbreakable);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem setEnchanted(boolean enchanted) {
		if (!enchanted) return this;

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem addFlag(ItemFlag itemFlag) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(itemFlag);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem setDespawnable(boolean despawnable) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(DESPAWNABLE_KEY, PersistentDataType.BOOLEAN, despawnable);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem setInvulnerable(boolean invulnerable) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(INVULNERABLE_KEY, PersistentDataType.BOOLEAN, invulnerable);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem addEnchantment(Enchantment enchantment, int level) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addEnchant(enchantment, level, true);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public PersistentDataContainer getPersistentDataContainer() {
        return itemStack.getItemMeta().getPersistentDataContainer();
    }

    public CustomItem makeForbidden() {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, "forbidden");
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem setLore(List<String> lore) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<Component> newLore = new ArrayList<>(lore.size());
        for (String s : lore) {
            newLore.add(MessageUtils.formatMsg(s));
        }
        itemMeta.lore(newLore);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public CustomItem addLore(String lore) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<Component> newLore = new ArrayList<>(Objects.requireNonNull(itemMeta.lore()));
        newLore.add(MessageUtils.formatMsg(lore));
        itemMeta.lore(newLore);
        itemStack.setItemMeta(itemMeta);

        return this;
    }

    /**
     * Sets the custom item ID using Paper's CustomModelData DataComponent.
     * @param stringId The string ID of the custom item.
     * @param intId The integer ID of the custom item. Use 0 if not needed.
     * @return The current CustomItem instance for method chaining.
     */
    public CustomItem setId(String stringId, int intId) {
        var customModelDataBuilder = CustomModelData.customModelData().addString(stringId);
        if (intId != 0) customModelDataBuilder.addFloat(intId);

        itemStack.setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                customModelDataBuilder.build()
        );
        return this;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
