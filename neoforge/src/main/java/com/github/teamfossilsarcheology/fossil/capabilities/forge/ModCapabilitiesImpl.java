package com.github.teamfossilsarcheology.fossil.capabilities.forge;

import com.github.teamfossilsarcheology.fossil.block.entity.CommonEnergyStorage;
import com.github.teamfossilsarcheology.fossil.config.FossilConfig;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.EntityInfo;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricEntityInfo;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.ModAttachments;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.mammal.IMammalCap;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.player.IFirstHatchCap;
import com.github.teamfossilsarcheology.fossil.forge.energy.FAEnergyStorage;
import com.github.teamfossilsarcheology.fossil.network.MessageHandler;
import com.github.teamfossilsarcheology.fossil.network.S2CMammalCapMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @see com.github.teamfossilsarcheology.fossil.capabilities.ModCapabilities
 * PORT(1.21.1): Forge capabilities -> NeoForge data attachments. The LazyOptional caches are no longer needed.
 */
public class ModCapabilitiesImpl {

    /**
     * Returns the mammal data if it already exists. Mirrors the old capability which was only attached to mammals:
     * non-mammals never get the attachment.
     */
    public static Optional<IMammalCap> getMammalCap(Animal animal) {
        Optional<IMammalCap> existing = animal.getExistingData(ModAttachments.MAMMAL);
        if (existing.isPresent()) {
            return existing;
        }
        //Fix crash when Minecolonies is installed https://github.com/ldtteam/minecolonies/pull/11498
        if (animal.getType() != null && PrehistoricEntityInfo.isMammal(animal)) {
            return Optional.of(animal.getData(ModAttachments.MAMMAL));
        }
        return Optional.empty();
    }

    public static Optional<IFirstHatchCap> getFirstHatchCap(Player player) {
        return Optional.of(player.getData(ModAttachments.FIRST_HATCH));
    }

    public static boolean hasEmbryo(Animal animal) {
        //Reading does not need to create the attachment
        return animal.getExistingData(ModAttachments.MAMMAL).map(iMammalCap -> iMammalCap.getEmbryo() != null).orElse(false);
    }

    public static int getEmbryoProgress(Animal animal) {
        return animal.getExistingData(ModAttachments.MAMMAL).map(IMammalCap::getEmbryoProgress).orElse(0);
    }

    public static EntityInfo getEmbryo(Animal animal) {
        return animal.getExistingData(ModAttachments.MAMMAL).map(IMammalCap::getEmbryo).orElse(null);
    }

    public static void setEmbryoProgress(Animal animal, int embryoProgress) {
        getMammalCap(animal).ifPresent(iMammalCap -> iMammalCap.setEmbryoProgress(embryoProgress));
    }

    public static void setEmbryo(Animal animal, @Nullable EntityInfo embryo) {
        getMammalCap(animal).ifPresent(iMammalCap -> iMammalCap.setEmbryo(embryo));
    }

    public static void syncMammalWithClient(Animal animal, int embryoProgress, EntityInfo embryo) {
        //PORT(1.21.1): network API owned by core (PayloadChannel keeps sendToPlayers)
        MessageHandler.CAP_CHANNEL.sendToPlayers(((ServerLevel) animal.level()).getPlayers(serverPlayer -> true),
                new S2CMammalCapMessage(animal, embryoProgress, embryo));
    }

    public static boolean hasHatchedDinosaur(Player player) {
        return getFirstHatchCap(player).map(IFirstHatchCap::hasHatchedDinosaur).orElse(false);
    }

    public static void setHatchedDinosaur(Player player, boolean hatched) {
        getFirstHatchCap(player).ifPresent(iFirstHatchCap -> iFirstHatchCap.setHatchedDinosaur(hatched));
    }

    public static CommonEnergyStorage createEnergyStorage(Runnable setChanged) {
        return new FAEnergyStorage(FossilConfig.getInt(FossilConfig.MACHINE_MAX_ENERGY), FossilConfig.getInt(FossilConfig.MACHINE_TRANSFER_RATE), FossilConfig.getInt(FossilConfig.MACHINE_ENERGY_USAGE), 0) {
            @Override
            protected void onChange() {
                setChanged.run();
            }
        };
    }
}
