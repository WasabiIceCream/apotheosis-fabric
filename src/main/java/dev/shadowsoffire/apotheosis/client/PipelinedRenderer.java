package dev.shadowsoffire.apotheosis.client;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Port note: upstream's ghosting is a two-part system — this {@code RENDER_ALPHA} data component
 * (already ported, see {@link Apoth.Components#RENDER_ALPHA}) plus a family of NeoForge
 * {@code PipelineModifier}s (registered in the not-yet-ported {@code AdventureModuleClient}) that
 * read the component and translate it into a custom translucent shader pipeline per alpha tier.
 * Fabric has no equivalent pipeline-modifier registry, and that shader system isn't ported, so
 * {@code RENDER_ALPHA} is currently inert: ghosted items render fully opaque instead of faded.
 * Kept as a self-contained utility (not pulling in the rest of {@code AdventureModuleClient}, which
 * has a large unported dependency graph of its own) so callers like {@code GemCaseScreen} still get
 * a correct, if visually simplified, "fake item preview" render.
 */
public final class PipelinedRenderer {

    public static void ghostFakeItem(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y, float alpha) {
        ItemStack copy = stack.copy();
        copy.set(Apoth.Components.RENDER_ALPHA, alpha);
        gfx.fakeItem(copy, x, y);
    }

    public static void ghostFakeItem(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y) {
        ghostFakeItem(gfx, stack, x, y, 0x44 / 255f);
    }

    public static void grayFakeItem(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y) {
        ItemStack copy = stack.copy();
        copy.set(Apoth.Components.RENDER_ALPHA, Float.NaN);
        gfx.fakeItem(copy, x, y);
    }

    private PipelinedRenderer() {}
}
