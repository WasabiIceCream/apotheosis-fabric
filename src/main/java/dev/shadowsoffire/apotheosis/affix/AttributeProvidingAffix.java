package dev.shadowsoffire.apotheosis.affix;

import java.util.function.Consumer;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * This is a bit of a hack to allow affixes to report back their attribute modifier tooltips so we can mark them with the star prefix.
 * TODO: Fold this back into {@link Affix}?
 */
public interface AttributeProvidingAffix {

    void gatherModifierTooltips(AffixInstance inst, AttributeTooltipContext ctx, Consumer<Component> list);

    /**
     * Allows the affix to hide any relevant attribute modifiers from the item's regular
     * (vanilla-rendered) attribute-modifier tooltip section.
     * <p>
     * When {@link WorldTier#isTutorialActive(Player)} is true, all modifiers should be skipped.
     * <p>
     * Port note (NeoForge -> Fabric): the original fires from NeoForge's
     * {@code GatherSkippedAttributeTooltipsEvent}. No Fabric/vanilla event exists for
     * "which attribute modifier tooltip lines should be hidden" — TODO: wire this up via a
     * mixin into {@code ItemStack.addAttributeTooltips}/{@code addDetailsToTooltip} once an
     * affix that actually needs to hide vanilla-rendered lines is reached (this method itself
     * ports cleanly; it's the call site that needs the mixin).
     *
     * @param inst The current affix instance.
     * @param ctx  The tooltip context.
     * @param skip A consumer that accepts resource locations to skip.
     */
    void skipModifierIds(AffixInstance inst, AttributeTooltipContext ctx, Consumer<Identifier> skip);

}
