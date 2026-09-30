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

    // Boss Stats (upstream's "bosses" config category; same defaults, plain constants like the rest of this class)

    /** If boss items are always cursed. Enable this if you want bosses to be less overpowered by always giving them a negative effect. */
    public static boolean curseBossItems = false;

    /** The range at which boss spawns will be announced. If you are closer than this number of blocks (ignoring y-level), you will receive the announcement. */
    public static float bossAnnounceRange = net.minecraft.world.level.NaturalSpawner.SPAWN_DISTANCE_BLOCK + 12;

    /**
     * The time, in ticks, that must pass before a player may trigger another natural invader spawn. When an invader spawns,
     * this cooldown is applied to the triggering player and to all same-tier players within the boss announcement range.
     * May be overridden per-dimension via the invader spawn rules.
     */
    public static int bossSpawnCooldown = 3600;

    /** If true, invading bosses will automatically target the closest player. */
    public static boolean bossAutoAggro = false;

    /** If true, bosses will glow when they spawn. */
    public static boolean bossGlowOnSpawn = true;

}
