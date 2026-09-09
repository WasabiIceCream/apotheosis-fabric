package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.world.entity.player.Player;

/**
 * Implements {@link Apoth.CustomAttributes#EXPERIENCE_GAINED} — multiplies XP awarded to a player by
 * {@code (1 + attributeValue)}. {@code Player#giveExperiencePoints} is the single funnel every
 * source of player XP (orb pickup, commands, etc.) goes through.
 */
@Mixin(Player.class)
public class PlayerExperienceMixin {

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int apoth$multiplyExperience(int amount) {
        double bonus = ((Player) (Object) this).getAttributeValue(Apoth.CustomAttributes.EXPERIENCE_GAINED);
        if (bonus > 0) {
            return Math.round(amount * (1F + (float) bonus));
        }
        return amount;
    }

}
