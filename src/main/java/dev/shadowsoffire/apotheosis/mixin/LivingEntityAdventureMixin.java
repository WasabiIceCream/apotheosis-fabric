package dev.shadowsoffire.apotheosis.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;

import dev.shadowsoffire.apotheosis.AdventureEvents;
import dev.shadowsoffire.apotheosis.util.DeathDropsCollector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Upstream {@code AdventureEvents} hooks on living entities that have no Fabric API event (see {@link AdventureEvents}):
 * <ul>
 * <li>{@code onDamage} ({@code LivingIncomingDamageEvent}): gem and affix {@code onHurt}. Injected at the same point as
 * Apothic Attributes' incoming damage hook (before blocking and armor); the higher mixin priority puts this callback after
 * Apothic's (projectile damage, crits, dodge), matching upstream's LOW priority against Apothic's HIGHEST/HIGH ones. Not
 * reached when Apothic cancels the hit (a dodge).</li>
 * <li>{@code shieldBlock} ({@code LivingShieldBlockEvent}): gem and affix {@code onShieldBlock} change the blocked amount
 * before the shield takes durability damage, as NeoForge's event does.</li>
 * <li>{@code drops}/{@code dropsLowest}/{@code deathMark}/{@code festive_removeMarker} ({@code LivingDeathEvent} and
 * {@code LivingDropsEvent}): the item entities added to the level while a non-player entity drops its death loot are
 * collected (see {@link DeathDropsCollector}), then handed to {@link AdventureEvents#onEntityDrops} (Festive, Telepathic).
 * Drops from the loot table are recorded separately (Festive copies only those).</li>
 * <li>Upstream {@code LivingEntityMixin}: when a mob's equipment changes its max health, keep its health percentage (mobs
 * don't regenerate, so gear with max health would otherwise leave them wounded). Port note: only when max health actually
 * changed, so the health isn't rewritten every tick.</li>
 * </ul>
 */
@Mixin(value = LivingEntity.class, priority = 1100)
public abstract class LivingEntityAdventureMixin implements DeathDropsCollector {

    @Unique
    @Nullable
    private List<ItemEntity> apoth$deathDrops;

    @Unique
    @Nullable
    private Set<ItemEntity> apoth$lootDrops;

    @Unique
    private boolean apoth$inLootTable;

    @Unique
    @Nullable
    private Set<ItemStack> apoth$lootStacks;

    @Unique
    private float apoth$healthPct = -1;

    @Unique
    private float apoth$maxHealth;

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    private void apoth_onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalFloatRef amount) {
        amount.set(AdventureEvents.onHurt((LivingEntity) (Object) this, source, amount.get()));
    }

    @ModifyExpressionValue(method = "applyItemBlocking", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"))
    private float apoth_onShieldBlock(float blocked, @Local(argsOnly = true) DamageSource source) {
        return AdventureEvents.onShieldBlock((LivingEntity) (Object) this, source, blocked);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void apoth_startCollectingDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (!((Object) this instanceof Player)) {
            this.apoth$deathDrops = new ArrayList<>();
            this.apoth$lootDrops = Collections.newSetFromMap(new IdentityHashMap<>());
            this.apoth$lootStacks = Collections.newSetFromMap(new IdentityHashMap<>());
            DeathDropsCollector.ACTIVE.get().push(this);
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"))
    private void apoth_startLootTable(ServerLevel level, DamageSource source, CallbackInfo ci) {
        this.apoth$inLootTable = true;
    }

    @Inject(method = "dropAllDeathLoot", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"))
    private void apoth_endLootTable(ServerLevel level, DamageSource source, CallbackInfo ci) {
        this.apoth$inLootTable = false;
    }

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void apoth_processDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        List<ItemEntity> drops = this.apoth$deathDrops;
        Set<ItemEntity> loot = this.apoth$lootDrops;
        if (drops != null) {
            DeathDropsCollector.ACTIVE.get().remove(this);
        }
        this.apoth$deathDrops = null;
        this.apoth$lootDrops = null;
        this.apoth$lootStacks = null;
        this.apoth$inLootTable = false;
        if (drops != null && !drops.isEmpty()) {
            AdventureEvents.onEntityDrops((LivingEntity) (Object) this, source, drops, loot);
        }
    }

    @Override
    public void apoth$collectDrop(ItemEntity item) {
        if (this.apoth$deathDrops != null && !this.apoth$deathDrops.contains(item)) {
            this.apoth$deathDrops.add(item);
            if (this.apoth$inLootTable || this.apoth$lootStacks.contains(item.getItem())) {
                this.apoth$lootDrops.add(item);
            }
        }
    }

    @Override
    public void apoth$recordDropStack(ItemStack stack) {
        if (this.apoth$inLootTable && this.apoth$lootStacks != null) {
            this.apoth$lootStacks.add(stack);
        }
    }

    @Inject(method = "detectEquipmentUpdates", at = @At("HEAD"))
    private void apoth_cacheHealthPct(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player) && self.getHealth() > 0 && self.getMaxHealth() > 0) {
            this.apoth$maxHealth = self.getMaxHealth();
            this.apoth$healthPct = self.getHealth() / this.apoth$maxHealth;
        }
        else {
            this.apoth$healthPct = -1;
        }
    }

    @Inject(method = "detectEquipmentUpdates", at = @At("TAIL"))
    private void apoth_keepHealthPct(CallbackInfo ci) {
        if (this.apoth$healthPct >= 0) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self.getMaxHealth() != this.apoth$maxHealth) {
                self.setHealth(self.getMaxHealth() * this.apoth$healthPct);
            }
            this.apoth$healthPct = -1;
        }
    }
}
