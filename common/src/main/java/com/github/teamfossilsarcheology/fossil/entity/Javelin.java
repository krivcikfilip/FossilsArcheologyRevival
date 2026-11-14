package com.github.teamfossilsarcheology.fossil.entity;

import com.github.teamfossilsarcheology.fossil.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class Javelin extends AbstractArrow {
    private static final EntityDataAccessor<Integer> TIER_ID = SynchedEntityData.defineId(Javelin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ANCIENT = SynchedEntityData.defineId(Javelin.class, EntityDataSerializers.BOOLEAN);
    private int itemDamage;
    /**
     * PORT(1.21.1): AbstractArrow#setKnockback/knockback were removed in 1.21 (knockback is a data driven enchantment effect of the
     * weapon now). The punch level is stored here and applied in {@link #doKnockback(LivingEntity, DamageSource)} like in 1.20.1.
     */
    private int knockback;

    public Javelin(EntityType<Javelin> type, Level level) {
        super(type, level);
    }

    public Javelin(Level level, LivingEntity shooter, Tier tier, boolean ancient, int itemDamage) {
        //PORT(1.21.1): AbstractArrow now takes the pickup stack and the (nullable) weapon. getPickupItem is still overridden below,
        //so the pickup stack is only used by vanilla for the custom name/INTANGIBLE_PROJECTILE checks and the "item" save tag.
        super(ModEntities.JAVELIN.get(), shooter, level, createPickupItem(tier, ancient, itemDamage), null);
        this.itemDamage = itemDamage;
        if (tier instanceof Tiers tiers) {
            setTier(tiers);
        }
        entityData.set(ANCIENT, ancient);
        //PORT-TODO(1.21.1): AbstractArrow#setPierceLevel is private in 1.21. Needs an access widener entry (requested from core)
        setPierceLevel((byte) 16);
        setBaseDamage(getDamage(tier, ancient));
    }

    private static double getDamage(Tier tier, boolean ancient) {
        if (ancient) {
            return 5;
        } else if (tier instanceof Tiers tiers) {
            switch (tiers) {
                case WOOD -> {
                    return 2;
                }
                case STONE -> {
                    return 2.5;
                }
                case GOLD -> {
                    return 3.5;
                }
                case IRON -> {
                    return 3;
                }
                case DIAMOND -> {
                    return 4;
                }
            }
        }
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANCIENT, false);
        builder.define(TIER_ID, 0);
    }

    public void setKnockback(int knockback) {
        this.knockback = knockback;
    }

    public int getKnockback() {
        return knockback;
    }

    @Override
    protected void doKnockback(LivingEntity entity, DamageSource damageSource) {
        super.doKnockback(entity, damageSource);
        //1.20.1 AbstractArrow#onHitEntity knockback logic
        if (knockback > 0) {
            double resistance = Math.max(0, 1 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 vec3 = getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback * 0.6 * resistance);
            if (vec3.lengthSqr() > 0) {
                entity.push(vec3.x, 0.1, vec3.z);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        //PORT-TODO(1.21.1): AbstractArrow#setPierceLevel is private in 1.21. Needs an access widener entry (requested from core)
        setPierceLevel((byte) 16);
        if (level() instanceof ServerLevel && isAncient() && random.nextInt(100) < 30) {
            LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(level());
            lightningBolt.moveTo(Vec3.atBottomCenterOf(blockPosition()));
            lightningBolt.setCause(getOwner() instanceof ServerPlayer ? (ServerPlayer) getOwner() : null);
            level().addFreshEntity(lightningBolt);
        }
    }

    @Override
    protected void tickDespawn() {

    }

    public Tier getTier() {
        if (!isAncient()) {
            return Tiers.values()[entityData.get(TIER_ID)];
        }
        return Tiers.WOOD;
    }

    public void setTier(Tiers tier) {
        entityData.set(TIER_ID, tier.ordinal());
    }

    public boolean isAncient() {
        return entityData.get(ANCIENT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("ancient", isAncient());
        if (getTier() instanceof Tiers tiers) {
            compound.putInt("Tier", tiers.ordinal());
        }
        compound.putInt("Damage", itemDamage);
        compound.putByte("Knockback", (byte) knockback);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        entityData.set(ANCIENT, compound.getBoolean("ancient"));
        if (!isAncient()) {
            setTier(Tiers.values()[compound.getInt("Tier")]);
        }
        itemDamage = compound.getInt("Damage");
        knockback = compound.getByte("Knockback");
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return createPickupItem(getTier(), isAncient(), itemDamage);
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        //PORT(1.21.1): new abstract method. Only used by vanilla before our data is known (constructor/old saves without "item")
        return new ItemStack(ModItems.WOODEN_JAVELIN.get());
    }

    private static ItemStack createPickupItem(Tier tier, boolean ancient, int itemDamage) {
        if (ancient) {
            return createStack(ModItems.ANCIENT_JAVELIN.get(), itemDamage);
        } else if (tier instanceof Tiers tiers) {
            switch (tiers) {
                case WOOD -> {
                    return createStack(ModItems.WOODEN_JAVELIN.get(), itemDamage);
                }
                case STONE -> {
                    return createStack(ModItems.STONE_JAVELIN.get(), itemDamage);
                }
                case GOLD -> {
                    return createStack(ModItems.GOLD_JAVELIN.get(), itemDamage);
                }
                case IRON -> {
                    return createStack(ModItems.IRON_JAVELIN.get(), itemDamage);
                }
                case DIAMOND -> {
                    return createStack(ModItems.DIAMOND_JAVELIN.get(), itemDamage);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack createStack(Item item, int itemDamage) {
        ItemStack stack = new ItemStack(item);
        //PORT(1.21.1): Damage is the DAMAGE data component now. Undamaged javelins do not get the component (JavelinProperties has no
        //default DAMAGE), so returned undamaged javelins still stack with fresh ones
        int damage = stack.getMaxDamage() - itemDamage;
        if (damage > 0) {
            stack.setDamageValue(damage);
        }
        return stack;
    }
}
