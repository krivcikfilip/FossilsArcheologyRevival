package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;


public class C2STameMessage implements CustomPacketPayload {
    public static final Type<C2STameMessage> TYPE = new Type<>(FossilMod.location("c2s_tame"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2STameMessage> STREAM_CODEC = StreamCodec.ofMember(C2STameMessage::write, C2STameMessage::new);

    private final int id;

    private C2STameMessage(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public C2STameMessage(int id) {
        this.id = id;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (Version.debugEnabled()) {
                Entity entity = context.getPlayer().level().getEntity(id);
                if (entity instanceof TamableAnimal animal) {
                    animal.tame(context.getPlayer());
                } else if (entity instanceof AbstractHorse horse) {
                    horse.tameWithName(context.getPlayer());
                }
            }
        });
    }

    @Override
    public Type<C2STameMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2STameMessage::apply);
    }
}
