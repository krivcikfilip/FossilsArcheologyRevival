package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import com.github.teamfossilsarcheology.fossil.client.gui.debug.InstructionTab;
import com.github.teamfossilsarcheology.fossil.client.gui.debug.instruction.Instruction;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

public class InstructionMessage implements PayloadChannel.BidirectionalPayload {
    //PORT(1.21.1): sent in both directions -> two payload ids (NeoForge forbids registering one id for both directions)
    public static final Type<InstructionMessage> TYPE_S2C = new Type<>(FossilMod.location("s2c_instruction"));
    public static final Type<InstructionMessage> TYPE_C2S = new Type<>(FossilMod.location("c2s_instruction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstructionMessage> STREAM_CODEC_S2C = StreamCodec.ofMember(InstructionMessage::write, buf -> new InstructionMessage(buf).withDirection(true));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstructionMessage> STREAM_CODEC_C2S = StreamCodec.ofMember(InstructionMessage::write, buf -> new InstructionMessage(buf).withDirection(false));
    private boolean clientbound;
    private final int entityId;
    private final boolean loop;
    private final List<Instruction> instructions;

    private InstructionMessage(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readBoolean(), Instruction.decodeBuffer(buf));
    }

    public InstructionMessage(int entityId, boolean loop, List<Instruction> instructions) {
        this.entityId = entityId;
        this.loop = loop;
        this.instructions = instructions;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(loop);
        Instruction.encodeBuffer(instructions, buf);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (Version.debugEnabled()) {
                Level level = context.getPlayer().level();
                Entity entity = level.getEntity(entityId);
                if (entity instanceof Prehistoric prehistoric) {
                    if (level.isClientSide) {
                        if (InstructionTab.activeEntity == null || InstructionTab.activeEntity.getId() != entity.getId()) {
                            InstructionTab.INSTRUCTIONS.put(entity.getUUID(), new InstructionTab.Pair(entityId, instructions));
                        }
                    } else {
                        prehistoric.getInstructionSystem().start(instructions, loop, !instructions.isEmpty());
                    }
                }
            }
        });
    }

    private InstructionMessage withDirection(boolean clientbound) {
        InstructionMessage message = new InstructionMessage(entityId, loop, instructions);
        message.clientbound = clientbound;
        return message;
    }

    @Override
    public PayloadChannel.BidirectionalPayload toClientbound() {
        return withDirection(true);
    }

    @Override
    public PayloadChannel.BidirectionalPayload toServerbound() {
        return withDirection(false);
    }

    @Override
    public Type<InstructionMessage> type() {
        return clientbound ? TYPE_S2C : TYPE_C2S;
    }

    public static void register(PayloadChannel channel) {
        channel.registerBidirectional(TYPE_S2C, STREAM_CODEC_S2C, TYPE_C2S, STREAM_CODEC_C2S, InstructionMessage::apply);
    }
}
