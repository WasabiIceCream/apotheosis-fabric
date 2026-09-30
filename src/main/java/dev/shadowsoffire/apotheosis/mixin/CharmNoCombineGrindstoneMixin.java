package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Two Potion Charms can't be combined in a grindstone to repair one (upstream's {@code setNoCombineRepair()}, see
 * {@link CharmNoCombineAnvilMixin}).
 */
@Mixin(GrindstoneMenu.class)
public abstract class CharmNoCombineGrindstoneMixin {

    @Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
    private void apoth_noCharmCombine(ItemStack top, ItemStack bottom, CallbackInfoReturnable<ItemStack> cir) {
        if (top.is(Apoth.Items.POTION_CHARM) && bottom.is(Apoth.Items.POTION_CHARM)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
