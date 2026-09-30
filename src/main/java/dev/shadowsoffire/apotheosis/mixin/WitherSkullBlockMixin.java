package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import dev.shadowsoffire.apotheosis.mobs.ApothMobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

/**
 * Upstream {@code WitherSkullBlockMixin}: a built Wither goes through the spawn finalization, as for the Ender Dragon (see
 * {@link BossFinalizeSpawnMixin}).
 */
@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlockMixin {

    @Inject(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void apoth_finalizeWithers(Level level, BlockPos pos, SkullBlockEntity be, CallbackInfo ci, @Local WitherBoss wither) {
        if (level instanceof ServerLevel serverLevel) {
            ApothMobEvents.onFinalizeSpawn(serverLevel, wither, EntitySpawnReason.EVENT);
        }
    }
}
