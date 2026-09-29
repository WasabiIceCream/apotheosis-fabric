package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

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
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
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
     * Upstream: affix and gem protection bonuses ({@code getDamageProtection} hooks), added to the enchantment protection total.
     */
    @Inject(at = @At("RETURN"), method = "getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F", cancellable = true)
    private static void apoth_getDamageProtection(ServerLevel level, LivingEntity entity, DamageSource source, CallbackInfoReturnable<Float> cir) {
        float prot = cir.getReturnValueF();
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            ItemStack s = entity.getItemBySlot(slot);
            prot += SocketHelper.getGems(s).getDamageProtection(source);

            var affixes = AffixHelper.getAffixes(s);
            for (AffixInstance inst : affixes.values()) {
                prot += inst.getDamageProtection(source);
            }
        }
        cir.setReturnValue(prot);
    }

    /**
     * Upstream: affix and gem flat damage bonuses ({@code getDamageBonus} hooks), added where enchantments modify damage.
     */
    @Inject(at = @At("RETURN"), method = "modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F", cancellable = true)
    private static void apoth_modifyDamage(ServerLevel level, ItemStack tool, Entity entity, DamageSource damageSource, float damage, CallbackInfoReturnable<Float> cir) {
        float dmg = cir.getReturnValueF();

        dmg += SocketHelper.getGems(tool).getDamageBonus(entity);

        var affixes = AffixHelper.getAffixes(tool);
        for (AffixInstance inst : affixes.values()) {
            dmg += inst.getDamageBonus(entity);
        }

        cir.setReturnValue(dmg);
    }

    /**
     * Upstream: the affix and gem on-hit ({@code doPostAttack}, attacker's gear) and when-hurt ({@code doPostHurt}, target's gear)
     * hooks. Port note: these were never wired in this port before 0.3.0, so every on-hit affix and gem effect was inert.
     */
    @Inject(at = @At("TAIL"), method = "doPostAttackEffectsWithItemSource(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;)V")
    private static void apoth_doPostAttackEffectsWithItemSource(ServerLevel level, Entity target, DamageSource damageSource, @Nullable ItemStack itemSource, CallbackInfo ci) {
        if (damageSource.getEntity() instanceof LivingEntity user) {
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                ItemStack s = user.getItemBySlot(slot);
                SocketHelper.getGems(s).doPostAttack(user, target);

                var affixes = AffixHelper.getAffixes(s);
                for (AffixInstance inst : affixes.values()) {
                    int old = target.invulnerableTime;
                    target.invulnerableTime = 0;
                    inst.doPostAttack(user, target);
                    target.invulnerableTime = old;
                }
            }
        }

        if (target instanceof LivingEntity livingTarget) {
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                ItemStack s = livingTarget.getItemBySlot(slot);
                SocketHelper.getGems(s).doPostHurt(livingTarget, damageSource);

                var affixes = AffixHelper.getAffixes(s);
                for (AffixInstance inst : affixes.values()) {
                    inst.doPostHurt(livingTarget, damageSource);
                }
            }
        }
    }

    /**
     * Upstream: durability bonuses from the item's rolled bonus, its gems and its affixes (a chance per point of damage to
     * ignore it, or with a negative bonus to take extra).
     */
    @Inject(at = @At("RETURN"), method = "processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I", cancellable = true)
    private static void apoth_processAffixDurability(ServerLevel level, ItemStack stack, int damage, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() <= 0) {
            return;
        }

        int amount = cir.getReturnValueI();
        double chance = stack.getOrDefault(Apoth.Components.DURABILITY_BONUS, 0F);

        if (stack.has(Apoth.Components.SOCKETED_GEMS)) {
            double socketBonus = SocketHelper.getGems(stack).getDurabilityBonusPercentage().reduce(0, ApothMiscUtil::duraProd);
            chance = ApothMiscUtil.duraProd(chance, socketBonus);
        }

        if (stack.has(Apoth.Components.AFFIXES)) {
            double afxBonus = AffixHelper.streamAffixes(stack).mapToDouble(AffixInstance::getDurabilityBonusPercentage).reduce(0, ApothMiscUtil::duraProd);
            chance = ApothMiscUtil.duraProd(chance, afxBonus);
        }

        int delta = 1;
        int blocked = 0;

        if (chance < 0) {
            delta = -1;
            chance = -chance;
        }

        if (chance > 0) {
            for (int i = 0; i < amount; i++) {
                if (level.getRandom().nextFloat() <= chance) {
                    blocked += delta;
                }
            }
        }

        cir.setReturnValue(amount - blocked);
    }
}
