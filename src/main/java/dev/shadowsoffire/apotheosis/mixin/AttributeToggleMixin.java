package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.attachments.AttributeToggles;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Caps the attribute value for players who have suppressed bonuses to it via an {@code AttributeToggleAffix} (upstream
 * 9.1.0, there in its {@code LivingEntityMixin}). See {@link AttributeToggles#getCappedValue(AttributeInstance)} for the
 * clamping rules. Runs on both sides: movement speed and step height are predicted by the client.
 */
@Mixin(LivingEntity.class)
public abstract class AttributeToggleMixin {

    @Inject(method = "getAttributeValue", at = @At("HEAD"), cancellable = true)
    private void apoth_suppressToggledAttributes(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof Player player) {
            AttributeToggles toggles = player.getAttached(Apoth.Attachments.ATTRIBUTE_TOGGLES);
            if (toggles != null && toggles.isSuppressed(attribute)) {
                AttributeInstance inst = player.getAttributes().getInstance(attribute);
                if (inst != null) {
                    cir.setReturnValue(toggles.getCappedValue(inst));
                }
            }
        }
    }
}
