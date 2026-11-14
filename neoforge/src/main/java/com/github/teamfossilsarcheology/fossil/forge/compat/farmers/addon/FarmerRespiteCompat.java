package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import umpaz.farmersrespite.common.registry.FRBlocks;
//import umpaz.farmersrespite.common.registry.FRItems;
//import vectorwing.farmersdelight.common.block.PieBlock;

//import static com.github.teamfossilsarcheology.fossil.forge.compat.farmers.FarmersDelightCompat.getPieValue;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has no 1.21.1 NeoForge release on Modrinth (checked during the port; CurseForge not checked).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class FarmerRespiteCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(FRBlocks.WILD_TEA_BUSH.get(), 20);
//        manager.addPlant(FRItems.TEA_SEEDS.get(), 5);
//        manager.addPlant(FRItems.GREEN_TEA_LEAVES.get(), 5);
//        manager.addPlant(FRItems.YELLOW_TEA_LEAVES.get(), 5);
//        manager.addPlant(FRItems.BLACK_TEA_LEAVES.get(), 5);
//        manager.addPlant(FRItems.COFFEE_BERRIES.get());
//        manager.addPlant(FRItems.ROSE_HIPS.get(), 5);
//        manager.addPlant(FRItems.GREEN_TEA_COOKIE.get());
//        manager.addPlant(FRItems.NETHER_WART_SOURDOUGH.get());
//        manager.addFish(FRItems.BLACK_COD.get());
//        manager.addPlant(FRItems.TEA_CURRY.get());
//        manager.addMeat(FRItems.BLAZING_CHILI.get());
//        manager.addPlant(FRItems.COFFEE_CAKE.get(), 7 * 7 * 3);
//        manager.addPlant(FRItems.COFFEE_CAKE_SLICE.get());
//        manager.addPlant(FRItems.ROSE_HIP_PIE_SLICE.get());
//        manager.addPlant(FRItems.ROSE_HIP_PIE.get(), getPieValue((PieBlock) FRBlocks.ROSE_HIP_PIE.get()));
//    }
}
