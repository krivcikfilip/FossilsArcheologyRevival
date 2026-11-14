package com.github.teamfossilsarcheology.fossil.recipe;

import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PORT(1.21.1): FinishedRecipe/Consumer<FinishedRecipe> -> RecipeOutput. The recipe object is built directly and
 * serialized by the recipe serializer codec (JSON shape: see {@link WithFuelRecipe}).
 */
public abstract class WithFuelRecipeBuilder implements RecipeBuilder {
    protected final String modId;
    protected final ItemLike itemInput;
    protected final ItemLike itemFuel;
    protected final ItemLike itemOutput;
    protected final int duration;

    protected WithFuelRecipeBuilder(String modId, ItemLike itemInput, ItemLike itemFuel, ItemLike itemOutput, int duration) {
        this.modId = modId;
        this.itemInput = itemInput;
        this.itemFuel = itemFuel;
        this.itemOutput = itemOutput;
        this.duration = duration;
    }

    @Override
    public @NotNull RecipeBuilder unlockedBy(@NotNull String criterionName, @NotNull Criterion<?> criterion) {
        return this;
    }

    @Override
    public @NotNull RecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public @NotNull Item getResult() {
        //Method is unused
        return itemOutput.asItem();
    }

    @Override
    public void save(@NotNull RecipeOutput recipeOutput) {
        save(recipeOutput, getDefaultRecipeId());
    }

    @Override
    public void save(@NotNull RecipeOutput recipeOutput, @NotNull String recipeId) {
        ResourceLocation resourceLocation = getDefaultRecipeId();
        ResourceLocation resourceLocation2 = ResourceLocation.parse(recipeId);
        if (resourceLocation2.equals(resourceLocation)) {
            throw new IllegalStateException("Recipe " + recipeId + " should remove its 'save' argument as it is equal to default one");
        } else {
            save(recipeOutput, resourceLocation2);
        }
    }

    @Override
    public void save(@NotNull RecipeOutput recipeOutput, @NotNull ResourceLocation recipeId) {
        recipeOutput.accept(recipeId, createRecipe(Ingredient.of(itemInput), Ingredient.of(itemFuel), new ItemStack(itemOutput), duration), null);
    }

    /**
     * The fuel duration is not set by the builders (serializer default, as in 1.20.1)
     */
    protected abstract WithFuelRecipe createRecipe(Ingredient input, Ingredient fuel, ItemStack output, int duration);

    protected abstract ResourceLocation getDefaultRecipeId();
}
