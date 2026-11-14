package com.github.teamfossilsarcheology.fossil.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Triggers whenever a player inserts an embryo into a mob
 * <p>
 * PORT(1.21.1): SimpleCriterionTrigger with a Codec (1.20.2+). JSON: {@code "conditions": {"player": .. (optional), "item": ItemPredicate (optional)}}
 */
public class ImplantEmbryoTrigger extends SimpleCriterionTrigger<ImplantEmbryoTrigger.TriggerInstance> {

    public void trigger(ServerPlayer player, ItemStack stack) {
        trigger(player, triggerInstance -> triggerInstance.matches(stack));
    }

    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> embryoItem) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::embryoItem)
        ).apply(instance, TriggerInstance::new));

        /**
         * PORT(1.21.1): returns a Criterion (was a TriggerInstance) for the datagen advancement builders
         */
        public static Criterion<TriggerInstance> implantEmbryo(Item embryoItem) {
            return ModTriggers.IMPLANT_EMBRYO_TRIGGER.createCriterion(new TriggerInstance(Optional.empty(),
                    Optional.of(ItemPredicate.Builder.item().of(embryoItem).build())));
        }

        public boolean matches(ItemStack itemStack) {
            //1.20.1: a missing "item" object resulted in an empty (matches everything) predicate
            return embryoItem.isEmpty() || embryoItem.get().test(itemStack);
        }
    }
}
