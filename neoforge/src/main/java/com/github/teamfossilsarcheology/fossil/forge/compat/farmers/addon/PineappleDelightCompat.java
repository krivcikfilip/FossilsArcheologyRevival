package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import com.github.teamfossilsarcheology.fossil.food.FoodType;
//import net.minecraft.core.registries.BuiltInRegistries;
//import net.minecraft.resources.ResourceLocation;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (pineapple-delight 1.1.4-1.21.1-neoforge).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class PineappleDelightCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        String modId = "pineapple_delight";
//        BuiltInRegistries.ITEM.getOptional(new ResourceLocation(modId, "pineapple")).ifPresent(manager::addPlant);
//        BuiltInRegistries.ITEM.getOptional(new ResourceLocation(modId, "pineapple_fried_rice")).ifPresent(manager::addPlant);
//        BuiltInRegistries.ITEM.getOptional(new ResourceLocation(modId, "pineapple_pie_side")).ifPresent(manager::addPlant);
//        BuiltInRegistries.ITEM.getOptional(new ResourceLocation(modId, "pineapple_side")).ifPresent(manager::addPlant);
//        BuiltInRegistries.ITEM.getOptional(new ResourceLocation(modId, "pineapple_pie")).ifPresent(manager::addPlant);
//        BuiltInRegistries.ITEM.getOptional(new ResourceLocation(modId, "pineapple_cake_slice")).ifPresent(manager::addPlant);
//        BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(modId, "pineapple_pie")).ifPresent(block -> {
//            manager.addPlant(block.asItem(), 4 * FoodType.PLANT.multiplier());
//        });
//        BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(modId, "pineapple_cake")).ifPresent(block -> {
//            manager.addPlant(block.asItem(), 3 * 4 * FoodType.PLANT.multiplier());
//        });
//        BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(modId, "pineapple_crop")).ifPresent(block -> {
//            manager.addPlant(block.asItem(), FoodType.PLANT.multiplier());
//        });
//        BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(modId, "pineapple_wild_crop")).ifPresent(block -> {
//            manager.addPlant(block.asItem(), FoodType.PLANT.multiplier());
//        });
//    }
}
