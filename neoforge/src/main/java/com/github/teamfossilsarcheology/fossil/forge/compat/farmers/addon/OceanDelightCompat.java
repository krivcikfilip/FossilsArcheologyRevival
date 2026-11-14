package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import com.scouter.oceansdelight.items.ODItems;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (oceans-delight 1.0.4 (neoforge build)).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class OceanDelightCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addFish(ODItems.TENTACLES.get());
//        manager.addFish(ODItems.CUT_TENTACLES.get());
//        manager.addFish(ODItems.SQUID_RINGS.get());
//        manager.addFish(ODItems.TENTACLE_ON_A_STICK.get());
//        manager.addFish(ODItems.BAKED_TENTACLE_ON_A_STICK.get());
//        manager.addFish(ODItems.BOWL_OF_GUARDIAN_SOUP.get());
//        manager.addFish(ODItems.GUARDIAN_TAIL.get());
//        manager.addFish(ODItems.COOKED_GUARDIAN_TAIL.get());
//        manager.addFish(ODItems.ELDER_GUARDIAN_SLAB.get());
//        manager.addFish(ODItems.ELDER_GUARDIAN_SLICE.get());
//        manager.addFish(ODItems.COOKED_ELDER_GUARDIAN_SLICE.get());
//        manager.addFish(ODItems.ELDER_GUARDIAN_ROLL.get());
//        manager.addFish(ODItems.CABBAGE_WRAPPED_ELDER_GUARDIAN.get());
//        manager.addFish(ODItems.FUGU_SLICE.get());
//        manager.addFish(ODItems.FUGU_ROLL.get());
//        manager.addPlant(ODItems.BRAISED_SEA_PICKLE.get());
//        //16/16 added
//    }
}
