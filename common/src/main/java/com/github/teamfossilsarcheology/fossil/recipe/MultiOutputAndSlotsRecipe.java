package com.github.teamfossilsarcheology.fossil.recipe;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * PORT(1.21.1): Recipe<Container> -> Recipe<RecipeInput>; the recipe id lives in the RecipeHolder now (getId() removed).
 * JSON shape (see PORT_NOTES/data.md):
 * <pre>{"input": Ingredient, "outputs": [{"id": "..", "count": n (optional), "components": {..} (optional), "weight": double}], "total": double (optional)}</pre>
 * The random source for {@link #assemble(RecipeInput, HolderLookup.Provider)} comes from a {@link MachineRecipeInput}
 * (1.20.1 used the BlockEntity passed as Container).
 */
public abstract class MultiOutputAndSlotsRecipe implements Recipe<RecipeInput> {
    protected final Ingredient input;
    private final List<WeightedOutput> outputs;
    private final Optional<Double> total;
    private final NavigableMap<Double, ItemStack> weightedOutputs;

    /**
     * Data driven constructor (codecs)
     */
    protected MultiOutputAndSlotsRecipe(Ingredient input, List<WeightedOutput> outputs, Optional<Double> total) {
        this.input = input;
        this.outputs = outputs;
        this.total = total;
        this.weightedOutputs = toWeightedOutputs(outputs, total);
    }

    /**
     * Code constructor (hardcoded recipes from {@link ModRecipes#initRecipes()})
     */
    protected MultiOutputAndSlotsRecipe(Ingredient input, NavigableMap<Double, ItemStack> weightedOutputs) {
        this.input = input;
        this.weightedOutputs = weightedOutputs;
        this.outputs = new ArrayList<>();
        double last = 0;
        for (Map.Entry<Double, ItemStack> entry : weightedOutputs.entrySet()) {
            outputs.add(new WeightedOutput(entry.getValue(), entry.getKey() - last));
            last = entry.getKey();
        }
        this.total = Optional.empty();
    }

    private static NavigableMap<Double, ItemStack> toWeightedOutputs(List<WeightedOutput> outputs, Optional<Double> optionalTotal) {
        NavigableMap<Double, ItemStack> items = new TreeMap<>();
        double runningTotal = 0;
        for (WeightedOutput output : outputs) {
            if (output.stack().isEmpty()) continue;
            runningTotal += output.weight();
            items.put(runningTotal, output.stack());
        }
        if (items.isEmpty()) {
            //PORT(1.21.1): 1.20.1 crashed on outputs.lastKey() here. Keep loading but this recipe produces nothing
            FossilMod.LOGGER.warn("Recipe has no outputs: {}", outputs);
            return items;
        }
        double sumOfWeights = items.lastKey();
        double total = optionalTotal.orElse(sumOfWeights);
        if (total > sumOfWeights) {
            items.put(total, new ItemStack(Items.AIR));
        } else if (total < sumOfWeights) {
            //PORT(1.21.1): the recipe id is not known inside the codec anymore
            FossilMod.LOGGER.warn("Recipe has total smaller than sum of weights {} < {}", total, sumOfWeights);
        }
        return items;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> nonNullList = NonNullList.create();
        nonNullList.add(input);
        return nonNullList;
    }

    public Ingredient getInput() {
        return input;
    }

    public NavigableMap<Double, ItemStack> getWeightedOutputs() {
        return weightedOutputs;
    }

    public List<WeightedOutput> getOutputs() {
        return outputs;
    }

    public Optional<Double> getTotal() {
        return total;
    }

    @Override
    public boolean matches(RecipeInput container, Level level) {
        for (int i = 0; i < container.size(); i++) {
            if (matches(container, i)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(RecipeInput container, int slot) {
        ItemStack itemStack = container.getItem(slot);
        if (itemStack.isEmpty()) return false;
        return input.test(itemStack);
    }

    @Override
    public @NotNull ItemStack assemble(RecipeInput container, HolderLookup.Provider registries) {
        if (container instanceof MachineRecipeInput machineInput && machineInput.random() != null) {
            return assemble(machineInput.random());
        }
        return ItemStack.EMPTY;
    }

    /**
     * Rolls one of the weighted outputs
     */
    public @NotNull ItemStack assemble(RandomSource random) {
        if (weightedOutputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return weightedOutputs.higherEntry(random.nextDouble() * weightedOutputs.lastKey()).getValue().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * One output entry: the ItemStack fields ("id", "count", "components") are inlined next to "weight".
     */
    public record WeightedOutput(ItemStack stack, double weight) {
        public static final MapCodec<WeightedOutput> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(output -> output.stack().getItemHolder()),
                ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(output -> output.stack().getCount()),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(output -> output.stack().getComponentsPatch()),
                Codec.DOUBLE.fieldOf("weight").forGetter(WeightedOutput::weight)
        ).apply(instance, WeightedOutput::create));
        public static final Codec<WeightedOutput> CODEC = MAP_CODEC.codec();
        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, WeightedOutput::stack,
                ByteBufCodecs.DOUBLE, WeightedOutput::weight,
                WeightedOutput::new);

        private static WeightedOutput create(Holder<Item> item, int count, DataComponentPatch components, double weight) {
            return new WeightedOutput(new ItemStack(item, count, components), weight);
        }
    }

    public static class Serializer<T extends MultiOutputAndSlotsRecipe> implements RecipeSerializer<T> {
        private final MapCodec<T> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

        public Serializer(Constructor<T> constructor) {
            this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(MultiOutputAndSlotsRecipe::getInput),
                    WeightedOutput.CODEC.listOf().fieldOf("outputs").forGetter(MultiOutputAndSlotsRecipe::getOutputs),
                    Codec.DOUBLE.optionalFieldOf("total").forGetter(MultiOutputAndSlotsRecipe::getTotal)
            ).apply(instance, constructor::construct));
            this.streamCodec = StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, MultiOutputAndSlotsRecipe::getInput,
                    WeightedOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), MultiOutputAndSlotsRecipe::getOutputs,
                    ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), MultiOutputAndSlotsRecipe::getTotal,
                    constructor::construct);
        }

        @Override
        public @NotNull MapCodec<T> codec() {
            return codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
            return streamCodec;
        }

        @FunctionalInterface
        public interface Constructor<R> {
            R construct(Ingredient input, List<WeightedOutput> outputs, Optional<Double> total);
        }
    }

    /**
     * Recipe input of our machines. {@code random} is needed by {@link #assemble(RecipeInput, HolderLookup.Provider)}
     */
    public record MachineRecipeInput(List<ItemStack> items, @Nullable RandomSource random) implements RecipeInput {
        public static MachineRecipeInput of(ItemStack... items) {
            return new MachineRecipeInput(List.of(items), null);
        }

        public static MachineRecipeInput of(RandomSource random, List<ItemStack> items) {
            return new MachineRecipeInput(items, random);
        }

        @Override
        public @NotNull ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }
}
