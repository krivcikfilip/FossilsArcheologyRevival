package com.github.teamfossilsarcheology.fossil.forge.capabilities;

import com.github.teamfossilsarcheology.fossil.block.entity.EnergyContainerBlockEntity;
import com.github.teamfossilsarcheology.fossil.block.entity.MachineContainerBlockEntity;
import com.github.teamfossilsarcheology.fossil.block.entity.ModBlockEntities;
import com.github.teamfossilsarcheology.fossil.config.FossilConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * PORT(1.21.1): replaces the Forge-only mixins EnergyContainerBlockEntityMixin and MachineContainerBlockEntityMixin, which
 * overrode {@code getCapability}. NeoForge 20.3+ block capabilities are registered per block entity type and are invalidated
 * automatically when the block entity is removed.
 */
public class ModBlockCapabilities {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerMachine(event, ModBlockEntities.ANALYZER.get());
        registerMachine(event, ModBlockEntities.CULTURE_VAT.get());
        registerMachine(event, ModBlockEntities.SIFTER.get());
        registerMachine(event, ModBlockEntities.WORKTABLE.get());
        registerEnergy(event, ModBlockEntities.ANALYZER.get());
        registerEnergy(event, ModBlockEntities.CULTURE_VAT.get());
        //Forge's BaseContainerBlockEntity exposed an unsided InvWrapper for every container by default; keep that for the feeder
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.FEEDER.get(), (feeder, side) -> new InvWrapper(feeder));
    }

    private static <T extends MachineContainerBlockEntity> void registerMachine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, ModBlockCapabilities::getItemHandler);
    }

    private static <T extends EnergyContainerBlockEntity> void registerEnergy(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, ModBlockCapabilities::getEnergyStorage);
    }

    /**
     * Same side mapping as the old mixin: UP and DOWN get their own wrapper, every horizontal side uses the NORTH wrapper and
     * the null side falls back to an unsided wrapper (Forge's BaseContainerBlockEntity default).
     */
    private static @Nullable IItemHandler getItemHandler(MachineContainerBlockEntity machine, @Nullable Direction side) {
        if (side == null) {
            return new InvWrapper(machine);
        }
        if (side == Direction.UP || side == Direction.DOWN) {
            return new SidedInvWrapper(machine, side);
        }
        return new SidedInvWrapper(machine, Direction.NORTH);
    }

    private static @Nullable IEnergyStorage getEnergyStorage(EnergyContainerBlockEntity machine, @Nullable Direction side) {
        if (side != null && FossilConfig.isEnabled(FossilConfig.MACHINES_REQUIRE_ENERGY)
                && machine.getEnergyStorage() instanceof IEnergyStorage storage) {
            return storage;
        }
        return null;
    }
}
