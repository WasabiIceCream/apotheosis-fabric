package dev.shadowsoffire.apotheosis.tiers.augments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.Apoth.Attachments;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.effect.DamageReductionAffix.DamageType;
import dev.shadowsoffire.apotheosis.attachments.DamageReductions;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Applies a damage reduction to the target entity via the {@link Attachments#DAMAGE_REDUCTIONS} attachment (upstream
 * 9.1.0). Read in {@code AdventureEvents#onHurt}.
 */
public record DamageReductionAugment(WorldTier tier, Target target, int sortIndex, DamageType type, float amount, Identifier id) implements TierAugment {

    public static final Codec<DamageReductionAugment> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            WorldTier.CODEC.fieldOf("tier").forGetter(TierAugment::tier),
            Target.CODEC.fieldOf("target").forGetter(TierAugment::target),
            Codec.intRange(0, 2000).optionalFieldOf("sort_index", 1000).forGetter(TierAugment::sortIndex),
            DamageType.CODEC.fieldOf("damage_type").forGetter(DamageReductionAugment::type),
            Codec.floatRange(0, 1).fieldOf("amount").forGetter(DamageReductionAugment::amount),
            Identifier.CODEC.fieldOf("reduction_id").forGetter(DamageReductionAugment::id))
        .apply(inst, DamageReductionAugment::new));

    @Override
    public Codec<? extends DamageReductionAugment> getCodec() {
        return CODEC;
    }

    @Override
    public void apply(ServerLevelAccessor level, LivingEntity entity) {
        DamageReductions.Mutable mutable = new DamageReductions.Mutable(entity.getAttachedOrElse(Attachments.DAMAGE_REDUCTIONS, DamageReductions.EMPTY));
        mutable.set(this.type, this.id, this.amount);
        set(entity, mutable.toImmutable());
    }

    @Override
    public void remove(ServerLevelAccessor level, LivingEntity entity) {
        DamageReductions.Mutable mutable = new DamageReductions.Mutable(entity.getAttachedOrElse(Attachments.DAMAGE_REDUCTIONS, DamageReductions.EMPTY));
        mutable.remove(this.type, this.id);
        set(entity, mutable.toImmutable());
    }

    private static void set(LivingEntity entity, DamageReductions reductions) {
        if (reductions.isEmpty()) {
            entity.removeAttached(Attachments.DAMAGE_REDUCTIONS);
        }
        else {
            entity.setAttached(Attachments.DAMAGE_REDUCTIONS, reductions);
        }
    }

    @Override
    public Component getDescription(AttributeTooltipContext ctx) {
        return Component.translatable("tier_augment.apotheosis.damage_reduction", Affix.fmt(100 * this.amount), Component.translatable("misc.apotheosis." + this.type.getSerializedName()));
    }

}
