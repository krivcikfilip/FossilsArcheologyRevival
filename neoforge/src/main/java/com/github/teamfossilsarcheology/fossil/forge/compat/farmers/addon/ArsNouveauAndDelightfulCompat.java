package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
//import net.brnbrd.delightful.common.block.DelightfulBlocks;
//import net.brnbrd.delightful.common.item.DelightfulItems;

//import static com.github.teamfossilsarcheology.fossil.forge.compat.farmers.FarmersDelightCompat.getPieValue;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has no 1.21.1 NeoForge release on Modrinth (checked during the port; CurseForge not checked).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class ArsNouveauAndDelightfulCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(ItemsRegistry.SOURCE_BERRY_PIE.get(), getPieValue(DelightfulBlocks.SOURCE_BERRY_PIE.get()));
//        manager.addPlant(DelightfulItems.SOURCE_BERRY_PIE_SLICE.get());
//        //2/2 added
//    }
}
