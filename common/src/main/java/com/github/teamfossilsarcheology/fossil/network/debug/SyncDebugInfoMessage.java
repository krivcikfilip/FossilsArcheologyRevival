package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import com.github.teamfossilsarcheology.fossil.capabilities.ModCapabilities;
import com.github.teamfossilsarcheology.fossil.client.gui.debug.DebugScreen;
import com.github.teamfossilsarcheology.fossil.entity.PrehistoricSkeleton;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.DinosaurEgg;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric;
import com.github.teamfossilsarcheology.fossil.util.Gender;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;


public class SyncDebugInfoMessage implements PayloadChannel.BidirectionalPayload {
    //PORT(1.21.1): sent in both directions -> two payload ids (NeoForge forbids registering one id for both directions)
    public static final Type<SyncDebugInfoMessage> TYPE_S2C = new Type<>(FossilMod.location("s2c_sync_debug_info"));
    public static final Type<SyncDebugInfoMessage> TYPE_C2S = new Type<>(FossilMod.location("c2s_sync_debug_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDebugInfoMessage> STREAM_CODEC_S2C = StreamCodec.ofMember(SyncDebugInfoMessage::write, buf -> new SyncDebugInfoMessage(buf).withDirection(true));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDebugInfoMessage> STREAM_CODEC_C2S = StreamCodec.ofMember(SyncDebugInfoMessage::write, buf -> new SyncDebugInfoMessage(buf).withDirection(false));
    private boolean clientbound;
    private final int entityId;
    private final String enumString;
    private final int age;
    private final int matingCooldown;
    private final int playingCooldown;
    private final int climbingCooldown;
    private final int hunger;
    private final int mood;

    private SyncDebugInfoMessage(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public SyncDebugInfoMessage(int entityId, String enumString, int age, int matingCooldown, int playingCooldown, int climbingCooldown, int hunger, int mood) {
        this.entityId = entityId;
        this.enumString = enumString;
        this.age = age;
        this.matingCooldown = matingCooldown;
        this.playingCooldown = playingCooldown;
        this.climbingCooldown = climbingCooldown;
        this.hunger = hunger;
        this.mood = mood;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(enumString);
        buf.writeInt(age);
        buf.writeInt(matingCooldown);
        buf.writeInt(playingCooldown);
        buf.writeInt(climbingCooldown);
        buf.writeInt(hunger);
        buf.writeInt(mood);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            if (Version.debugEnabled() && player != null) {
                if (player.level().isClientSide) {
                    if (Minecraft.getInstance().screen instanceof DebugScreen) {
                        if (DebugScreen.entity == null || DebugScreen.entity.getId() != entityId) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                if (player.level().getEntity(entityId) instanceof Prehistoric prehistoric) {
                    if (!player.level().isClientSide) {
                        prehistoric.setAgeInTicks(age);
                        prehistoric.setHunger(hunger);
                        prehistoric.moodSystem.setMood(mood);
                    }
                    prehistoric.setGender(Gender.valueOf(enumString));
                    prehistoric.setMatingCooldown(matingCooldown);
                    prehistoric.moodSystem.setPlayingCooldown(playingCooldown);
                    prehistoric.setClimbingCooldown(climbingCooldown);
                    prehistoric.refreshTexturePath();
                    prehistoric.refreshDimensions();
                    prehistoric.updateAbilities();
                } else if (player.level().getEntity(entityId) instanceof PrehistoricSkeleton fossil) {
                    fossil.setInfoType(enumString);
                    fossil.setAge(age);
                } else if (player.level().getEntity(entityId) instanceof DinosaurEgg egg) {
                    egg.setHatchingTime(age);
                } else if (player.level().getEntity(entityId) instanceof Animal animal && ModCapabilities.hasEmbryo(animal)) {
                    ModCapabilities.setEmbryoProgress(animal, age);
                    ModCapabilities.syncMammalWithClient(animal, age, ModCapabilities.getEmbryo(animal));
                }
            }
        });
    }

    private SyncDebugInfoMessage withDirection(boolean clientbound) {
        SyncDebugInfoMessage message = new SyncDebugInfoMessage(entityId, enumString, age, matingCooldown, playingCooldown, climbingCooldown, hunger, mood);
        message.clientbound = clientbound;
        return message;
    }

    @Override
    public PayloadChannel.BidirectionalPayload toClientbound() {
        return withDirection(true);
    }

    @Override
    public PayloadChannel.BidirectionalPayload toServerbound() {
        return withDirection(false);
    }

    @Override
    public Type<SyncDebugInfoMessage> type() {
        return clientbound ? TYPE_S2C : TYPE_C2S;
    }

    public static void register(PayloadChannel channel) {
        channel.registerBidirectional(TYPE_S2C, STREAM_CODEC_S2C, TYPE_C2S, STREAM_CODEC_C2S, SyncDebugInfoMessage::apply);
    }
}
