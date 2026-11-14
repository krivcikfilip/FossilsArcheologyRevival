package com.github.teamfossilsarcheology.fossil.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class AnalyzerRecipe extends MultiOutputAndSlotsRecipe {

    public AnalyzerRecipe(Ingredient input, List<WeightedOutput> outputs, Optional<Double> total) {
        super(input, outputs, total);
    }

    public AnalyzerRecipe(Ingredient input, NavigableMap<Double, ItemStack> weightedOutputs) {
        super(input, weightedOutputs);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<AnalyzerRecipe> {
        public static final Type INSTANCE = new Type();

        private Type() {
        }

        @Override
        public String toString() {
            return "fossil:analyzer";
        }
    }

    public static class Serializer extends MultiOutputAndSlotsRecipe.Serializer<AnalyzerRecipe> {
        public static final AnalyzerRecipe.Serializer INSTANCE = new AnalyzerRecipe.Serializer(AnalyzerRecipe::new);

        public Serializer(Constructor<AnalyzerRecipe> constructor) {
            super(constructor);
        }
    }

    public static class Builder {

        public final ItemLike item;
        private final NavigableMap<Double, ItemStack> map = new TreeMap<>();
        public double total;

        public Builder(ItemLike item) {
            this.item = item;
        }

        public Builder addOutput(ItemLike itemLike, double weight) {
            total += weight;
            map.put(total, new ItemStack(itemLike));
            return this;
        }

        public AnalyzerRecipe build() {
            //PORT(1.21.1): recipes no longer carry their id (was FossilMod.location(item.toString())), these are never registered
            return new AnalyzerRecipe(Ingredient.of(item), map);
        }
    }
}
