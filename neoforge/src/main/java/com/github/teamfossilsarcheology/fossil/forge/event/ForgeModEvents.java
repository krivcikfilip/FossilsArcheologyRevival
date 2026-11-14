package com.github.teamfossilsarcheology.fossil.forge.event;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.capabilities.ModCapabilities;
import com.github.teamfossilsarcheology.fossil.capabilities.forge.ModCapabilitiesImpl;
import com.github.teamfossilsarcheology.fossil.config.FossilConfig;
import com.github.teamfossilsarcheology.fossil.event.ModEvents;
import com.github.teamfossilsarcheology.fossil.forge.capabilities.ModAttachments;
import com.github.teamfossilsarcheology.fossil.forge.tests.HopperTests;
import com.github.teamfossilsarcheology.fossil.forge.tests.MiscTests;
import com.github.teamfossilsarcheology.fossil.forge.tests.RecipeTests;
import com.github.teamfossilsarcheology.fossil.network.MessageHandler;
import com.github.teamfossilsarcheology.fossil.network.S2CMammalCapMessage;
import com.github.teamfossilsarcheology.fossil.util.Version;
import com.github.teamfossilsarcheology.fossil.villager.ModTrades;
import com.github.teamfossilsarcheology.fossil.villager.ModVillagers;
import com.github.teamfossilsarcheology.fossil.world.effect.ComfyBedEffect;
import com.github.teamfossilsarcheology.fossil.world.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;
import java.util.Optional;

/**
 * Game bus listeners. {@link #registerTests} is a mod bus event and is registered in ForgeFossilMod's constructor.
 */
@EventBusSubscriber(modid = FossilMod.MOD_ID)
public class ForgeModEvents {

    //PORT(1.21.1): RegisterGameTestsEvent is an IModBusEvent -> added to the mod bus by ForgeFossilMod (no @SubscribeEvent here)
    public static void registerTests(RegisterGameTestsEvent event) {
        if (Version.debugEnabled()) {
            event.register(HopperTests.class);
            event.register(MiscTests.class);
            event.register(RecipeTests.class);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        /*if (Version.debugEnabled()) {
            if (!ArgumentTypes.isTypeRegistered(BatchArgument.batch())) {
                ArgumentTypes.register("batch", BatchArgument.class, new EmptyArgumentSerializer<>(BatchArgument::batch));
            }
            event.getDispatcher().register(Commands.literal(FossilMod.MOD_ID).then(BatchTestCommand.register()));
        }*/
    }

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if (event.getType() == ModVillagers.ARCHEOLOGIST.get()) {
            var trades = event.getTrades();
            trades.get(1).addAll(ModTrades.getArcheoList(1));
            trades.get(2).addAll(ModTrades.getArcheoList(2));
            trades.get(3).addAll(ModTrades.getArcheoList(3));
            trades.get(4).addAll(ModTrades.getArcheoList(4));
            trades.get(5).addAll(ModTrades.getArcheoList(5));
        } else if (event.getType() == ModVillagers.PALEONTOLOGIST.get()) {
            var trades = event.getTrades();
            trades.get(1).addAll(ModTrades.getPaleoList(1));
            trades.get(2).addAll(ModTrades.getPaleoList(2));
            trades.get(3).addAll(ModTrades.getPaleoList(3));
            trades.get(4).addAll(ModTrades.getPaleoList(4));
            trades.get(5).addAll(ModTrades.getPaleoList(5));
        }
    }

    //PORT(1.21.1): LivingEvent.LivingTickEvent -> EntityTickEvent.Pre (NeoForge 20.5)
    @SubscribeEvent
    public static void onLivingUpdate(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Animal animal) {
            int currentProgress = ModCapabilities.getEmbryoProgress(animal);
            if (currentProgress == 0) {
                return;
            }
            if (currentProgress >= FossilConfig.getInt(FossilConfig.PREGNANCY_DURATION)) {
                if (!animal.level().isClientSide) {
                    ModEvents.growEntity(ModCapabilities.getEmbryo(animal), animal);
                    ModCapabilities.stopPregnancy(animal);
                }
            } else {
                ModCapabilities.setEmbryoProgress(animal, currentProgress + 1);
            }
        }
    }

    //PORT(1.21.1): AttachCapabilitiesEvent removed. The data attachments (ModAttachments) are created lazily on first write.

    /**
     * PORT(1.21.1): copyOnDeath on {@link ModAttachments#FIRST_HATCH} handles deaths; this keeps the old behaviour of
     * copying the flag on every clone (e.g. returning from the End). reviveCaps/invalidateCaps no longer exist.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ModCapabilitiesImpl.getFirstHatchCap(event.getOriginal()).ifPresent(originalCap -> {
            ModCapabilitiesImpl.getFirstHatchCap(event.getEntity()).ifPresent(newCap -> {
                newCap.setHatchedDinosaur(originalCap.hasHatchedDinosaur());
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        if (event.getTarget() instanceof Animal animal) {
            animal.getExistingData(ModAttachments.MAMMAL).ifPresent(iMammalCap -> MessageHandler.CAP_CHANNEL.sendToPlayers(List.of(serverPlayer),
                    new S2CMammalCapMessage(animal, iMammalCap.getEmbryoProgress(), iMammalCap.getEmbryo())));
        }
    }

    /**
     * PORT(1.21.1): PlayerSleepInBedEvent/SleepingTimeCheckEvent with Event.Result.ALLOW -> CanPlayerSleepEvent /
     * CanContinueSleepingEvent. Only the "it is daytime" problem is lifted, as the old SleepingTimeCheckEvent did.
     */
    @SubscribeEvent
    public static void allowDaySleep(CanPlayerSleepEvent event) {
        if (event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW
                && ComfyBedEffect.canApply(Optional.of(event.getPos()), event.getLevel())) {
            event.setProblem(null);
        }
    }

    @SubscribeEvent
    public static void allowDaySleep(CanContinueSleepingEvent event) {
        if (event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW
                && ComfyBedEffect.canApply(event.getEntity().getSleepingPos(), event.getEntity().level())) {
            event.setContinueSleeping(true);
        }
    }

    @SubscribeEvent
    public static void addComfyBedEffect(PlayerWakeUpEvent event) {
        if (ComfyBedEffect.canApply(event.getEntity().getSleepingPos(), event.getEntity().level())) {
            //PORT(1.21.1): effects are Holder<MobEffect>
            event.getEntity().addEffect(new MobEffectInstance(comfyBed(), 24000, 0));
        }
    }

    @SubscribeEvent
    public static void addComfyBedEffect(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity().hasEffect(comfyBed())) {
            event.modifyVisibility(0.5);
        }
    }

    /**
     * The registry's own holder (not the Architectury RegistrySupplier) so that the effect map lookups use the same key
     * as effects loaded from NBT.
     */
    private static Holder<MobEffect> comfyBed() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.COMFY_BED.get());
    }

    @SubscribeEvent
    public static void onDatapackSyncEvent(OnDatapackSyncEvent event) {
        //PORT(1.21.1): getRelevantPlayers() covers both the single player join and the /reload case
        event.getRelevantPlayers().forEach(FossilMod::syncData);
    }
}
