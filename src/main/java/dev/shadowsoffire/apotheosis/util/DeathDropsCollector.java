package dev.shadowsoffire.apotheosis.util;

import java.util.ArrayDeque;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Implemented on {@code LivingEntity} by {@code mixin.LivingEntityAdventureMixin}: collects the item entities added to the
 * level while an entity drops its death loot, for {@code AdventureEvents#onEntityDrops}.
 * <p>
 * Items are collected where they enter the level ({@code ServerLevel#addFreshEntity}), since other mods capture
 * {@code spawnAtLocation} and add the drops themselves at the end of {@code dropAllDeathLoot} (Puzzles Lib's drops event
 * does). Which drops came from the loot table is recorded by stack identity in {@code Entity#spawnAtLocation}.
 */
public interface DeathDropsCollector {

    ThreadLocal<ArrayDeque<DeathDropsCollector>> ACTIVE = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * The entity currently dropping its death loot on this thread, if any.
     */
    @Nullable
    static DeathDropsCollector active() {
        return ACTIVE.get().peek();
    }

    /**
     * Records an item entity added to the level while this entity drops its death loot.
     */
    void apoth$collectDrop(ItemEntity item);

    /**
     * Records a stack this entity is dropping through {@code spawnAtLocation}; stacks dropped while its loot table is being
     * rolled count as loot.
     */
    void apoth$recordDropStack(ItemStack stack);
}
