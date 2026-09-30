package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Upstream {@code AbstractSkeletonMixin}: skeletons holding a crossbow use it (vanilla only knows bows, so a skeleton given a
 * crossbow by an invader or a gear set would walk up and punch). Port note: the client-side crossbow arm pose
 * ({@code client.AbstractSkeletonRendererMixin} upstream) isn't ported; the skeleton aims with the bow pose.
 */
@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin extends Monster implements CrossbowAttackMob {

    @Unique
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private final RangedCrossbowAttackGoal<?> apoth_crossbowGoal = new RangedCrossbowAttackGoal((Monster & CrossbowAttackMob) (Object) this, 1, 15F);

    @Final
    @Shadow
    private RangedBowAttackGoal<AbstractSkeleton> bowGoal;

    @Final
    @Shadow
    private MeleeAttackGoal meleeGoal;

    protected AbstractSkeletonMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void apoth_performRangedCrossbowAttack(LivingEntity target, float velocity, CallbackInfo ci) {
        if (!this.apoth_getCrossbow().isEmpty()) {
            this.performCrossbowAttack(this, 1.6F);
            ci.cancel();
        }
    }

    @Inject(method = "reassessWeaponGoal()V", at = @At("HEAD"), cancellable = true)
    private void apoth_pickCrossbowIfAvailable(CallbackInfo ci) {
        if (this.level() != null && !this.level().isClientSide()) {
            // Always remove the crossbow goal in case we pass to vanilla logic.
            this.goalSelector.removeGoal(this.apoth_crossbowGoal);

            if (!this.apoth_getCrossbow().isEmpty()) {
                this.goalSelector.removeGoal(this.meleeGoal);
                this.goalSelector.removeGoal(this.bowGoal);
                this.goalSelector.addGoal(4, this.apoth_crossbowGoal);
                ci.cancel();
            }
        }
    }

    @Override
    public void setChargingCrossbow(boolean chargingCrossbow) {
        // Nothing to record, as upstream: skeletons have no charging pose.
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Unique
    private ItemStack apoth_getCrossbow() {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = this.getItemInHand(hand);
            if (stack.getItem() instanceof CrossbowItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
