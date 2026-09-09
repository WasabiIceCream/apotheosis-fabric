package dev.shadowsoffire.apotheosis.affix.effect;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixBuilder;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Loot Pinata
 * <p>
 * Port note (NeoForge -> Fabric): dropped {@code markEquipment}/{@code modifyEntityLoot}/
 * {@code removeMarker} — all three are hooks for NeoForge's {@code LivingDeathEvent}/
 * {@code LivingDropsEvent} (no Fabric equivalent for inspecting/modifying a drop list before it
 * spawns — confirmed dead end during initial research) and NeoForge's {@code Capabilities.Item.ENTITY}
 * (mob-inventory-as-a-resource-handler capability lookup, no Fabric equivalent either). The
 * actual gameplay effect — spawning extra item copies on a mob's death — needs both a
 * drop-list mixin and an {@code IFestiveMarker}-equivalent (a per-ItemStack "already counted"
 * flag, likely a small Cardinal Components item component once ported) neither of which exist
 * yet. The affix's data/description/applicability logic ports cleanly on its own.
 */
public class FestiveAffix extends Affix {

    public static Codec<FestiveAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            affixDef(),
            LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories),
            LootRarity.mapCodec(FestiveData.CODEC).fieldOf("values").forGetter(a -> a.values))
        .apply(inst, FestiveAffix::new));

    protected final Set<LootCategory> categories;
    protected final Map<LootRarity, FestiveData> values;

    public FestiveAffix(AffixDefinition def, Set<LootCategory> categories, Map<LootRarity, FestiveData> values) {
        super(def);
        this.categories = categories;
        this.values = values;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        return Component.translatable("affix." + this.id() + ".desc", fmt(100 * this.getTrueLevel(inst.getRarity(), inst.level())));
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        MutableComponent comp = this.getDescription(inst, ctx);

        Component minComp = Component.translatable("%s%%", fmt(100 * this.getTrueLevel(inst.getRarity(), 0)));
        Component maxComp = Component.translatable("%s%%", fmt(100 * this.getTrueLevel(inst.getRarity(), 1)));
        return comp.append(valueBounds(minComp, maxComp));
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return this.categories.contains(cat) && this.values.containsKey(rarity);
    }

    private float getTrueLevel(LootRarity rarity, float level) {
        return this.values.get(rarity).chance().get(level);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean isLevelIndependent(AffixInstance inst) {
        return this.values.get(inst.getRarity()).chance.isConstant();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Data for the Festive Affix.
     *
     * @param chance The chance of the festive affix triggering, as a step function.
     * @param rolls  The number of extra copies of items to drop when the affix triggers.
     */
    public static record FestiveData(StepFunction chance, int rolls) {

        public static final Codec<FestiveData> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                StepFunction.CODEC.fieldOf("chance").forGetter(FestiveData::chance),
                Codec.INT.fieldOf("rolls").forGetter(FestiveData::rolls))
            .apply(inst, FestiveData::new));
    }

    public static class Builder extends AffixBuilder<Builder> {

        protected final Set<LootCategory> categories = new LinkedHashSet<>();
        protected final Map<LootRarity, FestiveData> values = new HashMap<>();

        public Builder categories(LootCategory... cats) {
            for (LootCategory cat : cats) {
                this.categories.add(cat);
            }
            return this;
        }

        public Builder value(LootRarity rarity, StepFunction chance, int rolls) {
            this.values.put(rarity, new FestiveData(chance, rolls));
            return this;
        }

        public FestiveAffix build() {
            return new FestiveAffix(this.definition, this.categories, this.values);
        }
    }

}
