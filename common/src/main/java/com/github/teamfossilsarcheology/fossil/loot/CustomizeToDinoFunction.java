package com.github.teamfossilsarcheology.fossil.loot;

import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric;
import com.github.teamfossilsarcheology.fossil.tags.ModItemTags;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Changes the amount of food a {@link Prehistoric} drops based on age
 */
public class CustomizeToDinoFunction extends LootItemConditionalFunction {
    /**
     * PORT(1.21.1): JSON {"function": "fossil:customize_to_dinosaur", "entity": "this", "conditions": [..] (optional)}
     */
    public static final MapCodec<CustomizeToDinoFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
            .and(LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(function -> function.entityTarget))
            .apply(instance, CustomizeToDinoFunction::new));
    private final LootContext.EntityTarget entityTarget;

    protected CustomizeToDinoFunction(List<LootItemCondition> lootItemConditions, LootContext.EntityTarget entityTarget) {
        super(lootItemConditions);
        this.entityTarget = entityTarget;
    }

    public static LootItemConditionalFunction.Builder<?> apply(LootContext.EntityTarget target) {
        return simpleBuilder(lootItemConditions -> new CustomizeToDinoFunction(lootItemConditions, target));
    }

    @Override
    protected @NotNull ItemStack run(ItemStack stack, LootContext context) {
        if (!stack.isEmpty()) {
            Entity entity = context.getParamOrNull(entityTarget.getParam());
            if (entity instanceof Prehistoric prehistoric) {
                if (stack.is(ModItemTags.UNCOOKED_MEAT)) {
                    stack.setCount(Math.min(prehistoric.getAgeInDays(), prehistoric.data().adultAgeDays()));
                    return prehistoric.isOnFire() ? new ItemStack(prehistoric.info().cookedFoodItem, stack.getCount()) : stack;
                }
            }
        }
        return stack;
    }

    @Override
    public @NotNull LootItemFunctionType<CustomizeToDinoFunction> getType() {
        return ModLootItemFunctionTypes.CUSTOMIZE_TO_DINOSAUR.get();
    }
}
