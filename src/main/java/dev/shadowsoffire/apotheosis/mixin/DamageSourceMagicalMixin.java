package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.affix.ProjectileHooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * The Magical affix: damage from an arrow fired by a Magical weapon counts as {@code #c:is_magic} and
 * {@code #minecraft:bypasses_armor}. Upstream adds both tags to the damage source through NeoForge's
 * {@code DamageSourceExtension} in {@code EntityInvulnerabilityCheckEvent}; vanilla damage sources can't gain tags, so
 * this answers the tag check instead.
 */
@Mixin(DamageSource.class)
public abstract class DamageSourceMagicalMixin {

    private static final TagKey<DamageType> APOTH_IS_MAGIC = TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("c", "is_magic"));

    @Shadow
    @Final
    private Entity directEntity;

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void apoth_magicalArrow(TagKey<DamageType> tag, CallbackInfoReturnable<Boolean> cir) {
        if (this.directEntity instanceof AbstractArrow arrow && (tag == DamageTypeTags.BYPASSES_ARMOR || tag.equals(APOTH_IS_MAGIC)) && ProjectileHooks.isMagical(arrow)) {
            cir.setReturnValue(true);
        }
    }
}
