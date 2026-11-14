package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.sounds.ModMusic;
import com.github.teamfossilsarcheology.fossil.sounds.ModSounds;
import com.github.teamfossilsarcheology.fossil.sounds.MusicHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;


/**
 * Sends a starting {@link SoundEvent} from server to client
 */
public class S2CMusicMessage implements CustomPacketPayload {
    public static final Type<S2CMusicMessage> TYPE = new Type<>(FossilMod.location("s2c_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMusicMessage> STREAM_CODEC = StreamCodec.ofMember(S2CMusicMessage::write, S2CMusicMessage::new);

    private final SoundEvent soundEvent;

    private S2CMusicMessage(FriendlyByteBuf buf) {
        this(BuiltInRegistries.SOUND_EVENT.byId(buf.readVarInt()));
    }

    public S2CMusicMessage(SoundEvent soundEvent) {
        this.soundEvent = soundEvent;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(BuiltInRegistries.SOUND_EVENT.getId(soundEvent));
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        context.queue(() -> MusicHandler.startMusic(getMusic(soundEvent)));
    }

    @Override
    public Type<S2CMusicMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CMusicMessage::apply);
    }

    private static Music getMusic(SoundEvent soundEvent) {
        if (soundEvent == ModSounds.MUSIC_ANU.get()) {
            return ModMusic.ANU;
        } else if (soundEvent == ModSounds.MUSIC_FIRST_DINOSAUR.get()) {
            return ModMusic.FIRST_DINOSAUR;
        }
        return Musics.GAME;
    }
}
