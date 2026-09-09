package dev.shadowsoffire.apotheosis;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Port note: upstream's {@code AdventureConfig} is a full Placebo {@code Configuration}-backed
 * (not ported yet, see {@code placebo-fabric}'s README) config class with its own sync payload,
 * covering torch item, sigil upgrade/reroll costs, Curios integration, and boss spawn-rate
 * tuning. Only the one field actually needed so far ({@link #torchItem}, used by
 * {@code EnlightenedAffix}) is stubbed here as a plain constant — TODO: port the real config
 * system once more of it is needed (`upgradeSigilCost` etc., once the augmenting table's
 * pricing logic is reached).
 */
public class AdventureConfig {

    public static Item torchItem = Items.TORCH;

    public static boolean cleaveHitsPlayers = false;

    public static boolean charmsInCuriosOnly = false;

    public static boolean enableItemLinking = true;

    public static boolean enableManualWorldTierChanges = true;

    public static int itemLinkingCooldown = 100;

    public static int upgradeSigilCost = 2;

    public static int upgradeLevelCost = 225;

    public static int rerollSigilCost = 1;

    public static int rerollLevelCost = 175;

}
