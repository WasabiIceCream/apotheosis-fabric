package dev.shadowsoffire.apotheosis.gen;

import java.util.List;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Adds the rogue spawner and boss dungeon placed features to biomes.
 * <p>
 * Port note (NeoForge -> Fabric): upstream uses six NeoForge biome modifiers of its own {@code apotheosis:blacklist}
 * type ({@code BlacklistModifier}), each adding one placed feature to {@code UNDERGROUND_STRUCTURES} in every biome
 * except a blacklist (the vanilla oceans and the deep dark). Fabric API's {@code BiomeModifications} does the same
 * here, with the blacklist moved into the biome tag {@code #apotheosis:worldgen_blacklist} so a datapack can still
 * change it. As upstream, the features are added to every other biome in every dimension and the dimension check
 * happens when they place ({@code AdventureConfig#canGenerateIn}, overworld only by default).
 */
public final class ApothWorldgen {

    /** The placed features, in upstream's {@code data/apotheosis/worldgen/placed_feature}. */
    public static final List<ResourceKey<PlacedFeature>> PLACED_FEATURES = List.of(
        key("rogue_spawner"), key("rogue_spawner_deep"),
        key("boss_dungeon"), key("boss_dungeon_deep"),
        key("boss_dungeon_2"), key("boss_dungeon_2_deep"));

    private ApothWorldgen() {}

    public static void init() {
        for (ResourceKey<PlacedFeature> feature : PLACED_FEATURES) {
            BiomeModifications.addFeature(BiomeSelectors.all().and(BiomeSelectors.tag(Apoth.Tags.WORLDGEN_BLACKLIST).negate()),
                GenerationStep.Decoration.UNDERGROUND_STRUCTURES, feature);
        }
    }

    private static ResourceKey<PlacedFeature> key(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Apotheosis.loc(path));
    }

}
