package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.entity.animation.AnimationCategory;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricAnimatable;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;


public class C2SForceAnimationMessage implements CustomPacketPayload {
    public static final Type<C2SForceAnimationMessage> TYPE = new Type<>(FossilMod.location("c2s_force_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SForceAnimationMessage> STREAM_CODEC = StreamCodec.ofMember(C2SForceAnimationMessage::write, C2SForceAnimationMessage::new);

    private final String controller;
    private final int entityId;
    private final String animation;
    private final double speed;
    private final int transitionLength;
    private final boolean loop;

    private C2SForceAnimationMessage(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readDouble(), buf.readInt(), buf.readBoolean());
    }

    public C2SForceAnimationMessage(String controller, int entityId, String animation, double speed, int transitionLength, boolean loop) {
        this.controller = controller;
        this.entityId = entityId;
        this.animation = animation;
        this.speed = speed;
        this.transitionLength = transitionLength;
        this.loop = loop;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(controller);
        buf.writeInt(entityId);
        buf.writeUtf(animation);
        buf.writeDouble(speed);
        buf.writeInt(transitionLength);
        buf.writeBoolean(loop);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Entity entity = context.getPlayer().level().getEntity(entityId);
            if (entity instanceof PrehistoricAnimatable<?> animatable && Version.debugEnabled()) {
                animatable.getAnimationLogic().forceAnimation(controller, animatable.getAllAnimations().get(animation), AnimationCategory.IDLE, speed, transitionLength, loop);
            }
        });
    }

    @Override
    public Type<C2SForceAnimationMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SForceAnimationMessage::apply);
    }
}
