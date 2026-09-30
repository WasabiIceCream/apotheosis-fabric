package dev.shadowsoffire.apotheosis.affix.effect;

import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.LivingDrops;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Teleport Drops
 * <p>
 * Port note (NeoForge -> Fabric): upstream's two {@code drops} hooks run on NeoForge's {@code LivingDropsEvent} and
 * {@code BlockDropsEvent}. Here {@link #drops(LivingDrops)} is called by {@code AdventureEvents} after a mob's death drops
 * are collected ({@code mixin.LivingEntityDropsMixin}), and block drops are moved by {@code mixin.BlockTelepathicMixin}
 * through {@link #blockDropTarget}.
 */
public class TelepathicAffix extends Affix {

    public static final Codec<TelepathicAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            affixDef(),
            PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
        .apply(inst, TelepathicAffix::new));


    protected Set<LootRarity> rarities;

    public TelepathicAffix(AffixDefinition def, Set<LootRarity> rarities) {
        super(def);
        this.rarities = rarities;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (cat.isRanged() || cat.isMelee() || cat.isBreaker()) && this.rarities.contains(rarity);
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        LootCategory cat = LootCategory.forItem(inst.stack());
        String type = cat.isRanged() || cat.isMelee() ? "weapon" : "tool";
        return Component.translatable("affix." + this.id() + ".desc." + type);
    }

    @Override
    public boolean enablesTelepathy() {
        return true;
    }

    /**
     * Upstream {@code drops(LivingDropsEvent)} (lowest priority): moves every drop to the killer when the weapon (or the arrow,
     * which carries its bow's affixes) has telepathy.
     */
    public static void drops(LivingDrops e) {
        DamageSource src = e.source();
        boolean canTeleport = false;
        Vec3 targetPos = null;
        if (src.getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() != null) {
            canTeleport = AffixHelper.streamAffixes(arrow).anyMatch(AffixInstance::enablesTelepathy);
            targetPos = arrow.getOwner().position();
        }
        else if (src.getDirectEntity() instanceof LivingEntity living) {
            ItemStack weapon = living.getMainHandItem();
            canTeleport = AffixHelper.streamAffixes(weapon).anyMatch(AffixInstance::enablesTelepathy);
            targetPos = living.position();
        }

        if (canTeleport && !targetPos.equals(Vec3.ZERO)) {
            for (ItemEntity item : e.drops()) {
                item.setPos(targetPos.x, targetPos.y, targetPos.z);
                item.setPickUpDelay(0);
            }
        }
    }

    /**
     * Upstream {@code drops(BlockDropsEvent)}: where the drops of a block broken by {@code breaker} go, or null to leave them.
     */
    @org.jetbrains.annotations.Nullable
    public static Vec3 blockDropTarget(@org.jetbrains.annotations.Nullable Entity breaker, ItemStack tool) {
        if (breaker instanceof LivingEntity living && !living.position().equals(Vec3.ZERO)) {
            if (AffixHelper.streamAffixes(living.getMainHandItem()).anyMatch(AffixInstance::enablesTelepathy)) {
                return living.position();
            }
        }
        return null;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean isLevelIndependent(AffixInstance inst) {
        return true;
    }

}
