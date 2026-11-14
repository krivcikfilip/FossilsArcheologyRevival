package com.github.teamfossilsarcheology.fossil.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

public class SifterRecipeBuilder extends MultiOutputAndSlotsRecipeBuilder<SifterRecipeBuilder> {
    public SifterRecipeBuilder(String modId, ItemLike itemInput) {
        super(modId, itemInput);
    }

    public SifterRecipeBuilder(String modId, TagKey<Item> tagInput) {
        super(modId, tagInput);
    }

    @Override
    protected MultiOutputAndSlotsRecipe createRecipe(Ingredient input, List<MultiOutputAndSlotsRecipe.WeightedOutput> outputs, Optional<Double> total) {
        return new SifterRecipe(input, outputs, total);
    }

    @Override
    protected ResourceLocation getDefaultRecipeId() {
        if (itemInput != null) {
            return ResourceLocation.fromNamespaceAndPath(modId, "sifter/" + BuiltInRegistries.ITEM.getKey(itemInput.asItem()).getPath());
        } else if (tagInput != null) {
            return ResourceLocation.fromNamespaceAndPath(modId, "sifter/" + tagInput.location().getPath());
        }
        return BuiltInRegistries.ITEM.getKey(Items.ENDER_PEARL);
    }
}
