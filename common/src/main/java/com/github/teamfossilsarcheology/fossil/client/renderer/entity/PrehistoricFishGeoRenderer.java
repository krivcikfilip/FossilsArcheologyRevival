package com.github.teamfossilsarcheology.fossil.client.renderer.entity;

import com.github.teamfossilsarcheology.fossil.client.model.PrehistoricFishGeoModel;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricFish;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.fish.Nautilus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PrehistoricFishGeoRenderer<T extends PrehistoricFish> extends GeoEntityRenderer<T> {

    public PrehistoricFishGeoRenderer(EntityRendererProvider.Context renderManager, String model) {
        super(renderManager, new PrehistoricFishGeoModel<>(model));
    }

    /**
     * PORT(1.21.1): GeckoLib 4.9 GeoEntityRenderer#actuallyRender already scales by {@code LivingEntity#getScale()} ("nativeScale"),
     * so {@code withScale(animatable.getScale())} is no longer applied here (it would scale twice).
     */
    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    //PORT(1.21.1): GeckoLib 4.9 only calls the overload with nativeScale
    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        if (animatable instanceof Nautilus) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = entity.getBbWidth() * 0.45F;
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}