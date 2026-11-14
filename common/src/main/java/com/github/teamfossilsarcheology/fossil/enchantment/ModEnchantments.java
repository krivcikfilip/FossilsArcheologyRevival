package com.github.teamfossilsarcheology.fossil.enchantment;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Optional;

/**
 * PORT(1.21.1): Enchantments are a datapack registry since 1.21. The old Enchantment subclasses
 * (ArcheologyEnchantment/PaleontologyEnchantment) are replaced by data/fossil/enchantment/{archeology,paleontology}.json.
 * Their behaviour is unchanged: the effect is implemented in the fossil block loot tables (match_tool with enchantment predicate).
 * <p>
 * Old values: Rarity.VERY_RARE (weight 1, anvil cost 8), EnchantmentCategory.DIGGER (#minecraft:enchantable/mining_loot),
 * max level 3, min cost 1 + 10 * (level - 1), max cost min cost + 50, MAINHAND, incompatible with silk touch and each other.
 */
public class ModEnchantments {
    public static final ResourceKey<Enchantment> ARCHEOLOGY = key("archeology");
    public static final ResourceKey<Enchantment> PALEONTOLOGY = key("paleontology");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, FossilMod.location(name));
    }

    /**
     * For datagen (RegistrySetBuilder.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap))
     */
    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
        register(context, ARCHEOLOGY, Enchantment.enchantment(Enchantment.definition(items.getOrThrow(ItemTags.MINING_LOOT_ENCHANTABLE), 1, 3,
                        Enchantment.dynamicCost(1, 10), Enchantment.dynamicCost(51, 10), 8, EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.SILK_TOUCH), enchantments.getOrThrow(PALEONTOLOGY))));
        register(context, PALEONTOLOGY, Enchantment.enchantment(Enchantment.definition(items.getOrThrow(ItemTags.MINING_LOOT_ENCHANTABLE), 1, 3,
                        Enchantment.dynamicCost(1, 10), Enchantment.dynamicCost(51, 10), 8, EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.SILK_TOUCH), enchantments.getOrThrow(ARCHEOLOGY))));
    }

    private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }

    /**
     * Resolves an enchantment holder. Empty if the enchantment is not loaded (e.g. disabled by a datapack)
     */
    public static Optional<Holder.Reference<Enchantment>> get(HolderLookup.Provider registries, ResourceKey<Enchantment> key) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(key);
    }

    /**
     * Replacement for {@code EnchantmentHelper.getItemEnchantmentLevel(Enchantment, ItemStack)} with static enchantment fields
     */
    public static int getItemEnchantmentLevel(HolderLookup.Provider registries, ResourceKey<Enchantment> key, ItemStack stack) {
        return get(registries, key).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }

    public static void register() {
        //PORT(1.21.1): Nothing to register anymore (data driven). Kept so that FossilMod#init does not change.
    }
}
