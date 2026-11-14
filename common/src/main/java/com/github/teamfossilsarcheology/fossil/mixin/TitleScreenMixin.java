package com.github.teamfossilsarcheology.fossil.mixin;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.config.FossilConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Custom parallax main menu background.
 * <p>
 * PORT(1.21.1): In 1.20.1 this mixin targeted TitleScreen#render (PanoramaRenderer#render(FF) + the panorama overlay blit).
 * Since 1.20.5 the title screen renders the (shared, static) Screen.PANORAMA via Screen#renderPanorama, and the overlay blit
 * moved into PanoramaRenderer#render(GuiGraphics, int, int, float, float). The mixin now replaces that method while the
 * title screen is open (same visual result: no vanilla panorama/overlay, our two layers instead).
 * The class name is kept so fossil_common.mixins.json does not change.
 * PORT-TODO(1.21.1): verify visually in game (blur of Screen#renderBlurredBackground may now apply on top)
 */
@Mixin(PanoramaRenderer.class)
public abstract class TitleScreenMixin {
    @Unique
    private static final ResourceLocation LAYER_TEXTURE_BACK = FossilMod.location("textures/gui/parallax/layer_0.png");
    @Unique
    private static final ResourceLocation LAYER_TEXTURE_FRONT = FossilMod.location("textures/gui/parallax/layer_1.png");
    @Unique
    private final int fossil$initialOffsetFront = new Random().nextInt(1027);
    @Unique
    private final int fossil$initialOffsetBack = new Random().nextInt(2047);
    @Unique
    private int fossil$layerTick;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    protected void renderCustomTitleScreen(GuiGraphics guiGraphics, int width, int height, float fade, float partialTick, CallbackInfo ci) {
        if (FossilConfig.isEnabled(FossilConfig.CUSTOM_MAIN_MENU) && Minecraft.getInstance().screen instanceof TitleScreen) {
            fossil$layerTick++;
            float u = fossil$initialOffsetBack + ((fossil$layerTick + partialTick) / 2f) + 1;
            guiGraphics.blit(LAYER_TEXTURE_BACK, 0, 0, u / (960f / width), 0, width, height, (int) (1024 * (height / 128f)), height);
            u = fossil$initialOffsetFront + fossil$layerTick + partialTick + 2 + 512;
            guiGraphics.blit(LAYER_TEXTURE_FRONT, 0, 0, u / (960f / width), 0, width, height, (int) (2048 * (height / 128f)), height);
            ci.cancel();
        }
    }
}
