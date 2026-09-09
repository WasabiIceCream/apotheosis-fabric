package dev.shadowsoffire.apotheosis.socket.gem.cutting;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.include.com.google.common.base.Preconditions;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingRecipe.CuttingRecipeInput;
import dev.shadowsoffire.apotheosis.util.SizedIngredient;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * Port note (NeoForge -> Fabric): {@code CuttingRecipeInput} is rebacked against Placebo-Fabric's
 * {@link InternalItemHandler}, which (unlike the NeoForge {@code transfer}-API-based original)
 * stores plain {@link ItemStack}s directly — {@code getResource}/{@code getAmountAsInt}/{@code set}
 * become {@code getStackInSlot}/{@code setStackInSlot} on a real {@link ItemStack}.
 */
public interface GemCuttingRecipe extends Recipe<CuttingRecipeInput> {

    /**
     * Reduces the count of the inputs, based on how many items should be consumed.
     * <p>
     * Note, the {@link CuttingRecipeInput#getBase() base item} does not need to
     * be decremented, as it will be replaced.
     *
     * @param input The recipe input that was just matched.
     * @param level The level.
     */
    void decrementInputs(CuttingRecipeInput input, Level level);

    /**
     * Checks if {@code stack} is a valid item to be placed in the base slot.
     */
    boolean isValidBaseItem(CuttingRecipeInput input, ItemStack stack);

    /**
     * Checks if {@code stack} is a valid item to be placed in the top slot.
     */
    boolean isValidTopItem(CuttingRecipeInput input, ItemStack stack);

    /**
     * Checks if {@code stack} is a valid item to be placed in the left slot.
     */
    boolean isValidLeftItem(CuttingRecipeInput input, ItemStack stack);

    /**
     * Checks if {@code stack} is a valid item to be placed in the right slot.
     */
    boolean isValidRightItem(CuttingRecipeInput input, ItemStack stack);

    @Override
    default RecipeType<? extends GemCuttingRecipe> getType() {
        return Apoth.RecipeTypes.GEM_CUTTING;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    default List<RecipeDisplay> display() {
        return List.of();
    }

    @Nullable
    public static SizedIngredient getMatch(ItemStack stack, List<SizedIngredient> ingredients) {
        for (SizedIngredient si : ingredients) {
            if (si.test(stack)) {
                return si;
            }
        }
        return null;
    }

    public static SizedIngredient getMatchOrThrow(ItemStack stack, List<SizedIngredient> ingredients) {
        return Preconditions.checkNotNull(getMatch(stack, ingredients), "Failed to find a match for " + stack);
    }

    public static boolean anyMatch(ItemStack stack, List<SizedIngredient> ingredients) {
        return getMatch(stack, ingredients) != null;
    }

    public static class CuttingRecipeInput implements RecipeInput {

        private final InternalItemHandler inv;

        public CuttingRecipeInput(InternalItemHandler inv) {
            this.inv = inv;
        }

        @Override
        public int size() {
            return this.inv.size();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.inv.getStackInSlot(slot);
        }

        public ItemStack getBase() {
            return this.getItem(GemCuttingMenu.BASE_SLOT);
        }

        public ItemStack getTop() {
            return this.getItem(GemCuttingMenu.TOP_SLOT);
        }

        public ItemStack getLeft() {
            return this.getItem(GemCuttingMenu.LEFT_SLOT);
        }

        public ItemStack getRight() {
            return this.getItem(GemCuttingMenu.RIGHT_SLOT);
        }

        public void shrink(int slot, int amount) {
            this.inv.getStackInSlot(slot).shrink(amount);
        }

    }
}
