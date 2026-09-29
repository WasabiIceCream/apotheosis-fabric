package dev.shadowsoffire.apotheosis.client;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * {@code "property": "apotheosis:gem"} for {@code minecraft:select} item models: the id of the gem on the stack.
 * <p>
 * Port note: upstream dispatches per-gem models with its own item model type backed by NeoForge standalone models;
 * vanilla's select model with this property does the same with data only (see {@code assets/apotheosis/items/gem.json}).
 */
public record GemSelectProperty() implements SelectItemModelProperty<Identifier> {

    public static final SelectItemModelProperty.Type<GemSelectProperty, Identifier> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new GemSelectProperty()), Identifier.CODEC);

    @Override
    @Nullable
    public Identifier get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        DynamicHolder<Gem> gem = stack.get(Apoth.Components.GEM);
        return gem == null ? null : gem.getId();
    }

    @Override
    public Codec<Identifier> valueCodec() {
        return Identifier.CODEC;
    }

    @Override
    public SelectItemModelProperty.Type<GemSelectProperty, Identifier> type() {
        return TYPE;
    }
}
