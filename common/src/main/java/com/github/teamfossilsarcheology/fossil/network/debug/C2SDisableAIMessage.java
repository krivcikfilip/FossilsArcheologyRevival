package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricDebug;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;


public class C2SDisableAIMessage implements PayloadChannel.BidirectionalPayload {
    //PORT(1.21.1): sent in both directions -> two payload ids (NeoForge forbids registering one id for both directions)
    public static final Type<C2SDisableAIMessage> TYPE_S2C = new Type<>(FossilMod.location("s2c_disable_ai"));
    public static final Type<C2SDisableAIMessage> TYPE_C2S = new Type<>(FossilMod.location("c2s_disable_ai"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDisableAIMessage> STREAM_CODEC_S2C = StreamCodec.ofMember(C2SDisableAIMessage::write, buf -> new C2SDisableAIMessage(buf).withDirection(true));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDisableAIMessage> STREAM_CODEC_C2S = StreamCodec.ofMember(C2SDisableAIMessage::write, buf -> new C2SDisableAIMessage(buf).withDirection(false));
    private boolean clientbound;
    private final int id;
    private final boolean disableAI;
    private final byte type;

    private C2SDisableAIMessage(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readBoolean(), buf.readByte());
    }

    public C2SDisableAIMessage(int id, boolean disableAI, byte type) {
        this.id = id;
        this.disableAI = disableAI;
        this.type = type;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeBoolean(disableAI);
        buf.writeByte(type);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Entity entity = context.getPlayer().level().getEntity(id);
            if (entity instanceof PrehistoricDebug prehistoric && Version.debugEnabled()) {
                prehistoric.disableCustomAI(type, disableAI);
            }
        });
    }

    private C2SDisableAIMessage withDirection(boolean clientbound) {
        C2SDisableAIMessage message = new C2SDisableAIMessage(id, disableAI, type);
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
    public Type<C2SDisableAIMessage> type() {
        return clientbound ? TYPE_S2C : TYPE_C2S;
    }

    public static void register(PayloadChannel channel) {
        channel.registerBidirectional(TYPE_S2C, STREAM_CODEC_S2C, TYPE_C2S, STREAM_CODEC_C2S, C2SDisableAIMessage::apply);
    }
}
