package dev.shadowsoffire.apotheosis.loot.conditions;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Checks that the {@link LootContextParams#ATTACKING_ENTITY attacker} in a loot context is a real player.
 * <p>
 * Port note (NeoForge -> Fabric): upstream also excludes {@code net.neoforged.neoforge.common.util.FakePlayer}
 * instances (used by various mods to represent non-player actors, e.g. quarries). Fabric has no
 * equivalent standardized fake-player marker type, so this simplifies to a plain
 * {@code instanceof ServerPlayer} check — loot generation always happens server-side, so this is
 * still "a real connected player," just without the fake-player exclusion.
 */
public class KilledByRealPlayerCondition implements LootItemCondition {

    public static final KilledByRealPlayerCondition INSTANCE = new KilledByRealPlayerCondition();
    public static final MapCodec<KilledByRealPlayerCondition> CODEC = MapCodec.unit(INSTANCE);

    private KilledByRealPlayerCondition() {}

    @Override
    public MapCodec<KilledByRealPlayerCondition> codec() {
        return CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ATTACKING_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        Entity attacker = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
        return attacker instanceof ServerPlayer;
    }

    public static LootItemCondition.Builder killedByPlayer() {
        return () -> INSTANCE;
    }
}
