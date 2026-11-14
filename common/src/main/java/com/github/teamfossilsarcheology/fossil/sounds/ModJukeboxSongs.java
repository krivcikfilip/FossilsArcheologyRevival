package com.github.teamfossilsarcheology.fossil.sounds;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;

/**
 * PORT(1.21.1): RecordItem was removed in 1.21. Music discs are plain items with the {@code jukebox_playable} component,
 * songs are a datapack registry ({@code data/fossil/jukebox_song/<name>.json}).
 * The values (length, comparator output, description key) are the ones of the old 1.20.1 RecordItems.
 */
public class ModJukeboxSongs {
    public static final ResourceKey<JukeboxSong> BONES = create("bones");
    public static final ResourceKey<JukeboxSong> ANU = create("anu");
    public static final ResourceKey<JukeboxSong> SCARAB = create("scarab");
    public static final ResourceKey<JukeboxSong> DISCOVERY = create("discovery");

    private static ResourceKey<JukeboxSong> create(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, FossilMod.location(name));
    }

    /**
     * For datagen (RegistrySetBuilder.add(Registries.JUKEBOX_SONG, ModJukeboxSongs::bootstrap))
     */
    public static void bootstrap(BootstrapContext<JukeboxSong> context) {
        register(context, BONES, ModSounds.MUSIC_BONES, "music_disc_bones", 196, 0);
        register(context, ANU, ModSounds.MUSIC_ANU, "music_disc_anu", 221, 1);
        register(context, SCARAB, ModSounds.MUSIC_SCARAB, "music_disc_scarab", 153, 2);
        register(context, DISCOVERY, ModSounds.MUSIC_DISCOVERY, "music_disc_discovery", 250, 3);
    }

    private static void register(BootstrapContext<JukeboxSong> context, ResourceKey<JukeboxSong> key, RegistrySupplier<SoundEvent> sound, String item, int lengthInSeconds, int comparatorOutput) {
        //Keeps the old lang key of the record description
        //PORT-TODO(1.21.1): verify the RegistrySupplier works as Holder<SoundEvent> when the song is encoded by datagen
        context.register(key, new JukeboxSong(sound, Component.translatable("item." + FossilMod.MOD_ID + "." + item + ".desc"), lengthInSeconds, comparatorOutput));
    }
}
