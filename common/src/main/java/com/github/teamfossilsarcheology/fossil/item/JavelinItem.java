package com.github.teamfossilsarcheology.fossil.item;

import com.github.teamfossilsarcheology.fossil.enchantment.ModEnchantments;
import com.github.teamfossilsarcheology.fossil.entity.Javelin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class JavelinItem extends TieredItem {
    private final boolean ancient;

    public JavelinItem(Tier tier, boolean ancient) {
        super(tier, new JavelinProperties().stacksToWithDurability(16, 30).arch$tab(ModTabs.FA_OTHER_ITEM_TAB));
        this.ancient = ancient;
    }

    public JavelinItem(Tier tier) {
        this(tier, false);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeCharged) {
        if (!(shooter instanceof Player player)) {
            return;
        }
        boolean infiniteAmmo = player.getAbilities().instabuild || ModEnchantments.getItemEnchantmentLevel(level.registryAccess(), Enchantments.INFINITY, stack) > 0;
        int i = this.getUseDuration(stack, shooter) - timeCharged;
        float speed = BowItem.getPowerForTime(i);
        if (speed < 0.1) {
            return;
        }
        if (!level.isClientSide) {
            int damage = stack.getMaxDamage() - (stack.getDamageValue() + (player.getAbilities().instabuild ? 0 : 1));
            Javelin javelin = new Javelin(level, shooter, getTier(), ancient, damage);
            javelin.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, speed * 3, 1);
            if (speed == 1) {
                javelin.setCritArrow(true);
            }
            int powerLevel = ModEnchantments.getItemEnchantmentLevel(level.registryAccess(), Enchantments.POWER, stack);
            if (powerLevel > 0) {
                javelin.setBaseDamage(javelin.getBaseDamage() + powerLevel * 0.5 + 0.5);
            }
            int punchLevel = ModEnchantments.getItemEnchantmentLevel(level.registryAccess(), Enchantments.PUNCH, stack);
            if (punchLevel > 0) {
                //PORT(1.21.1): AbstractArrow#setKnockback was removed in 1.21. Javelin must provide setKnockback(int) (entity-b)
                javelin.setKnockback(punchLevel);
            }
            int flameLevel = ModEnchantments.getItemEnchantmentLevel(level.registryAccess(), Enchantments.FLAME, stack);
            if (flameLevel > 0) {
                javelin.igniteForSeconds(100);
            }
            if (infiniteAmmo) {
                javelin.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(javelin);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1,
                1 / (level.random.nextFloat() * 0.4f + 1.2f) + speed * 0.5f);
        if (!infiniteAmmo) {
            stack.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    public static class JavelinProperties extends Item.Properties {
        private boolean durabilityLocked;

        /**
         * PORT(1.21.1): Item.Properties#durability also sets the DAMAGE component and a max stack size of 1, and
         * Item.Properties#buildAndValidateComponents forbids a default DAMAGE component on stackable items.
         * We only set MAX_DAMAGE + MAX_STACK_SIZE, the DAMAGE component gets added by ItemStack#setDamageValue/hurtAndBreak.
         * TieredItem's constructor calls durability(tier.getUses()), which would override the javelin durability
         * (1.20.1 used defaultDurability, which did not), so further durability calls are ignored.
         * PORT-TODO(1.21.1): verify in game that damaged javelin stacks (DAMAGE + max stack size 16) save/load and sync fine
         */
        public JavelinProperties stacksToWithDurability(int maxStackSize, int durability) {
            component(DataComponents.MAX_DAMAGE, durability);
            component(DataComponents.MAX_STACK_SIZE, maxStackSize);
            durabilityLocked = true;
            return this;
        }

        @Override
        public Item.Properties durability(int maxDamage) {
            if (durabilityLocked) {
                return this;
            }
            return super.durability(maxDamage);
        }
    }
}
