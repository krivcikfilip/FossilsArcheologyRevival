package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

//import club.someoneice.cofe_delight.init.ItemInit;
import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (coffee-delight 1.4.1).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class CoffeeDelightCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(ItemInit.COFFEE_BERRIES.get(), 5);
//        manager.addPlant(ItemInit.COFFEE_FLOUR.get(), 15);
//        manager.addPlant(ItemInit.COFFEE_BEANS.get());
//        manager.addPlant(ItemInit.COFFEE_BEANS_COOKED.get());
//        manager.addPlant(ItemInit.COFFEE_PIE_SIDE.get());
//        manager.addPlant(ItemInit.COFFEE_PIE.get()); //Not a block!
//        manager.addPlant(ItemInit.QUICHE.get());
//        manager.addPlant(ItemInit.WAFER.get());
//        manager.addPlant(ItemInit.BERRIES_WAFER.get());
//        manager.addPlant(ItemInit.COFFEE_WAFER.get());
//        //10/10 added
//    }
}
