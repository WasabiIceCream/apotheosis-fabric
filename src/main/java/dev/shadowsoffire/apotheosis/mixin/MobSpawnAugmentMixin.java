package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment.Target;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugmentRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Applies {@link Target#MONSTERS}-targeted {@link TierAugment}s to naturally-spawned monsters,
 * scaling their difficulty with the world's progression.
 * <p>
 * Port note (NeoForge -> Fabric): upstream applies these in {@code ApothMobEvents}, part of the
 * {@code mobs}/{@code spawner} (Gateways) package — explicitly out of this port's scope entirely.
 * Rather than pull in that whole package just for this one hook, this is a minimal, self-contained
 * mixin doing only the monster-augment application, nothing else from that package.
 * <p>
 * Since {@link WorldTier} is tracked per-player in this port (not per-region, unlike upstream's
 * Gateways-integrated model), the tier used is the highest tier among online players within
 * {@value #TIER_SEARCH_RADIUS} blocks of the spawn — falling back to {@link WorldTier#HAVEN} if
 * no player is nearby.
 */
@Mixin(Mob.class)
public class MobSpawnAugmentMixin {

    private static final double TIER_SEARCH_RADIUS = 128.0;

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void apoth$applyMonsterAugments(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        Mob self = (Mob) (Object) this;
        if (!(self instanceof Monster)) {
            return;
        }
        ServerLevel level = levelAccessor.getLevel();

        WorldTier tier = WorldTier.HAVEN;
        double radiusSqr = TIER_SEARCH_RADIUS * TIER_SEARCH_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(self) <= radiusSqr) {
                WorldTier playerTier = WorldTier.getTier(player);
                if (playerTier.ordinal() > tier.ordinal()) {
                    tier = playerTier;
                }
            }
        }

        for (TierAugment aug : TierAugmentRegistry.getAugments(tier, Target.MONSTERS)) {
            aug.apply(level, self);
        }
    }

}
