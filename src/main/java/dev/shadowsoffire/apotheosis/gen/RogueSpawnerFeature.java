package dev.shadowsoffire.apotheosis.gen;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.spawner.RogueSpawner;
import dev.shadowsoffire.apotheosis.spawner.RogueSpawnerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public class RogueSpawnerFeature extends Feature<SuccessChanceFeatureConfig> {

    public static final RuleTest STONE_TEST = new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD);

    public RogueSpawnerFeature() {
        super(SuccessChanceFeatureConfig.CODEC);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean place(FeaturePlaceContext<SuccessChanceFeatureConfig> ctx) {
        WorldGenLevel world = ctx.level();
        BlockPos pos = ctx.origin();
        RandomSource rand = ctx.random();
        if (!AdventureConfig.canGenerateIn(world) || rand.nextFloat() > ctx.config().successChance()) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        BlockState downState = world.getBlockState(pos.below());
        BlockState upState = world.getBlockState(pos.above());
        if (STONE_TEST.test(downState, rand) && upState.isAir() && (state.isAir() || STONE_TEST.test(state, rand))) {
            RogueSpawner item = RogueSpawnerRegistry.INSTANCE.getRandomItem(rand);
            if (item == null) {
                return false;
            }
            // Port addition: an exception thrown from a feature leaves the chunk's generation future incomplete, and the
            // server thread then waits on it until the watchdog kills the server (seen once while testing this port).
            // Rogue spawners are data-driven, so a bad preset is logged and skipped instead.
            try {
                item.place(world, pos, rand);
            }
            catch (Exception ex) {
                Apotheosis.LOGGER.error("Failed to place rogue spawner {} at {}", RogueSpawnerRegistry.INSTANCE.getKey(item), pos, ex);
                return false;
            }
            Apotheosis.debugLog(pos, "Rogue Spawner - " + RogueSpawnerRegistry.INSTANCE.getKey(item));
            return true;
        }

        return false;
    }

}
