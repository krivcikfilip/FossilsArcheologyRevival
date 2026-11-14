package com.github.teamfossilsarcheology.fossil.forge.tests;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.entity.ModEntities;
import com.github.teamfossilsarcheology.fossil.entity.prehistoric.Triceratops;
import com.github.teamfossilsarcheology.fossil.food.FoodMappings;
import com.github.teamfossilsarcheology.fossil.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PORT(1.21.1, phase 2): player interactions with dinosaurs that changed in the port (feeding, owner-only leash via the
 * Architectury INTERACT_ENTITY hook, whip mounting). Uses mock server players going through Player#interactOn, like a real click.
 */
@PrefixGameTestTemplate(value = false)
@GameTestHolder(FossilMod.MOD_ID)
public class DinoInteractionTests {

    private static Triceratops spawnAdult(GameTestHelper helper) {
        Triceratops dino = helper.spawn(ModEntities.TRICERATOPS.get(), 1, 2, 1);
        dino.setNoAi(true);
        dino.setAgeInDays(dino.data().adultAgeDays());
        dino.grow(0);
        return dino;
    }

    /**
     * A FakePlayer added to the level: TamableAnimal#isOwnedBy looks the owner up in the level's player list, and
     * GameTestHelper#makeMockServerPlayerInLevel cannot be used because its fake connection never negotiated our payload channels.
     * FakePlayer refuses to ride anything, so the requested vehicle is recorded instead (seat position was checked in game).
     */
    private static TestPlayer survivalPlayer(GameTestHelper helper, ItemStack held) {
        TestPlayer player = new TestPlayer(helper.getLevel());
        player.moveTo(helper.absoluteVec(new Vec3(0.5, 2, 0.5)));
        helper.getLevel().addNewPlayer(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static class TestPlayer extends FakePlayer {
        private Entity requestedVehicle;

        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "fossil-test-player"));
        }

        @Override
        public boolean startRiding(Entity entity, boolean force) {
            requestedVehicle = entity;
            return false;
        }
    }

    private static void removePlayers(GameTestHelper helper, Player... players) {
        for (Player player : players) {
            player.stopRiding();
            helper.getLevel().removePlayerImmediately((ServerPlayer) player, Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * Feeding a hungry dinosaur with food of its diet increases its hunger and uses the item
     */
    @GameTest(batch = "dino", template = "empty", timeoutTicks = 20)
    public static void feedingIncreasesHunger(GameTestHelper helper) {
        Triceratops dino = spawnAdult(helper);
        Item food = BuiltInRegistries.ITEM.stream().filter(item -> FoodMappings.getFoodAmount(item, dino.data().diet()) > 0).findFirst()
                .orElseThrow(() -> new GameTestAssertException("No food found for the triceratops diet"));
        dino.setHunger(1);
        Player player = survivalPlayer(helper, new ItemStack(food, 4));
        player.interactOn(dino, InteractionHand.MAIN_HAND);
        if (dino.getHunger() <= 1) {
            throw new GameTestAssertException("Hunger did not increase after feeding " + food + " (hunger " + dino.getHunger() + ")");
        }
        if (player.getMainHandItem().getCount() != 3) {
            throw new GameTestAssertException("Food was not consumed: " + player.getMainHandItem());
        }
        removePlayers(helper, player);
        helper.succeed();
    }

    /**
     * Only the owner may put a lead on a tamed dinosaur (1.20.1 canBeLeashed(Player), now ModEvents INTERACT_ENTITY hook)
     */
    @GameTest(batch = "dino", template = "empty", timeoutTicks = 20)
    public static void leashOwnerOnly(GameTestHelper helper) {
        Triceratops dino = spawnAdult(helper);
        Player owner = survivalPlayer(helper, new ItemStack(Items.LEAD));
        Player stranger = survivalPlayer(helper, new ItemStack(Items.LEAD));
        dino.tame(owner);
        stranger.interactOn(dino, InteractionHand.MAIN_HAND);
        if (dino.isLeashed()) {
            throw new GameTestAssertException("A stranger could leash a tamed dinosaur");
        }
        if (stranger.getMainHandItem().getCount() != 1) {
            throw new GameTestAssertException("The stranger lost the lead");
        }
        var result = owner.interactOn(dino, InteractionHand.MAIN_HAND);
        if (!dino.isLeashed() || dino.getLeashHolder() != owner) {
            throw new GameTestAssertException("The owner could not leash the dinosaur (" + result + ", owned " + dino.isOwnedBy(owner)
                    + ", canBeLeashed " + dino.canBeLeashed() + ", lead " + owner.getMainHandItem() + ")");
        }
        removePlayers(helper, owner, stranger);
        helper.succeed();
    }

    /**
     * The owner mounts an adult rideable dinosaur with a whip, a stranger does not
     */
    @GameTest(batch = "dino", template = "empty", timeoutTicks = 20)
    public static void whipMountOwnerOnly(GameTestHelper helper) {
        Triceratops dino = spawnAdult(helper);
        TestPlayer owner = survivalPlayer(helper, new ItemStack(ModItems.WHIP.get()));
        TestPlayer stranger = survivalPlayer(helper, new ItemStack(ModItems.WHIP.get()));
        dino.tame(owner);
        stranger.interactOn(dino, InteractionHand.MAIN_HAND);
        if (stranger.requestedVehicle != null) {
            throw new GameTestAssertException("A stranger could ride a tamed dinosaur");
        }
        var result = owner.interactOn(dino, InteractionHand.MAIN_HAND);
        if (owner.requestedVehicle != dino) {
            throw new GameTestAssertException("The owner could not mount the dinosaur with a whip (" + result + ", owned " + dino.isOwnedBy(owner)
                    + ", adult " + dino.isAdult() + ", rideable " + dino.data().canBeRidden() + ")");
        }
        removePlayers(helper, owner, stranger);
        helper.succeed();
    }
}
