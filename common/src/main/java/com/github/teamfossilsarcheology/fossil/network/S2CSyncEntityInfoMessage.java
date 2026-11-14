package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.data.EntityDataLoader;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * Sync {@link com.github.teamfossilsarcheology.fossil.entity.data.EntityDataLoader.Data entity data} on server join to client
 */
public class S2CSyncEntityInfoMessage implements CustomPacketPayload {
    public static final Type<S2CSyncEntityInfoMessage> TYPE = new Type<>(FossilMod.location("s2c_sync_entity_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncEntityInfoMessage> STREAM_CODEC = StreamCodec.ofMember(S2CSyncEntityInfoMessage::write, S2CSyncEntityInfoMessage::new);

    private final Map<String, EntityDataLoader.Data> data;

    private S2CSyncEntityInfoMessage(FriendlyByteBuf buf) {
        data = buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, EntityDataLoader.Data::readBuf);
    }

    public S2CSyncEntityInfoMessage(Map<String, EntityDataLoader.Data> data) {
        this.data = data;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeMap(data, FriendlyByteBuf::writeUtf, EntityDataLoader.Data::writeBuf);
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        context.queue(() -> EntityDataLoader.INSTANCE.replaceData(data));
    }

    @Override
    public Type<S2CSyncEntityInfoMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CSyncEntityInfoMessage::apply);
    }
}
