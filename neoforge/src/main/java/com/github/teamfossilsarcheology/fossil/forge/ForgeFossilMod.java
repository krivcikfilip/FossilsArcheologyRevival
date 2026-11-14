package com.github.teamfossilsarcheology.fossil.forge;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.advancements.ModTriggers;
import com.github.teamfossilsarcheology.fossil.client.ClientInit;
import com.github.teamfossilsarcheology.fossil.client.model.block.PlantBlockModel;
import com.github.teamfossilsarcheology.fossil.config.forge.ForgeConfig;
import com.github.teamfossilsarcheology.fossil.config.forge.ForgeConfigFix;
import com.github.teamfossilsarcheology.fossil.entity.ModEntities;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricFish;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.fish.Coelacanth;
import com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.ModAttachments;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.ModBlockCapabilities;
import com.github.teamfossilsarcheology.fossil.forge.client.model.PlantModelLoader;
import com.github.teamfossilsarcheology.fossil.forge.compat.alexsmobs.AlexsMobsCompat;
import com.github.teamfossilsarcheology.fossil.forge.compat.farmers.FarmersDelightCompat;
import com.github.teamfossilsarcheology.fossil.forge.event.ForgeModEvents;
import com.github.teamfossilsarcheology.fossil.forge.world.biome.ForgeFossilRegion;
import com.github.teamfossilsarcheology.fossil.util.ModConstants;
import com.github.teamfossilsarcheology.fossil.world.chunk.AnuLairChunkGenerator;
import com.github.teamfossilsarcheology.fossil.world.chunk.TreasureChunkGenerator;
import com.github.teamfossilsarcheology.fossil.world.feature.placement.ModPlacedFeatures;
import com.github.teamfossilsarcheology.fossil.world.feature.structures.ModStructureType;
import com.github.teamfossilsarcheology.fossil.world.surfacerules.ModSurfaceRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

@Mod(FossilMod.MOD_ID)
public class ForgeFossilMod {

    //PORT(1.21.1): FML 4 injects the mod event bus and container; Architectury 13 no longer needs EventBuses.registerModEventBus
    public ForgeFossilMod(IEventBus modEventBus, ModContainer modContainer) {
        FossilMod.init();

        //PORT(1.21.1): capabilities -> data attachments (entities) + RegisterCapabilitiesEvent (block entities)
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModBlockCapabilities::registerCapabilities);

        modEventBus.addListener(this::onClient);
        modEventBus.addListener(this::onCommon);
        modEventBus.addListener(this::registerSpawnPlacements);
        modEventBus.addListener(this::registerChunkGenerators);
        //Mod bus events of ForgeModEvents (that class itself only subscribes to the game bus)
        modEventBus.addListener(ForgeModEvents::registerTests);
        modEventBus.addListener(ForgeConfigFix::fixConfig);
        modContainer.registerConfig(ModConfig.Type.COMMON, ForgeConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ForgeConfig.CLIENT_SPEC);
        //PORT(1.21.1): DistExecutor was removed from FML
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ForgeFossilMod::onModelRegistryEvent);
            ClientInit.immediate();
        }
    }

    public void onClient(FMLClientSetupEvent event) {
        ClientInit.later();
    }

    public void onCommon(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(ModConstants.FARMERS)) {
                FoodMappingsManager.INSTANCE.listen(FarmersDelightCompat::registerFoodMappings);
            }
            if (ModList.get().isLoaded(ModConstants.ALEXS_MOBS)) {
                FoodMappingsManager.INSTANCE.listen(AlexsMobsCompat::register);
            }
            ModPlacedFeatures.register();
            ModStructureType.register();
            ModTriggers.register();
            Regions.register(new ForgeFossilRegion("overworld", RegionType.OVERWORLD, 4));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, FossilMod.MOD_ID, ModSurfaceRules.VOLCANIC_SURFACE_RULE);
        });
    }

    /**
     * PORT(1.21.1): vanilla registries are frozen after the registration phase on NeoForge, so the chunk generator codecs
     * (previously Registry.register in common setup) are registered through {@link RegisterEvent}.
     */
    private void registerChunkGenerators(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, helper -> {
            helper.register(FossilMod.location("treasure_room"), TreasureChunkGenerator.CODEC);
            helper.register(FossilMod.location("anu_lair"), AnuLairChunkGenerator.CODEC);
        });
    }

    /**
     * PORT(1.21.1): SpawnPlacements.register is no longer accessible on NeoForge, {@link RegisterSpawnPlacementsEvent} replaces it.
     * SpawnPlacements.Type.IN_WATER -> SpawnPlacementTypes.IN_WATER (1.20.5)
     */
    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.ALLIGATOR_GAR.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PrehistoricFish::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.COELACANTH.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Coelacanth::canCoelacanthSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.NAUTILUS.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PrehistoricFish::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.STURGEON.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PrehistoricFish::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void onModelRegistryEvent(ModelEvent.RegisterGeometryLoaders event) {
        //PORT(1.21.1): the loader is registered with its full id (was only the path, namespaced by the active mod)
        event.register(PlantBlockModel.LOADER, new PlantModelLoader());
    }
}
