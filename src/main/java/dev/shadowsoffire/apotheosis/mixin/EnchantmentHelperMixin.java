package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.effect.EnchantmentAffix;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.EnchantmentBonus;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Fabric-native replacement for NeoForge's {@code GetEnchantmentLevelEvent} (no Fabric API
 * equivalent — confirmed during initial research; {@code EnchantmentEvents.MODIFY} only affects
 * enchantment *definitions* at registry-build time, not live per-lookup queries). Injects into
 * the one vanilla method that every enchantment-level query ultimately goes through —
 * confirmed via bytecode inspection that {@code EnchantmentHelper.getEnchantmentLevel(Holder,
 * LivingEntity)} (the per-entity/equipment-scan overload) itself calls
 * {@code getItemEnchantmentLevel} per equipped item, so instrumenting only the single-item
 * overload covers both without double-firing.
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "getItemEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void apoth$addAffixEnchantmentLevels(Holder<Enchantment> ench, ItemInstance item, CallbackInfoReturnable<Integer> cir) {
        if (item instanceof ItemStack stack) {
            int level = cir.getReturnValue();
            for (AffixInstance inst : AffixHelper.streamAffixes(stack).toList()) {
                if (inst.getAffix() instanceof EnchantmentAffix ea) {
                    level = ea.applyBonus(inst, ench, level);
                }
            }
            for (GemInstance inst : SocketHelper.getGems(stack).streamValidGems().toList()) {
                if (inst.getBonus().orElse(null) instanceof EnchantmentBonus eb) {
                    level = eb.applyBonus(inst, ench, level);
                }
            }
            if (level != cir.getReturnValue()) {
                cir.setReturnValue(level);
            }
        }
    }

    /**
     * Implements {@link Apoth.CustomAttributes#PROT_PIERCE} — reduces the defender's aggregate
     * Protection-family enchantment damage reduction by the attacker's {@code prot_pierce}
     * attribute. {@code getDamageProtection} is the single vanilla method that sums Protection
     * points across all worn armor into one value, mirroring how {@code getArmorValue()} is the
     * single aggregate for armor (see {@code CombatRulesMixin}).
     */
    @Inject(method = "getDamageProtection", at = @At("RETURN"), cancellable = true)
    private static void apoth$pierceProtection(ServerLevel level, LivingEntity entity, DamageSource source, CallbackInfoReturnable<Float> cir) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            float pierce = (float) living.getAttributeValue(Apoth.CustomAttributes.PROT_PIERCE);
            if (pierce > 0) {
                cir.setReturnValue(Math.max(0, cir.getReturnValue() - pierce));
            }
        }
    }

}
