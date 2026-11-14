package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import net.brnbrd.delightful.common.item.DelightfulItems;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has no 1.21.1 NeoForge release on Modrinth (checked during the port; CurseForge not checked).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class BYGAndDelightfuLCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(DelightfulItems.SALMONBERRY_PIE_SLICE.get());
//        manager.addPlant(DelightfulItems.GLOOMGOURD_PIE_SLICE.get());
//        manager.addPlant(DelightfulItems.BLUEBERRY_PIE_SLICE.get());
//        manager.addPlant(DelightfulItems.GREEN_APPLE_PIE_SLICE.get());
//        manager.addPlant(DelightfulItems.MULBERRY_PIE_SLICE.get());
//        manager.addPlant(DelightfulItems.PASSION_FRUIT_TART_SLICE.get());
//        manager.addPlant(DelightfulItems.CHORUS_PIE_SLICE.get());
//        manager.addPlant(DelightfulItems.SOURCE_BERRY_PIE_SLICE.get());
//        //8/8 added
//    }
}
