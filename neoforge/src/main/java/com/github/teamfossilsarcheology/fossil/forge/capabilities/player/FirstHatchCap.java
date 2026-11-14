package com.github.teamfossilsarcheology.fossil.forge.capabilities.player;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * PORT(1.21.1): stored as a NeoForge data attachment ({@link com.github.teamfossilsarcheology.fossil.forge.capabilities.ModAttachments#FIRST_HATCH})
 */
public class FirstHatchCap implements IFirstHatchCap {
    private boolean hatchedDinosaur;

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("HatchedDinosaur", hatchedDinosaur);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        setHatchedDinosaur(tag.getBoolean("HatchedDinosaur"));
    }

    @Override
    public boolean hasHatchedDinosaur() {
        return hatchedDinosaur;
    }

    @Override
    public void setHatchedDinosaur(boolean hatched) {
        this.hatchedDinosaur = hatched;
    }
}
