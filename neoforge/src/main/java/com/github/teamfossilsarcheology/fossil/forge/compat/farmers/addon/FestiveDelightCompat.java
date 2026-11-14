package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import net.toopa.festivedelight.init.FestiveDelightModBlocks;
//import net.toopa.festivedelight.init.FestiveDelightModItems;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (festive-delight 2.0.0 (neoforge build)).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class FestiveDelightCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addMeat(FestiveDelightModItems.FESTIVE_CHIKEN.get());
//        manager.addPlant(FestiveDelightModItems.CINNAMON_POWDER.get(), 10);
//        manager.addPlant(FestiveDelightModItems.GINGERBREAD_DOUGH.get(), 10);
//        manager.addPlant(FestiveDelightModItems.GINGERBREAD_MAN_DOUGH.get(), 10);
//        manager.addPlant(FestiveDelightModItems.GINGERBREAD_COOKIE_DOUGH_CREEPER.get(), 10);
//        manager.addPlant(FestiveDelightModItems.GINGERBREAD_COOKIE_CREEPER.get());
//        manager.addPlant(FestiveDelightModItems.GINGERBREAD_MAN.get());
//        manager.addPlant(FestiveDelightModItems.GINGERBREAD_MAN_BASE.get());
//        //manager.addPlant(FestiveDelightModItems.SUGAR_CANE.get()); Not suitable for dinos
//        manager.addPlant(FestiveDelightModBlocks.CINNAMON_BUSH.get(), 15);
//        //14/15 added, 1 excluded
//    }
}
