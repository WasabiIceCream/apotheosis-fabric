package dev.shadowsoffire.apotheosis.socket.gem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.TieredWeights;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.shadowsoffire.placebo.color.GradientColor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedRandom;

/**
 * Purity represents a fixed set of gem tiers. Gems are expected to have increasingly powerful stats with each purity level.
 * <p>
 * Port note: {@link #PERFECT} used to store {@code GradientColor.RAINBOW} directly as its
 * {@code TextColor} (the original {@code GradientColor} subclassed {@code TextColor} to make
 * {@code getValue()} dynamic). Since {@code GradientColor} can no longer subclass
 * {@code TextColor} at all in this vanilla version (see its own port note), {@link #getColor}
 * now resolves the gradient fresh on every call for {@code PERFECT} instead of holding one
 * static instance.
 */
public enum Purity implements StringRepresentable, TieredWeights.Weighted {
    CRACKED("cracked", 0x808080),
    CHIPPED("chipped", 0x33FF33),
    FLAWED("flawed", 0x5555FF),
    NORMAL("normal", 0xBB00BB),
    FLAWLESS("flawless", 0xED7014),
    PERFECT("perfect", GradientColor.RAINBOW);

    public static final IntFunction<Purity> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final Codec<Purity> CODEC = StringRepresentable.fromValues(Purity::values);
    public static final StreamCodec<ByteBuf, Purity> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    public static final Set<Purity> ALL_PURITIES = ApothMiscUtil.linkedSet(CRACKED, CHIPPED, FLAWED, NORMAL, FLAWLESS, PERFECT);

    private final String name;
    @Nullable
    private final TextColor color;
    @Nullable
    private final GradientColor gradient;

    private Purity(String name, int color) {
        this.name = name;
        this.color = TextColor.fromRgb(color);
        this.gradient = null;
    }

    private Purity(String name, GradientColor gradient) {
        this.name = name;
        this.color = null;
        this.gradient = gradient;
    }

    public String getName() {
        return this.name;
    }

    public TextColor getColor() {
        return this.gradient != null ? this.gradient.resolve() : this.color;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public TieredWeights weights() {
        return PurityWeightsRegistry.getWeights().get(this);
    }

    public Purity next() {
        return this == PERFECT ? this : BY_ID.apply(this.ordinal() + 1);
    }

    public boolean isAtLeast(Purity other) {
        return this.ordinal() >= other.ordinal();
    }

    public MutableComponent toComponent() {
        return Apotheosis.lang("purity", this.getSerializedName()).withStyle(Style.EMPTY.withColor(this.getColor()));
    }

    public static Purity max(Purity p1, Purity p2) {
        return BY_ID.apply(Math.max(p1.ordinal(), p2.ordinal()));
    }

    public static Purity random(GenContext ctx) {
        return random(ctx, ALL_PURITIES);
    }

    /**
     * Returns a random purity from the given pool, or from all purities if the pool is empty.
     * <p>
     * If the effective weights of all given purities are zero, a random purity is selected from the pool uniformly.
     */
    public static Purity random(GenContext ctx, Set<Purity> pool) {
        if (pool.isEmpty()) {
            pool = ALL_PURITIES;
        }

        List<Weighted<Purity>> list = pool.stream().mapMulti(TieredWeights.wrapFilter(ctx)).toList();
        return WeightedRandom.getRandomItem(ctx.rand(), list, Weighted::weight).map(Weighted::value).orElse(ApothMiscUtil.getRandomElement(pool, ctx.rand()));
    }

    public static <T> MapCodec<Map<Purity, T>> mapCodec(Codec<T> elementCodec) {
        return Codec.simpleMap(Purity.CODEC, elementCodec,
            Keyable.forStrings(() -> Arrays.stream(Purity.values()).map(StringRepresentable::getSerializedName)));
    }
}
