package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricFlying;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


public class C2SRiderForceFlyingMessage implements CustomPacketPayload {
    public static final Type<C2SRiderForceFlyingMessage> TYPE = new Type<>(FossilMod.location("c2s_rider_force_flying"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRiderForceFlyingMessage> STREAM_CODEC = StreamCodec.ofMember(C2SRiderForceFlyingMessage::write, C2SRiderForceFlyingMessage::new);

    private static final Component FLYING_DISABLED = Component.translatable("entity.fossil.flying.disabled");
    private final int entityId;
    private final boolean flying;

    private C2SRiderForceFlyingMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.flying = buf.readBoolean();
    }

    public C2SRiderForceFlyingMessage(int entityId, boolean flying) {
        this.entityId = entityId;
        this.flying = flying;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(flying);
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.CLIENT) return;
        context.queue(() -> {
            Player player = context.getPlayer();
            Entity entity = player.level().getEntity(entityId);
            if (player.getServer() != null && !player.getServer().isFlightAllowed()) {
                player.displayClientMessage(FLYING_DISABLED, false);
            } else if (entity instanceof PrehistoricFlying mob) {
                Entity rider = entity.getControllingPassenger();
                if (rider != null && rider.getId() == player.getId()) {
                    if (flying) {
                        if (mob.hasTakeOffAnimation()) {
                            mob.startTakeOff();
                        } else {
                            mob.setFlying(true);
                        }
                    } else {
                        mob.setFlying(true);
                        mob.setFlying(false);
                    }
                }
            }
        });
    }

    @Override
    public Type<C2SRiderForceFlyingMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SRiderForceFlyingMessage::apply);
    }
}
