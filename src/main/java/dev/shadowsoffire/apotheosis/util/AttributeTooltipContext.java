package dev.shadowsoffire.apotheosis.util;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Port of NeoForge's {@code net.neoforged.neoforge.common.util.AttributeTooltipContext} — a
 * vanilla {@link TooltipContext} extended with the viewing {@link Player} (nullable — e.g.
 * commands may run without one), the item's {@link TooltipDisplay} (which components are
 * hidden), and the current {@link TooltipFlag} (advanced/normal), none of which vanilla's own
 * {@code TooltipContext} carries but the attribute-modifier tooltip formatting needs. There's
 * no Fabric equivalent (this is NeoForge's own addition, not a vanilla or Fabric API type),
 * so this is a straight structural port — used throughout {@code affix} wherever an affix
 * needs to format a one-line description for an item tooltip.
 */
public interface AttributeTooltipContext extends TooltipContext {

    @Nullable
    Player player();

    TooltipDisplay display();

    TooltipFlag flag();

    static AttributeTooltipContext of(@Nullable Player player, TooltipContext ctx, TooltipDisplay display, TooltipFlag flag) {
        return new AttributeTooltipContext() {

            @Override
            @Nullable
            public Player player() {
                return player;
            }

            @Override
            public TooltipDisplay display() {
                return display;
            }

            @Override
            public TooltipFlag flag() {
                return flag;
            }

            @Override
            public net.minecraft.core.HolderLookup.Provider registries() {
                return ctx.registries();
            }

            @Override
            public float tickRate() {
                return ctx.tickRate();
            }

            @Override
            public net.minecraft.world.level.saveddata.maps.MapItemSavedData mapData(net.minecraft.world.level.saveddata.maps.MapId mapId) {
                return ctx.mapData(mapId);
            }

            @Override
            public boolean isPeaceful() {
                return ctx.isPeaceful();
            }
        };
    }

    /**
     * Port of Apothic-Attributes' {@code ApothicAttributes.getTooltipFlag()} (out of scope as
     * a dependency, but this one small static helper is used throughout {@code affix} to build
     * an {@link AttributeTooltipContext}) — the advanced/normal + creative tooltip flag for the
     * current client, matching vanilla's own {@code advancedItemTooltips} option and the local
     * player's creative-mode status. Server-side (or with no client player available) falls
     * back to {@link TooltipFlag#NORMAL}.
     */
    static TooltipFlag getTooltipFlag() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return ClientAccess.getTooltipFlag();
        }
        return TooltipFlag.NORMAL;
    }

    class ClientAccess {
        static TooltipFlag getTooltipFlag() {
            Minecraft mc = Minecraft.getInstance();
            boolean advanced = mc.options.advancedItemTooltips;
            boolean creative = mc.player != null && mc.player.getAbilities().instabuild;
            TooltipFlag.Default flag = advanced ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
            return creative ? flag.asCreative() : flag;
        }
    }

}
