package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Dropped items with a rarity get {@link dev.shadowsoffire.apotheosis.client.AffixItemEffectRenderer}'s tinted shadow
 * instead of the vanilla one. Port of upstream's client {@code EntityRendererMixin} (same injection).
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(at = @At("TAIL"), method = "finalizeRenderState")
    private void apoth_finalizeRenderState(Entity entity, EntityRenderState state, CallbackInfo ci) {
        if (AdventureConfig.enableAffixItemEffects && entity instanceof ItemEntity item && AffixHelper.getRarity(item.getItem()).isBound()) {
            state.shadowRadius = 0F;
        }
    }
}
