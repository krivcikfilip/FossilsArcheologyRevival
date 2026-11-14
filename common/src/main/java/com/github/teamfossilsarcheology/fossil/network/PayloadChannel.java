package com.github.teamfossilsarcheology.fossil.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.21.1 replacement for the deprecated Architectury {@code NetworkChannel}. Keeps the old call-site shape
 * ({@code MessageHandler.SYNC_CHANNEL.sendToPlayer(player, msg)}) but every message is now a {@link CustomPacketPayload}
 * with its own {@link CustomPacketPayload.Type} and {@link StreamCodec}.
 * <p>
 * Registration rules (Architectury 13 on NeoForge):
 * <ul>
 *     <li>C2S: {@link NetworkManager#registerReceiver} on both sides.</li>
 *     <li>S2C: {@link NetworkManager#registerReceiver} on the client, {@link NetworkManager#registerS2CPayloadType} on a
 *     dedicated server (the codec must be known to encode).</li>
 *     <li>One payload id can only be registered for ONE direction on NeoForge. Messages that travel both ways implement
 *     {@link BidirectionalPayload} and use two ids, see {@link #registerBidirectional}.</li>
 * </ul>
 */
public final class PayloadChannel {
    private final String name;

    private PayloadChannel(String name) {
        this.name = name;
    }

    public static PayloadChannel create(String name) {
        return new PayloadChannel(name);
    }

    public String getName() {
        return name;
    }

    /**
     * Registers a server -> client payload
     */
    public <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkManager.NetworkReceiver<T> receiver) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, type, codec, receiver);
        } else {
            NetworkManager.registerS2CPayloadType(type, codec);
        }
    }

    /**
     * Registers a client -> server payload
     */
    public <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkManager.NetworkReceiver<T> receiver) {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, type, codec, receiver);
    }

    /**
     * Registers a payload that can be sent in both directions. Needs two distinct types because NeoForge does not allow
     * registering the same id twice.
     */
    public <T extends BidirectionalPayload> void registerBidirectional(CustomPacketPayload.Type<T> s2cType, StreamCodec<? super RegistryFriendlyByteBuf, T> s2cCodec,
                                                                     CustomPacketPayload.Type<T> c2sType, StreamCodec<? super RegistryFriendlyByteBuf, T> c2sCodec,
                                                                     NetworkManager.NetworkReceiver<T> receiver) {
        registerS2C(s2cType, s2cCodec, receiver);
        registerC2S(c2sType, c2sCodec, receiver);
    }

    public <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player, T message) {
        NetworkManager.sendToPlayer(player, clientbound(message));
    }

    public <T extends CustomPacketPayload> void sendToPlayers(Iterable<ServerPlayer> players, T message) {
        NetworkManager.sendToPlayers(players, clientbound(message));
    }

    /**
     * Client only
     */
    public <T extends CustomPacketPayload> void sendToServer(T message) {
        NetworkManager.sendToServer(serverbound(message));
    }

    private static CustomPacketPayload clientbound(CustomPacketPayload message) {
        if (message instanceof BidirectionalPayload bidirectional) {
            return bidirectional.toClientbound();
        }
        return message;
    }

    private static CustomPacketPayload serverbound(CustomPacketPayload message) {
        if (message instanceof BidirectionalPayload bidirectional) {
            return bidirectional.toServerbound();
        }
        return message;
    }

    /**
     * A payload that is sent in both directions. {@link #type()} must return the type matching the current direction.
     */
    public interface BidirectionalPayload extends CustomPacketPayload {
        BidirectionalPayload toClientbound();

        BidirectionalPayload toServerbound();
    }
}
