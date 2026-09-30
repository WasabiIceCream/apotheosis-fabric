package dev.shadowsoffire.apotheosis.advancements.predicates;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.util.PersistentDataComponent;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Checks that the target entity is an {@link Invader} (a boss that replaced a natural spawn, or one spawned by
 * {@code /apoth spawn_boss}).
 * <p>
 * Port note: upstream's predicate is a unit codec with no fields. This port adds two optional filters, both of which
 * default to "any":
 *
 * <pre>
 * { "type": "apotheosis:is_invader" }
 * { "type": "apotheosis:is_invader", "min_rarity": "apotheosis:rare" }
 * { "type": "apotheosis:is_invader", "id": "apotheosis:overworld/zombie" }
 * </pre>
 *
 * {@code min_rarity} passes when the invader's rarity sorts at or above the given rarity (by the rarities'
 * {@code sort_index}: common &lt; uncommon &lt; rare &lt; epic &lt; mythic). {@code id} is the invader definition's id
 * ({@code data/<ns>/apotheosis/apothic_invaders/<path>.json}). Invaders spawned before this port recorded the id
 * never match an {@code id} filter. Use it under an entity predicate's {@code type_specific} field, e.g. in a
 * {@code minecraft:player_killed_entity} criterion.
 */
public record InvaderPredicate(Optional<DynamicHolder<LootRarity>> minRarity, Optional<Identifier> id) implements EntitySubPredicate {

    public static final InvaderPredicate INSTANCE = new InvaderPredicate(Optional.empty(), Optional.empty());

    public static final MapCodec<InvaderPredicate> CODEC = RecordCodecBuilder.mapCodec(inst -> inst
        .group(
            RarityRegistry.INSTANCE.holderCodec().optionalFieldOf("min_rarity").forGetter(InvaderPredicate::minRarity),
            Identifier.CODEC.optionalFieldOf("id").forGetter(InvaderPredicate::id))
        .apply(inst, InvaderPredicate::new));

    @Override
    public MapCodec<? extends EntitySubPredicate> codec() {
        return CODEC;
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, Vec3 position) {
        CompoundTag data = PersistentDataComponent.get(entity);
        if (!data.contains(Invader.BOSS_KEY)) {
            return false;
        }
        return matchesId(data, Invader.ID_KEY, this.id) && meetsRarity(data, Invader.RARITY_KEY, this.minRarity);
    }

    static boolean matchesId(CompoundTag data, String key, Optional<Identifier> id) {
        return id.isEmpty() || id.get().toString().equals(data.getStringOr(key, ""));
    }

    /**
     * @return True if no minimum is set, or the rarity stored under {@code key} sorts at or above the minimum.
     *         An entity without a stored (or with an unknown) rarity fails any minimum.
     */
    static boolean meetsRarity(CompoundTag data, String key, Optional<DynamicHolder<LootRarity>> min) {
        if (min.isEmpty()) {
            return true;
        }
        if (!min.get().isBound()) {
            return false;
        }
        Identifier stored = Identifier.tryParse(data.getStringOr(key, ""));
        if (stored == null) {
            return false;
        }
        DynamicHolder<LootRarity> rarity = RarityRegistry.INSTANCE.holder(stored);
        return rarity.isBound() && rarity.get().sortIndex() >= min.get().get().sortIndex();
    }

}
