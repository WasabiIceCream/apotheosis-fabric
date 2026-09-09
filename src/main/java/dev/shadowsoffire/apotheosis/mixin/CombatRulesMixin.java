package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Implements {@link Apoth.CustomAttributes#ARMOR_PIERCE} — reduces the defender's effective armor
 * value, as seen by the standard armor-damage-reduction formula, by the attacker's
 * {@code armor_pierce} attribute. Mirrors Apothic-Attributes' own mechanic (out of this port's
 * scope as a dependency, but this specific attribute is re-created — see
 * {@link Apoth.CustomAttributes}' javadoc).
 * <p>
 * Redirects the call to {@code CombatRules#getDamageAfterAbsorb} from
 * {@code LivingEntity#getDamageAfterArmorAbsorb}, giving direct access to the full call — both
 * the {@code DamageSource} (to resolve the attacker) and the {@code armor} value being passed in.
 */
@Mixin(LivingEntity.class)
public class CombatRulesMixin {

    @Redirect(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
    private float apoth$pierceArmor(LivingEntity entity, float damage, DamageSource source, float armor, float armorToughness) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            armor = Math.max(0, armor - (float) living.getAttributeValue(Apoth.CustomAttributes.ARMOR_PIERCE));
        }
        return CombatRules.getDamageAfterAbsorb(entity, damage, source, armor, armorToughness);
    }

}
