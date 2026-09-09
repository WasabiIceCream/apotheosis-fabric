package dev.shadowsoffire.apotheosis.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Fires {@link dev.shadowsoffire.apotheosis.advancements.EquippedItemTrigger} for the equipped-item
 * progression advancements.
 * <p>
 * Port note (NeoForge -> Fabric): upstream fires this from NeoForge's
 * {@code LivingEquipmentChangeEvent}, which has no Fabric equivalent. Vanilla's own
 * {@code LivingEntity#collectEquipmentChanges()} already computes exactly the same
 * "which slots actually changed, and to what" data every tick (it's what
 * {@code detectEquipmentUpdates()} uses internally to fire attribute-modifier updates) — this
 * mixin just taps its return value instead of needing a new detection mechanism.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipmentTriggerMixin {

    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void apoth$fireEquippedItemTrigger(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        Map<EquipmentSlot, ItemStack> changes = cir.getReturnValue();
        if (changes == null) {
            return;
        }
        for (Map.Entry<EquipmentSlot, ItemStack> entry : changes.entrySet()) {
            Apoth.Triggers.EQUIPPED_ITEM.trigger(player, entry.getKey(), entry.getValue());
        }
    }

}
