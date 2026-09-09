package dev.shadowsoffire.apotheosis.affix;

import javax.annotation.Nullable;

import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.AttributeTooltipContext;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.HitResult;

/**
 * An Affix Instance is a wrapper around the necessary parameters for all affix methods.
 * Prefer using this over directly invoking methods on {@link Affix}.
 *
 * @param affix  The affix this instance is for.
 * @param level  The level of the affix.
 * @param rarity The rarity of the source item.
 * @param stack  The source item stack.
 */
public record AffixInstance(DynamicHolder<Affix> affix, float level, DynamicHolder<LootRarity> rarity, ItemStack stack) {

    public LootCategory category() {
        return LootCategory.forItem(this.stack);
    }

    public boolean isValid() {
        return this.affix.isBound() && this.rarity.isBound();
    }

    /**
     * Resolves the underlying {@link #rarity}. Throws if unbound.
     */
    public LootRarity getRarity() {
        return this.rarity.get();
    }

    /**
     * Resolves the underlying {@link #affix}. Throws if unbound.
     */
    public Affix getAffix() {
        return this.affix.get();
    }

    /**
     * @see Affix#addModifiers(AffixInstance, StackAttributeModifiersEvent)
     */
    public void addModifiers(StackAttributeModifiersEvent event) {
        this.getAffix().addModifiers(this, event);
    }

    /**
     * @see Affix#getDescription(AffixInstance, AttributeTooltipContext)
     */
    public MutableComponent getDescription(AttributeTooltipContext ctx) {
        return this.getAffix().getDescription(this, ctx);
    }

    /**
     * @see Affix#getAugmentingText(AffixInstance, AttributeTooltipContext)
     */
    public Component getAugmentingText(AttributeTooltipContext ctx) {
        return this.getAffix().getAugmentingText(this, ctx);
    }

    /**
     * @see Affix#getName(boolean)
     */
    public Component getName(boolean prefix) {
        return this.getAffix().getName(prefix);
    }

    /**
     * @see Affix#getDamageProtection(AffixInstance, DamageSource)
     */
    public float getDamageProtection(DamageSource source) {
        return this.getAffix().getDamageProtection(this, source);
    }

    /**
     * @see Affix#getDamageBonus(AffixInstance, Entity)
     */
    public float getDamageBonus(Entity entity) {
        return this.getAffix().getDamageBonus(this, entity);
    }

    /**
     * @see Affix#doPostAttack(AffixInstance, LivingEntity, Entity)
     */
    public void doPostAttack(LivingEntity user, Entity target) {
        this.getAffix().doPostAttack(this, user, target);
    }

    /**
     * @see Affix#doPostHurt(AffixInstance, LivingEntity, DamageSource)
     */
    public void doPostHurt(LivingEntity user, DamageSource source) {
        this.getAffix().doPostHurt(this, user, source);
    }

    /**
     * @see Affix#onProjectileFired(AffixInstance, LivingEntity, Projectile)
     */
    public void onProjectileFired(LivingEntity user, Projectile proj) {
        this.getAffix().onProjectileFired(this, user, proj);
    }

    /**
     * @see Affix#onItemUse(AffixInstance, UseOnContext)
     */
    @Nullable
    public InteractionResult onItemUse(UseOnContext ctx) {
        return this.getAffix().onItemUse(this, ctx);
    }

    /**
     * @see Affix#onShieldBlock(AffixInstance, LivingEntity, DamageSource, float)
     */
    public float onShieldBlock(LivingEntity entity, DamageSource source, float amount) {
        return this.getAffix().onShieldBlock(this, entity, source, amount);
    }

    /**
     * @see Affix#onBlockBreak(AffixInstance, Player, LevelAccessor, BlockPos, BlockState)
     */
    public void onBlockBreak(Player player, LevelAccessor world, BlockPos pos, BlockState state) {
        this.getAffix().onBlockBreak(this, player, world, pos, state);
    }

    /**
     * @see Affix#getDurabilityBonusPercentage(AffixInstance)
     */
    public float getDurabilityBonusPercentage() {
        return this.getAffix().getDurabilityBonusPercentage(this);
    }

    /**
     * @see Affix#onProjectileImpact(float, LootRarity, Projectile, HitResult, HitResult.Type)
     */
    public void onProjectileImpact(Projectile proj, HitResult res, HitResult.Type type) {
        this.getAffix().onProjectileImpact(this.level, this.getRarity(), proj, res, type);
    }

    /**
     * @see Affix#enablesTelepathy()
     */
    public boolean enablesTelepathy() {
        return this.getAffix().enablesTelepathy();
    }

    /**
     * @see Affix#onHurt(AffixInstance, DamageSource, LivingEntity, float)
     */
    public float onHurt(DamageSource src, LivingEntity ent, float amount) {
        return this.getAffix().onHurt(this, src, ent, amount);
    }

    /**
     * @see Affix#modifyLoot(AffixInstance, ObjectArrayList, LootContext)
     */
    public void modifyLoot(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        this.getAffix().modifyLoot(this, loot, ctx);
    }

    /**
     * @see Affix#isLevelIndependent(AffixInstance)
     */
    public boolean isLevelIndependent() {
        return this.getAffix().isLevelIndependent(this);
    }

    public AffixInstance withNewLevel(float level) {
        return new AffixInstance(this.affix, Mth.clamp(level, 0, Affix.MAX_LEVEL), this.rarity, this.stack);
    }

    public Identifier makeUniqueId(String salt) {
        return Affix.makeUniqueId(this, salt);
    }

    public Identifier makeUniqueId() {
        return Affix.makeUniqueId(this);
    }
}
