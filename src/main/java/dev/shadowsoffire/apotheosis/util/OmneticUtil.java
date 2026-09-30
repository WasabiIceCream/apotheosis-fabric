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
 * Port note (NeoForge -> Fabric): upstream's {@code applyOmneticData} overloads take NeoForge's
 * {@code PlayerEvent.BreakSpeed} and {@code HarvestCheck}. Here they take the vanilla values instead and are called from
 * {@code mixin.PlayerOmneticMixin} ({@code Player#getDestroySpeed} and {@code Player#hasCorrectToolForDrops}).
 */
public class OmneticUtil {

    /**
     * Upstream {@code applyOmneticData(BreakSpeed, OmneticData)}: the best speed among the omnetic tools, or the current
     * speed if that is higher.
     */
    public static float applyOmneticSpeed(Player player, BlockState state, float speed, OmneticData data) {
        for (ItemStackTemplate template : data.items()) {
            speed = Math.max(getBaseSpeed(player, template.create(), state, BlockPos.ZERO), speed);
        }
        return speed;
    }

    /**
     * Upstream {@code applyOmneticData(HarvestCheck, OmneticData)}: true if any of the omnetic tools can harvest the block.
     */
    public static boolean canOmneticHarvest(BlockState state, OmneticData data) {
        for (ItemStackTemplate template : data.items()) {
            if (template.create().isCorrectToolForDrops(state)) {
                return true;
            }
        }
        return false;
    }

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
