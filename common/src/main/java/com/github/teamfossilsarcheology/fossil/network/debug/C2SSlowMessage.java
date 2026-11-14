package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;


public class C2SSlowMessage implements CustomPacketPayload {
    public static final Type<C2SSlowMessage> TYPE = new Type<>(FossilMod.location("c2s_slow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSlowMessage> STREAM_CODEC = StreamCodec.ofMember(C2SSlowMessage::write, C2SSlowMessage::new);

    private final double modifier;

    private C2SSlowMessage(FriendlyByteBuf buf) {
        this(buf.readDouble());
    }

    public C2SSlowMessage(double modifier) {
        this.modifier = modifier;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(modifier);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            if (!player.level().isClientSide) {
                var instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
                instance.removeModifiers();

                AttributeModifier attributeModifier = new AttributeModifier(FossilMod.location("debug_slow_down"), -(1-modifier), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                instance.addPermanentModifier(attributeModifier);
            }
        });
    }

    @Override
    public Type<C2SSlowMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SSlowMessage::apply);
    }
}
