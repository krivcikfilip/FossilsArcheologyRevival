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


/**
 * Activate a mobs attack boxes on the client. Ideally only sent to the player that is being attack
 */
public class S2CActivateAttackBoxesMessage implements CustomPacketPayload {
    public static final Type<S2CActivateAttackBoxesMessage> TYPE = new Type<>(FossilMod.location("s2c_activate_attack_boxes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CActivateAttackBoxesMessage> STREAM_CODEC = StreamCodec.ofMember(S2CActivateAttackBoxesMessage::write, S2CActivateAttackBoxesMessage::new);

    private final int entityId;
    private final double attackDuration;

    private S2CActivateAttackBoxesMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.attackDuration = buf.readDouble();
    }

    public S2CActivateAttackBoxesMessage(Entity entity, double attackDuration) {
        this.entityId = entity.getId();
        this.attackDuration = attackDuration;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(attackDuration);
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        context.queue(() -> {
            Entity entity = context.getPlayer().level().getEntity(entityId);
            if (entity instanceof Prehistoric prehistoric) {
                prehistoric.getEntityHitboxData().getAttackBoxData().activateAttackBoxes(context.getPlayer().level(), attackDuration);
            }
        });
    }

    @Override
    public Type<S2CActivateAttackBoxesMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CActivateAttackBoxesMessage::apply);
    }
}
