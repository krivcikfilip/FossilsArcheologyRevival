package com.github.teamfossilsarcheology.fossil.forge.mixin;

import com.github.teamfossilsarcheology.fossil.block.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// PORT(1.21.1-review): split out of WalkNodeEvaluatorMixin. In 1.21.1 the static isBurningBlock(BlockState) lives in NodeEvaluator
// (vanilla 1.21.1 NodeEvaluator:104; NeoForge does not patch it), the first BlockState#is(TagKey) call is still the BlockTags.FIRE check.
@Mixin(NodeEvaluator.class)
public abstract class NodeEvaluatorMixin {

    @Inject(method = "isBurningBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 0), cancellable = true)
    private static void addTarAsDangerousPath(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ModBlocks.TAR.isPresent() && state.is(ModBlocks.TAR.get())) {
            cir.setReturnValue(true);
        }
    }
}
