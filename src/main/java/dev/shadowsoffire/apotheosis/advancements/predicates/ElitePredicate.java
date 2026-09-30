package dev.shadowsoffire.apotheosis.advancements.predicates;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Elite;
import dev.shadowsoffire.apotheosis.util.PersistentDataComponent;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Checks that the target entity has been transformed into an {@link Elite} (a miniboss).
 * <p>
 * Port addition, not in upstream. Same optional filters as {@link InvaderPredicate}:
 *
 * <pre>
 * { "type": "apotheosis:is_elite" }
 * { "type": "apotheosis:is_elite", "min_rarity": "apotheosis:epic" }
 * { "type": "apotheosis:is_elite", "id": "apotheosis:overworld/craig" }
 * </pre>
 *
 * An elite's rarity is the rarity of its affixed item. Elites whose {@code affix_data} didn't produce an affixed item
 * (chance failed, or no affixable gear) have no rarity and fail any {@code min_rarity} filter. A mob that was marked
 * for an elite but hasn't been transformed yet (it transforms when it joins the level) doesn't match.
 */
public record ElitePredicate(Optional<DynamicHolder<LootRarity>> minRarity, Optional<Identifier> id) implements EntitySubPredicate {

    public static final ElitePredicate INSTANCE = new ElitePredicate(Optional.empty(), Optional.empty());

    public static final MapCodec<ElitePredicate> CODEC = RecordCodecBuilder.mapCodec(inst -> inst
        .group(
            RarityRegistry.INSTANCE.holderCodec().optionalFieldOf("min_rarity").forGetter(ElitePredicate::minRarity),
            Identifier.CODEC.optionalFieldOf("id").forGetter(ElitePredicate::id))
        .apply(inst, ElitePredicate::new));

    @Override
    public MapCodec<? extends EntitySubPredicate> codec() {
        return CODEC;
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, Vec3 position) {
        CompoundTag data = PersistentDataComponent.get(entity);
        if (!data.getBooleanOr(Elite.MINIBOSS_KEY, false)) {
            return false;
        }
        return InvaderPredicate.matchesId(data, Elite.ID_KEY, this.id) && InvaderPredicate.meetsRarity(data, Elite.RARITY_KEY, this.minRarity);
    }

}
