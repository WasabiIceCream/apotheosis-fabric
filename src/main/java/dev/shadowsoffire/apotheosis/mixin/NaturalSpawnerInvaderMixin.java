package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

import dev.shadowsoffire.apotheosis.mobs.ApothMobEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Lets an Apothic Invader take the place of a natural or chunk-generation monster spawn.
 * <p>
 * Port note (NeoForge -> Fabric): upstream cancels the {@code FinalizeSpawnEvent} and sets NeoForge's "spawn cancelled"
 * flag, which NeoForge's patched {@code NaturalSpawner} honours by skipping both {@code finalizeSpawn} and the add. Both
 * spawn paths that produce {@code NATURAL}/{@code CHUNK_GENERATION} spawns are wrapped here the same way: when
 * {@link ApothMobEvents#tryReplaceWithInvader} spawns an invader, the original mob's {@code finalizeSpawn} is not
 * called and the following {@code addFreshEntityWithPassengers} for it is skipped, so it never enters the world.
 * The flag is shared between the two wrappers of one method and reset on every finalize call. In
 * {@code spawnCategoryForPosition}, the spawn callback still counts the slot (the invader took it).
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerInvaderMixin {

    @WrapOperation(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"))
    private static SpawnGroupData apoth$naturalInvader(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData,
        Operation<SpawnGroupData> original, @Share("apothInvaded") LocalBooleanRef invaded) {
        return apoth$maybeInvade(mob, level, difficulty, reason, groupData, original, invaded);
    }

    @WrapOperation(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void apoth$naturalSkipAdd(ServerLevel level, Entity entity, Operation<Void> original, @Share("apothInvaded") LocalBooleanRef invaded) {
        if (!invaded.get()) {
            original.call(level, entity);
        }
    }

    @WrapOperation(method = "spawnMobsForChunkGeneration",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"))
    private static SpawnGroupData apoth$chunkGenInvader(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData,
        Operation<SpawnGroupData> original, @Share("apothInvaded") LocalBooleanRef invaded) {
        return apoth$maybeInvade(mob, level, difficulty, reason, groupData, original, invaded);
    }

    @WrapOperation(method = "spawnMobsForChunkGeneration",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void apoth$chunkGenSkipAdd(ServerLevelAccessor level, Entity entity, Operation<Void> original, @Share("apothInvaded") LocalBooleanRef invaded) {
        if (!invaded.get()) {
            original.call(level, entity);
        }
    }

    private static SpawnGroupData apoth$maybeInvade(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData,
        Operation<SpawnGroupData> original, LocalBooleanRef invaded) {
        if (ApothMobEvents.tryReplaceWithInvader(level, mob, reason)) {
            invaded.set(true);
            return groupData;
        }
        invaded.set(false);
        return original.call(mob, level, difficulty, reason, groupData);
    }

}
