package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.AntiGravityArrowBonus;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * Upstream 9.1.0 {@code AdventureEvents#update} ({@code EntityTickEvent.Post}), arrow half: gives gravity back to an
 * anti-gravity arrow once it is spent or has flown too long.
 */
@Mixin(AbstractArrow.class)
public abstract class AntiGravityArrowMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void apoth_antiGravityTick(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!arrow.level().isClientSide() && arrow.hasAttached(Apoth.Attachments.ANTI_GRAVITY_ARROW_START)) {
            AntiGravityArrowBonus.tick(arrow);
        }
    }
}
