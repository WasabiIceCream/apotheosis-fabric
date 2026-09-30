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
 * Port note (NeoForge -> Fabric): upstream's {@code modifyIncomingDamageTags} adds {@code is_magic} and
 * {@code bypasses_armor} to the damage source through NeoForge's {@code DamageSourceExtension}. Here
 * {@code ProjectileHooks} flags the arrow when it's fired and {@code DamageSourceMagicalMixin} answers those two tag
 * checks for its damage (0.4.3).
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
