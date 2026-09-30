package dev.shadowsoffire.apotheosis.mobs.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.mobs.util.EntityModifier;
import dev.shadowsoffire.apotheosis.mobs.util.SpawnCondition;
import dev.shadowsoffire.apotheosis.tiers.Constraints;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * An Augmentation is a non-exclusive modifier that may apply to any naturally spawned mob.
 * <p>
 * Augmentations are applied very early in the mob spawn pipeline, immediately after world tier modifiers.
 * <p>
 * Mobs have a chance to be selected for augmenting. If they are selected, every loaded augmentation will attempt to apply.
 *
 * @param chance      The per-world-tier chance that this augmentation is selected. A tier missing from the map has a 0% chance.
 * @param constraints Any context-based restrictions on the application of this augmentation.
 * @param conditions  Any entity-based restrictions on the application of this augmentation.
 * @param modifiers   The list of modifiers that will be applied to the target entity.
 * <p>
 * Port note (Gameoverse): upstream's {@code application_chance} is a single float. Here it also accepts a
 * per-world-tier map, the same shape as {@code InvaderSpawnRules#spawn_chances} and our
 * {@code random_enchant} loot modifier:
 *
 * <pre>
 * "application_chance": 0.12
 * "application_chance": { "haven": 0.02, "frontier": 0.04, "ascent": 0.07, "summit": 0.10, "pinnacle": 0.12 }
 * </pre>
 *
 * A plain float applies to every tier (upstream's format, still valid); a tier left out of the map rolls 0%.
 * The chance is read from the {@link GenContext#tier()} of the spawn, i.e. the nearest player's tier.
 */
public record Augmentation(Map<WorldTier, Float> chances, Constraints constraints, List<SpawnCondition> conditions, List<EntityModifier> modifiers) {

    /**
     * Either a single chance for every tier, or a per-tier map. Encodes back to a single float when every tier shares one value.
     */
    public static final Codec<Map<WorldTier, Float>> CHANCE_CODEC = Codec.either(Codec.floatRange(0, 1), WorldTier.mapCodec(Codec.floatRange(0, 1)).codec())
        .xmap(either -> either.map(Augmentation::uniform, Map::copyOf), map -> {
            if (map.size() == WorldTier.values().length && map.values().stream().distinct().count() == 1) {
                return Either.left(map.values().iterator().next());
            }
            return Either.right(map);
        });

    public static final Codec<Augmentation> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            CHANCE_CODEC.fieldOf("application_chance").forGetter(Augmentation::chances),
            Constraints.CODEC.optionalFieldOf("constraints", Constraints.EMPTY).forGetter(Augmentation::constraints),
            SpawnCondition.CODEC.listOf().optionalFieldOf("conditions", Collections.emptyList()).forGetter(Augmentation::conditions),
            EntityModifier.CODEC.listOf().fieldOf("modifiers").forGetter(Augmentation::modifiers))
        .apply(inst, Augmentation::new));

    public boolean canApply(ServerLevelAccessor level, Mob mob, EntitySpawnReason type, GenContext ctx) {
        if (!this.constraints.test(ctx)) {
            return false;
        }

        return SpawnCondition.checkAll(this.conditions, mob, level, type);
    }

    /**
     * @return The chance that this augmentation applies at the given world tier (0 if the tier is not listed).
     */
    public float chance(WorldTier tier) {
        return this.chances.getOrDefault(tier, 0F);
    }

    private static Map<WorldTier, Float> uniform(float chance) {
        Map<WorldTier, Float> map = new EnumMap<>(WorldTier.class);
        for (WorldTier tier : WorldTier.values()) {
            map.put(tier, chance);
        }
        return Map.copyOf(map);
    }

    public void apply(Mob mob, GenContext ctx) {
        for (EntityModifier em : this.modifiers) {
            em.apply(mob, ctx);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<WorldTier, Float> chances = uniform(1F);
        private Constraints constraints = Constraints.EMPTY;
        private List<SpawnCondition> conditions = new ArrayList<>();
        private List<EntityModifier> modifiers = new ArrayList<>();

        public Builder chance(float chance) {
            this.chances = uniform(chance);
            return this;
        }

        public Builder chance(WorldTier tier, float chance) {
            Map<WorldTier, Float> copy = new EnumMap<>(WorldTier.class);
            copy.putAll(this.chances);
            copy.put(tier, chance);
            this.chances = Map.copyOf(copy);
            return this;
        }

        public Builder constraints(Constraints constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder conditions(SpawnCondition... condition) {
            this.conditions.addAll(Arrays.asList(condition));
            return this;
        }

        public Builder modifiers(EntityModifier... modifiers) {
            this.modifiers.addAll(Arrays.asList(modifiers));
            return this;
        }

        public Augmentation build() {
            if (this.modifiers.isEmpty()) {
                throw new IllegalStateException("At least one modifier must be added");
            }
            return new Augmentation(this.chances, this.constraints, this.conditions, this.modifiers);
        }
    }

}
