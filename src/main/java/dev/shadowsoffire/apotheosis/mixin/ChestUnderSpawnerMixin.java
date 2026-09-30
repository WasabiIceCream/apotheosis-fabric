package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;

/**
 * A chest with a spawner on top can be opened.
 * <p>
 * Port addition: rogue spawners place their loot chest directly under the spawner, and a solid block above a chest
 * stops it opening. Upstream expects the spawner to be broken first, but a spawner can be unbreakable (Multiplayer
 * Spawners only disables it), which left the chest's loot out of reach. Vanilla never puts a chest under a spawner.
 */
@Mixin(ChestBlock.class)
public class ChestUnderSpawnerMixin {

    @Inject(method = "isBlockedChestByBlock", at = @At("HEAD"), cancellable = true)
    private static void apoth_openUnderSpawner(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBlockState(pos.above()).is(Blocks.SPAWNER)) {
            cir.setReturnValue(false);
        }
    }
}
