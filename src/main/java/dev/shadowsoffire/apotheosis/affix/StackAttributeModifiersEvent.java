package dev.shadowsoffire.apotheosis.affix;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.function.TriConsumer;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Port of Apothic-Attributes' {@code dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent}
 * — the mechanism by which an affix dynamically contributes attribute modifiers to the item
 * it's on, without physically writing them into the item's {@code ATTRIBUTE_MODIFIERS} data
 * component.
 * <p>
 * Port note (NeoForge/Apothic-Attributes -> Fabric): the original is fired from
 * Apothic-Attributes' own mixin hook into {@code ItemStack}'s attribute-modifier gathering.
 * Apothic-Attributes isn't a dependency of this port (see README.md's "known coupling point"
 * note), so this class plus {@code mixin.ItemStackAttributesMixin} reimplement that hook
 * directly: the mixin injects into both {@code ItemStack.forEachModifier} overloads (the
 * per-slot one used for live combat stat computation, and the per-{@link EquipmentSlotGroup}
 * one used for tooltip rendering — confirmed via bytecode inspection that neither delegates to
 * the other, so both need instrumenting, and doing so can't double-fire), each constructing an
 * event scoped to what's being queried and delegating to
 * {@code AffixHelper.streamAffixes(stack)}.
 */
public final class StackAttributeModifiersEvent {

    private final Predicate<EquipmentSlotGroup> groupTest;
    private final BiConsumer<Holder<Attribute>, AttributeModifier> sink;

    private StackAttributeModifiersEvent(Predicate<EquipmentSlotGroup> groupTest, BiConsumer<Holder<Attribute>, AttributeModifier> sink) {
        this.groupTest = groupTest;
        this.sink = sink;
    }

    /**
     * For {@link net.minecraft.world.item.ItemStack#forEachModifier(EquipmentSlot, BiConsumer)} —
     * the item is equipped in exactly one slot; a declared modifier applies if its group covers
     * that slot.
     */
    public static StackAttributeModifiersEvent forSlot(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> sink) {
        return new StackAttributeModifiersEvent(group -> group.test(slot), sink);
    }

    /**
     * For {@link net.minecraft.world.item.ItemStack#forEachModifier(EquipmentSlotGroup, TriConsumer)}
     * — used for tooltip rendering across all groups the item could occupy; a declared modifier
     * applies only when it's declared for exactly the group being queried. Affix-added modifiers
     * are always shown with the default display (no per-affix custom tooltip layout).
     */
    public static StackAttributeModifiersEvent forGroup(EquipmentSlotGroup queried, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> sink) {
        return new StackAttributeModifiersEvent(group -> group == queried, (attr, mod) -> sink.accept(attr, mod, ItemAttributeModifiers.Display.attributeModifiers()));
    }

    /**
     * Adds an attribute modifier for the given equipment slot group, if it's relevant to
     * whatever this event was scoped to.
     *
     * @param attribute The attribute to modify.
     * @param modifier  The modifier to add.
     * @param group     The equipment slot group this modifier applies in.
     */
    public void addModifier(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup group) {
        if (this.groupTest.test(group)) {
            this.sink.accept(attribute, modifier);
        }
    }

}
