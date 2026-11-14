package com.github.teamfossilsarcheology.fossil.mixin;

import com.github.teamfossilsarcheology.fossil.world.chunk.AnuLairChunkGenerator;
import com.github.teamfossilsarcheology.fossil.world.chunk.TreasureChunkGenerator;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Always generates the structures of our fixed dimensions, even if structure generation is disabled in the world options.
 * <p>
 * PORT(1.21.1): 1.20.1 targeted the STRUCTURE_STARTS lambda in ChunkStatus (fabric intermediary name "method_39464",
 * which never matched on Forge -> require = 0). Since 1.21 the generation tasks live in
 * {@link ChunkStatusTasks#generateStructureStarts}, the class name of the mixin is kept.
 * PORT-TODO(1.21.1): verify in game that structures generate in the anu lair/treasure room with generate-structures=false
 */
@Mixin(ChunkStatusTasks.class)
public class ChunkStatusMixin {

    //require 0 to prevent crash in development
    @Inject(method = "generateStructureStarts", at = @At("HEAD"), require = 0)
    private static void allow(WorldGenContext worldGenContext, ChunkStep step, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunkAccess, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        ChunkGenerator chunkGenerator = worldGenContext.generator();
        if (chunkGenerator instanceof TreasureChunkGenerator || chunkGenerator instanceof AnuLairChunkGenerator) {
            ServerLevel serverLevel = worldGenContext.level();
            if (!chunkAccess.getPersistedStatus().isOrAfter(step.targetStatus()) && !serverLevel.getServer().getWorldData().worldGenOptions().generateStructures()) {
                chunkGenerator.createStructures(serverLevel.registryAccess(), serverLevel.getChunkSource().getGeneratorState(), serverLevel.structureManager(), chunkAccess, worldGenContext.structureManager());
            }
        }
    }
}
