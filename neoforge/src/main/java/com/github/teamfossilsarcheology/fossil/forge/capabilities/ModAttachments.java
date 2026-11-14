package com.github.teamfossilsarcheology.fossil.forge.capabilities;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.mammal.IMammalCap;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.mammal.MammalCap;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.player.FirstHatchCap;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.player.IFirstHatchCap;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * PORT(1.21.1): Forge entity capabilities (MammalCapProvider/FirstHatchCapProvider + AttachCapabilitiesEvent) were replaced
 * by NeoForge data attachments. The ids are the same as the old capability ids.
 * <p>
 * PORT-TODO(1.21.1): data saved by 1.20.1 under "ForgeCaps" is not migrated (attachments live in "neoforge:attachments"),
 * so pregnancies in progress and the "first hatch" flag reset when an old world is loaded.
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FossilMod.MOD_ID);

    /**
     * Only read/created for mammals, see {@link com.github.teamfossilsarcheology.fossil.capabilities.forge.ModCapabilitiesImpl}
     */
    public static final Supplier<AttachmentType<IMammalCap>> MAMMAL = ATTACHMENT_TYPES.register("mammal",
            () -> AttachmentType.<CompoundTag, IMammalCap>serializable(() -> new MammalCap()).build());

    /**
     * copyOnDeath replaces the old PlayerEvent.Clone capability copy (non-death clones are handled in ForgeModEvents)
     */
    public static final Supplier<AttachmentType<IFirstHatchCap>> FIRST_HATCH = ATTACHMENT_TYPES.register("player",
            () -> AttachmentType.<CompoundTag, IFirstHatchCap>serializable(() -> new FirstHatchCap()).copyOnDeath().build());

    private ModAttachments() {
    }
}
