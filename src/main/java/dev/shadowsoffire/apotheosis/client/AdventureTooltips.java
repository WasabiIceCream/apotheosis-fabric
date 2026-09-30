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
 * Scope note: the affix lines, since 0.4.4 also the durability bonus and malice lines and
 * upstream's {@code showBlacklistedPotions}. Not ported: the world-tier-tutorial gating (upstream
 * also hides the affix attribute lines then, which this port can't) and the "star-prefix
 * over-max-affix attribute modifiers" search. The socket line here is
 * a plain text line rather than upstream's custom icon-based {@code SocketComponent}
 * {@code TooltipComponent} (which needs its own client tooltip-component-factory registration
 * and renderer, not yet ported) — functional confirmation of "you have N sockets", not the
 * polished gem-icon row.
 */
public final class AdventureTooltips {

    private AdventureTooltips() {}

    public static void register() {
        // Upstream renderCanSocketTooltip (ScreenEvent.Render.Post): while carrying a gem over an item it fits, say that a
        // right click sockets it (AdventureEvents#stackedOnOther).
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(screen).register((scn, graphics, mouseX, mouseY, delta) -> {
                    var carried = cs.getMenu().getCarried();
                    var slot = cs.hoveredSlot;
                    if (slot == null || !carried.is(dev.shadowsoffire.apotheosis.Apoth.Items.GEM) || !SocketHelper.canSocketGemInItem(slot.getItem(), carried)) {
                        return;
                    }
                    Component itemName = Component.translatable("%s", slot.getItem().getHoverName()).withStyle(ChatFormatting.WHITE);
                    Component line = dev.shadowsoffire.apotheosis.Apotheosis.lang("misc", "right_click_to_socket", carried.getHoverName(), itemName).withStyle(ChatFormatting.GRAY);
                    graphics.setTooltipForNextFrame(Minecraft.getInstance().font, line, mouseX, mouseY);
                });
            }
        });

        ItemTooltipCallback.EVENT.register((stack, context, flag, tooltip) -> {
            // Upstream showBlacklistedPotions.
            if (stack.getItem() == net.minecraft.world.item.Items.POTION) {
                var potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY).potion()
                    .orElse(net.minecraft.world.item.alchemy.Potions.WATER);
                if (!dev.shadowsoffire.apotheosis.item.PotionCharmItem.isValidPotion(potion)) {
                    tooltip.add(Component.translatable("misc.apotheosis.blacklisted_potion").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
                return;
            }

            if (!stack.has(Components.AFFIXES) && !stack.has(Components.RARITY) && SocketHelper.getSockets(stack) <= 0
                && !stack.has(Components.DURABILITY_BONUS) && !stack.has(Components.MALICE_MARKER) && !stack.has(Components.TOUCHED_BY_MALICE)) {
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

            // Upstream affixTooltips: the rolled durability bonus and the malice markers.
            if (stack.has(Components.DURABILITY_BONUS) && !stack.has(DataComponents.UNBREAKABLE)) {
                Component desc = Component.translatable("affix.apotheosis:durable.desc", Math.round(100 * stack.get(Components.DURABILITY_BONUS)));
                lines.add(ApothMiscUtil.dotPrefix(desc).withStyle(ChatFormatting.YELLOW));
            }
            if (stack.getOrDefault(Components.MALICE_MARKER, false)) {
                lines.add(dev.shadowsoffire.apotheosis.Apotheosis.lang("text", "malice_marker").withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
            }
            if (stack.getOrDefault(Components.TOUCHED_BY_MALICE, false)) {
                lines.add(ApothMiscUtil.dotPrefix(dev.shadowsoffire.apotheosis.Apotheosis.lang("text", "touched_by_malice")).withStyle(ChatFormatting.RED));
            }

            int sockets = SocketHelper.getSockets(stack);
            if (sockets > 0) {
                lines.add(Component.translatable("misc.apotheosis.sockets", sockets).withStyle(ChatFormatting.YELLOW));
                // One line per socket, as upstream's SocketTooltipRenderer shows (text only: that renderer also draws a socket
                // icon and a small gem icon per line, which needs a tooltip component this port doesn't register yet).
                var gems = SocketHelper.getGems(stack);
                for (int i = 0; i < gems.size(); i++) {
                    var inst = gems.get(i);
                    Component desc = inst.isValid() ? inst.getSocketBonusTooltip(ctx) : Component.translatable("socket.apotheosis.empty");
                    lines.add(Component.literal(inst.isValid() ? " \u25C6 " : " \u25C7 ").append(desc).withColor(0xAABBCC));
                }
            }

            if (!lines.isEmpty()) {
                tooltip.addAll(1, lines);
            }
        });
    }
}
