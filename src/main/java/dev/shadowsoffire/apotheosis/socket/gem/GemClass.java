package dev.shadowsoffire.apotheosis.socket.gem;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.Apoth.BuiltInRegs;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

/**
 * A Gem Class is the set of types of items it may be applied to.
 * This comes in the form of a named group of LootCategories.
 */
public record GemClass(String key, HolderSet<LootCategory> types) {

    // Port note: found by actually launching a dev server. Upstream's field used vanilla
    // RegistryCodecs.homogeneousList, which needs RegistryOps (a HolderGetter) to resolve tag or
    // direct-list references — but Placebo's DynamicRegistry decodes datapack JSON with plain
    // JsonOps.INSTANCE (see its javadoc: this is a bespoke Fabric-side reload-listener registry,
    // not a real vanilla dynamic registry, so it never gets RegistryOps context). Against JsonOps,
    // homogeneousList silently failed to resolve any "types" list with more than one entry, which
    // in turn made every multi-type GemBonus fail to decode, which in turn made every Gem with
    // such a bonus decode to an empty bonus list and fail its own "no bonuses" precondition check —
    // i.e. this one codec bug was why 100% of gems failed to load. Fixed with a plain
    // holderByNameCodec() list, which only needs a name->value registry lookup, not RegistryOps.
    // Port note: found live — upstream's real Apotheosis lets a gem bonus's "types" field be
    // NeoForge's `{"type": "neoforge:any"}` wildcard holder-set (part of a small family of
    // any/all/and/or/not combinators NeoForge adds on top of vanilla's holder-set codec), meaning
    // "every LootCategory, don't make me list them" — e.g. `the_end/endersurge`'s Sharpness bonus
    // applies to any socketed item. Vanilla/Fabric's holder-set system has no such wildcard, so
    // there's nothing to port the combinator itself to — but since `BuiltInRegs.LOOT_CATEGORY` is a
    // registry this port owns outright (not a real dynamic registry), "every value in it" is just a
    // concrete, enumerable list, so the wildcard can be special-cased directly instead: decode
    // `{"type": "neoforge:any"}` as shorthand for a `HolderSet` of every registered `LootCategory`.
    // Only "neoforge:any" is recognized; NeoForge's other combinators (all/and/or/not) aren't used by
    // any ported content and error out with an explicit "unsupported" message instead of silently
    // matching wrong. Encode direction is decode-only-safe: nothing in this port ever encodes a
    // `GemClass`, so the wildcard shape doesn't round-trip (it always encodes as an explicit list).
    private static final Codec<HolderSet<LootCategory>> WILDCARD_TYPES_CODEC = RecordCodecBuilder.<String>create(
        inst -> inst.group(Codec.STRING.fieldOf("type").forGetter(Function.identity())).apply(inst, Function.identity()))
        .comapFlatMap(
            type -> "neoforge:any".equals(type)
                ? DataResult.success(allLootCategories())
                : DataResult.<HolderSet<LootCategory>>error(() -> "Unsupported gem class wildcard type: " + type),
            types -> "neoforge:any");

    private static final Codec<HolderSet<LootCategory>> EXPLICIT_LIST_TYPES_CODEC = BuiltInRegs.LOOT_CATEGORY.holderByNameCodec().listOf()
        .xmap(list -> (HolderSet<LootCategory>) HolderSet.direct(list), holderSet -> holderSet.stream().toList());

    private static final Codec<HolderSet<LootCategory>> TYPES_CODEC = Codec
        .either(WILDCARD_TYPES_CODEC, EXPLICIT_LIST_TYPES_CODEC)
        .xmap(either -> either.map(Function.identity(), Function.identity()), Either::right);

    private static HolderSet<LootCategory> allLootCategories() {
        List<Holder<LootCategory>> all = BuiltInRegs.LOOT_CATEGORY.listElements().map(h -> (Holder<LootCategory>) h).toList();
        return HolderSet.direct(all);
    }

    public static Codec<GemClass> EXPLICIT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.fieldOf("key").forGetter(GemClass::key),
        TYPES_CODEC.fieldOf("types").forGetter(GemClass::types))
        .apply(inst, GemClass::new));

    public static Codec<GemClass> CODEC = Codec.either(EXPLICIT_CODEC, LootCategory.CODEC)
        .xmap(e -> e.map(Function.identity(), GemClass::new), GemClass::toEither);

    public GemClass(LootCategory category) {
        this(category.getKey().getPath(), category);
    }

    public GemClass(String key, LootCategory... types) {
        this(key, HolderSet.direct(Arrays.stream(types).map(BuiltInRegs.LOOT_CATEGORY::wrapAsHolder).toList()));
    }

    public GemClass(String key, HolderSet<LootCategory> types) {
        this.key = key;
        this.types = types;
        Preconditions.checkArgument(!Strings.isNullOrEmpty(this.key), "Invalid GemClass with null key");
        Preconditions.checkArgument(this.types != null && this.types.size() > 0, "Invalid GemClass with null or empty types");
    }

    private static Either<GemClass, LootCategory> toEither(GemClass gc) {
        if (gc.types.size() == 1) {
            return Either.right(gc.types.iterator().next().value());
        }
        return Either.left(gc);
    }
}
