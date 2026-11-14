package com.github.teamfossilsarcheology.fossil.forge.data.providers;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.entity.ModEntities;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricMobType;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.VanillaEntityInfo;
import com.github.teamfossilsarcheology.fossil.tags.ModEntityTypeTags;
import com.github.teamfossilsarcheology.fossil.util.ModConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.world.entity.EntityType.*;

public class ModEntityTypeTagsProvider extends EntityTypeTagsProvider {

    public ModEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookup, FossilMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        addTag(ModEntityTypeTags.LIVESTOCK, AXOLOTL, CHICKEN, COW, DONKEY, GOAT, HORSE, LLAMA, MULE, PANDA, PIG, RABBIT, SHEEP);
        var mammal = tag(ModEntityTypeTags.MAMMAL);
        for (VanillaEntityInfo info : VanillaEntityInfo.values()) {
            if (info.mobType == PrehistoricMobType.MAMMAL) {
                mammal.add(info.entityType());
            }
        }
        addTag(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES, ModEntities.MAMMOTH.get(), ModEntities.ELASMOTHERIUM.get());
        //PORT(1.21.1): MobType/WaterAnimal/canBreatheUnderwater were replaced by vanilla entity type tags (see PORT_NOTES/entity-a.md)
        addFossilIds(EntityTypeTags.AQUATIC, "aquilolamna", "crassigyrinus", "diplocaulus", "henodus", "ichthyosaurus", "liopleurodon", "megalodon", "mosasaurus", "plesiosaurus", "sarcosuchus", "spinosaurus", "tiktaalik", "alligator_gar", "coelacanth", "nautilus", "sturgeon");
        addFossilIds(EntityTypeTags.UNDEAD, "friendly_piglin");
        addFossilIds(EntityTypeTags.ARTHROPOD, "arthropleura", "meganeura", "megalograptus", "dicranurus", "lonchodomas", "scotoharpes", "walliserops");
        addFossilIds(EntityTypeTags.CAN_BREATHE_UNDER_WATER, "aquilolamna", "crassigyrinus", "diplocaulus", "henodus", "ichthyosaurus", "liopleurodon", "megalodon", "mosasaurus", "plesiosaurus", "sarcosuchus", "spinosaurus", "tiktaalik", "alligator_gar", "coelacanth", "nautilus", "sturgeon", "megalograptus", "dicranurus", "lonchodomas", "scotoharpes", "walliserops");
        if (ModList.get().isLoaded(ModConstants.PREHISTORIC_FAUNA)) {
            PFaunaTagsProvider.addEntityTypeTags(this);
        }
    }

    private void addFossilIds(TagKey<EntityType<?>> key, String... ids) {
        var appender = tag(key);
        for (String id : ids) {
            appender.add(ResourceKey.create(Registries.ENTITY_TYPE, FossilMod.location(id)));
        }
    }

    protected IntrinsicTagAppender<EntityType<?>> addTag(TagKey<EntityType<?>> key, EntityType<?>... toAdd) {
        return tag(key).add(Arrays.stream(toAdd).toArray(EntityType<?>[]::new));
    }
}
