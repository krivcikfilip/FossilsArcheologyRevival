package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.ToyTetheredLog;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;


/**
 * Signals the start of a toy animation
 */
public class S2CSyncToyAnimationMessage implements CustomPacketPayload {
    public static final Type<S2CSyncToyAnimationMessage> TYPE = new Type<>(FossilMod.location("s2c_sync_toy_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncToyAnimationMessage> STREAM_CODEC = StreamCodec.ofMember(S2CSyncToyAnimationMessage::write, S2CSyncToyAnimationMessage::new);

    private final int entityId;
    private final float animationX;
    private final float animationZ;

    private S2CSyncToyAnimationMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.animationX = buf.readFloat();
        this.animationZ = buf.readFloat();
    }

    public S2CSyncToyAnimationMessage(int entityId, float animationX, float animationZ) {
        this.entityId = entityId;
        this.animationX = animationX;
        this.animationZ = animationZ;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeFloat(animationX);
        buf.writeFloat(animationZ);
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        context.queue(() -> {
            Entity entity = context.getPlayer().level().getEntity(entityId);
            if (entity instanceof ToyTetheredLog toy) {
                toy.startAnimation(animationX, animationZ);
            }
        });
    }

    @Override
    public Type<S2CSyncToyAnimationMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CSyncToyAnimationMessage::apply);
    }
}
