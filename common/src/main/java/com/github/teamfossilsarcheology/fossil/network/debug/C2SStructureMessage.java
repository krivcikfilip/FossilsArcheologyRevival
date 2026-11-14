package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.client.gui.debug.instruction.Instruction;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricEntityInfo;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricFlying;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricSwimming;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class C2SStructureMessage implements CustomPacketPayload {
    public static final Type<C2SStructureMessage> TYPE = new Type<>(FossilMod.location("c2s_structure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SStructureMessage> STREAM_CODEC = StreamCodec.ofMember(C2SStructureMessage::write, C2SStructureMessage::new);

    private final boolean onlyStructure;

    private C2SStructureMessage(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public C2SStructureMessage(boolean onlyStructure) {
        this.onlyStructure = onlyStructure;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(onlyStructure);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (Version.debugEnabled() && context.getPlayer().level() instanceof ServerLevel serverLevel) {
                spawnTestStructure(serverLevel, onlyStructure);
            }
        });
    }

    private static void spawnTestStructure(ServerLevel level, boolean onlyStructure) {
        int length = 0;
        List<Prehistoric> toAdd = new ArrayList<>();
        for (PrehistoricEntityInfo info : PrehistoricEntityInfo.values()) {
            Entity entity = info.entityType().create(level);
            if (entity instanceof Prehistoric prehistoric) {
                toAdd.add(prehistoric);
                length++;
            }
        }
        BlockState ironBlock = Blocks.JUNGLE_PLANKS.defaultBlockState();
        for (BlockPos blockPos : BlockPos.betweenClosed(0, 119, 0, length * 5 + 20, 119, 50)) {
            level.setBlock(blockPos, ironBlock, 2);
        }
        for (int i = 0; i < toAdd.size(); i++) {
            Prehistoric prehistoric = toAdd.get(i);
            double xOffset = i * 5d + 3;
            List<Instruction> instructions = List.of(
                    new Instruction.TeleportTo(BlockPos.containing(xOffset, 120, 0), 0),
                    new Instruction.MoveTo(BlockPos.containing(xOffset, 120, 45)));
            prehistoric.finalizeSpawn(level, level.getCurrentDifficultyAt(prehistoric.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            prehistoric.moveTo(xOffset, 120, 0, 0, 0);
            if (prehistoric instanceof PrehistoricSwimming swimming && !swimming.isAmphibious()) {
                swimming.setNoAi(true);
            } else {
                prehistoric.getInstructionSystem().start(instructions, true, true);
                //prehistoric.setNoAi(true);
            }
            if (prehistoric instanceof PrehistoricFlying flying) {
                flying.setNoAi(true);
            }
            prehistoric.getInstructionSystem().syncWithClients();
            level.addFreshEntity(prehistoric);
            /*
            PrehistoricLeaping velociraptor = ModEntities.DEINONYCHUS.get().create(level);
            velociraptor.moveTo(xOffset, 120, 0, 0, 0);
            velociraptor.finalizeSpawn(level, level.getCurrentDifficultyAt(velociraptor.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            velociraptor.startAttackRiding(prehistoric);
            velociraptor.startRiding(prehistoric, true);
            level.addFreshEntity(velociraptor);
            */
        }
    }

    @Override
    public Type<C2SStructureMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SStructureMessage::apply);
    }
}
