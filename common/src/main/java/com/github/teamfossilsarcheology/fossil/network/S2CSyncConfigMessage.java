package com.github.teamfossilsarcheology.fossil.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.config.FossilConfig;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sync certain config options on server join to client. Also called when the player joins a client world to reset overrides
 */
public class S2CSyncConfigMessage implements CustomPacketPayload {
    public static final Type<S2CSyncConfigMessage> TYPE = new Type<>(FossilMod.location("s2c_sync_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncConfigMessage> STREAM_CODEC = StreamCodec.ofMember(S2CSyncConfigMessage::write, S2CSyncConfigMessage::new);

    private final Map<String, Integer> ints;
    private final Map<String, Boolean> bools;

    private S2CSyncConfigMessage(FriendlyByteBuf buf) {
        ints = buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readInt);
        bools = buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readBoolean);
    }

    public S2CSyncConfigMessage() {
        ints = new HashMap<>();
        bools = new HashMap<>();
        ints.put(FossilConfig.PREGNANCY_DURATION, FossilConfig.getInt(FossilConfig.PREGNANCY_DURATION));
        ints.put(FossilConfig.ANUBITE_COOLDOWN, FossilConfig.getInt(FossilConfig.ANUBITE_COOLDOWN));
        ints.put(FossilConfig.MACHINE_MAX_ENERGY, FossilConfig.getInt(FossilConfig.MACHINE_MAX_ENERGY));
        bools.put(FossilConfig.HEALING_DINOS, FossilConfig.isEnabled(FossilConfig.HEALING_DINOS));
        bools.put(FossilConfig.WHIP_TO_TAME_DINO, FossilConfig.isEnabled(FossilConfig.WHIP_TO_TAME_DINO));
        bools.put(FossilConfig.MACHINES_REQUIRE_ENERGY, FossilConfig.isEnabled(FossilConfig.MACHINES_REQUIRE_ENERGY));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeMap(ints, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeInt);
        buf.writeMap(bools, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeBoolean);
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        FossilMod.LOGGER.info("Received config from the server: {}", ints.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(", ", "{", "}")));
        FossilMod.LOGGER.info("Received config from the server: {}", bools.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(", ", "{", "}")));
        FossilConfig.overrideEntries(ints, bools);
    }

    @Override
    public Type<S2CSyncConfigMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CSyncConfigMessage::apply);
    }
}
