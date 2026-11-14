package com.github.teamfossilsarcheology.fossil.block.entity;

import net.minecraft.core.HolderLookup;
import com.github.teamfossilsarcheology.fossil.capabilities.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EnergyContainerBlockEntity extends MachineContainerBlockEntity {
    protected final CommonEnergyStorage energyStorage = ModCapabilities.createEnergyStorage(this::setChanged);

    protected EnergyContainerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.load(tag.get("Energy"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energyStorage.save());
    }

    public CommonEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
