package dev.shadowsoffire.apotheosis.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;

/**
 * Calls the protected {@code BaseSpawner#setNextSpawnData} (virtual, so the block entity's override still runs).
 * Same as upstream's accessor.
 */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {

    @Invoker
    void callSetNextSpawnData(@Nullable Level level, BlockPos pos, SpawnData data);

}
