package dev.shadowsoffire.apotheosis.mobs.registries;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.tiers.Constraints;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.TieredDynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;

public class InvaderRegistry extends TieredDynamicRegistry<Invader> {

    public static final InvaderRegistry INSTANCE = new InvaderRegistry();

    public InvaderRegistry() {
        super(Apotheosis.LOGGER, Apotheosis.loc("apothic_invaders"), RegistrySerializer.simple(Invader.CODEC));
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

    @Override
    @Nullable
    public Invader getRandomItem(GenContext ctx) {
        return this.getRandomItem(ctx, Constraints.eval(ctx));
    }

}
