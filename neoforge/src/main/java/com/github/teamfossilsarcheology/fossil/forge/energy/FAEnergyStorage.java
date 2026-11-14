package com.github.teamfossilsarcheology.fossil.forge.energy;

import com.github.teamfossilsarcheology.fossil.block.entity.CommonEnergyStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

public abstract class FAEnergyStorage extends EnergyStorage implements CommonEnergyStorage {

    protected FAEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received != 0) {
            onChange();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted != 0) {
            onChange();
        }
        return extracted;
    }

    protected abstract void onChange();

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (nbt == null) {
            energy = 0;
        } else {
            super.deserializeNBT(provider, nbt);
        }
    }

    @Override
    public int getEnergy() {
        return getEnergyStored();
    }

    @Override
    public void extractEnergy(int maxExtract) {
        extractEnergy(maxExtract, false);
    }

    //PORT(1.21.1): NeoForge's EnergyStorage (de)serialization takes a HolderLookup.Provider which it does not use (plain IntTag).
    //CommonEnergyStorage (core) keeps the provider-less signatures, so the IntTag logic is inlined here.
    //PORT-TODO(1.21.1): if core changes CommonEnergyStorage#load/save to take a HolderLookup.Provider, forward it instead.
    @Override
    public void load(Tag tag) {
        if (tag instanceof IntTag intTag) {
            energy = intTag.getAsInt();
        } else {
            energy = 0;
        }
    }

    @Override
    public Tag save() {
        return IntTag.valueOf(getEnergyStored());
    }
}
