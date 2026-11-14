package com.github.teamfossilsarcheology.fossil.forge.data;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.forge.data.advancements.FossilAdvancements;
import com.github.teamfossilsarcheology.fossil.forge.data.providers.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * PORT(1.21.1): providers that write registry-dependent data now receive the {@code CompletableFuture<HolderLookup.Provider>}
 * (recipes, loot tables). ForgeAdvancementProvider -> NeoForge AdvancementProvider.
 * <p>
 * Phase 2: wipe {@code common/src/generated/resources/{data,assets}} before re-running datagen, the old cache still lists the
 * 1.20 (plural) folder names.
 */
@EventBusSubscriber(modid = FossilMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void register(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper efh = event.getExistingFileHelper();
        //Datapack registries (biomes, enchantments, jukebox songs...). Its lookup also contains our own entries, which the loot
        //tables need (archeology/paleontology enchantments)
        var worldGen = new ModWorldGenProvider(generator.getPackOutput(), lookup);
        generator.addProvider(event.includeServer(), worldGen);
        CompletableFuture<HolderLookup.Provider> fullLookup = worldGen.getRegistryProvider();
        generator.addProvider(event.includeClient(), (DataProvider.Factory<DataProvider>) output -> new ModBlockStateProvider(output, efh));
        generator.addProvider(event.includeClient(), (DataProvider.Factory<DataProvider>) output -> new ModItemProvider(output, efh));
        var blockTags = new ModBlockTagsProvider(generator.getPackOutput(), lookup, efh);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), (DataProvider.Factory<DataProvider>) output -> new ModItemTagsProvider(output, lookup, blockTags.contentsGetter(), efh));
        generator.addProvider(event.includeServer(), (DataProvider.Factory<DataProvider>) output -> new ModEntityTypeTagsProvider(output, lookup, efh));
        generator.addProvider(event.includeServer(), (DataProvider.Factory<DataProvider>) output -> new ModRecipeProvider(output, fullLookup));
        generator.addProvider(event.includeServer(), (DataProvider.Factory<DataProvider>) output -> new AdvancementProvider(
                output, lookup, efh, List.of(new FossilAdvancements())));
        generator.addProvider(event.includeServer(), (DataProvider.Factory<DataProvider>) output -> new ModLootProvider(output, fullLookup));
        generator.addProvider(event.includeServer(), (DataProvider.Factory<DataProvider>) ModFoodValueProvider::new);
    }
}
