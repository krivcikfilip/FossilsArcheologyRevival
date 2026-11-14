package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.capabilities.ModCapabilities;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.EntityInfo;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import org.jetbrains.annotations.Nullable;


/**
 * Only used by the forge side. Sends the current embryo status from server to client
 */
public class S2CMammalCapMessage implements CustomPacketPayload {
    public static final Type<S2CMammalCapMessage> TYPE = new Type<>(FossilMod.location("s2c_mammal_cap"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMammalCapMessage> STREAM_CODEC = StreamCodec.ofMember(S2CMammalCapMessage::write, S2CMammalCapMessage::new);

    private final int entityId;
    private final int embryoProgress;
    private final EntityInfo embryo;

    private S2CMammalCapMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.embryoProgress = buf.readInt();
        EntityInfo temp;
        try {
            temp = EntityInfo.fromNbt(buf.readUtf());
        } catch (IllegalArgumentException e) {
            temp = null;
        }
        this.embryo = temp;
    }

    public S2CMammalCapMessage(Animal animal, int embryoProgress, @Nullable EntityInfo embryo) {
        this.entityId = animal.getId();
        this.embryoProgress = embryoProgress;
        this.embryo = embryo;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(embryoProgress);
        if (embryo != null) {
            buf.writeUtf(embryo.name());
        } else {
            buf.writeUtf("null");
        }
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        context.queue(() -> {
            Entity entity = context.getPlayer().level().getEntity(entityId);
            if (entity instanceof Animal animal) {
                ModCapabilities.setEmbryoProgress(animal, embryoProgress);
                ModCapabilities.setEmbryo(animal, embryo);
            }
        });
    }

    @Override
    public Type<S2CMammalCapMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CMammalCapMessage::apply);
    }
}
