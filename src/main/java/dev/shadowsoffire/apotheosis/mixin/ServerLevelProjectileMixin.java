package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.affix.ProjectileHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Upstream's {@code EntityJoinLevelEvent} projectile hook, for freshly fired projectiles only (loaded ones never pass
 * through {@code addFreshEntity}). Priority 900 so it runs before Apothic Attributes' arrow damage hook at the same
 * point: Spectral Shot copies the arrow's base damage, and the copy gets Apothic's multiplier on its own.
 */
@Mixin(value = ServerLevel.class, priority = 900)
public abstract class ServerLevelProjectileMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void apoth_projectileFired(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Projectile proj) {
            ProjectileHooks.onProjectileAdded(proj);
        }
        else if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
            // Death drops for AdventureEvents#onEntityDrops (see DeathDropsCollector).
            var collector = dev.shadowsoffire.apotheosis.util.DeathDropsCollector.active();
            if (collector != null) {
                collector.apoth$collectDrop(item);
            }
        }
    }
}
