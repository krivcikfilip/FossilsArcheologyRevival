package com.github.teamfossilsarcheology.fossil.recipe;

import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * PORT(1.21.1): FinishedRecipe/Consumer<FinishedRecipe> -> RecipeOutput. The recipe object is built directly and
 * serialized by the recipe serializer codec (JSON shape: see {@link MultiOutputAndSlotsRecipe}).
 */
public abstract class MultiOutputAndSlotsRecipeBuilder<T extends MultiOutputAndSlotsRecipeBuilder<T>> implements RecipeBuilder {
    protected final String modId;
    protected final ItemLike itemInput;
    protected final TagKey<Item> tagInput;
    protected final NavigableMap<ItemHolder, Double> weightedOutputs = new TreeMap<>();
    protected double total;
    protected double nothingWeight;

    protected MultiOutputAndSlotsRecipeBuilder(String modId, ItemLike itemInput) {
        this.modId = modId;
        this.itemInput = itemInput;
        this.tagInput = null;
    }

    protected MultiOutputAndSlotsRecipeBuilder(String modId, TagKey<Item> tagInput) {
        this.modId = modId;
        this.itemInput = null;
        this.tagInput = tagInput;
    }

    public MultiOutputAndSlotsRecipeBuilder<T> addOutput(ItemLike itemLike, double weight) {
        return addOutput(itemLike, 1, weight);
    }

    public MultiOutputAndSlotsRecipeBuilder<T> addOutput(ItemLike itemLike, int count, double weight) {
        total += weight;
        weightedOutputs.put(new ItemHolder(BuiltInRegistries.ITEM.getKey(itemLike.asItem()), count), weight);
        return this;
    }

    public MultiOutputAndSlotsRecipeBuilder<T> nothing(double weight) {
        nothingWeight = weight;
        return this;
    }

    public double getTotal() {
        return total + nothingWeight;
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
        return Items.ENDER_PEARL;
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
        recipeOutput.accept(recipeId, createRecipe(input(), outputs(), nothingWeight > 0 ? Optional.of(total + nothingWeight) : Optional.empty()), null);
    }

    protected abstract MultiOutputAndSlotsRecipe createRecipe(Ingredient input, List<MultiOutputAndSlotsRecipe.WeightedOutput> outputs, Optional<Double> total);

    protected Ingredient input() {
        return itemInput != null ? Ingredient.of(itemInput) : Ingredient.of(tagInput);
    }

    protected List<MultiOutputAndSlotsRecipe.WeightedOutput> outputs() {
        List<MultiOutputAndSlotsRecipe.WeightedOutput> outputs = new ArrayList<>();
        for (Map.Entry<ItemHolder, Double> output : weightedOutputs.entrySet()) {
            ItemHolder holder = output.getKey();
            outputs.add(new MultiOutputAndSlotsRecipe.WeightedOutput(new ItemStack(BuiltInRegistries.ITEM.get(holder.location()), holder.count()), output.getValue()));
        }
        return outputs;
    }

    protected abstract ResourceLocation getDefaultRecipeId();

    public record ItemHolder(ResourceLocation location, int count) implements Comparable<ItemHolder> {

        @Override
        public int compareTo(@NotNull ItemHolder o) {
            return location.getPath().compareTo(o.location.getPath());
        }
    }
}
