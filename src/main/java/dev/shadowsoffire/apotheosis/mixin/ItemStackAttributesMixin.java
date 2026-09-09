package dev.shadowsoffire.apotheosis.mixin;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.StackAttributeModifiersEvent;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Injects affix-derived attribute modifiers into vanilla's attribute-modifier gathering — the
 * Fabric-native replacement for Apothic-Attributes' own mixin hook that fires
 * {@code StackAttributeModifiersEvent}. See {@link StackAttributeModifiersEvent}'s javadoc for
 * why both overloads need instrumenting (confirmed via bytecode inspection that neither
 * delegates to the other — no double-fire risk).
 */
@Mixin(ItemStack.class)
public abstract class ItemStackAttributesMixin {

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
    private void apoth$addAffixModifiersForSlot(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> sink, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        StackAttributeModifiersEvent event = StackAttributeModifiersEvent.forSlot(slot, sink);
        AffixHelper.streamAffixes(self).forEach(inst -> inst.addModifiers(event));
        SocketHelper.getGems(self).addModifiers(event);
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("TAIL"))
    private void apoth$addAffixModifiersForGroup(EquipmentSlotGroup group, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> sink, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        StackAttributeModifiersEvent event = StackAttributeModifiersEvent.forGroup(group, sink);
        AffixHelper.streamAffixes(self).forEach(inst -> inst.addModifiers(event));
        SocketHelper.getGems(self).addModifiers(event);
    }

}
