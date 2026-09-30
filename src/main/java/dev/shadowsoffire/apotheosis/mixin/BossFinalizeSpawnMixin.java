package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import dev.shadowsoffire.apotheosis.mobs.ApothMobEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

/**
 * Upstream {@code EnderDragonFightMixin}: a summoned Ender Dragon goes through the spawn finalization (NeoForge
 * {@code FinalizeSpawnEvent}), so World Tier augments and augmentations can apply to it. Port note: only Apotheosis's own
 * finalize-spawn handling runs, not vanilla {@code Mob#finalizeSpawn} (NeoForge's {@code EventHooks.finalizeMobSpawn} calls
 * both; vanilla's only rolls follow range and handedness).
 */
@Mixin(EnderDragonFight.class)
public abstract class BossFinalizeSpawnMixin {

    @Inject(method = "createNewDragon", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void apoth_finalizeEnderDragons(CallbackInfoReturnable<EnderDragon> cir, @Local EnderDragon dragon) {
        if (dragon.level() instanceof ServerLevel level) {
            ApothMobEvents.onFinalizeSpawn(level, dragon, EntitySpawnReason.EVENT);
        }
    }
}
