package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.affix.ProjectileHooks;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;

/**
 * Upstream's {@code ProjectileImpactEvent} hook: right before {@code onHit}, so a deflected projectile (or one Apothic
 * Attributes' dodge cancelled at the head of this method) doesn't count as an impact. Fires once per entity a piercing
 * arrow passes through, as the NeoForge event does.
 */
@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {

    @Inject(method = "hitTargetOrDeflectSelf", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void apoth_projectileImpact(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> cir) {
        ProjectileHooks.onProjectileImpact((Projectile) (Object) this, hit);
    }
}
