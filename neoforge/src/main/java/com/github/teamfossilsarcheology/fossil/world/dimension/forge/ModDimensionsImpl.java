package com.github.teamfossilsarcheology.fossil.world.dimension.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;

/**
 * @see com.github.teamfossilsarcheology.fossil.world.dimension.ModDimensions
 * PORT(1.21.1): PortalInfo and Forge's ITeleporter are gone. Vanilla 1.21 teleports with
 * {@link Entity#changeDimension(DimensionTransition)}; the transition already carries the target level, position, speed and
 * rotation, and {@link DimensionTransition#DO_NOTHING} skips the portal sound like the old ModTeleporter did.
 * <p>
 * PORT(1.21.1, verified phase 2): requires the common @ExpectPlatform signature -> compiles
 * {@code changeDimension(Entity, ServerLevel, DimensionTransition)} (world owner); since this is now plain vanilla code
 * the @ExpectPlatform can also be dropped and this class deleted.
 */
public class ModDimensionsImpl {
    public static void changeDimension(Entity entity, ServerLevel level, DimensionTransition transition) {
        if (transition.newLevel() != level) {
            transition = new DimensionTransition(level, transition.pos(), transition.speed(), transition.yRot(), transition.xRot(), transition.missingRespawnBlock(), transition.postDimensionTransition());
        }
        entity.changeDimension(transition);
    }
}
