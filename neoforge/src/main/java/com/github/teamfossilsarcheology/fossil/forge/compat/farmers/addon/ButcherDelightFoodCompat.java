package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
//import net.mcreator.butchersdelightfoods.init.ButchersdelightfoodsModItems;
//import net.minecraft.world.item.Item;
//import net.minecraftforge.registries.RegistryObject;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has no 1.21.1 NeoForge release on Modrinth (checked during the port; CurseForge not checked).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class ButcherDelightFoodCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        for (RegistryObject<Item> meat : ButchersdelightfoodsModItems.REGISTRY.getEntries()) {
//            manager.addMeat(meat.get());
//        }
//        //This is safe to do because literally all Butcher Delight Foods items are raw and cooked meat
//    }
}
