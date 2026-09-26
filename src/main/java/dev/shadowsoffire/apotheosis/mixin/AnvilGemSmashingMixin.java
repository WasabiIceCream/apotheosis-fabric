package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Gem smashing: a falling anvil that lands on dropped gems turns them into Gem Dust.
 * <p>
 * Port note (NeoForge -> Fabric): upstream listens to NeoForge's {@code AnvilLandEvent}
 * ({@code AdventureEvents#gemSmashing}), which has no Fabric API equivalent, so this hooks
 * the vanilla landing callback directly. Without it Gem Dust had no source at all, and the
 * Salvaging and Gem Cutting Tables both need it to craft.
 */
@Mixin(AnvilBlock.class)
public class AnvilGemSmashingMixin {

    @Inject(method = "onLand", at = @At("HEAD"))
    private void apoth_smashGems(Level level, BlockPos pos, BlockState state, BlockState replacedBlock, FallingBlockEntity entity, CallbackInfo ci) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos))) {
            ItemStack stack = item.getItem();
            if (stack.is(Apoth.Items.GEM)) {
                item.setItem(new ItemStack(Apoth.Items.GEM_DUST, stack.getCount()));
            }
        }
    }
}
