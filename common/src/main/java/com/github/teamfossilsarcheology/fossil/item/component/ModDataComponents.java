package com.github.teamfossilsarcheology.fossil.item.component;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * All custom {@link DataComponentType ItemStack data components} of the mod (1.20.5+ replacement for ItemStack NBT).
 * <p>
 * Vanilla components that replace other old NBT usages (see PORT_NOTES/API.md):
 * <ul>
 *     <li>Bucketed entities ({@code Bucketable#saveToBucketTag}): {@code DataComponents.BUCKET_ENTITY_DATA} via {@code CustomData.update}</li>
 *     <li>Entity data on spawn items ({@code EntityTag}): {@code DataComponents.ENTITY_DATA}</li>
 *     <li>Enchantments: {@code DataComponents.ENCHANTMENTS} / {@code EnchantmentHelper}</li>
 *     <li>Generic copy of "all NBT" from one stack to another: {@code target.applyComponents(source.getComponentsPatch())}</li>
 * </ul>
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(FossilMod.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    /**
     * Replaces the {@code "LaserEntityUUID"} NBT key of the laser pointer. UUID of the spawned {@code LaserPointEntity}.
     */
    public static final RegistrySupplier<DataComponentType<UUID>> LASER_ENTITY = register("laser_entity",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /**
     * Replaces the {@code "UseStartTime"} NBT key of the laser pointer. Game time at which the player started using the item.
     */
    public static final RegistrySupplier<DataComponentType<Long>> LASER_USE_START_TIME = register("laser_use_start_time",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    private static <T> RegistrySupplier<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return DATA_COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    public static void register() {
        DATA_COMPONENTS.register();
    }
}
