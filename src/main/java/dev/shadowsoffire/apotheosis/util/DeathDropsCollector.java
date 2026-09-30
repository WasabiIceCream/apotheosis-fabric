package dev.shadowsoffire.apotheosis.util;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * Implemented on {@code LivingEntity} by {@code mixin.LivingEntityAdventureMixin}: collects the item entities an entity spawns
 * while it drops its death loot, for {@code AdventureEvents#onEntityDrops}.
 */
public interface DeathDropsCollector {

    /**
     * Records an item entity spawned by this entity. Does nothing unless the entity is currently dropping its death loot.
     */
    void apoth$collectDrop(ItemEntity item);
}
