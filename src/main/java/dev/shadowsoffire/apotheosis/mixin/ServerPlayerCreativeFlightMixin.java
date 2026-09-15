package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.server.level.ServerPlayer;

/**
 * Restores the actual behavior of {@code apotheosis:creative_flight} (see
 * {@link Apoth.CustomAttributes#CREATIVE_FLIGHT}'s javadoc for why this attribute needed
 * re-registering at all): NeoForge's real {@code neoforge:creative_flight} attribute works because
 * NeoForge itself patches player-ability-tick code to read it and toggle flight. Fabric has no such
 * hook, so this mixin is that hook — mirroring vanilla's own {@code ServerPlayer#updatePlayerAttributes()}
 * pattern (which reads {@code Attributes.BLOCK_INTERACTION_RANGE} every tick to toggle a
 * creative-only reach bonus) but for this attribute instead.
 * <p>
 * Only ever acts on non-creative, non-spectator players (those game modes already grant real flight
 * through a completely different path, and this mixin must never interfere with that), and only ever
 * revokes a grant it made itself ({@link #apoth$grantedFlight}) — so an admin's own fly permission
 * grant (e.g. via a LuckPerms-driven `/fly`) is never touched by a player equipping or unequipping
 * the affixed item.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerCreativeFlightMixin {

    @Unique
    private boolean apoth$grantedFlight = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void apoth$updateCreativeFlight(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.isCreative() || self.isSpectator()) {
            return;
        }

        boolean shouldFly = self.getAttributeValue(Apoth.CustomAttributes.CREATIVE_FLIGHT) >= 0.5;
        if (shouldFly && !self.getAbilities().mayfly) {
            self.getAbilities().mayfly = true;
            this.apoth$grantedFlight = true;
            self.onUpdateAbilities();
        }
        else if (!shouldFly && this.apoth$grantedFlight) {
            self.getAbilities().mayfly = false;
            self.getAbilities().flying = false;
            this.apoth$grantedFlight = false;
            self.onUpdateAbilities();
        }
    }

}
