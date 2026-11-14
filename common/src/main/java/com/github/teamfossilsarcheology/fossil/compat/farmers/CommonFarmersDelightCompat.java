package com.github.teamfossilsarcheology.fossil.compat.farmers;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.util.ModConstants;
import dev.architectury.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Safe to call if mob not loaded
 * <p>
 * PORT(1.21.1): currently not called from anywhere (it was already unused in 1.20.1). The conflicting egg recipes are now handled by
 * {@code neoforge:conditions} (mod_loaded / not mod_loaded farmersdelight) in data/fossil/recipe/fa_cookable_eggs_to_*.json.
 * Kept (API-ported) so the feature is not lost.
 */
public class CommonFarmersDelightCompat {

    public static void removeConflictingRecipes(MinecraftServer server) {
        //PORT(1.21.1): RecipeManager#getRecipes returns an immutable view of RecipeHolders now (was a mutable copy of Recipes) -> copy it.
        //Ids come from RecipeHolder#id() (Recipe#getId() is gone)
        //Signatures as used by JEI 1.21.1 (JustEnoughItemsClient: List.copyOf(getRecipes()), JeiStarter: replaceRecipes(List<RecipeHolder<?>>))
        List<RecipeHolder<?>> recipes = new ArrayList<>(server.getRecipeManager().getRecipes());
        if (Platform.isModLoaded(ModConstants.FARMERS)) {
            FossilMod.LOGGER.info("Farmer's Delight detected and incompatible recipes removed.");
            recipes.removeIf(recipe -> {
                String name = recipe.id().getPath();
                return name.startsWith("egg_to_cooked_egg") || name.startsWith("dino_eggs_to_cooked_egg");
            });
        } else {
            recipes.removeIf(recipe -> {
                String name = recipe.id().getPath();
                return name.startsWith("fa_eggs_to_fried_egg");
            });
        }
        server.getRecipeManager().replaceRecipes(recipes);
    }
}
