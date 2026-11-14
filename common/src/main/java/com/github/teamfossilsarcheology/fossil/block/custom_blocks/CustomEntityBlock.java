package com.github.teamfossilsarcheology.fossil.block.custom_blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;
import com.github.teamfossilsarcheology.fossil.util.InteractionResults;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for our "machine" blocks
 */
public abstract class CustomEntityBlock extends BaseEntityBlock {
    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        //PORT(1.21.1): codec required since 1.20.3. The constructor arguments are not serializable, so this codec
        //always returns this block instance (only used for block type serialization)
        return MapCodec.unit(this);
    }

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    protected CustomEntityBlock(Properties properties) {
        super(properties);
    }

    protected void dropInventory(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container) {
            Containers.dropContents(level, pos, (Container) blockEntity);
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack heldStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        //PORT(1.21.1): Block#use was split, see InteractionResults
        return InteractionResults.toItemResult(use(state, level, pos, player, hand, hit));
    }

    private @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider) {
            player.openMenu((MenuProvider) blockEntity);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }
        dropInventory(level, pos);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
