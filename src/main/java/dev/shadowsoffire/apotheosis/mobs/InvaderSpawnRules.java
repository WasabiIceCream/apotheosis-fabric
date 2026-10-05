package dev.shadowsoffire.apotheosis.mobs;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.mobs.registries.InvaderSpawnRulesRegistry;
import dev.shadowsoffire.apotheosis.mobs.util.SurfaceType;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;

/**
 * Record holding the per-dimension invader spawn rules, loaded by {@link InvaderSpawnRulesRegistry}
 * (upstream: the NeoForge data map {@code apotheosis:invader_spawn_rules} on dimension types).
 *
 * @param spawnChances The per-world-tier spawn chances for invaders in the target dimension.
 * @param cooldown     An optional cooldown override for this dimension. If not set, the configured default cooldown will be used.
 * @param surfaceType  The surface type used for this dimension.
 * @param cursed       Overrides {@code AdventureConfig.curseBossItems} in this dimension (upstream 9.1.0).
 * @param autoAggro    Overrides {@code AdventureConfig.bossAutoAggro} in this dimension (upstream 9.1.0).
 */
public record InvaderSpawnRules(Map<WorldTier, Float> spawnChances, Optional<Integer> cooldown, SurfaceType surfaceType, Optional<Boolean> cursed, Optional<Boolean> autoAggro) {

    public static final Codec<InvaderSpawnRules> CODEC = RecordCodecBuilder.<InvaderSpawnRules>create(inst -> inst
        .group(
            WorldTier.mapCodec(Codec.floatRange(0, 1)).fieldOf("spawn_chances").forGetter(InvaderSpawnRules::spawnChances),
            Codec.intRange(0, 720000).optionalFieldOf("cooldown").forGetter(InvaderSpawnRules::cooldown),
            SurfaceType.CODEC.fieldOf("surface_type").forGetter(InvaderSpawnRules::surfaceType),
            Codec.BOOL.optionalFieldOf("cursed").forGetter(InvaderSpawnRules::cursed),
            Codec.BOOL.optionalFieldOf("auto_aggro").forGetter(InvaderSpawnRules::autoAggro))
        .apply(inst, InvaderSpawnRules::new))
        .validate(InvaderSpawnRules::validate);

    private static DataResult<InvaderSpawnRules> validate(InvaderSpawnRules rules) {
        if (rules.spawnChances.size() == WorldTier.values().length) {
            return DataResult.success(rules);
        }
        else {
            StringBuilder sb = new StringBuilder("Missing Spawn Chances for the following world tiers: ");
            for (WorldTier tier : WorldTier.values()) {
                if (!rules.spawnChances.containsKey(tier)) {
                    sb.append(tier.getSerializedName()).append(" ");
                }
            }
            return DataResult.error(() -> sb.toString());
        }
    }
}
