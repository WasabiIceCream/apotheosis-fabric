package dev.shadowsoffire.apotheosis.util;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * Port note (NeoForge -> Fabric): upstream's {@code BASE_PLACEHOLDER} is a NeoForge
 * {@code ICustomIngredient} ({@code AffixItemIngredient}) whose {@code items()} deliberately
 * returns every enabled item, purely so the smithing table UI highlights the base slot as
 * accepting anything — the real match logic always lives in each recipe's own
 * {@code matches(SmithingRecipeInput, Level)} override, never in the placeholder's {@code test()}.
 * Reproducing that "matches everything" behavior needs no custom ingredient machinery at all: a
 * plain vanilla {@link Ingredient} built from every non-air item in {@link BuiltInRegistries#ITEM}
 * does the same job.
 */
public abstract class ApothSmithingRecipe implements SmithingRecipe {

    public static final int TEMPLATE = 0, BASE = 1, ADDITION = 2;

    /**
     * Placeholder ingredient for recipes where the base match is handled by a custom {@link #matches(SmithingRecipeInput, Level)} override.
     * 26.1 forbids empty Ingredients, so subclasses that historically passed {@code Ingredient.of()} pass this instead.
     */
    public static final Ingredient BASE_PLACEHOLDER = Ingredient.of(BuiltInRegistries.ITEM.listElements().map(net.minecraft.core.Holder::value).filter(i -> i != Items.AIR));

    protected final Ingredient base;
    protected final Ingredient addition;
    protected final ItemStack result;

    public ApothSmithingRecipe(Ingredient pBase, Ingredient pAddition, ItemStack pResult) {
        this.base = pBase;
        this.addition = pAddition;
        this.result = pResult;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    /**
     * Port note (NeoForge -> Fabric): upstream returns {@link PlacementInfo#NOT_PLACEABLE} here
     * unconditionally, same as we did until this was traced down live. That's harmless on
     * NeoForge, whose {@code SmithingMenu} doesn't gate manual slot placement through
     * {@code PlacementInfo}/{@code RecipePropertySet} the way vanilla's does. On vanilla (and
     * thus Fabric), {@code SmithingMenu.createInputSlotDefinitions} builds each of the three
     * input slots' {@code mayPlace} predicate from {@code RecipePropertySet.SMITHING_BASE}/
     * {@code _TEMPLATE}/{@code _ADDITION} — sets populated by scanning every loaded recipe's own
     * {@code placementInfo()}. A recipe that reports {@code NOT_PLACEABLE} contributes nothing to
     * those sets, so any item that isn't already valid for some *other*, unrelated smithing
     * recipe can never even be placed in the slot — found live by discovering a gem couldn't be
     * placed in the addition slot at all (no exception, no feedback, just a rejected placement),
     * even though the recipe's own {@link #matches(SmithingRecipeInput, Level)} would have
     * accepted it. Returning a real {@link PlacementInfo} — built the same way vanilla's own
     * {@code SmithingTransformRecipe} builds one, straight from this recipe's own ingredients —
     * fixes it for every subclass (gems, sigils, whatever a data-driven {@code AddSocketsRecipe}
     * input ends up being) without hardcoding any item list.
     */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.createFromOptionals(List.of(this.templateIngredient(), Optional.of(this.baseIngredient()), this.additionIngredient()));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.empty();
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(this.addition);
    }

}
