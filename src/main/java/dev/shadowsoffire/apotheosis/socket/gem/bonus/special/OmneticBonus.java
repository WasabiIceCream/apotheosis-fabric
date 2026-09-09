package dev.shadowsoffire.apotheosis.socket.gem.bonus.special;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.socket.gem.GemClass;
import dev.shadowsoffire.apotheosis.socket.gem.GemView;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.apotheosis.util.OmneticUtil.OmneticData;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Port note: the {@code harvest}/{@code speed} static event-hook methods (NeoForge
 * {@code PlayerEvent.HarvestCheck}/{@code BreakSpeed}) are dropped — see
 * {@code util.OmneticUtil}'s javadoc. This bonus's data/tooltip is fully functional; only the
 * actual break-speed/harvest-check wiring is pending a Fabric-side hook.
 */
public class OmneticBonus extends GemBonus {

    public static final Codec<OmneticBonus> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            gemClass(),
            Purity.mapCodec(OmneticData.CODEC).fieldOf("values").forGetter(a -> a.values))
        .apply(inst, OmneticBonus::new));

    protected final Map<Purity, OmneticData> values;

    public OmneticBonus(GemClass gemClass, Map<Purity, OmneticData> values) {
        super(gemClass);
        this.values = values;
    }

    @Override
    public Codec<? extends GemBonus> getCodec() {
        return CODEC;
    }

    @Override
    public boolean supports(Purity purity) {
        return this.values.containsKey(purity);
    }

    @Override
    public Component getSocketBonusTooltip(GemView gem, AttributeTooltipContext ctx) {
        return Component.translatable("affix.apotheosis:breaker/effect/omnetic.desc", Component.translatable("misc.apotheosis." + this.values.get(gem.purity()).name())).withStyle(ChatFormatting.YELLOW);
    }

    public Map<Purity, OmneticData> getValues() {
        return this.values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends GemBonus.Builder {

        private final Map<Purity, OmneticData> values = new LinkedHashMap<>();

        public Builder value(Purity rarity, String name, Item... items) {
            OmneticData data = new OmneticData(name, Arrays.stream(items).map(ItemStackTemplate::new).toArray(ItemStackTemplate[]::new));
            this.values.put(rarity, data);
            return this;
        }

        @Override
        public OmneticBonus build(GemClass gClass) {
            return new OmneticBonus(gClass, this.values);
        }

    }

}
