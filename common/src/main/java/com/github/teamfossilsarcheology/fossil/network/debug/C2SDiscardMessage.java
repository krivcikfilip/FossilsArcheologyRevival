package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.stream.StreamSupport;

public class C2SDiscardMessage implements CustomPacketPayload {
    public static final Type<C2SDiscardMessage> TYPE = new Type<>(FossilMod.location("c2s_discard"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDiscardMessage> STREAM_CODEC = StreamCodec.ofMember(C2SDiscardMessage::write, C2SDiscardMessage::new);

    private final int entityId;

    private C2SDiscardMessage(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public C2SDiscardMessage(int entityId) {
        this.entityId = entityId;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player.level() instanceof ServerLevel serverLevel) {
                if (entityId == -1) {
                    var list = StreamSupport.stream(serverLevel.getAllEntities().spliterator(), false)
                            .filter(entity -> !(entity instanceof Player)).toList();
                    list.forEach(Entity::discard);
                } else {
                    StreamSupport.stream(serverLevel.getAllEntities().spliterator(), false)
                            .filter(entity -> entity.getId() == entityId).forEach(Entity::discard);
                }
            }
        });
    }

    @Override
    public Type<C2SDiscardMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SDiscardMessage::apply);
    }
}
