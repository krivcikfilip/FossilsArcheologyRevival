package com.github.teamfossilsarcheology.fossil.advancements;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;

/**
 * PORT(1.21.1): triggers are registered in Registries.TRIGGER_TYPE (CriteriaTriggers.register(T) no longer exists and
 * registering into the frozen built-in registry would fail). The static instances are kept so call sites don't change.
 */
public class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(FossilMod.MOD_ID, Registries.TRIGGER_TYPE);

    public static final OpenSarcophagusTrigger OPEN_SARCOPHAGUS_TRIGGER = new OpenSarcophagusTrigger();
    public static final ScarabTameTrigger SCARAB_TAME_TRIGGER = new ScarabTameTrigger();
    public static final ImplantEmbryoTrigger IMPLANT_EMBRYO_TRIGGER = new ImplantEmbryoTrigger();
    public static final IncubateEggTrigger INCUBATE_EGG_TRIGGER = new IncubateEggTrigger();
    private static boolean registered;

    static {
        TRIGGERS.register("open_sarcophagus", () -> OPEN_SARCOPHAGUS_TRIGGER);
        TRIGGERS.register("scarab_tame", () -> SCARAB_TAME_TRIGGER);
        TRIGGERS.register("implant_embryo", () -> IMPLANT_EMBRYO_TRIGGER);
        TRIGGERS.register("incubate_egg", () -> INCUBATE_EGG_TRIGGER);
    }

    /**
     * Called from FossilMod#init. Safe to call more than once (the platform module used to call it as well)
     */
    public static void register() {
        if (!registered) {
            registered = true;
            TRIGGERS.register();
        }
    }
}
