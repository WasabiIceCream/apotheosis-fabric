package dev.shadowsoffire.apotheosis.affix;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.apotheosis.util.SpellWeapons;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Gameoverse addition ({@code apotheosis:optional_attribute}): an {@link AttributeAffix} whose attribute may belong to a
 * mod that isn't installed (then the affix never rolls, instead of failing to load), and which can be limited to items
 * that already have that attribute ({@code "require_item_attribute": true}). Used by the spell weapon affixes: a fire
 * staff only rolls Fire Spell Power.
 */
public class OptionalAttributeAffix extends AttributeAffix {

    public static final Codec<OptionalAttributeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            affixDef(),
            Identifier.CODEC.fieldOf("attribute").forGetter(a -> a.attributeId),
            PlaceboCodecs.enumCodec(Operation.class).fieldOf("operation").forGetter(a -> a.operation),
            LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories),
            Codec.BOOL.optionalFieldOf("require_item_attribute", false).forGetter(a -> a.requireItemAttribute))
        .apply(inst, OptionalAttributeAffix::new));

    protected final Identifier attributeId;
    protected final boolean present;
    protected final boolean requireItemAttribute;

    public OptionalAttributeAffix(AffixDefinition def, Identifier attributeId, Operation op, Map<LootRarity, StepFunction> values, Set<LootCategory> categories, boolean requireItemAttribute) {
        this(def, attributeId, BuiltInRegistries.ATTRIBUTE.get(attributeId).map(h -> (Holder<net.minecraft.world.entity.ai.attributes.Attribute>) h), op, values, categories, requireItemAttribute);
    }

    private OptionalAttributeAffix(AffixDefinition def, Identifier attributeId, Optional<Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attr, Operation op, Map<LootRarity, StepFunction> values, Set<LootCategory> categories,
        boolean requireItemAttribute) {
        // A missing attribute keeps a placeholder so the parent's fields are set; present=false stops every use of it.
        super(def, attr.orElse(Attributes.LUCK), op, values, categories);
        this.attributeId = attributeId;
        this.present = attr.isPresent();
        this.requireItemAttribute = requireItemAttribute;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return this.present && super.canApplyTo(stack, cat, rarity) && (!this.requireItemAttribute || SpellWeapons.itemHasAttribute(stack, this.attribute));
    }

    @Override
    public void addModifiers(AffixInstance inst, StackAttributeModifiersEvent event) {
        if (this.present) super.addModifiers(inst, event);
    }

    @Override
    public void gatherModifierTooltips(AffixInstance inst, AttributeTooltipContext ctx, Consumer<Component> list) {
        if (this.present) super.gatherModifierTooltips(inst, ctx, list);
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        return this.present ? super.getAugmentingText(inst, ctx) : Component.literal(this.attributeId.toString());
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}
