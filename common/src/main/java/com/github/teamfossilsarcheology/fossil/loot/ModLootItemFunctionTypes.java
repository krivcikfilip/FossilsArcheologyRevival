package com.github.teamfossilsarcheology.fossil.loot;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public class ModLootItemFunctionTypes {

    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTION_TYPES = DeferredRegister.create(FossilMod.MOD_ID, Registries.LOOT_FUNCTION_TYPE);

    //PORT(1.21.1): LootItemFunctionType is generic and takes a MapCodec (was a Gson Serializer)
    public static final RegistrySupplier<LootItemFunctionType<CustomizeToDinoFunction>> CUSTOMIZE_TO_DINOSAUR = register("customize_to_dinosaur", CustomizeToDinoFunction.CODEC);

    private static <T extends LootItemFunction> RegistrySupplier<LootItemFunctionType<T>> register(String name, MapCodec<T> codec) {
        return LOOT_FUNCTION_TYPES.register(name, () -> new LootItemFunctionType<>(codec));
    }

    public static void register() {
        LOOT_FUNCTION_TYPES.register();
    }
}
