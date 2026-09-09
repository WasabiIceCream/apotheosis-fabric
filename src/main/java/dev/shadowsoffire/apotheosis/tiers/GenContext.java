package dev.shadowsoffire.apotheosis.tiers;

import java.util.Set;

import javax.annotation.Nullable;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.compat.GameStagesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Generation context for random objects which are sensitive to the current world tier and luck levels.
 *
 * @param random The random source
 * @param tier   The relevant (or closest) player's world tier.
 * @param luck   The relevant (or closest) player's luck level.
 */
public record GenContext(RandomSource rand, WorldTier tier, float luck, ResourceKey<Level> dimension, Holder<Biome> biome, Set<String> stages) {

    /**
     * Returns a generation context for the target player, using an external random source.
     */
    public static GenContext forPlayerAtPos(RandomSource rand, Player player, BlockPos pos) {
        Level level = player.level();
        return new GenContext(rand, WorldTier.getTier(player), player.getLuck(), level.dimension(), level.getBiome(pos), GameStagesCompat.getStages(player));
    }

    /**
     * Returns a generation context for the target player, using an external random source.
     */
    public static GenContext forPlayer(RandomSource rand, Player player) {
        return forPlayerAtPos(rand, player, player.blockPosition());
    }

    /**
     * Returns a generation context for the target player, using the player's random source.
     */
    public static GenContext forPlayer(Player player) {
        return forPlayer(player.getRandom(), player);
    }

    @Nullable
    public static GenContext forLoot(LootContext ctx) {
        Player player = findPlayer(ctx);
        if (player != null) {
            return new GenContext(ctx.getRandom(), WorldTier.getTier(player), ctx.getLuck(), ctx.getLevel().dimension(), ctx.getLevel().getBiome(player.blockPosition()), GameStagesCompat.getStages(player));
        }
        return null;
    }

    public static GenContext standalone(RandomSource rand, WorldTier tier, float luck, ServerLevel level, BlockPos pos) {
        return new GenContext(rand, tier, luck, level.dimension(), level.getBiome(pos), Set.of());
    }

    /**
     * Port note (NeoForge -> Fabric): the original resolves the biome lookup via NeoForge's
     * {@code CommonHooks.resolveLookup}, a convenience for reaching the "current" registry
     * access with no live player/level in hand. Fabric has no equivalent, so this uses
     * {@link Apotheosis#getCurrentServer()} (tracked via server lifecycle events) instead.
     */
    public static GenContext dummy(RandomSource rand) {
        // Nobody should be deleting plains outright, right?
        MinecraftServer server = Apotheosis.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("GenContext.dummy() called with no server running");
        }
        Holder<Biome> plains = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        return new GenContext(rand, WorldTier.HAVEN, 0, Level.OVERWORLD, plains, Set.of());
    }

    @Nullable
    public static Player findPlayer(LootContext ctx) {
        if (ctx.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player p) {
            return p;
        }
        if (ctx.getOptionalParameter(LootContextParams.ATTACKING_ENTITY) instanceof Player p) {
            return p;
        }
        if (ctx.getOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY) instanceof Player p) {
            return p;
        }
        if (ctx.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER) != null) {
            return ctx.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER);
        }
        return null;
    }

    @Override
    public final String toString() {
        return "GenContext[tier=%s, luck=%s, dimension=%s, biome=%s, stages=%s]".formatted(this.tier.getSerializedName(), this.luck, this.dimension.identifier(), this.biome.unwrapKey().map(k -> k.identifier().toString()).orElse("unknown"), this.stages);
    }

}
