package dev.shadowsoffire.apotheosis.client;

import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.util.DrawsOnLeft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Base class implementing common functionality for dark-mode menus used in the Adventure Module.
 * <p>
 * Port note: upstream also overrides {@code extractSlotHighlightBack}/{@code extractSlotHighlightFront}
 * to draw a darker slot-highlight tint matching the Adventure module's dark GUI theme. In this MC
 * version's mapped jar those two methods are {@code private} on {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen},
 * not overridable, so that cosmetic tint is dropped — slots fall back to the vanilla highlight.
 */
public abstract class AdventureContainerScreen<T extends AbstractContainerMenu> extends PlaceboContainerScreen<T> implements DrawsOnLeft {

    public AdventureContainerScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    public AdventureContainerScreen(T pMenu, Inventory pPlayerInventory, Component pTitle, int imageWidth, int imageHeight) {
        super(pMenu, pPlayerInventory, pTitle, imageWidth, imageHeight);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {}

}
