package dev.shadowsoffire.apotheosis.util;

import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

import net.minecraft.world.entity.Entity;

/**
 * Cardinal Components API entrypoint (registered under the {@code "cardinal-components"}
 * entrypoint key in {@code fabric.mod.json}) for general-purpose components not specific to
 * any one package — {@link PersistentDataComponent} (registered broadly for every
 * {@link Entity}) and {@link RadialMiningComponent} (players only).
 */
public class ApothComponents implements EntityComponentInitializer {

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerFor(Entity.class, PersistentDataComponent.KEY, e -> new PersistentDataComponent());
        registry.registerForPlayers(RadialMiningComponent.KEY, player -> new RadialMiningComponent(), RespawnCopyStrategy.ALWAYS_COPY);
    }

}
