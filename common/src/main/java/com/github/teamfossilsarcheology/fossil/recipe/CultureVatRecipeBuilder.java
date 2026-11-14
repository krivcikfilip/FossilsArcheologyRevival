package com.github.teamfossilsarcheology.fossil.recipe;

import com.github.teamfossilsarcheology.fossil.inventory.CultureVatMenu;
import com.github.teamfossilsarcheology.fossil.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class CultureVatRecipeBuilder extends WithFuelRecipeBuilder {
    public CultureVatRecipeBuilder(String modId, ItemLike itemInput, ItemLike itemOutput) {
        //TODO: Duration not actually used
        this(modId, itemInput, ModItems.BIO_GOO.get(), itemOutput, 6000);
    }

    public CultureVatRecipeBuilder(String modId, ItemLike itemInput, ItemLike itemFuel, ItemLike itemOutput, int duration) {
        super(modId, itemInput, itemFuel, itemOutput, duration);
    }

    @Override
    protected WithFuelRecipe createRecipe(Ingredient input, Ingredient fuel, ItemStack output, int duration) {
        return new CultureVatRecipe(input, fuel, output, duration, CultureVatMenu.DEFAULT_FUEL_DURATION);
    }

    @Override
    protected ResourceLocation getDefaultRecipeId() {
        return ResourceLocation.fromNamespaceAndPath(modId, "culture_vat/" + BuiltInRegistries.ITEM.getKey(itemInput.asItem()).getPath());
    }
}
