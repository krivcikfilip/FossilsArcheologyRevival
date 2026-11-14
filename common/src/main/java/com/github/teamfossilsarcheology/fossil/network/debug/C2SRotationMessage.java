package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;


public class C2SRotationMessage implements CustomPacketPayload {
    public static final Type<C2SRotationMessage> TYPE = new Type<>(FossilMod.location("c2s_rotation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRotationMessage> STREAM_CODEC = StreamCodec.ofMember(C2SRotationMessage::write, C2SRotationMessage::new);

    public static final byte Y_ROT = 0;
    public static final byte X_ROT = 1;
    private final int entityId;
    private final double rotation;
    private final byte flag;

    private C2SRotationMessage(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readDouble(), buf.readByte());
    }

    public C2SRotationMessage(int entityId, double rotation, byte flag) {
        this.entityId = entityId;
        this.rotation = rotation;
        this.flag = flag;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(rotation);
        buf.writeByte(flag);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (Version.debugEnabled()) {
                Entity entity = context.getPlayer().level().getEntity(entityId);
                switch (flag) {
                    case 0 -> {
                        if (entity instanceof LivingEntity) {
                            entity.setYBodyRot((float) rotation);
                            entity.setYHeadRot((float) rotation);
                        } else {
                            entity.setYRot((float) rotation);
                        }
                    }
                    case 1 -> {
                        if (entity != null) {
                            entity.setXRot((float) rotation);
                        }
                    }
                }
            }
        });
    }

    @Override
    public Type<C2SRotationMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerC2S(TYPE, STREAM_CODEC, C2SRotationMessage::apply);
    }
}
