package com.github.teamfossilsarcheology.fossil.compat.rei;

import com.github.teamfossilsarcheology.fossil.recipe.WithFuelRecipe;
import com.google.common.collect.Lists;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


public abstract class WithFuelDisplay implements Display {
    protected final EntryIngredient input;
    protected final EntryIngredient fuel;
    protected final EntryStack<?> output;
    protected final ResourceLocation location;


    //PORT(1.21.1): REI 16 recipe fillers receive RecipeHolders; the id comes from RecipeHolder#id(), getResultItem(null) -> getResult()
    protected WithFuelDisplay(RecipeHolder<? extends WithFuelRecipe> holder) {
        this(EntryIngredients.ofIngredient(holder.value().getInput()), EntryIngredients.ofIngredient(holder.value().getFuel()),
                EntryStacks.of(holder.value().getResult().copy()), holder.id());
    }

    protected WithFuelDisplay(EntryIngredient input, EntryIngredient fuel, EntryStack<?> output, ResourceLocation location) {
        this.input = input;
        this.fuel = fuel;
        this.output = output;
        this.location = location;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return Lists.newArrayList(input, fuel);
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return Collections.singletonList(EntryIngredient.of(output));
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(location);
    }

    public static class Serializer<T extends WithFuelDisplay> implements DisplaySerializer<T> {
        protected final Constructor<T> constructor;

        protected Serializer(Constructor<T> constructor) {
            this.constructor = constructor;
        }

        @Override
        public CompoundTag save(CompoundTag tag, T display) {
            tag.put("Input", display.input.saveIngredient());
            tag.put("Fuel", display.fuel.saveIngredient());
            tag.put("Output", display.output.saveStack());
            tag.putString("Location", display.location.toString());
            return tag;
        }

        @Override
        public T read(CompoundTag tag) {
            EntryIngredient input = EntryIngredient.read(tag.getList("Input", Tag.TAG_COMPOUND));
            EntryIngredient fuel = EntryIngredient.read(tag.getList("Fuel", Tag.TAG_COMPOUND));
            EntryStack<?> output = EntryStack.read(tag.getCompound("Output"));
            ResourceLocation location = ResourceLocation.parse(tag.getString("Location"));
            return constructor.construct(input, fuel, output, location);
        }

        @FunctionalInterface
        public interface Constructor<R> {
            R construct(EntryIngredient input, EntryIngredient fuel, EntryStack<?> output, ResourceLocation location);
        }
    }
}
