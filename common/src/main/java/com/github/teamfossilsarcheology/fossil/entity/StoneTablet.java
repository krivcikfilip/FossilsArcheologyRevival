package com.github.teamfossilsarcheology.fossil.entity;

import com.github.teamfossilsarcheology.fossil.item.ModItems;
import dev.architectury.extensions.network.EntitySpawnExtension;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StoneTablet extends HangingEntity implements EntitySpawnExtension {
    public Variant variant;

    public StoneTablet(EntityType<? extends HangingEntity> entityType, Level level) {
        super(entityType, level);
    }

    public StoneTablet(Level level, BlockPos blockPos, Direction direction) {
        super(ModEntities.STONE_TABLET.get(), level, blockPos);
        List<Variant> validVariants = new ArrayList<>();
        this.variant = Variant.SOCIAL;
        for (Variant variant : Variant.values()) {
            this.variant = variant;
            setDirection(direction);
            if (!this.survives()) continue;
            validVariants.add(variant);
        }
        if (!validVariants.isEmpty()) {
            variant = validVariants.get(level.random.nextInt(validVariants.size()));
        }
        setDirection(direction);
    }

    public StoneTablet(Level level, BlockPos blockPos, Direction direction, Variant variant) {
        this(level, blockPos, direction);
        this.variant = variant;
        this.setDirection(direction);
    }

    /**
     * PORT(1.21.1): HangingEntity no longer implements the (now abstract) Entity#defineSynchedData(Builder). 1.20.1 HangingEntity's was empty
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Variant", variant.ordinal());
        compound.putByte("Facing", (byte) direction.get2DDataValue());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        variant = Variant.values()[compound.getInt("Variant")];
        direction = Direction.from2DDataValue(compound.getByte("Facing"));
        setDirection(direction);
    }

    /**
     * PORT(1.21.1): HangingEntity#getWidth/getHeight/recalculateBoundingBox logic was replaced by calculateBoundingBox(BlockPos, Direction).
     * This is the 1.20.1 HangingEntity#recalculateBoundingBox math (sizes in pixels)
     * PORT-TODO(1.21.1): verify placement/hitbox of all tablet sizes in game (1.21 HangingEntity sets the position to the box center)
     */
    @Override
    protected @NotNull AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        double x = pos.getX() + 0.5 - direction.getStepX() * 0.46875;
        double y = pos.getY() + 0.5 + offs(getHeight());
        double z = pos.getZ() + 0.5 - direction.getStepZ() * 0.46875;
        Direction counterClockWise = direction.getCounterClockWise();
        double widthOffset = offs(getWidth());
        x += widthOffset * counterClockWise.getStepX();
        z += widthOffset * counterClockWise.getStepZ();
        double xSize = getWidth();
        double ySize = getHeight();
        double zSize = getWidth();
        if (direction.getAxis() == Direction.Axis.Z) {
            zSize = 1;
        } else {
            xSize = 1;
        }
        xSize /= 32;
        ySize /= 32;
        zSize /= 32;
        return new AABB(x - xSize, y - ySize, z - zSize, x + xSize, y + ySize, z + zSize);
    }

    private static double offs(int pixels) {
        return pixels % 32 == 0 ? 0.5 : 0;
    }

    public int getWidth() {
        if (variant == null) {
            return 0;
        }
        return variant.sizeX;
    }

    public int getHeight() {
        if (variant == null) {
            return 0;
        }
        return variant.sizeY;
    }

    @Override
    public void dropItem(@Nullable Entity brokenEntity) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        playSound(SoundEvents.STONE_BREAK, 1.0f, 1.0f);
        if (brokenEntity instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        spawnAtLocation(ModItems.STONE_TABLET.get());
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.STONE_PLACE, 1.0f, 1.0f);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return NetworkManager.createAddEntityPacket(this, serverEntity);
    }

    @Override
    public void saveAdditionalSpawnData(FriendlyByteBuf buf) {
        buf.writeVarInt(this.variant.ordinal());
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.direction.get2DDataValue());
    }

    @Override
    public void loadAdditionalSpawnData(FriendlyByteBuf buf) {
        this.variant = Variant.values()[buf.readVarInt()];
        this.pos = buf.readBlockPos();
        setDirection(Direction.from2DDataValue(buf.readUnsignedByte()));
    }

    public enum Variant {
        LIGHTNING("Lightning", 32, 16, 0, 0),
        SOCIAL("Social", 16, 16, 32, 0),
        GREAT_WAR("Greatwar", 32, 32, 0, 16),
        CLOCK("clock", 32, 16, 0, 48),
        PORTAL("Portal", 32, 32, 0, 64),
        HEROBRINE("Herobrine", 32, 32, 32, 32),
        FLAT_CREEP("FlatCreep", 16, 16, 48, 0),
        ANGRY("annoyangry", 16, 16, 48, 16),
        REX_1("Rex1", 32, 32, 64, 0),
        REX_2("Rex2", 32, 16, 64, 32),
        REX_3("Rex3", 32, 16, 64, 48),
        REX_4("Rex4", 32, 32, 64, 64),
        PUZZLE("Puzzle", 32, 32, 32, 64),
        GUN_FIGHT("GunFight", 64, 32, 32, 96),
        PRINCESS("Princess", 32, 32, 0, 96),
        MOSAURUS("Mosa", 32, 16, 224, 48),
        HOLY_MOSAURUS("HolyMosasaurus", 64, 32, 160, 48),
        ANCI_TM("AnciTM", 32, 32, 96, 0),
        MOD_TM("ModTM", 16, 32, 128, 0),
        VIG_TM("VigTM", 32, 32, 144, 0),
        SABER_HUNT("SaberHunt", 32, 16, 96, 32),
        ANU_PORTAL("AnuPortal", 32, 32, 96, 48),
        ANUBITE_1("Anubite1", 16, 16, 128, 32),
        ANUBITE_2("Anubite2", 16, 16, 144, 32),
        ANUBITE_3("Anubite3", 16, 16, 160, 32),
        ANUBITE_4("Anubite4", 16, 16, 176, 32),
        SARCOPHAGUS_OPEN("sarcophagus_open", 32, 32, 128, 48),
        SARCOPHAGUS_KILL("sarcophagus_kill", 32, 32, 96, 80),
        DEAD_ANU("deadAnu", 32, 32, 128, 80);

        public final String title;
        public final int sizeX;
        public final int sizeY;
        public final int offsetX;
        public final int offsetY;

        Variant(String title, int xSize, int ySize, int textureX, int textureY) {
            this.title = title;
            this.sizeX = xSize;
            this.sizeY = ySize;
            this.offsetX = textureX;
            this.offsetY = textureY;
        }
    }
}
