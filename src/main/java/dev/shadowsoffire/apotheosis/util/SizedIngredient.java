package dev.shadowsoffire.apotheosis.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Port note (NeoForge -> Fabric): self-contained reimplementation of NeoForge's
 * {@code net.neoforged.neoforge.common.crafting.SizedIngredient} — a plain vanilla-typed
 * {@code Ingredient} paired with a required count, with no NeoForge-specific behavior. Per the
 * plan's research this has no Fabric equivalent but needs none — it's portable as-is.
 *
 * @param ingredient The item-matching predicate.
 * @param count      The number of items of a matching type that must be present.
 */
public record SizedIngredient(Ingredient ingredient, int count) {

    public static final Codec<SizedIngredient> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
            Codec.intRange(1, 99).fieldOf("count").forGetter(SizedIngredient::count))
        .apply(inst, SizedIngredient::new));

    /**
     * Codec form used when a bare ingredient (no explicit count) should default to a count of 1,
     * matching NeoForge's {@code NESTED_CODEC} semantics.
     */
    public static final Codec<SizedIngredient> NESTED_CODEC = Codec.either(CODEC, Ingredient.CODEC)
        .xmap(
            either -> either.map(sized -> sized, ing -> new SizedIngredient(ing, 1)),
            sized -> sized.count == 1 ? com.mojang.datafixers.util.Either.right(sized.ingredient) : com.mojang.datafixers.util.Either.left(sized));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
        ByteBufCodecs.VAR_INT, SizedIngredient::count,
        SizedIngredient::new);

    public SizedIngredient(Ingredient ingredient) {
        this(ingredient, 1);
    }

    /**
     * Checks if the given stack matches this ingredient and has at least {@link #count} items.
     */
    public boolean test(ItemStack stack) {
        return this.ingredient.test(stack) && stack.getCount() >= this.count;
    }

    /**
     * Returns copies of every item that satisfies {@link #ingredient}, each with its count set to {@link #count}.
     */
    public ItemStack[] getItems() {
        return this.ingredient.items()
            .map(h -> h.value().getDefaultInstance().copyWithCount(this.count))
            .toArray(ItemStack[]::new);
    }

    public static SizedIngredient of(Ingredient ingredient, int count) {
        return new SizedIngredient(ingredient, count);
    }

    public static SizedIngredient single(Ingredient ingredient) {
        return new SizedIngredient(ingredient, 1);
    }

    /**
     * Utility used by callers migrating from vanilla {@code NonNullList<Ingredient>} shaping.
     */
    public static NonNullList<SizedIngredient> wrapAll(NonNullList<Ingredient> ingredients) {
        NonNullList<SizedIngredient> out = NonNullList.createWithCapacity(ingredients.size());
        for (Ingredient ing : ingredients) {
            out.add(new SizedIngredient(ing));
        }
        return out;
    }

}
