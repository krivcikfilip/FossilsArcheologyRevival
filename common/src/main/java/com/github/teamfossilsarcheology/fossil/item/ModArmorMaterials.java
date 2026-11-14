package com.github.teamfossilsarcheology.fossil.item;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * PORT(1.21.1): ArmorMaterial is a registered record since 1.20.5 (ArmorItem takes a {@code Holder<ArmorMaterial>}).
 * The durability is no longer part of the material: use {@link #durability(ArmorItem.Type, int)} or
 * {@code ArmorItem.Type#getDurability(int)} with {@link #ANCIENT_DURABILITY_MULTIPLIER}/{@link #BONE_DURABILITY_MULTIPLIER}
 * in the item properties.
 */
public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(FossilMod.MOD_ID, Registries.ARMOR_MATERIAL);

    public static final int ANCIENT_DURABILITY_MULTIPLIER = 15;
    public static final int BONE_DURABILITY_MULTIPLIER = 25;

    //Texture: fossil/textures/item/armor/ancient_helmet.png (geckolib, the layer is unused)
    public static final RegistrySupplier<ArmorMaterial> ANCIENT = register("ancient", Util.make(new EnumMap<>(ArmorItem.Type.class), enumMap -> {
        enumMap.put(ArmorItem.Type.BOOTS, 2);
        enumMap.put(ArmorItem.Type.LEGGINGS, 5);
        enumMap.put(ArmorItem.Type.CHESTPLATE, 6);
        enumMap.put(ArmorItem.Type.HELMET, 2);
    }), 9, SoundEvents.ARMOR_EQUIP_CHAIN, 0, 0, () -> Ingredient.of(ModItems.SCARAB_GEM.get()));
    //Texture: minecraft/textures/models/armor/bone_layer_{1,2}.png (same location as in 1.20.1)
    public static final RegistrySupplier<ArmorMaterial> BONE = register("bone", Util.make(new EnumMap<>(ArmorItem.Type.class), enumMap -> {
        enumMap.put(ArmorItem.Type.BOOTS, 2);
        enumMap.put(ArmorItem.Type.LEGGINGS, 7);
        enumMap.put(ArmorItem.Type.CHESTPLATE, 6);
        enumMap.put(ArmorItem.Type.HELMET, 2);
    }), 15, SoundEvents.ARMOR_EQUIP_CHAIN, 0, 0, () -> Ingredient.of(Items.BONE));

    private static RegistrySupplier<ArmorMaterial> register(String name, EnumMap<ArmorItem.Type, Integer> defense, int enchantmentValue,
                                                            Holder<SoundEvent> sound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        //1.20.1 used the material name without namespace for the armor texture -> minecraft namespace
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace(name)));
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(defense, enchantmentValue, sound, repairIngredient, layers, toughness, knockbackResistance));
    }

    /**
     * Same values as the 1.20.1 {@code HEALTH_FUNCTION_FOR_TYPE * durabilityMultiplier}
     */
    public static int durability(ArmorItem.Type type, int durabilityMultiplier) {
        return type.getDurability(durabilityMultiplier);
    }

    public static void register() {
        ARMOR_MATERIALS.register();
    }
}
