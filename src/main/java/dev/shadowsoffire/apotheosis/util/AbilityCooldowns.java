package dev.shadowsoffire.apotheosis.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Delegates to Apothic Attributes' {@code AbilityCooldowns}, as upstream Apotheosis uses it directly, so
 * the Cooldown Reduction attribute (the Timekeeping affix) shortens affix and gem cooldowns.
 * <p>
 * Port note: before 0.3.0 this was a stand-in keeping last-use times in {@link PersistentDataComponent};
 * cooldowns in progress under the old storage are simply forgotten (they're a few seconds long).
 */
public class AbilityCooldowns {

    /**
     * @return true if fewer than {@code cooldown} ticks (reduced by Cooldown Reduction) have passed since
     *         this ability was last started on this entity.
     */
    public static boolean isOnCooldown(LivingEntity entity, Identifier id, int cooldown) {
        return dev.shadowsoffire.apothic_attributes.api.AbilityCooldowns.isOnCooldown(entity, id, cooldown);
    }

    public static void startCooldown(LivingEntity entity, Identifier id) {
        dev.shadowsoffire.apothic_attributes.api.AbilityCooldowns.startCooldown(entity, id);
    }
}
