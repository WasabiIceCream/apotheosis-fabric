package dev.shadowsoffire.apotheosis.compat;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.item.PotionCharmItem;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Potion Charms in the Trinkets "Charm" slot (Gameoverse addition, 0.4.4): the Fabric counterpart of upstream's Curios compat
 * ({@code compat/curios/CuriosCompat} and {@code data/apotheosis/curios/entities/player_charm.json}).
 * <p>
 * The slot is {@code charm/charm}, the one Friends &amp; Foes already defines for totems; this mod ships the same slot
 * definition, adds it to players ({@code data/trinkets/entities/apotheosis_charm.json}) and puts the Potion Charm in its tag
 * ({@code trinkets:charm/charm}), so the slot exists without Friends &amp; Foes too. A worn charm ticks with the same logic as
 * one in the inventory ({@link PotionCharmItem#tickCharm}, with no equipment slot, like a Curios slot upstream).
 * <p>
 * Only loaded when Trinkets is installed ({@code Apotheosis#onInitialize} checks first), so this class is the only one that
 * touches the Trinkets API.
 */
public final class TrinketsCompat {

    private TrinketsCompat() {}

    public static void init() {
        TrinketCallback.setCallback(Apoth.Items.POTION_CHARM.value(), new TrinketCallback() {
            @Override
            public void tick(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
                if (entity.level() instanceof ServerLevel level) {
                    PotionCharmItem.tickCharm(stack, level, entity, null, true);
                }
            }
        });
    }
}
