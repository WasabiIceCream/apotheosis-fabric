package dev.shadowsoffire.apotheosis.socket.gem.bonus;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.socket.gem.GemClass;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.GemView;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Port note: {@code getEnchantmentLevels(GemInstance, GetEnchantmentLevelEvent)} is replaced by
 * {@link #applyBonus}, called from {@code mixin.EnchantmentHelperMixin} — same pattern as
 * {@code affix.effect.EnchantmentAffix}, see that class's javadoc for why the three
 * {@link Mode}s reduce cleanly to a per-query decision.
 */
public class EnchantmentBonus extends GemBonus {

    public static Codec<EnchantmentBonus> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            gemClass(),
            Enchantment.CODEC.fieldOf("enchantment").forGetter(a -> a.ench),
            Mode.CODEC.optionalFieldOf("mode", Mode.SINGLE).forGetter(a -> a.mode),
            Purity.mapCodec(Codec.intRange(1, 127)).fieldOf("values").forGetter(a -> a.values))
        .apply(inst, EnchantmentBonus::new));

    protected final Holder<Enchantment> ench;
    protected final Mode mode;
    protected final Map<Purity, Integer> values;

    public EnchantmentBonus(GemClass gemClass, Holder<Enchantment> ench, Mode mode, Map<Purity, Integer> values) {
        super(gemClass);
        this.ench = ench;
        this.values = values;
        this.mode = mode;
    }

    @Override
    public Component getSocketBonusTooltip(GemView gem, AttributeTooltipContext ctx) {
        int level = this.values.get(gem.purity());
        String desc = "bonus." + this.getTypeKey() + ".desc";
        if (this.mode == Mode.GLOBAL) {
            desc += ".global";
        }
        else if (this.mode == Mode.EXISTING) {
            desc += ".mustExist";
        }
        Component enchName = this.ench.value().description().plainCopy();
        return Component.translatable(desc, level, Component.translatable("misc.apotheosis.level" + (level > 1 ? ".many" : "")), enchName).withStyle(ChatFormatting.GREEN);
    }

    /**
     * @param gem       This gem instance.
     * @param queried   The enchantment being queried.
     * @param realLevel The enchantment's real (stored) level for the queried enchantment, before this bonus's contribution.
     * @return The level to report for {@code queried}, after this bonus's contribution (if applicable).
     */
    public int applyBonus(GemInstance gem, Holder<Enchantment> queried, int realLevel) {
        int level = this.values.get(gem.purity());
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
    public boolean supports(Purity purity) {
        return this.values.containsKey(purity);
    }

    @Override
    public Codec<? extends GemBonus> getCodec() {
        return CODEC;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static enum Mode {
        SINGLE,
        EXISTING,
        GLOBAL;

        public static final Codec<Mode> CODEC = PlaceboCodecs.enumCodec(Mode.class);
    }

    public static class Builder extends GemBonus.Builder {
        private Holder<Enchantment> enchantment;
        private Mode mode;
        private Map<Purity, Integer> values;

        public Builder() {
            this.values = new HashMap<>();
            this.mode = Mode.SINGLE;
        }

        public Builder enchantment(Holder<Enchantment> enchantment) {
            this.enchantment = enchantment;
            return this;
        }

        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder value(Purity purity, int value) {
            if (value < 1 || value > 127) {
                throw new IllegalArgumentException("EnchantmentBonus is limited to values between 1 and 127 (inclusive).");
            }
            this.values.put(purity, value);
            return this;
        }

        @Override
        public EnchantmentBonus build(GemClass gemClass) {
            return new EnchantmentBonus(gemClass, this.enchantment, this.mode, this.values);
        }
    }

}
