package com.github.teamfossilsarcheology.fossil.world.effect;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(FossilMod.MOD_ID, Registries.MOB_EFFECT);

    //PORT(1.21.1): MobEffectInstance/hasEffect take Holder<MobEffect> (1.20.5). Use BuiltInRegistries.MOB_EFFECT.wrapAsHolder(COMFY_BED.get())
    //(as neoforge's ForgeModEvents does) or the RegistrySupplier itself, which implements Holder in Architectury 13
    public static final RegistrySupplier<MobEffect> COMFY_BED =MOB_EFFECTS.register("comfy_bed", () -> new ComfyBedEffect(MobEffectCategory.BENEFICIAL, 0x70573d));

    public static void register() {
        MOB_EFFECTS.register();
    }
}
