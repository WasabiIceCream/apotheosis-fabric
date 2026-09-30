package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Exposes {@link Entity#readAdditionalSaveData}, which invaders and elites use to re-apply their configured NBT on top
 * of an already-built mob (same as upstream's {@code EntityInvoker}; upstream gets it from a NeoForge access transformer).
 */
@Mixin(Entity.class)
public interface EntityInvoker {

    @Invoker("readAdditionalSaveData")
    void callReadAdditionalSaveData(ValueInput input);

}
