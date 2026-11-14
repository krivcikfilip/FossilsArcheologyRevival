package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

//import cn.mcmod.corn_delight.item.ItemRegistry;
import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (corn-delight 1.2.10-1.21.1).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class CornDelightCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(ItemRegistry.BOILED_CORN.get());
//        manager.addPlant(ItemRegistry.CARAMEL_POPCORN.get());
//        manager.addMeat(ItemRegistry.CLASSIC_CORN_DOG.get());
//        manager.addPlant(ItemRegistry.CORN.get());
//        manager.addMeat(ItemRegistry.CORN_DOG.get());
//        manager.addPlant(ItemRegistry.CORN_SEEDS.get(), 5);
//        manager.addPlant(ItemRegistry.CORN_SOUP.get());
//        manager.addPlant(ItemRegistry.CORNBREAD.get());
//        manager.addPlant(ItemRegistry.CORNBREAD_BATTER.get());
//        manager.addPlant(ItemRegistry.CORNBREAD_STUFFING.get());
//        manager.addPlant(ItemRegistry.CREAMED_CORN.get());
//        manager.addPlant(ItemRegistry.GRILLED_CORN.get());
//        manager.addMeat(ItemRegistry.NACHOS.get()); //The recipe has meat in it
//        manager.addPlant(ItemRegistry.POPCORN.get()); //This is made from seeds so it is okay for it to be eaten by dinos
//        manager.addPlant(ItemRegistry.RAW_TORTILLA.get());
//        manager.addMeat(ItemRegistry.TACO.get());
//        manager.addPlant(ItemRegistry.TORTILLA.get());
//        manager.addPlant(ItemRegistry.TORTILLA_CHIP.get());
//        //18/18 added
//    }
}
