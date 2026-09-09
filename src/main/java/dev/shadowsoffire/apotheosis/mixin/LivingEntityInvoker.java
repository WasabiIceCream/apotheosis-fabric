package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Exposes a few private/protected {@link LivingEntity} methods needed by affix effects:
 * {@code checkTotemDeathProtection}/{@code getDeathSound}/{@code getSoundVolume} let
 * {@code ExecutingAffix} replicate a "real" instant-kill death (respecting totems, playing the
 * correct death sound) rather than just zeroing health; {@code onEffectUpdated} lets
 * {@code MobEffectAffix} fire the same "effect was refreshed" callback vanilla itself uses when
 * stacking a mob effect's duration/amplifier onto an existing instance.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("checkTotemDeathProtection")
    boolean callCheckTotemDeathProtection(DamageSource source);

    @Invoker("getDeathSound")
    SoundEvent callGetDeathSound();

    @Invoker("getSoundVolume")
    float callGetSoundVolume();

    @Invoker("onEffectUpdated")
    void callOnEffectUpdated(MobEffectInstance instance, boolean forced, Entity source);

}
