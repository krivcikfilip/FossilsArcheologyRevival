package com.github.teamfossilsarcheology.fossil.forge.data.providers;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.entity.damagesource.ModDamageTypes;
import com.github.teamfossilsarcheology.fossil.forge.world.biome.ForgeFossilBiomeModifiers;
import com.github.teamfossilsarcheology.fossil.world.biome.ModBiomes;
import com.github.teamfossilsarcheology.fossil.world.dimension.ModDimensions;
import com.github.teamfossilsarcheology.fossil.world.feature.configuration.ModConfiguredFeatures;
import com.github.teamfossilsarcheology.fossil.world.feature.placement.ModPlacedFeatures;
import com.github.teamfossilsarcheology.fossil.world.feature.structures.ModStructureSets;
import com.github.teamfossilsarcheology.fossil.world.feature.structures.ModStructures;
import com.github.teamfossilsarcheology.fossil.world.feature.structures.ModTemplatePools;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import com.github.teamfossilsarcheology.fossil.enchantment.ModEnchantments;
import com.github.teamfossilsarcheology.fossil.sounds.ModJukeboxSongs;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

//PORT(1.21.1, verified phase 2): every bootstrap(...) above must take net.minecraft.data.worldgen.BootstrapContext (renamed from BootstapContext -> datagen runs
// in 1.20.5); world/entity owners still had the old name when this was written
public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
            .add(Registries.STRUCTURE, ModStructures::bootstrap)
            .add(Registries.STRUCTURE_SET, ModStructureSets::bootstrap)
            .add(Registries.BIOME, ModBiomes::bootstrap)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ForgeFossilBiomeModifiers::bootstrap)
            .add(Registries.TEMPLATE_POOL, ModTemplatePools::bootstrap)
            .add(Registries.DIMENSION_TYPE, ModDimensions::bootstrap)
            .add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap)
            //PORT(1.21.1): enchantments (1.21) and jukebox songs (1.21) are data driven; bootstraps provided by core
            .add(Registries.ENCHANTMENT, ModEnchantments::bootstrap)
            .add(Registries.JUKEBOX_SONG, ModJukeboxSongs::bootstrap);

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(FossilMod.MOD_ID));
    }
}
