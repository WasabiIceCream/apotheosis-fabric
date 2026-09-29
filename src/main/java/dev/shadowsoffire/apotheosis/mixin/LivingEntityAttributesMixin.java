package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * Retrofits {@link Apoth.CustomAttributes#CREATIVE_FLIGHT} onto every {@link LivingEntity}
 * (the other stand-ins are now Apothic Attributes' own and stay registered only so saved data still loads)
 * type (players and mobs alike), since Fabric API has no event for adding a default attribute to
 * existing entity types after the fact (NeoForge's {@code EntityAttributeModificationEvent} has
 * no Fabric equivalent). {@code Player#createAttributes} and {@code Mob#createMobAttributes} both
 * build on top of {@code LivingEntity#createLivingAttributes}, so injecting here covers both
 * (and every mob type, including modded ones) in one place.
 */
@Mixin(LivingEntity.class)
public class LivingEntityAttributesMixin {

    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void apoth$addCustomAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue()
            .add(Apoth.CustomAttributes.CREATIVE_FLIGHT);
    }

}
