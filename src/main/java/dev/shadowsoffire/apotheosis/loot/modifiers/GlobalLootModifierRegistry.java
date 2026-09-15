package dev.shadowsoffire.apotheosis.loot.modifiers;

import java.util.Comparator;
import java.util.List;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.placebo.dynreg.DynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;

/**
 * Datapack-driven registry of {@link GlobalLootModifier}s, loaded from
 * {@code data/<ns>/loot_modifier/*.json}. See {@link GlobalLootModifier}'s javadoc for why this
 * exists (no Fabric equivalent of NeoForge's {@code IGlobalLootModifier} system) and how it's
 * actually applied ({@code mixin.LootTableGlobalModifiersMixin}).
 */
public class GlobalLootModifierRegistry extends DynamicRegistry<GlobalLootModifier> {

    // Port note: SERIALIZER must be declared (and thus statically initialized) before INSTANCE —
    // Java runs static field initializers in textual declaration order, and the constructor
    // invoked by INSTANCE's initializer reads SERIALIZER. With the fields in the other order,
    // SERIALIZER is still null (its own initializer hasn't run yet) when the constructor runs,
    // causing a NullPointerException inside DynamicRegistry's constructor at class-load time.
    // Caught by actually launching a dev server — this is a real bug, not a hypothetical.
    private static final RegistrySerializer<GlobalLootModifier> SERIALIZER = buildSerializer();

    public static final GlobalLootModifierRegistry INSTANCE = new GlobalLootModifierRegistry();

    private GlobalLootModifierRegistry() {
        super(Apotheosis.LOGGER, Apotheosis.loc("loot_modifier"), SERIALIZER);
    }

    private static RegistrySerializer<GlobalLootModifier> buildSerializer() {
        // Port note: these type keys were fixed to match upstream Apotheosis's real datapack
        // content exactly (checked against the actual 26.1-branch loot_modifiers/*.json files) —
        // they previously used ad-hoc names ("affix", "affix_convert", "affix_hook", "gem") that
        // don't match what any real datapack (including upstream's own) actually ships.
        var serializer = dev.shadowsoffire.placebo.dynreg.RegistrySerializer.<GlobalLootModifier>subtyped("Global Loot Modifier");
        serializer.register(Apotheosis.loc("affix_loot"), dev.shadowsoffire.apotheosis.loot.modifiers.AffixLootModifier.CODEC.codec());
        serializer.register(Apotheosis.loc("affix_conversion"), dev.shadowsoffire.apotheosis.loot.modifiers.AffixConvertLootModifier.CODEC.codec());
        serializer.register(Apotheosis.loc("code_hook"), dev.shadowsoffire.apotheosis.loot.modifiers.AffixHookLootModifier.CODEC.codec());
        serializer.register(Apotheosis.loc("gems"), dev.shadowsoffire.apotheosis.loot.modifiers.GemLootModifier.CODEC.codec());
        // Server-specific, not upstream Apotheosis content — see RandomEnchantLootModifier's javadoc.
        serializer.register(Apotheosis.loc("random_enchant"), dev.shadowsoffire.apotheosis.loot.modifiers.RandomEnchantLootModifier.CODEC.codec());
        return serializer;
    }

    /**
     * Returns all currently-registered modifiers, ordered by descending priority (matching
     * NeoForge's own GlobalLootModifier ordering convention — higher priority runs first).
     */
    public List<GlobalLootModifier> getOrdered() {
        return this.getValues().stream().sorted(Comparator.comparingInt(GlobalLootModifier::priority).reversed()).toList();
    }

}
