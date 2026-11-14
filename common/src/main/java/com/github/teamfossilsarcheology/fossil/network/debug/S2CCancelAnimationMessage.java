package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricAnimatable;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;


public class S2CCancelAnimationMessage implements CustomPacketPayload {
    public static final Type<S2CCancelAnimationMessage> TYPE = new Type<>(FossilMod.location("s2c_cancel_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCancelAnimationMessage> STREAM_CODEC = StreamCodec.ofMember(S2CCancelAnimationMessage::write, S2CCancelAnimationMessage::new);

    private final int entityId;
    private final String controller;

    private S2CCancelAnimationMessage(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readUtf());
    }

    public S2CCancelAnimationMessage(int entityId, String controller) {
        this.entityId = entityId;
        this.controller = controller;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(controller);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Entity entity = context.getPlayer().level().getEntity(entityId);
            if (entity instanceof PrehistoricAnimatable<?> prehistoric && Version.debugEnabled()) {
                prehistoric.getAnimationLogic().cancelAnimation(controller);
            }
        });
    }

    @Override
    public Type<S2CCancelAnimationMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CCancelAnimationMessage::apply);
    }
}
