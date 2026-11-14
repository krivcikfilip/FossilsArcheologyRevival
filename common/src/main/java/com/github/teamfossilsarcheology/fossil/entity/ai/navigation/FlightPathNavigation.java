package com.github.teamfossilsarcheology.fossil.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public class FlightPathNavigation extends FlyingPathNavigation {
    public FlightPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
        nodeEvaluator = new FlightNodeEvaluator();
        return new PathFinder(nodeEvaluator, maxVisitedNodes);
    }

    @Nullable
    @Override
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange) {
        return CenteredPath.createFromPath(super.createPath(targets, regionOffset, offsetUpward, accuracy, followRange));
    }

    private static class FlightNodeEvaluator extends FlyNodeEvaluator {
        /**
         * PORT(1.21.1): was getBlockPathTypes(BlockGetter, int, int, int, EnumSet, BlockPathTypes, BlockPos) which also returned the type
         * of the center block. 1.21 checks the center type in getPathTypeOfMob itself, evaluateBlockPathType was inlined into
         * getPathTypeWithinMobBB (see {@link NavUtil#evaluatePathType}).
         * PORT(1.21.1, verified phase 2): 1.20.1 FlyNodeEvaluator#getBlockPathType(..., Mob) called WalkNodeEvaluator#getBlockPathTypes
         * with invokespecial (checked in the 1.20.1 bytecode), so the 1.20.1 override was never used. In 1.21.1 FlyNodeEvaluator inherits
         * WalkNodeEvaluator#getPathTypeOfMob, which calls this.getPathTypeWithinMobBB virtually, so an override would now change flying
         * pathing. To keep the 1.20.1 behaviour it no longer overrides (renamed, unused). Rename back to getPathTypeWithinMobBB (+ @Override)
         * to enable the centered check for flyers.
         */
        public @NotNull Set<PathType> getCenteredPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
            EnumSet<PathType> nodeTypeEnum = EnumSet.noneOf(PathType.class);
            float width = Math.max(0, entityWidth - 2);
            int widthEachSide = Mth.ceil(width / 2.0f) + 1;
            for (int i = 0; i < widthEachSide; ++i) {
                for (int j = 0; j < entityHeight; ++j) {
                    for (int k = 0; k < widthEachSide; ++k) {
                        PathType blockPathType = this.getPathType(context, x + i, y + j, z + k);
                        blockPathType = NavUtil.evaluatePathType(this, context, mob, blockPathType);
                        nodeTypeEnum.add(blockPathType);
                        if (i != 0 || k != 0) {
                            blockPathType = this.getPathType(context, x - i, y + j, z - k);
                            blockPathType = NavUtil.evaluatePathType(this, context, mob, blockPathType);
                            nodeTypeEnum.add(blockPathType);
                        }
                    }
                }
            }
            return nodeTypeEnum;
        }
    }
}
