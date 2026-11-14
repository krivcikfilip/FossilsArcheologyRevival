package com.github.teamfossilsarcheology.fossil.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Triggers whenever an egg hatches
 * <p>
 * PORT(1.21.1): SimpleCriterionTrigger with a Codec (1.20.2+). JSON: {@code "conditions": {"player": .. (optional), "entity": [loot conditions] (optional)}}
 */
public class IncubateEggTrigger extends SimpleCriterionTrigger<IncubateEggTrigger.TriggerInstance> {

    public void trigger(ServerPlayer player, Entity entity) {
        LootContext lootContext = EntityPredicate.createContext(player, entity);
        trigger(player, triggerInstance -> triggerInstance.matches(lootContext));
    }

    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
        ).apply(instance, TriggerInstance::new));

        /**
         * PORT(1.21.1): returns a Criterion (was a TriggerInstance) for the datagen advancement builders
         */
        public static Criterion<TriggerInstance> incubateEgg(EntityType<?> entityType) {
            return ModTriggers.INCUBATE_EGG_TRIGGER.createCriterion(new TriggerInstance(Optional.empty(),
                    Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(entityType).build()))));
        }

        public boolean matches(LootContext context) {
            return entity.isEmpty() || entity.get().matches(context);
        }
    }
}
