package dev.shadowsoffire.apotheosis.util;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.UnsocketedGem;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Matches any valid gem of one purity. Used by the {@code salvaging/gem/*} recipes.
 * <p>
 * Port note (NeoForge -> Fabric): see {@link AffixItemIngredient}. Upstream also takes an
 * optional {@code gems} set to restrict which gems match; none of upstream's generated
 * recipes use it, so this port reads only {@code purity}.
 */
public record GemIngredient(Purity purity) implements CustomIngredient {

    public static final MapCodec<GemIngredient> CODEC = Purity.CODEC.fieldOf("purity").xmap(GemIngredient::new, GemIngredient::purity);
    public static final StreamCodec<RegistryFriendlyByteBuf, GemIngredient> STREAM_CODEC = Purity.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().map(GemIngredient::new, GemIngredient::purity);

    public static final CustomIngredientSerializer<GemIngredient> SERIALIZER = new CustomIngredientSerializer<>() {
        private final Identifier id = Apotheosis.loc("gem");

        @Override
        public Identifier getIdentifier() {
            return this.id;
        }

        @Override
        public MapCodec<GemIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GemIngredient> getStreamCodec() {
            return STREAM_CODEC;
        }
    };

    @Override
    public boolean test(ItemStack stack) {
        UnsocketedGem inst = UnsocketedGem.of(stack);
        return inst.isValid() && inst.purity() == this.purity;
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(Apoth.Items.GEM);
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
