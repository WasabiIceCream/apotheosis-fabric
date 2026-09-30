package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Two Potion Charms can't be combined in an anvil to repair one.
 * <p>
 * Port note (NeoForge -> Fabric): upstream sets {@code Item.Properties#setNoCombineRepair()} on the charm, a NeoForge-only
 * property that blocks both anvil and grindstone combining. Without it, any two charms merged, keeping the left one's
 * potion, so a strong charm could be kept alive with cheap ones. See {@link CharmNoCombineGrindstoneMixin}.
 */
@Mixin(AnvilMenu.class)
public abstract class CharmNoCombineAnvilMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    public CharmNoCombineAnvilMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition itemInputSlots) {
        super(menuType, containerId, inventory, access, itemInputSlots);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void apoth_noCharmCombine(CallbackInfo ci) {
        if (this.inputSlots.getItem(0).is(Apoth.Items.POTION_CHARM) && this.inputSlots.getItem(1).is(Apoth.Items.POTION_CHARM)) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            ci.cancel();
        }
    }
}
