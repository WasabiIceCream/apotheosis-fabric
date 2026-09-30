package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.Apoth.Attachments;
import dev.shadowsoffire.apotheosis.attachments.BonusLootTables;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.util.PersistentDataComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.level.Level;

/**
 * Boss-related {@link Mob} hooks.
 * <ul>
 * <li>Bonus loot tables: same injection as upstream's {@code MobMixin}, reading the Fabric attachment instead of the
 * NeoForge one.</li>
 * <li>Golem despawn: upstream's {@code AdventureEvents#despawn} ({@code MobDespawnEvent}). Invaders that descend from
 * {@link AbstractGolem} (the End's shulker invaders) never despawn on their own, so they may despawn once they have
 * existed for 10 minutes and no player is within their despawn distance.
 * See https://github.com/Shadows-of-Fire/Apotheosis/issues/1248.</li>
 * <li>Burns in sun: upstream's {@code AdventureEvents#update} ({@code EntityTickEvent.Post}). Mobs whose persistent data
 * has {@code apoth.burns_in_sun} (the Undead Knight's skeleton horse) catch fire in daylight like undead. Only mobs
 * are checked here (upstream: every entity); the flag is only ever set on mobs.</li>
 * </ul>
 */
@Mixin(Mob.class)
public abstract class MobBossMixin extends LivingEntity {

    protected MobBossMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("TAIL"), method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V")
    private void apoth$dropBonusLootTables(ServerLevel level, DamageSource damageSource, boolean hitByPlayer, CallbackInfo ci) {
        BonusLootTables tables = this.getAttached(Attachments.BONUS_LOOT_TABLES);
        if (tables != null) {
            tables.drop((Mob) (Object) this, damageSource, hitByPlayer);
        }
    }

    @Inject(at = @At("TAIL"), method = "aiStep()V")
    private void apoth$burnInSun(CallbackInfo ci) {
        if (this.level().isClientSide() || !PersistentDataComponent.get(this).contains("apoth.burns_in_sun")) {
            return;
        }
        // Copy of Mob#isSunBurnTick(), as upstream.
        if (this.level().isBrightOutside()) {
            float f = this.getLightLevelDependentMagicValue();
            net.minecraft.core.BlockPos blockpos = net.minecraft.core.BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
            boolean flag = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
            if (f > 0.5F && this.getRandom().nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !flag && this.level().canSeeSky(blockpos)) {
                this.setRemainingFireTicks(160);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "checkDespawn()V", cancellable = true)
    private void apoth$despawnGolemBosses(CallbackInfo ci) {
        if ((Object) this instanceof AbstractGolem && this.tickCount > 12000 && PersistentDataComponent.get(this).getBooleanOr(Invader.BOSS_KEY, false)) {
            Entity player = this.level().getNearestPlayer(this, -1.0D);
            if (player != null) {
                int despawnDist = this.getType().getCategory().getDespawnDistance();
                if (player.distanceToSqr(this) > despawnDist * despawnDist) {
                    this.discard();
                    ci.cancel();
                }
            }
        }
    }

}
