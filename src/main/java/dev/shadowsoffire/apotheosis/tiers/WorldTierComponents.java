package dev.shadowsoffire.apotheosis.tiers;

import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

/**
 * Cardinal Components API entrypoint (registered under the {@code "cardinal-components"}
 * entrypoint key in {@code fabric.mod.json}) that registers {@link WorldTierComponent} on
 * players.
 */
public class WorldTierComponents implements EntityComponentInitializer {

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(WorldTierComponent.KEY, player -> new WorldTierComponent(), RespawnCopyStrategy.ALWAYS_COPY);
    }

}
