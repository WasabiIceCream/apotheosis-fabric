package dev.shadowsoffire.apotheosis.affix.salvaging;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Port note (NeoForge -> Fabric): drops the automation-facing {@code ResourceHandler} wrapper
 * ({@code SalvagingItemHandler}, which also fed a virtual "insert here to salvage" slot 0 for
 * hoppers/pipes) — same call as {@code socket.gem.storage.GemCaseTile}. The direct-use salvage
 * flow ({@code SalvagingMenu#salvageAll}) is unaffected; it writes into {@link #output} directly.
 */
public class SalvagingTableTile extends BlockEntity implements Clearable {

    public SalvagingTableTile(BlockPos pPos, BlockState pBlockState) {
        super(Apoth.Tiles.SALVAGING_TABLE, pPos, pBlockState);
    }

    /**
     * "Real" output inventory, as reflected in the container menu.
     */
    protected final InternalItemHandler output = new InternalItemHandler(6);

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        var list = out.list("output", ItemStack.CODEC);
        for (int i = 0; i < this.output.size(); i++) {
            list.add(this.output.getStackInSlot(i));
        }
    }

    @Override
    public void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        NonNullList<ItemStack> loaded = NonNullList.withSize(this.output.size(), ItemStack.EMPTY);
        in.list("output", ItemStack.CODEC).ifPresent(list -> {
            int i = 0;
            for (ItemStack stack : list) {
                if (i >= loaded.size()) {
                    break;
                }
                loaded.set(i++, stack);
            }
        });
        for (int i = 0; i < loaded.size(); i++) {
            this.output.setStackInSlot(i, loaded.get(i));
        }
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.output.size(); i++) {
            this.output.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

}
