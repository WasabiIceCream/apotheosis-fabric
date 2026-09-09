package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Overrides {@link ItemStack#getHoverName()} to apply an item's affix name (if any) — see
 * {@link AffixHelper#getModifiedStackName}.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void apoth$modifyHoverName(CallbackInfoReturnable<Component> cir) {
        ItemStack self = (ItemStack) (Object) this;
        Component modified = AffixHelper.getModifiedStackName(self, cir.getReturnValue());
        if (modified != null) {
            cir.setReturnValue(modified);
        }
    }

}
