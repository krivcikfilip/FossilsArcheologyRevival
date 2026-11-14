package com.github.teamfossilsarcheology.fossil.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PORT(1.21.1): Recipe<ContainerWithAnyFuel> where ContainerWithAnyFuel is now a {@link RecipeInput} (was a SimpleContainer).
 * The recipe id lives in the RecipeHolder now (getId() removed).
 * JSON shape (see PORT_NOTES/data.md):
 * <pre>{"input": Ingredient, "fuel": Ingredient, "result": ItemStack ({"id", "count"?, "components"?}), "duration": int (optional), "fuel_duration": int (optional)}</pre>
 * 1.20.1 read "result" as an item id string.
 */
public abstract class WithFuelRecipe implements Recipe<WithFuelRecipe.ContainerWithAnyFuel> {
    private final Ingredient input;
    private final Ingredient fuel;
    private final ItemStack result;
    private final int duration;
    private final int fuelDuration;

    protected WithFuelRecipe(Ingredient input, Ingredient fuel, ItemStack result, int duration, int fuelDuration) {
        this.input = input;
        this.fuel = fuel;
        this.result = result;
        this.duration = duration;
        this.fuelDuration = fuelDuration;
    }

    @Override
    public boolean matches(ContainerWithAnyFuel container, Level level) {
        return input.test(container.getItem(0)) && (container.anyFuel || fuel.test(container.getItem(1)));
    }

    @Override
    public @NotNull ItemStack assemble(ContainerWithAnyFuel container, HolderLookup.Provider registries) {
        ItemStack itemStack = result.copy();
        //PORT(1.21.1): copying the input NBT (setTag) -> copying the input component patch
        itemStack.applyComponents(container.getItem(0).getComponentsPatch());
        return itemStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> nonNullList = NonNullList.create();
        nonNullList.add(input);
        return nonNullList;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public boolean isFuel(ItemStack itemStack) {
        return fuel.test(itemStack);
    }

    public Ingredient getInput() {
        return input;
    }

    public Ingredient getFuel() {
        return fuel;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getDuration() {
        return duration;
    }

    public int getFuelDuration() {
        return fuelDuration;
    }

    /**
     * Slot 0: input, slot 1: fuel. {@code anyFuel} skips the fuel check.
     */
    public static class ContainerWithAnyFuel implements RecipeInput {
        public final boolean anyFuel;
        private final List<ItemStack> items;

        public ContainerWithAnyFuel(boolean anyFuel, ItemStack... items) {
            this.items = List.of(items);
            this.anyFuel = anyFuel;
        }

        public ContainerWithAnyFuel(ItemStack input, ItemStack fuel) {
            this(false, input, fuel);
        }

        @Override
        public @NotNull ItemStack getItem(int index) {
            //SimpleContainer returned EMPTY for missing slots (e.g. ContainerWithAnyFuel(true, input) has no fuel slot)
            return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static abstract class WithFuelRecipeSerializer<T extends WithFuelRecipe> implements RecipeSerializer<T> {
        protected final Constructor<T> constructor;
        private MapCodec<T> codec;
        private StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

        protected WithFuelRecipeSerializer(Constructor<T> constructor) {
            this.constructor = constructor;
        }

        abstract int defaultDuration();

        abstract int defaultFuelDuration();

        @Override
        public @NotNull MapCodec<T> codec() {
            if (codec == null) {
                //Lazy because the defaults are provided by the subclass
                codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(WithFuelRecipe::getInput),
                        Ingredient.CODEC_NONEMPTY.fieldOf("fuel").forGetter(WithFuelRecipe::getFuel),
                        ItemStack.CODEC.fieldOf("result").forGetter(WithFuelRecipe::getResult),
                        Codec.INT.optionalFieldOf("duration", defaultDuration()).forGetter(WithFuelRecipe::getDuration),
                        Codec.INT.optionalFieldOf("fuel_duration", defaultFuelDuration()).forGetter(WithFuelRecipe::getFuelDuration)
                ).apply(instance, constructor::construct));
            }
            return codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
            if (streamCodec == null) {
                streamCodec = StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, WithFuelRecipe::getInput,
                        Ingredient.CONTENTS_STREAM_CODEC, WithFuelRecipe::getFuel,
                        ItemStack.STREAM_CODEC, WithFuelRecipe::getResult,
                        ByteBufCodecs.INT, WithFuelRecipe::getDuration,
                        ByteBufCodecs.INT, WithFuelRecipe::getFuelDuration,
                        constructor::construct);
            }
            return streamCodec;
        }

        @FunctionalInterface
        public interface Constructor<R> {
            R construct(Ingredient input, Ingredient fuel, ItemStack output, int duration, int fuelDuration);
        }
    }
}
