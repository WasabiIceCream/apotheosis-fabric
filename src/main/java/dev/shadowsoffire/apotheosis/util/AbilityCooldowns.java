package dev.shadowsoffire.apotheosis.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port of Apothic-Attributes' {@code AbilityCooldowns} (out of scope as a dependency) — tracks
 * a per-entity, per-ability last-used game time, used by affix effects that should only trigger
 * once every N ticks. Backed by {@link PersistentDataComponent} rather than a NeoForge
 * attachment/capability.
 */
public class AbilityCooldowns {

    private static String key(Identifier id) {
        return "apoth_cd_" + id;
    }

    /**
     * @return true if fewer than {@code cooldown} ticks have passed since this ability was last
     *         started on this entity.
     */
    public static boolean isOnCooldown(LivingEntity entity, Identifier id, int cooldown) {
        CompoundTag tag = PersistentDataComponent.get(entity);
        long lastUsed = tag.getLongOr(key(id), Long.MIN_VALUE / 2);
        return entity.level().getGameTime() - lastUsed < cooldown;
    }

    public static void startCooldown(LivingEntity entity, Identifier id) {
        PersistentDataComponent.get(entity).putLong(key(id), entity.level().getGameTime());
    }

}
