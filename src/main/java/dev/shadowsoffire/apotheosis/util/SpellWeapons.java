package dev.shadowsoffire.apotheosis.util;

import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Gameoverse addition: recognizes Spell Engine casting weapons (RPG Series staves and wands) without depending on Spell
 * Power, by reading the {@code spell_power} namespace of the item's own attribute modifiers.
 */
public final class SpellWeapons {

    public static final String SPELL_POWER = "spell_power";
    /** Spell Power attributes that aren't a school's power. */
    private static final Set<String> NON_SCHOOL = Set.of("generic", "critical_chance", "critical_damage", "haste");

    private SpellWeapons() {}

    public static boolean isSchoolPower(Holder<Attribute> attr) {
        Identifier id = attr.unwrapKey().map(k -> k.identifier()).orElse(null);
        return id != null && SPELL_POWER.equals(id.getNamespace()) && !NON_SCHOOL.contains(id.getPath()) && !id.getPath().startsWith("resistance");
    }

    /**
     * A spell weapon: not a sword or axe, and its main hand modifiers add at least as much power in one spell school as
     * they add attack damage. Matches every RPG Series staff and wand (school power 3-8, attack damage 2-4); spell
     * blades and melee weapons with a small school bonus stay melee weapons.
     */
    public static boolean isSpellWeapon(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) return false;
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double school = 0;
        double attack = 0;
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (!e.slot().test(EquipmentSlot.MAINHAND) || e.modifier().operation() != Operation.ADD_VALUE) continue;
            if (e.attribute().is(Attributes.ATTACK_DAMAGE)) {
                attack += e.modifier().amount();
            }
            else if (isSchoolPower(e.attribute())) {
                school = Math.max(school, e.modifier().amount());
            }
        }
        return school > 0 && school >= attack;
    }

    /** True if the item's own main hand modifiers include the given attribute with a positive amount. */
    public static boolean itemHasAttribute(ItemStack stack, Holder<Attribute> attr) {
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().is(attr) && e.modifier().amount() > 0 && e.slot().test(EquipmentSlot.MAINHAND)) return true;
        }
        return false;
    }
}
