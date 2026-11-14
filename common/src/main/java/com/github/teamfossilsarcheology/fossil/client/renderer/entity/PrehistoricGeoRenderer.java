package com.github.teamfossilsarcheology.fossil.client.renderer.entity;

import com.github.teamfossilsarcheology.fossil.client.gui.debug.InstructionTab;
import com.github.teamfossilsarcheology.fossil.client.model.PrehistoricGeoModel;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.Arthropleura;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric;
import com.github.teamfossilsarcheology.fossil.entity.util.Util;
import com.github.teamfossilsarcheology.fossil.util.Version;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.function.Function;

public class PrehistoricGeoRenderer<T extends Prehistoric> extends GeoEntityRenderer<T> {
    /**
     * @param model the file model name (excluding extension)
     */
    public PrehistoricGeoRenderer(EntityRendererProvider.Context renderManager, String model, Function<ResourceLocation, RenderType> renderType) {
        super(renderManager, new PrehistoricGeoModel<>(model, renderType));
    }

    /**
     * PORT(1.21.1): GeckoLib 4.9 GeoEntityRenderer#actuallyRender already scales the pose by {@code LivingEntity#getScale()}
     * ("nativeScale"), which {@link Prehistoric#getScale()} overrides. The 1.20.1 {@code withScale(animatable.getScale())} would
     * therefore scale the model twice, so the scale is no longer applied here (scaleWidth/scaleHeight stay 1).
     * PORT-TODO(1.21.1): relies on Prehistoric#getScale overriding LivingEntity#getScale (entity-a); visual size check in game
     */
    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = entity.getBbWidth() * 0.45F;
        poseStack.pushPose();
        if (entity.getClimbingDirection() != Direction.UP || (entity.prevClimbDirection != Direction.UP && entity.climbTick > 0)) {
            float progress = Mth.lerp(partialTick, entity.prevClimbTick, entity.climbTick) / 5f;
            Direction dir = entity.getClimbingDirection() != Direction.UP ? entity.getClimbingDirection() : entity.prevClimbDirection;
            float offset = entity.getBbWidth() / 2 * progress;
            poseStack.translate(dir.getStepX() * offset, 0.5 * progress, dir.getStepZ() * offset);
            Direction dirRot = dir.getClockWise();
            poseStack.mulPose(Axis.of(new Vector3f(dirRot.getStepX(), 0, dirRot.getStepZ())).rotationDegrees(90 * progress));
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        if (Version.debugEnabled()) {
            if (InstructionTab.entityListHighlight != null && InstructionTab.entityListHighlight.getId() == animatable.getId()) {
                return Color.RED;
            } else if (InstructionTab.highlightInstructionEntity != null && InstructionTab.highlightInstructionEntity.getId() == animatable.getId()) {
                return Color.RED;
            }
        }
        if (animatable instanceof Arthropleura arthropleura && arthropleura.isBee()) {
            return Color.YELLOW;
        }
        return super.getRenderColor(animatable, partialTick, packedLight);
    }

    @Override
    public float getMotionAnimThreshold(T animatable) {
        //TODO: get from animatable
        return Util.SWING_ANIM_THRESHOLD;
    }

    /**
     * PORT(1.21.1): GeckoLib 4.9 calls the overload with {@code nativeScale}; the 5 parameter version is deprecated and no longer
     * called by GeoEntityRenderer. The Dinnerbone translation happens after the native scale, like after withScale in 1.20.1.
     */
    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - rotationYaw));
        if (animatable.deathTime > 0) {
            float deathRotation = (animatable.deathTime + partialTick - 1f) / 20f * 1.6f;

            poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathRotation), 1) * getDeathMaxRotation(animatable)));
        } else if (animatable.hasCustomName()) {
            String name = ChatFormatting.stripFormatting(animatable.getName().getString());

            if (name != null && (name.equals("Dinnerbone") || name.equalsIgnoreCase("Grumm"))) {
                poseStack.translate(0, animatable.getBbHeight() + 0.1f, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            }
        }
    }
}
