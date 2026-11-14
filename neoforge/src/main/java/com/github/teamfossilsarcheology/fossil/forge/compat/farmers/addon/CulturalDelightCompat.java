package com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon;

//import com.baisylia.culturaldelights.block.ModBlocks;
//import com.baisylia.culturaldelights.item.ModItems;
import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;

//import java.util.Objects;

/**
 * PORT-TODO(1.21.1): disabled until verified against a 1.21.1 jar. The add-on has a 1.21.1 NeoForge release on Modrinth (cultural-delights 0.19.5-1.21.1).
 * The original 1.20.1 code is kept below as line comments; re-enable by uncommenting, adding the dependency in
 * neoforge/build.gradle and fixing renamed registry fields (DeferredHolder#get still works for NeoForge registries).
 */
public class CulturalDelightCompat {
    public static void registerFoodMappings(FoodMappingsManager manager) {
    }

//    public static void registerFoodMappings(FoodMappingsManager manager) {
//        manager.addPlant(ModItems.CUCUMBER_SEEDS.get(), 5);
//        manager.addPlant(ModItems.CORN_KERNELS.get(), 5);
//        manager.addPlant(ModItems.EGGPLANT_SEEDS.get(), 5);
//        manager.addPlant(ModItems.AVOCADO.get());
//        manager.addPlant(ModItems.CUT_AVOCADO.get());
//        manager.addPlant(ModItems.CUCUMBER.get());
//        manager.addPlant(ModItems.PICKLE.get());
//        manager.addPlant(ModItems.CUT_CUCUMBER.get());
//        manager.addPlant(ModItems.CUT_PICKLE.get());
//        manager.addPlant(ModItems.EGGPLANT.get());
//        manager.addPlant(ModItems.CUT_EGGPLANT.get());
//        manager.addPlant(ModItems.SMOKED_EGGPLANT.get());
//        manager.addPlant(ModItems.SMOKED_TOMATO.get());
//        manager.addPlant(ModItems.SMOKED_CUT_EGGPLANT.get());
//        manager.addPlant(ModItems.SMOKED_WHITE_EGGPLANT.get());
//        manager.addPlant(ModItems.WHITE_EGGPLANT.get());
//        manager.addPlant(ModItems.CORN_COB.get());
//        manager.addFish(ModItems.SQUID.get());
//        manager.addFish(ModItems.COOKED_SQUID.get());
//        manager.addFish(ModItems.RAW_CALAMARI.get());
//        manager.addFish(ModItems.COOKED_CALAMARI.get());
//        manager.addPlant(ModItems.POPCORN.get());
//        manager.addPlant(ModItems.CORN_DOUGH.get());
//        manager.addPlant(ModItems.TORTILLA.get());
//        manager.addPlant(ModItems.TORTILLA_CHIPS.get());
//        manager.addPlant(ModItems.ELOTE.get());
//        manager.addPlant(ModItems.HEARTY_SALAD.get());
//        manager.addMeat(ModItems.BEEF_BURRITO.get());
//        manager.addMeat(ModItems.MUTTON_SANDWICH.get());
//        manager.addPlant(ModItems.EGGPLANT_PARMESAN.get());
//        manager.addPlant(ModItems.EGGPLANT_BURGER.get()); //This has no meat in it so I consider it a plant.
//        manager.addPlant(ModItems.CREAMED_CORN.get());
//        manager.addPlant(ModItems.CORN_COB.get());
//        manager.addMeat(ModItems.CHICKEN_TACO.get());
//        manager.addMeat(ModItems.SPICY_CURRY.get());
//        manager.addMeat(ModItems.PORK_WRAP.get());
//        manager.addFish(ModItems.FISH_TACO.get());
//        manager.addPlant(ModItems.MIDORI_ROLL.get());
//        manager.addPlant(ModItems.MIDORI_ROLL_SLICE.get());
//        manager.addEgg(ModItems.EGG_ROLL.get());
//        manager.addMeat(ModItems.CHICKEN_ROLL.get());
//        manager.addMeat(ModItems.CHICKEN_ROLL_SLICE.get());
//        manager.addFish(ModItems.PUFFERFISH_ROLL.get());
//        manager.addFish(ModItems.TROPICAL_ROLL.get());
//        manager.addFish(ModItems.RICE_BALL.get()); //This has fish in it so I count it as fish
//        manager.addFish(ModItems.CALAMARI_ROLL.get());
//        manager.addPlant(ModBlocks.AVOCADO_LEAVES.get(), 20);
//        manager.addPlant(ModBlocks.AVOCADO_SAPLING.get(), 15);
//        manager.addPlant(ModItems.AVOCADO_TOAST.get());
//        manager.addPlant(ModItems.ITEMS.getEntries().stream().filter(v -> Objects.equals(v.get().toString(), "avocado_pit")).toList().get(0).get(), 5);
//        //51/51 added! That is a lot of food!
//    }
}
