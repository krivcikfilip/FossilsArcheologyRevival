package com.github.teamfossilsarcheology.fossil.world.feature.structures;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * PORT(1.21.1): 1.20.1 called {@code Registry.register(BuiltInRegistries.STRUCTURE_TYPE, ...)} from a static initializer that ran
 * during common setup. On NeoForge the vanilla registries are frozen by then, so the structure types now use an Architectury
 * {@link DeferredRegister}. {@link #register()} must be called from the mod constructor ({@code FossilMod.init()}), like the other
 * deferred registers. It is idempotent, so the extra call in the NeoForge common setup is a harmless no-op.
 * StructureType#codec returns a {@link MapCodec} since 1.20.5.
 */
public class ModStructureType {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(FossilMod.MOD_ID, Registries.STRUCTURE_TYPE);

    public static final RegistrySupplier<StructureType<AnuCastleStructure>> ANU_CASTLE = register("anu_castle", AnuCastleStructure.CODEC);
    public static final RegistrySupplier<StructureType<HellBoatStructure>> HELL_BOAT = register("hell_boat", HellBoatStructure.CODEC);
    public static final RegistrySupplier<StructureType<TreasureRoomStructure>> TREASURE_ROOM = register("treasure_room", TreasureRoomStructure.CODEC);

    private static boolean registered;

    private static <S extends Structure> RegistrySupplier<StructureType<S>> register(String name, MapCodec<S> codec) {
        return STRUCTURE_TYPES.register(name, () -> () -> codec);
    }

    //PORT-TODO(1.21.1): needs core to call ModStructureType.register() from FossilMod.init() (see PORT_NOTES/world.md); a first call
    // from FMLCommonSetupEvent would make Architectury Registry.register into the frozen registry and crash
    public static void register() {
        if (registered) {
            //PORT(1.21.1): also called from the NeoForge common setup (too late for a first registration)
            return;
        }
        registered = true;
        STRUCTURE_TYPES.register();
    }
}
