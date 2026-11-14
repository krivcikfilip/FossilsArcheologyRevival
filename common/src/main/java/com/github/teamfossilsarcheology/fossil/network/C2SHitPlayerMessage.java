package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


/**
 * Cause a player hit on the server side. Will only trigger for the player that sent the message
 */
public class C2SHitPlayerMessage implements CustomPacketPayload {
    public static final Type<C2SHitPlayerMessage> TYPE = new Type<>(FossilMod.location("c2s_hit_player"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SHitPlayerMessage> STREAM_CODEC = StreamCodec.ofMember(C2SHitPlayerMessage::write, C2SHitPlayerMessage::new);

    private final int entityId;
    private final int targetId;

    private C2SHitPlayerMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.targetId = buf.readInt();
    }

    public C2SHitPlayerMessage(Entity entity, Entity target) {
        this.entityId = entity.getId();
        this.targetId = target.getId();
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(targetId);
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.CLIENT) return;
        context.queue(() -> {
            Player player = context.getPlayer();
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof Prehistoric prehistoric && player.getId() == targetId) {
                prehistoric.attackTarget(player);
            }
        });
    }

    @Override
    public Type<C2SHitPlayerMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SHitPlayerMessage::apply);
    }
}
