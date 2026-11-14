package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import umpaz.brewinandchewin.common.registry.BnCItems;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (brewin-and-chewin 4.5.0+1.21.1-neoforge).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class BrewinAndChewinCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(BnCItems.KIMCHI.get());
//        manager.addMeat(BnCItems.JERKY.get());
//        manager.addPlant(BnCItems.PICKLED_PICKLES.get());
//        manager.addFish(BnCItems.KIPPERS.get());
//        manager.addPlant(BnCItems.COCOA_FUDGE.get());
//        manager.addPlant(BnCItems.KIMCHI.get());
//        manager.addMeat(BnCItems.PIZZA_SLICE.get()); //Contains meat
//        manager.addMeat(BnCItems.HAM_AND_CHEESE_SANDWICH.get());
//        //8/8 added
//    }
}
