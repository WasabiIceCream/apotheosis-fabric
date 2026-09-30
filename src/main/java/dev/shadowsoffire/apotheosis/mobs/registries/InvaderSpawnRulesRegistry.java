package dev.shadowsoffire.apotheosis.mobs.registries;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.mobs.InvaderSpawnRules;
import dev.shadowsoffire.placebo.dynreg.DynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Per-dimension-type {@link InvaderSpawnRules}, keyed by the dimension type's id.
 * <p>
 * Port note (NeoForge -> Fabric): upstream stores these in a NeoForge data map on the {@code dimension_type} registry
 * ({@code data/apotheosis/data_maps/dimension_type/invader_spawn_rules.json}, one file holding every dimension). Fabric
 * has no data maps, so this port uses a Placebo {@link DynamicRegistry} instead, one file per dimension type, where the
 * file's datapack namespace and path are the dimension type's id:
 *
 * <pre>
 * data/minecraft/apotheosis/invader_spawn_rules/overworld.json          -> minecraft:overworld
 * data/minecraft/apotheosis/invader_spawn_rules/the_nether.json         -> minecraft:the_nether
 * data/twilightforest/apotheosis/invader_spawn_rules/twilight_forest_type.json -> twilightforest:twilight_forest_type
 * </pre>
 *
 * The file body is one upstream data-map value ({@code spawn_chances}, optional {@code cooldown}, {@code surface_type}).
 * A dimension type with no file never spawns invaders, the same as a missing data-map entry upstream. An entry for a
 * dimension type that doesn't exist (a mod that isn't installed) is simply never looked up, so upstream's
 * {@code neoforge:mod_loaded} condition on the Twilight Forest entry isn't needed. Overriding or disabling a dimension
 * works like any other datapack file: ship a file at the same path.
 */
public class InvaderSpawnRulesRegistry extends DynamicRegistry<InvaderSpawnRules> {

    public static final InvaderSpawnRulesRegistry INSTANCE = new InvaderSpawnRulesRegistry();

    public InvaderSpawnRulesRegistry() {
        super(Apotheosis.LOGGER, Apotheosis.loc("invader_spawn_rules"), RegistrySerializer.simple(InvaderSpawnRules.CODEC));
    }

    @Nullable
    public InvaderSpawnRules getRules(ResourceKey<DimensionType> dimType) {
        return this.getValue(dimType.identifier());
    }

}
