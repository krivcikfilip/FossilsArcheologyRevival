package com.github.teamfossilsarcheology.fossil.item;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;

/**
 * PORT(1.21.1): RecordItem no longer exists. The old @ExpectPlatform {@code get(int, RegistrySupplier<SoundEvent>, Properties, int)}
 * (platform impls only existed to pass the sound supplier to the RecordItem) is replaced by the jukebox_playable component.
 * Analog output and length now live in the {@link JukeboxSong} json, see {@link com.github.teamfossilsarcheology.fossil.sounds.ModJukeboxSongs}.
 */
public class CustomRecordItem {
    public static Item get(ResourceKey<JukeboxSong> song, Item.Properties properties) {
        return new Item(properties.jukeboxPlayable(song));
    }
}
