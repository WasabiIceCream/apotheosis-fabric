package dev.shadowsoffire.apotheosis.affix.effect;

import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * Port note (NeoForge -> Fabric): dropped {@code modifyIncomingDamageTags} — the actual gameplay
 * effect, which mutates an incoming {@code DamageSource}'s type tags (adds "is_magic" +
 * "bypasses_armor") when hit by a magical-arrow-affixed arrow. It used NeoForge's
 * {@code EntityInvulnerabilityCheckEvent} and {@code DamageSourceExtension} (a NeoForge-only
 * interface mixed into {@code DamageSource} allowing mutable damage tags after creation) —
 * neither has a Fabric/vanilla equivalent; a real implementation needs a mixin into damage-type
 * resolution. Not called from anywhere in the ported affix spine itself (upstream wires it from
 * the not-yet-ported {@code AdventureEvents} event-bus hub) — TODO: design the mixin when
 * {@code AdventureEvents} is reached.
 */
public class MagicalArrowAffix extends Affix {

    public static final Codec<MagicalArrowAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            affixDef(),
            PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
        .apply(inst, MagicalArrowAffix::new));

    protected Set<LootRarity> rarities;

    public MagicalArrowAffix(AffixDefinition def, Set<LootRarity> rarities) {
        super(def);
        this.rarities = rarities;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return cat.isRanged() && this.rarities.contains(rarity);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean isLevelIndependent(AffixInstance inst) {
        return true;
    }

}
