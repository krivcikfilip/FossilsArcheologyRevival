package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
import com.github.teamfossilsarcheology.fossil.food.FoodType;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sync {@link com.github.teamfossilsarcheology.fossil.food.FoodMappings food mappings} on server join to client
 */
public class S2CSyncFoodMappingsMessage implements CustomPacketPayload {
    public static final Type<S2CSyncFoodMappingsMessage> TYPE = new Type<>(FossilMod.location("s2c_sync_food_mappings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncFoodMappingsMessage> STREAM_CODEC = StreamCodec.ofMember(S2CSyncFoodMappingsMessage::write, S2CSyncFoodMappingsMessage::new);

    private final Map<FoodType, Map<Block, Integer>> blockValues;
    private final Map<FoodType, Map<Item, Integer>> itemValues;
    private final Map<FoodType, Map<EntityType<?>, Integer>> entityValues;
    private final Set<EntityType<?>> entities;

    private S2CSyncFoodMappingsMessage(FriendlyByteBuf buf) {
        blockValues = buf.readMap(HashMap::new, FoodType::readBuf, friendlyByteBuf -> friendlyByteBuf.readMap(HashMap::new, buf1 -> BuiltInRegistries.BLOCK.byId(buf1.readInt()), FriendlyByteBuf::readInt));
        itemValues = buf.readMap(HashMap::new, FoodType::readBuf, friendlyByteBuf -> friendlyByteBuf.readMap(HashMap::new, buf1 -> Item.byId(buf1.readInt()), FriendlyByteBuf::readInt));
        entityValues = buf.readMap(HashMap::new, FoodType::readBuf, friendlyByteBuf -> friendlyByteBuf.readMap(HashMap::new, buf1 -> BuiltInRegistries.ENTITY_TYPE.get(buf1.readResourceLocation()), FriendlyByteBuf::readInt));
        entities = new HashSet<>(buf.readList(buf1 -> BuiltInRegistries.ENTITY_TYPE.get(buf1.readResourceLocation())));
    }

    public S2CSyncFoodMappingsMessage(Map<FoodType, Map<Block, Integer>> blockValues, Map<FoodType, Map<Item, Integer>> itemValues, Map<FoodType, Map<EntityType<?>, Integer>> entityValues, Set<EntityType<?>> entities) {
        this.blockValues = blockValues;
        this.itemValues = itemValues;
        this.entityValues = entityValues;
        this.entities = entities;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeMap(blockValues, FoodType::writeBuf, (innerBuf, map) -> innerBuf.writeMap(map, (buf1, block) -> buf1.writeInt(BuiltInRegistries.BLOCK.getId(block)), FriendlyByteBuf::writeInt));
        buf.writeMap(itemValues, FoodType::writeBuf, (innerBuf, map) -> innerBuf.writeMap(map, (buf1, item) -> buf1.writeInt(Item.getId(item)), FriendlyByteBuf::writeInt));
        buf.writeMap(entityValues, FoodType::writeBuf, (innerBuf, map) -> innerBuf.writeMap(map, (buf1, entityType) -> buf1.writeResourceLocation(EntityType.getKey(entityType)), FriendlyByteBuf::writeInt));
        buf.writeCollection(entities, (buf1, entityType) -> buf1.writeResourceLocation(EntityType.getKey(entityType)));
    }

    private void apply(NetworkManager.PacketContext context) {
        if (context.getEnvironment() == Env.SERVER) return;
        context.queue(() -> FoodMappingsManager.INSTANCE.replaceValues(blockValues, itemValues, entityValues, entities));
    }

    @Override
    public Type<S2CSyncFoodMappingsMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CSyncFoodMappingsMessage::apply);
    }
}
