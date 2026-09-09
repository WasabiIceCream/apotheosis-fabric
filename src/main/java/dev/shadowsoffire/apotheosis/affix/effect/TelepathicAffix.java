package dev.shadowsoffire.apotheosis.affix.effect;

import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Teleport Drops
 * <p>
 * Port note (NeoForge -> Fabric): dropped the two static {@code drops(...)} event-hook helpers
 * (one for {@code LivingDropsEvent}, one for {@code BlockDropsEvent}) — both NeoForge-only
 * events for inspecting/relocating a drop list before it spawns, with no Fabric/vanilla
 * equivalent (confirmed during initial research — see the plan doc's mixin-needed list). The
 * affix itself (what makes {@link #enablesTelepathy()} true, and where it applies) ports
 * cleanly; only the actual drop-relocation behavior needs a mixin, deferred until
 * {@code AdventureEvents} (the not-yet-ported event-bus wiring hub these were called from) is
 * reached.
 */
public class TelepathicAffix extends Affix {

    public static final Codec<TelepathicAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            affixDef(),
            PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
        .apply(inst, TelepathicAffix::new));

    public static Vec3 blockDropTargetPos = null;

    protected Set<LootRarity> rarities;

    public TelepathicAffix(AffixDefinition def, Set<LootRarity> rarities) {
        super(def);
        this.rarities = rarities;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (cat.isRanged() || cat.isMelee() || cat.isBreaker()) && this.rarities.contains(rarity);
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        LootCategory cat = LootCategory.forItem(inst.stack());
        String type = cat.isRanged() || cat.isMelee() ? "weapon" : "tool";
        return Component.translatable("affix." + this.id() + ".desc." + type);
    }

    @Override
    public boolean enablesTelepathy() {
        return true;
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
