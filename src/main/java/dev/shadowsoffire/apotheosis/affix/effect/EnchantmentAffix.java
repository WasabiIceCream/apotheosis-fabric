package dev.shadowsoffire.apotheosis.affix.effect;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.spongepowered.include.com.google.common.base.Preconditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixBuilder.ValuedAffixBuilder;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Port note (NeoForge -> Fabric): {@code getEnchantmentLevels(AffixInstance,
 * GetEnchantmentLevelEvent)} is replaced by {@link #applyBonus}, called from
 * {@code mixin.EnchantmentHelperMixin} — see that mixin's javadoc. The three {@link Mode}s
 * reduce cleanly to a per-query decision (no full-enchantment-map iteration needed): SINGLE
 * always adds the bonus to this affix's own enchantment; EXISTING/GLOBAL only add it if the
 * queried enchantment already has a real (stored) level greater than zero — EXISTING further
 * requires the queried enchantment to be this affix's own.
 */
public class EnchantmentAffix extends Affix {

    public static Codec<EnchantmentAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            affixDef(),
            Enchantment.CODEC.fieldOf("enchantment").forGetter(a -> a.ench),
            Mode.CODEC.optionalFieldOf("mode", Mode.SINGLE).forGetter(a -> a.mode),
            LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories))
        .apply(inst, EnchantmentAffix::new));

    protected final Holder<Enchantment> ench;
    protected final Mode mode;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> categories;

    public EnchantmentAffix(AffixDefinition def, Holder<Enchantment> ench, Mode mode, Map<LootRarity, StepFunction> values, Set<LootCategory> categories) {
        super(def);
        this.ench = ench;
        this.values = values;
        this.mode = mode;
        this.categories = categories;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        int level = this.values.get(inst.getRarity()).getInt(inst.level());
        String desc = "bonus.apotheosis:enchantment.desc";
        if (this.mode == Mode.GLOBAL) {
            desc += ".global";
        }
        else if (this.mode == Mode.EXISTING) {
            desc += ".mustExist";
        }
        Component enchName = this.ench.value().description().plainCopy();
        return Component.translatable(desc, level, Component.translatable("misc.apotheosis.level" + (level > 1 ? ".many" : "")), enchName).withStyle(ChatFormatting.GREEN);
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        MutableComponent comp = this.getDescription(inst, ctx);
        StepFunction value = this.values.get(inst.getRarity());
        return comp.append(valueBounds(Component.literal("" + (int) value.min()), Component.literal("" + (int) value.max())));
    }

    /**
     * @param inst      This affix instance.
     * @param queried   The enchantment being queried.
     * @param realLevel The enchantment's real (stored) level for the queried enchantment, before this affix's bonus.
     * @return The level to report for {@code queried}, after this affix's bonus (if applicable).
     */
    public int applyBonus(AffixInstance inst, Holder<Enchantment> queried, int realLevel) {
        int level = this.values.get(inst.getRarity()).getInt(inst.level());
        if (this.mode == Mode.GLOBAL) {
            return realLevel > 0 ? realLevel + level : realLevel;
        }
        else if (this.mode == Mode.EXISTING) {
            return queried.equals(this.ench) && realLevel > 0 ? realLevel + level : realLevel;
        }
        else {
            return queried.equals(this.ench) ? realLevel + level : realLevel;
        }
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat.isNone()) {
            return false;
        }
        return (this.categories.isEmpty() || this.categories.contains(cat)) && this.values.containsKey(rarity);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean isLevelIndependent(AffixInstance inst) {
        return this.values.get(inst.getRarity()).isConstant();
    }

    public static enum Mode {
        SINGLE,
        EXISTING,
        GLOBAL;

        public static final Codec<Mode> CODEC = PlaceboCodecs.enumCodec(Mode.class);
    }

    public static class Builder extends ValuedAffixBuilder<Builder> {
        protected final Holder<Enchantment> enchantment;
        protected final Mode mode;
        protected final Set<LootCategory> categories = new HashSet<>();

        public Builder(Holder<Enchantment> enchantment, Mode mode) {
            this.enchantment = enchantment;
            this.mode = mode;
        }

        public Builder categories(LootCategory... cats) {
            for (LootCategory cat : cats) {
                this.categories.add(cat);
            }
            return this;
        }

        public EnchantmentAffix build() {
            Preconditions.checkArgument(!this.values.isEmpty());
            return new EnchantmentAffix(this.definition, this.enchantment, this.mode, this.values, this.categories);
        }
    }

}
