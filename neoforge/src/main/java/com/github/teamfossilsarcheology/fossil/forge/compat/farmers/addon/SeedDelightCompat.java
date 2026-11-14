package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import org.hiedacamellia.seeddelight.registry.BlockRegistry;
//import org.hiedacamellia.seeddelight.registry.ItemRegistry;
//import vectorwing.farmersdelight.common.block.PieBlock;

//import static com.github.teamfossilsarcheology.fossil.forge.compat.farmers.FarmersDelightCompat.getPieValue;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (seed-delight 1.0.1 (neoforge build)).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class SeedDelightCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(ItemRegistry.Acorn.get(), 5);
//        manager.addPlant(ItemRegistry.AcornKernel.get());
//        manager.addPlant(ItemRegistry.Pinecone.get(), 5);
//        manager.addPlant(ItemRegistry.PineNutKernel.get());
//        manager.addPlant(ItemRegistry.SunflowerSeed.get(), 5);
//        manager.addPlant(ItemRegistry.FriedSunflowerSeed.get());
//        manager.addPlant(ItemRegistry.DriedWatermelonSeed.get());
//        manager.addPlant(ItemRegistry.DriedWatermelonSeed.get());
//        manager.addPlant(ItemRegistry.Rosehip.get());
//        manager.addPlant(ItemRegistry.Cherry.get());
//        manager.addPlant(ItemRegistry.RawAcornNoodle.get());
//        manager.addPlant(ItemRegistry.RosehipJamSandwich.get());
//        manager.addPlant(ItemRegistry.CherryJamSandwich.get());
//        manager.addPlant(ItemRegistry.SunflowerSeedCrisp.get());
//        manager.addPlant(ItemRegistry.SunflowerSeedToast.get());
//        manager.addPlant(ItemRegistry.SeedRosehipPie.get());
//        manager.addMeat(ItemRegistry.RoastedBeefWithSeed.get());
//        manager.addPlant(ItemRegistry.AcornTofu.get());
//        manager.addPlant(ItemRegistry.StirFriedCabbageWithAcorn.get());
//        manager.addPlant(ItemRegistry.AcornBread.get());
//        manager.addPlant(ItemRegistry.PinenutGruel.get());
//        manager.addPlant(ItemRegistry.PinenutCake.get());
//        manager.addPlant(ItemRegistry.SeedTart.get());
//        manager.addMeat(ItemRegistry.PinenutWithMeatballs.get());
//        manager.addPlant(ItemRegistry.RoseCookie.get());
//        manager.addPlant(ItemRegistry.RosehipCake.get());
//        manager.addMeat(ItemRegistry.CherryPork.get());
//        manager.addPlant(ItemRegistry.MilkCherryMouss.get());
//        manager.addPlant(ItemRegistry.RosehipPie.get(), getPieValue((PieBlock) BlockRegistry.RosehipPie.get()));
//        manager.addPlant(ItemRegistry.RosehipPieSlice.get());
//        //30/30 added
//    }
}
