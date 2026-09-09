package dev.shadowsoffire.apotheosis.util;

import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port note (NeoForge -> Fabric): dropped both {@code applyOmneticData} overloads (one for
 * {@code PlayerEvent.BreakSpeed}, one for {@code PlayerEvent.HarvestCheck}) — both NeoForge-only
 * events. Per the plan's earlier research, break-speed modification has no Fabric API event at
 * all (needs a mixin into {@code ServerPlayerGameMode}/{@code Player.getDestroySpeed}), while
 * harvest-check is likely covered by Fabric API's {@code PlayerBlockBreakEvents.BEFORE/AFTER}.
 * {@link #getBaseSpeed} (the actual dig-speed math, a copy of vanilla's own
 * {@code Player#getDigSpeed} minus its event-firing tail) and {@link OmneticData} are real
 * vanilla-only logic and port cleanly — only the event-hook wiring needs a mixin, deferred
 * until {@code OmneticAffix}'s dropped {@code harvest}/{@code speed} methods are revisited.
 */
public class OmneticUtil {

    /**
     * Resolves the base dig speed for a player. This is effectively a copy of {@link Player#getDigSpeed}
     * with the event-firing code near the end removed.
     */
    public static float getBaseSpeed(Player player, ItemStack tool, BlockState state, BlockPos pos) {
        float f = tool.getDestroySpeed(state);
        if (f > 1.0F) {
            f += (float) player.getAttributeValue(Attributes.MINING_EFFICIENCY);
        }

        if (MobEffectUtil.hasDigSpeed(player)) {
            f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2F;
        }

        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            f *= switch (player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }

        f *= (float) player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (player.isEyeInFluid(FluidTags.WATER)) {
            f *= (float) player.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
        }

        if (!player.onGround()) {
            f /= 5.0F;
        }

        return f;
    }

    public static record OmneticData(String name, ItemStackTemplate[] items) {

        public static Codec<OmneticData> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.STRING.fieldOf("name").forGetter(OmneticData::name),
                Codec.list(ItemStackTemplate.CODEC).xmap(l -> l.toArray(new ItemStackTemplate[0]), Arrays::asList).fieldOf("items").forGetter(OmneticData::items))
            .apply(inst, OmneticData::new));

    }
}
