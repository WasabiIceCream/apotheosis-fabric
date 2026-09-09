package dev.shadowsoffire.apotheosis.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Port note: vanilla's {@code Attribute.toComponent(AttributeModifier, TooltipFlag)} and
 * {@code Attribute.toValueComponent(Operation, double, TooltipFlag)} — both used throughout
 * upstream's affix code for attribute-modifier tooltip text — no longer exist in this vanilla
 * version at all; the equivalent logic moved into (and is now private inside)
 * {@link ItemAttributeModifiers.Display.Default#apply}. {@link #toComponent} delegates to that
 * (a straight drop-in, not a reimplementation — see {@code AttributeAugment}/
 * {@code AttributeAffix}'s own port notes for the same pattern). {@link #toValueComponent},
 * used for min/max bound display (a narrower need {@code Display} doesn't expose at all), is a
 * best-effort reimplementation of the same value-only formatting — purely cosmetic tooltip
 * text, not gameplay-affecting, so a close approximation is an acceptable risk here.
 */
public final class AttributeFormatUtil {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private AttributeFormatUtil() {}

    /**
     * Formats a float the way upstream's {@code Affix.fmt} did via NeoForge's
     * {@code IAttributeExtension.FORMAT} — whole numbers with no decimal point, otherwise up
     * to 2 decimal places.
     */
    public static String fmt(float f) {
        if (f == (long) f) {
            return String.format("%d", (long) f);
        }
        return FORMAT.format(f);
    }

    /**
     * Formats a single attribute modifier as a full tooltip line, delegating to vanilla's own
     * default attribute-modifier display logic.
     */
    public static Component toComponent(Holder<Attribute> attribute, AttributeModifier modifier, Player player) {
        List<Component> lines = new ArrayList<>(1);
        ItemAttributeModifiers.Display.attributeModifiers().apply(lines::add, player, attribute, modifier);
        return lines.isEmpty() ? Component.empty() : lines.get(0);
    }

    /**
     * Formats just the numeric value of a modifier (e.g. for min/max bound display), matching
     * vanilla's sign + percent-for-multiplicative-operations convention.
     */
    public static MutableComponent toValueComponent(Operation op, double value) {
        String formatted = FORMAT.format(op == Operation.ADD_VALUE ? value : value * 100);
        String sign = value >= 0 ? "+" : "";
        String suffix = op == Operation.ADD_VALUE ? "" : "%";
        return Component.literal(sign + formatted + suffix);
    }

}
