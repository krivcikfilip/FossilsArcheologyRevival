package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricFlying;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


public class C2SVerticalFlightMessage implements CustomPacketPayload {
    public static final Type<C2SVerticalFlightMessage> TYPE = new Type<>(FossilMod.location("c2s_vertical_flight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SVerticalFlightMessage> STREAM_CODEC = StreamCodec.ofMember(C2SVerticalFlightMessage::write, C2SVerticalFlightMessage::new);

    private static final int STOP = 0;
    private static final int FLY_UP = 1;
    private static final int FLY_DOWN = 2;
    private final int entityId;
    private final int code;

    public static C2SVerticalFlightMessage flyUp(int entityId) {
        return new C2SVerticalFlightMessage(entityId, FLY_UP);
    }

    public static C2SVerticalFlightMessage flyDown(int entityId) {
        return new C2SVerticalFlightMessage(entityId, FLY_DOWN);
    }

    public static C2SVerticalFlightMessage stop(int entityId) {
        return new C2SVerticalFlightMessage(entityId, STOP);
    }

    private C2SVerticalFlightMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.code = buf.readInt();
    }

    private C2SVerticalFlightMessage(int entityId, int code) {
        this.entityId = entityId;
        this.code = code;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(code);
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.CLIENT) return;
        context.queue(() -> {
            Player player = context.getPlayer();
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof PrehistoricFlying mob) {
                Entity rider = entity.getControllingPassenger();
                if (rider != null && rider.getId() == player.getId()) {
                    if (code == FLY_UP) {
                        mob.setFlyingUp(true);
                        mob.setFlyingDown(false);
                    } else if (code == FLY_DOWN) {
                        mob.setFlyingUp(false);
                        mob.setFlyingDown(true);
                    } else if (code == STOP) {
                        mob.setFlyingUp(false);
                        mob.setFlyingDown(false);
                    }
                }
            }
        });
    }

    @Override
    public Type<C2SVerticalFlightMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SVerticalFlightMessage::apply);
    }
}
