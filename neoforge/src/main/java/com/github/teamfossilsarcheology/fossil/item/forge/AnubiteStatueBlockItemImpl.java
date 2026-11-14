package com.github.teamfossilsarcheology.fossil.item.forge;

import com.github.teamfossilsarcheology.fossil.client.renderer.CustomItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class AnubiteStatueBlockItemImpl {
    public static BlockItem get(Block block, Item.Properties properties) {
        return new BlockItem(block, properties) {

            //PORT-TODO(1.21.1): initializeClient is deprecated for removal in NeoForge 21.1 (still called); move to RegisterClientExtensionsEvent
            @Override
            public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
                consumer.accept(new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return CustomItemRenderer.INSTANCE;
                    }
                });
            }
        };
    }
}
