package dev.shadowsoffire.apotheosis.affix;

import java.util.List;
import java.util.Set;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * The drops of a dying (non-player) entity, the port's stand-in for NeoForge's {@code LivingDropsEvent}.
 * <p>
 * Port note (NeoForge -> Fabric): NeoForge collects the drops before they enter the world and fires the event on the list.
 * Here the item entities are already in the level ({@code mixin.LivingEntityDropsMixin} records every item the entity
 * spawns while {@code dropAllDeathLoot} runs); moving them works the same, and any entity a hook adds to {@link #drops()}
 * is added to the level afterwards.
 * <p>
 * {@link #isMarked} replaces upstream's festive marker (a flag on every stack in the entity's equipment and inventory):
 * everything the entity dropped outside its loot table (equipment, inventories, custom death loot) is marked, so Festive
 * only copies loot table drops and can't duplicate items a mob was carrying, modded inventories included.
 *
 * @param entity  The entity that died.
 * @param source  The killing damage source.
 * @param drops   Every item entity dropped on death. Mutable.
 * @param unmarked The drops that came from the entity's loot table.
 */
public record LivingDrops(LivingEntity entity, DamageSource source, List<ItemEntity> drops, Set<ItemEntity> unmarked) {

    public boolean isMarked(ItemEntity item) {
        return !this.unmarked.contains(item);
    }
}
