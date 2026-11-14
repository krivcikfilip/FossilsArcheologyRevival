package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.variant.EntityVariantLoader;
import com.github.teamfossilsarcheology.fossil.entity.variant.Variant;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * Sync {@link Variant entity variants} on server join to client
 */
public class S2CSyncEntityVariantsMessage implements CustomPacketPayload {
    public static final Type<S2CSyncEntityVariantsMessage> TYPE = new Type<>(FossilMod.location("s2c_sync_entity_variants"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncEntityVariantsMessage> STREAM_CODEC = StreamCodec.ofMember(S2CSyncEntityVariantsMessage::write, S2CSyncEntityVariantsMessage::new);

    private final Map<String, Map<String, Variant>> data;

    private S2CSyncEntityVariantsMessage(FriendlyByteBuf buf) {
        data = buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, friendlyByteBuf -> friendlyByteBuf.readMap(HashMap::new, FriendlyByteBuf::readUtf, Variant::readBuf));
    }

    public S2CSyncEntityVariantsMessage(Map<String, Map<String, Variant>> data) {
        this.data = data;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeMap(data, FriendlyByteBuf::writeUtf, (innerBuf, map) -> innerBuf.writeMap(map, FriendlyByteBuf::writeUtf, Variant::writeBuf));
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        context.queue(() -> EntityVariantLoader.INSTANCE.replaceVariants(data));
    }

    @Override
    public Type<S2CSyncEntityVariantsMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CSyncEntityVariantsMessage::apply);
    }
}
