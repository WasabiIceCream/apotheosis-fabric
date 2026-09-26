package dev.shadowsoffire.apotheosis.util;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Matches any item carrying affixes of one specific rarity. Used by the
 * {@code salvaging/affix_item/*} recipes, the only source of the rarity
 * salvage materials.
 * <p>
 * Port note (NeoForge -> Fabric): upstream is a NeoForge {@code ICustomIngredient}
 * selected in JSON by {@code "neoforge:ingredient_type": "apotheosis:affix"}. This is
 * the same thing as a Fabric {@link CustomIngredient}, selected by
 * {@code "fabric:type": "apotheosis:affix"}; the recipe JSON is converted to match.
 */
public class AffixItemIngredient implements CustomIngredient {

    public static final MapCodec<AffixItemIngredient> CODEC = RarityRegistry.INSTANCE.holderCodec().fieldOf("rarity").xmap(AffixItemIngredient::new, a -> a.rarity);
    public static final StreamCodec<RegistryFriendlyByteBuf, AffixItemIngredient> STREAM_CODEC = RarityRegistry.INSTANCE.holderStreamCodec().<RegistryFriendlyByteBuf>cast().map(AffixItemIngredient::new, a -> a.rarity);

    public static final CustomIngredientSerializer<AffixItemIngredient> SERIALIZER = new CustomIngredientSerializer<>() {
        private final Identifier id = Apotheosis.loc("affix");

        @Override
        public Identifier getIdentifier() {
            return this.id;
        }

        @Override
        public MapCodec<AffixItemIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AffixItemIngredient> getStreamCodec() {
            return STREAM_CODEC;
        }
    };

    protected final DynamicHolder<LootRarity> rarity;

    public AffixItemIngredient(DynamicHolder<LootRarity> rarity) {
        this.rarity = rarity;
    }

    @Override
    public boolean test(ItemStack stack) {
        var rarity = AffixHelper.getRarity(stack);
        return !AffixHelper.getAffixes(stack).isEmpty() && rarity.isBound() && rarity.equals(this.rarity);
    }

    /**
     * Affixes could be on anything, so this has to return every item.
     */
    @Override
    public Stream<Holder<Item>> items() {
        return BuiltInRegistries.ITEM.listElements().filter(i -> i.value() != Items.AIR).map(h -> (Holder<Item>) h);
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public LootRarity getRarity() {
        return this.rarity.get();
    }
}
