package dev.shadowsoffire.apotheosis.mobs.registries;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.mobs.types.Elite;
import dev.shadowsoffire.apotheosis.tiers.Constraints;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.TieredDynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EliteRegistry extends TieredDynamicRegistry<Elite> {

    public static final EliteRegistry INSTANCE = new EliteRegistry();

    public EliteRegistry() {
        super(Apotheosis.LOGGER, Apotheosis.loc("apothic_elites"), RegistrySerializer.simple(Elite.CODEC));
    }

    /**
     * Port note: the codec resolves rarities eagerly ({@code LootRarity.CODEC}), so the rarity registry must apply first.
     * NeoForge runs reload listeners in registration order; Fabric sorts independent ones by id, which put this
     * registry ahead of {@code apotheosis:rarities}. Same fix as {@code AffixRegistry}.
     */
    @Override
    public java.util.Collection<net.minecraft.resources.Identifier> getFabricDependencies() {
        java.util.Set<net.minecraft.resources.Identifier> deps = new java.util.HashSet<>(super.getFabricDependencies());
        deps.add(dev.shadowsoffire.apotheosis.loot.RarityRegistry.INSTANCE.getFabricId());
        return deps;
    }

    @Nullable
    public Elite getRandomItem(GenContext ctx, Entity target) {
        return this.getRandomItem(ctx, Constraints.eval(ctx), IEntityMatch.matches(target));
    }

    /**
     * An item that is limited on a per-entity basis.
     */
    public static interface IEntityMatch {

        /**
         * An empty set means "all entities". To make an item invalid, return 0 weight.
         *
         * @return A set of all entities that this item can be applied to.
         */
        HolderSet<EntityType<?>> getEntities();

        public static <T extends IEntityMatch> Predicate<T> matches(EntityType<?> type) {
            return obj -> {
                var types = obj.getEntities();
                return types == null || types.size() == 0 || types.contains(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type));
            };
        }

        public static <T extends IEntityMatch> Predicate<T> matches(Entity entity) {
            return matches(entity.getType());
        }
    }

}
