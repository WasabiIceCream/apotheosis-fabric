package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.affix.ProjectileHooks;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/**
 * Upstream's {@code ProjectileImpactEvent} hook, at the head of {@code Projectile.onHit}: vanilla reaches it from
 * {@code hitTargetOrDeflectSelf} only when the projectile wasn't deflected (or dodged: Apothic Attributes cancels that
 * method first), and Spell Engine's spell projectiles call it directly. Vanilla overrides all call {@code super.onHit}.
 * Fires once per entity a piercing arrow passes through, as the NeoForge event does.
 */
@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {

    @Inject(method = "onHit", at = @At("HEAD"))
    private void apoth_projectileImpact(HitResult hit, CallbackInfo ci) {
        ProjectileHooks.onProjectileImpact((Projectile) (Object) this, hit);
    }
}
