package com.github.teamfossilsarcheology.fossil.mixin;

import com.github.teamfossilsarcheology.fossil.client.FAPlayerCapes;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {

    @Shadow
    @Final
    private GameProfile profile;

    /**
     * PORT(1.21.1): getCapeLocation/getElytraLocation were replaced by getSkin() returning a PlayerSkin record (1.20.2)
     */
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void fossilsArcheologyRevival$getCape(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin skin = cir.getReturnValue();
        if (fossilsArcheologyRevival$hasDevCape(profile.getId())) {
            cir.setReturnValue(new PlayerSkin(skin.texture(), skin.textureUrl(), FAPlayerCapes.DEVELOPER_CAPE_TEXTURE, FAPlayerCapes.DEVELOPER_ELYTRA_TEXTURE, skin.model(), skin.secure()));
        } else if (fossilsArcheologyRevival$hasContributorCape(profile.getId())) {
            cir.setReturnValue(new PlayerSkin(skin.texture(), skin.textureUrl(), FAPlayerCapes.CONTRIBUTOR_CAPE_TEXTURE, FAPlayerCapes.CONTRIBUTOR_ELYTRA_TEXTURE, skin.model(), skin.secure()));
        }
    }

    @Unique
    private boolean fossilsArcheologyRevival$hasDevCape(UUID playerId) {
        return FAPlayerCapes.DEV_UUIDS.contains(playerId);
    }

    @Unique
    private boolean fossilsArcheologyRevival$hasContributorCape(UUID playerId) {
        return FAPlayerCapes.CONTRIBUTOR_UUIDS.contains(playerId);
    }
}
