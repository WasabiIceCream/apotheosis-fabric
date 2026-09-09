package dev.shadowsoffire.apotheosis.client;

import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.item.component.TooltipDisplay;

import dev.shadowsoffire.apotheosis.Apoth.Components;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;

/**
 * Port of {@code AdventureModuleClient#affixTooltips(ItemTooltipEvent)} (NeoForge) — adds the
 * per-affix description lines to an item's tooltip.
 * <p>
 * Port bug found via live runtime testing: nothing in the port hooked into any tooltip pipeline
 * at all. {@code AffixHelper}/{@code AffixInstance} et al. compute the right data (confirmed via
 * {@code /data get entity ... SelectedItem} showing correct {@code apotheosis:rarity} and
 * {@code apotheosis:sockets} components after {@code /apoth} commands), and the item's display
 * name is already correctly recolored by {@code ItemStackMixin} + {@code AffixHelper#getModifiedStackName}
 * — but the tooltip itself stayed completely vanilla-plain, since only Apotheosis's own custom
 * item classes (gems, reforging table, etc.) had {@code appendHoverText} overrides; nothing
 * applied to arbitrary vanilla items carrying affix/rarity/socket components.
 * <p>
 * Scope note: this ports the affix-description-lines half only (the highest-value, most visible
 * piece). Not yet ported: the world-tier-tutorial gating, the durability-bonus/malice-marker
 * lines, and the "star-prefix over-max-affix attribute modifiers" search (all in the original
 * method but not required for the socket/affix display to work at all). The socket line here is
 * a plain text line rather than upstream's custom icon-based {@code SocketComponent}
 * {@code TooltipComponent} (which needs its own client tooltip-component-factory registration
 * and renderer, not yet ported) — functional confirmation of "you have N sockets", not the
 * polished gem-icon row.
 */
public final class AdventureTooltips {

    private AdventureTooltips() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, tooltip) -> {
            if (!stack.has(Components.AFFIXES) && !stack.has(Components.RARITY) && SocketHelper.getSockets(stack) <= 0) {
                return;
            }

            AttributeTooltipContext ctx = AttributeTooltipContext.of(Minecraft.getInstance().player, context,
                stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT), flag);

            List<Component> lines = new java.util.ArrayList<>();

            if (stack.has(Components.AFFIXES)) {
                AffixHelper.streamAffixes(stack)
                    .sorted(Comparator.comparingInt(a -> a.getAffix().definition().type().ordinal()))
                    .forEach(inst -> {
                        Component desc = inst.getDescription(ctx);
                        if (!(desc.getContents() instanceof PlainTextContents pt) || !pt.text().isEmpty()) {
                            if (inst.level() > Affix.STANDARD_MAX_LEVEL) {
                                lines.add(ApothMiscUtil.starPrefix(desc).withStyle(ChatFormatting.YELLOW));
                            }
                            else {
                                lines.add(ApothMiscUtil.dotPrefix(desc).withStyle(ChatFormatting.YELLOW));
                            }
                        }
                    });
            }

            int sockets = SocketHelper.getSockets(stack);
            if (sockets > 0) {
                lines.add(Component.translatable("misc.apotheosis.sockets", sockets).withStyle(ChatFormatting.YELLOW));
            }

            if (!lines.isEmpty()) {
                tooltip.addAll(1, lines);
            }
        });
    }
}
