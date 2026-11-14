package com.github.teamfossilsarcheology.fossil.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.animation.AnimatableManager;

@Mixin(AnimatableManager.class)
public interface AnimatableManagerAccessor {

    /**
     * PORT(1.21.1): GeckoLib 4.x (1.21.1) has a public AnimatableManager#isFirstTick(), an accessor with the same name would
     * clash with it. Renamed; callers can also use manager.isFirstTick() directly.
     */
    @Accessor("isFirstTick")
    boolean fossil$isFirstTick();
}
