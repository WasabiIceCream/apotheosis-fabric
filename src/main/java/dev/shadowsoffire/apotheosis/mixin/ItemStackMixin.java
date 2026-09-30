package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import dev.shadowsoffire.apotheosis.AdventureEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;

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

        // Upstream: any item with a rarity shows its name in the rarity colour (also renamed items and items whose affix name
        // was removed with a Sigil of Unnaming).
        var rarity = AffixHelper.getRarity(self);
        if (rarity.isBound()) {
            cir.setReturnValue(cir.getReturnValue().copy().withStyle(s -> s.withColor(rarity.get().color())));
        }
    }

    /**
     * Upstream {@code AdventureEvents#stackedOnOther} ({@code ItemStackedOnOtherEvent}): right-click a gem onto an item in an
     * inventory to socket it. Port note: added 0.4.4; the smithing table socketing recipe was the only way before.
     */
    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void apoth$socketOnRightClick(ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess access, CallbackInfoReturnable<Boolean> cir) {
        if (AdventureEvents.stackedOnOther((ItemStack) (Object) this, carried, slot, action, player, access)) {
            cir.setReturnValue(true);
        }
    }


    /**
     * Upstream: after the item's own use-on-block logic, if it didn't consume the action, gems and then affixes get a turn
     * ({@code onItemUse}; e.g. Enlightened places torches, Stoneforming converts blocks). Port note: not wired before 0.3.0.
     */
    @Inject(method = "useOn", at = @At("RETURN"), cancellable = true)
    private void apoth$useItemOnBlockPost(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) {
            ItemStack s = (ItemStack) (Object) this;
            InteractionResult socketRes = SocketHelper.getGems(s).onItemUse(ctx);
            if (socketRes != null) {
                cir.setReturnValue(socketRes);
                return;
            }

            InteractionResult afxRes = AffixHelper.streamAffixes(s).map(afx -> afx.onItemUse(ctx)).filter(r -> r != null).findFirst().orElse(null);
            if (afxRes != null) {
                cir.setReturnValue(afxRes);
            }
        }
    }
}
