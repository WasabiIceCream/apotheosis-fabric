package dev.shadowsoffire.apotheosis.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.shadowsoffire.apotheosis.affix.effect.TelepathicAffix;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Upstream {@code TelepathicAffix#drops(BlockDropsEvent)}: the drops of a block broken with a Telepathic tool appear at the
 * breaker with no pickup delay. NeoForge's {@code BlockDropsEvent} covers the drops of
 * {@code Block#dropResources(BlockState, Level, BlockPos, BlockEntity, Entity, ItemStack)}; the target is kept for the
 * duration of that call and applied as each drop is added to the level.
 */
@Mixin(Block.class)
public abstract class BlockTelepathicMixin {

    @Unique
    private static final ThreadLocal<Vec3> APOTH_TELEPATHY_TARGET = new ThreadLocal<>();

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private static void apoth_telepathyStart(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity be, @Nullable Entity breaker, ItemStack tool, CallbackInfo ci) {
        if (!level.isClientSide()) {
            APOTH_TELEPATHY_TARGET.set(TelepathicAffix.blockDropTarget(breaker, tool));
        }
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private static void apoth_telepathyEnd(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity be, @Nullable Entity breaker, ItemStack tool, CallbackInfo ci) {
        APOTH_TELEPATHY_TARGET.remove();
    }

    @WrapOperation(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean apoth_telepathyMove(Level level, Entity entity, Operation<Boolean> original) {
        Vec3 target = APOTH_TELEPATHY_TARGET.get();
        if (target != null && entity instanceof ItemEntity item) {
            item.setPos(target.x, target.y, target.z);
            item.setPickUpDelay(0);
        }
        return original.call(level, entity);
    }
}
