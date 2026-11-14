package com.github.teamfossilsarcheology.fossil.client.renderer.entity.layers;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.client.model.AnuTotemModel;
import com.github.teamfossilsarcheology.fossil.entity.AnuTotem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

public class AnuTotemOverlayRenderer extends RenderLayer<AnuTotem, AnuTotemModel> {
    private static final ResourceLocation TEXTURE = FossilMod.location("textures/entity/anu_totem_layer.png");

    public AnuTotemOverlayRenderer(RenderLayerParent<AnuTotem, AnuTotemModel> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AnuTotem livingEntity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        //PORT(1.21.1): tint is a packed ARGB int (alpha was tickCount / 200f)
        getParentModel().renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color((int) (livingEntity.tickCount / 200f * 255), 255, 255, 255));
    }
}
