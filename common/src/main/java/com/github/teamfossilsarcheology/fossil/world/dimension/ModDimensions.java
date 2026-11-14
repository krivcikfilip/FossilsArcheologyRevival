package com.github.teamfossilsarcheology.fossil.world.dimension;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;

import java.util.OptionalLong;

public class ModDimensions {
    public static final ResourceKey<Level> TREASURE_ROOM = ResourceKey.create(Registries.DIMENSION, FossilMod.location("treasure_room"));
    public static final ResourceKey<Level> ANU_LAIR = ResourceKey.create(Registries.DIMENSION, FossilMod.location("anu_lair"));
    public static final ResourceKey<DimensionType> TREASURE_ROOM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, TREASURE_ROOM.location());
    public static final ResourceKey<DimensionType> ANU_LAIR_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ANU_LAIR.location());
    public static final DimensionType CUSTOM = new DimensionType(
            OptionalLong.of(6000), // fixed_time
            false, // has_skylight
            false, // has_ceiling
            true, // ultrawarm
            false, // natural
            1, // coordinate_scale
            false, // bed_works
            false, // respawn_anchor_works
            0, // min_y
            256, // height
            256, // logical_height
            BlockTags.INFINIBURN_OVERWORLD, // infiniburn
            BuiltinDimensionTypes.END_EFFECTS, // effects
            0, // ambient_light
            new DimensionType.MonsterSettings(true, false, ConstantInt.of(0), 0));

    public static void register() {
    }

    public static void bootstrap(BootstrapContext<DimensionType> context) {
        context.register(ANU_LAIR_TYPE, CUSTOM);
        context.register(TREASURE_ROOM_TYPE, CUSTOM);
    }

    /**
     * Teleports the entity to the anu dimension at a fixed point and places an obsidian platform below the entity
     *
     * @param server
     * @param entity
     */
    public static void teleportToAnuLair(MinecraftServer server, Entity entity) {
        ServerLevel anuLair = server.getLevel(ModDimensions.ANU_LAIR);
        Vec3 spawnPoint = new Vec3(70.5, 63, -17.5);
        int x = (int) spawnPoint.x;
        int y = (int) (spawnPoint.y - 2);
        int z = (int) spawnPoint.z;
        BlockPos.betweenClosed(x - 2, y + 1, z - 2, x + 2, y + 3, z + 2).forEach(blockPos -> anuLair.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState()));
        BlockPos.betweenClosed(x - 2, y, z - 2, x + 2, y, z + 2).forEach(blockPos -> anuLair.setBlockAndUpdate(blockPos, Blocks.OBSIDIAN.defaultBlockState()));
        //PORT(1.21.1): PortalInfo(pos, speed, yRot, xRot) + Forge ITeleporter -> vanilla DimensionTransition (1.21).
        //DO_NOTHING = no portal sound/ticket/portal placement, same as the old ModTeleporter (playTeleportSound=false, placeEntity without portal)
        //PORT-TODO(1.21.1): verify in game for players and non-player entities (AnuPortal calls this from entityInside; 1.21 vanilla
        // portals go through the Portal interface/PortalProcessor instead, direct changeDimension is still supported)
        changeDimension(entity, anuLair, new DimensionTransition(anuLair, spawnPoint, entity.getDeltaMovement(), 0, 90, DimensionTransition.DO_NOTHING));
    }

    /**
     * PORT(1.21.1): takes a {@link DimensionTransition} (was PortalInfo). The NeoForge impl calls
     * {@link Entity#changeDimension(DimensionTransition)}. Since this is plain vanilla now, the @ExpectPlatform could be inlined
     * later (then delete neoforge's ModDimensionsImpl).
     */
    @ExpectPlatform
    public static void changeDimension(Entity entity, ServerLevel level, DimensionTransition transition) {
        throw new NotImplementedException();
    }
}
