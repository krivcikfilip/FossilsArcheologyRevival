package com.github.teamfossilsarcheology.fossil.util;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/**
 * PORT(1.21.1): Block#use was split into useItemOn (any hand, any item incl. empty) and useWithoutItem (main hand only)
 * in 1.20.5. To keep the exact 1.20.1 behaviour (called for both hands) the old logic is kept in a private
 * {@code use(...)} method and called from {@code useItemOn}, mapping the result with {@link #toItemResult(InteractionResult)}.
 */
public final class InteractionResults {
    private InteractionResults() {
    }

    /**
     * PASS maps to PASS_TO_DEFAULT_BLOCK_INTERACTION, which falls through to the default useWithoutItem (PASS for our blocks).
     */
    public static ItemInteractionResult toItemResult(InteractionResult result) {
        if (result == InteractionResult.PASS) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        } else if (result == InteractionResult.FAIL) {
            return ItemInteractionResult.FAIL;
        } else if (result == InteractionResult.CONSUME) {
            return ItemInteractionResult.CONSUME;
        } else if (result == InteractionResult.CONSUME_PARTIAL) {
            return ItemInteractionResult.CONSUME_PARTIAL;
        }
        return ItemInteractionResult.SUCCESS;
    }
}
