package dev.shadowsoffire.apotheosis.socket.gem;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import dev.shadowsoffire.apotheosis.Apoth.Components;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Port note (NeoForge -> Fabric): dropped {@code canBeHurtBy} (item-entity immunity to
 * falling-anvil damage) and {@code getCreatorModId} (advanced-tooltip "mod name" override) —
 * both are NeoForge-only {@code IItemExtension} additions with no vanilla or Fabric equivalent,
 * and both are cosmetic/minor, not core functionality.
 */
public class GemItem extends Item implements ITabFiller {

    public static final String HAS_REFRESHED = "has_refreshed";
    public static final String UUID_ARRAY = "uuids";
    public static final String GEM = "gem";

    public GemItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        UnsocketedGem inst = UnsocketedGem.of(stack);
        if (!inst.isValid()) {
            tooltip.accept(Component.literal("Errored gem with no bonus!").withStyle(ChatFormatting.GRAY));
            return;
        }
        inst.addInformation(tooltip, AttributeTooltipContext.of(ApothMiscUtil.getClientPlayer(), ctx, display, flag));
    }

    @Override
    public Component getName(ItemStack pStack) {
        UnsocketedGem inst = UnsocketedGem.of(pStack);
        if (!inst.isValid()) {
            return super.getName(pStack);
        }
        MutableComponent comp = Component.translatable(this.getGemDescriptionId(pStack));
        comp = Component.translatable("item.apotheosis.gem." + inst.purity().getSerializedName(), comp);
        return comp.withStyle(Style.EMPTY.withColor(inst.purity().getColor()));
    }

    public String getGemDescriptionId(ItemStack pStack) {
        DynamicHolder<Gem> gem = getGem(pStack);
        if (!gem.isBound()) {
            return super.getDescriptionId();
        }
        return super.getDescriptionId() + "." + gem.getId();
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        UnsocketedGem inst = UnsocketedGem.of(pStack);
        return inst.isValid() && inst.isPerfect();
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, Output out) {
        GemRegistry.INSTANCE.getValues().stream().sorted(Comparator.comparing(Gem::getId)).forEach(gem -> {
            Arrays.stream(Purity.values()).forEach(purity -> {
                if (purity.isAtLeast(gem.getMinPurity())) {
                    ItemStack stack = gem.toStack(purity);
                    out.accept(stack);
                }
            });
        });
    }

    /**
     * Retrieves the underlying Gem instance of this gem stack.
     *
     * @param gem The gem stack
     * @returns A {@link DynamicHolder} targetting the gem, which may be unbound if the gem is missing or invalid.
     */
    public static DynamicHolder<Gem> getGem(ItemStack gem) {
        return gem.getOrDefault(Components.GEM, GemRegistry.INSTANCE.emptyHolder());
    }

    /**
     * Sets the ID of the gem stored in this gem stack.
     *
     * @param gemStack The gem stack
     * @param gem      The Gem to store
     */
    public static void setGem(ItemStack gemStack, Gem gem) {
        gemStack.set(Components.GEM, GemRegistry.INSTANCE.holder(gem));
    }

    public static Purity getPurity(ItemStack stack) {
        return stack.getOrDefault(Components.PURITY, Purity.CRACKED);
    }

    public static void setPurity(ItemStack stack, Purity purity) {
        stack.set(Components.PURITY, purity);
    }

    public static ItemStack createStack(Gem gem, Purity purity, int count) {
        ItemStack stack = gem.toStack(purity);
        stack.setCount(count);
        return stack;
    }

    public static ItemStack createStack(Gem gem, Purity purity) {
        return createStack(gem, purity, 1);
    }

}
