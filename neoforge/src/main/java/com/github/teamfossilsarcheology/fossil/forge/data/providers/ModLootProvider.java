package com.github.teamfossilsarcheology.fossil.forge.data.providers;

import com.github.teamfossilsarcheology.fossil.forge.data.loot.ModBlockLootTables;
import com.github.teamfossilsarcheology.fossil.forge.data.loot.ModEntityLootTables;
import com.github.teamfossilsarcheology.fossil.forge.data.loot.ModGenericLootTables;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * PORT(1.21.1): LootTableProvider takes the registry lookup, sub providers are created with it. The old validate override
 * (validate every table, no "missing table" check) is what NeoForge's default {@code validate} does with an empty required set.
 */
public class ModLootProvider extends LootTableProvider {
    public ModLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK),
                new SubProviderEntry(ModEntityLootTables::new, LootContextParamSets.ENTITY),
                new SubProviderEntry(ModGenericLootTables::new, LootContextParamSets.ALL_PARAMS)), registries);
    }
}
