package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.mobs.ApothMobEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Runs Apotheosis's per-spawn mob processing (monster tier augments, {@code apothic_augments}, elite selection) for
 * every mob that is finalized, whatever the spawn reason.
 * <p>
 * Port note (NeoForge -> Fabric): replaces the non-invader half of upstream's {@code FinalizeSpawnEvent} handler (see
 * {@link ApothMobEvents}). Injected at the head of {@code Mob#finalizeSpawn}, which every override reaches through
 * {@code super}, so it runs before the subclass's own equipment/variant logic, like the NeoForge event. The invader
 * half lives in {@link NaturalSpawnerInvaderMixin}. This supersedes the old {@code MobSpawnAugmentMixin}, which only
 * applied tier augments (using the highest tier among players within 128 blocks); the tier now comes from the nearest
 * player, as upstream.
 */
@Mixin(Mob.class)
public class MobFinalizeSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void apoth$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        ApothMobEvents.onFinalizeSpawn(level, (Mob) (Object) this, spawnReason);
    }

}
