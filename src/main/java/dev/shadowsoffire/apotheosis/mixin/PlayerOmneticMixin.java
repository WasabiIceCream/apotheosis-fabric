package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.AdventureEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Upstream {@code AdventureEvents#speed} ({@code PlayerEvent.BreakSpeed}, fired at the end of {@code Player#getDigSpeed}) and
 * {@code #harvest} ({@code PlayerEvent.HarvestCheck}, fired from {@code Player#hasCorrectToolForDrops}): the Omnetic affix and
 * gem. Both sides run these (the client predicts mining speed), as on NeoForge.
 */
@Mixin(Player.class)
public abstract class PlayerOmneticMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void apoth_omneticSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        float speed = AdventureEvents.omneticSpeed((Player) (Object) this, state, cir.getReturnValueF());
        if (speed != cir.getReturnValueF()) {
            cir.setReturnValue(speed);
        }
    }

    @Inject(method = "hasCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
    private void apoth_omneticHarvest(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && AdventureEvents.omneticHarvest((Player) (Object) this, state)) {
            cir.setReturnValue(true);
        }
    }
}
