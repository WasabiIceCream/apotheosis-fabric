package dev.shadowsoffire.apotheosis.util;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Generates flavor names for loot-generated items, based on their type/material.
 * <p>
 * Port note: upstream's {@code NameHelper} also generates boss-entity names (random full names /
 * name-part combinations, with configurable prefix/suffix pools loaded from a Placebo
 * {@code Configuration} file) — all boss/spawner scoped, out of this port's range, so dropped
 * along with the {@code Configuration}-backed {@code load(Configuration)} method. Only
 * {@link #setItemName}, the piece actually called by the loot-modifier package, is ported.
 * <p>
 * Also drops the {@code canPerformAction(ItemAbilities....)} fallback checks upstream layers on
 * top of its item-tag checks (e.g. {@code stack.canPerformAction(ItemAbilities.SWORD_SWEEP)}) —
 * {@code ItemAbilities} is a NeoForge-only tool-ability API with no Fabric equivalent; the
 * vanilla tag checks alone (```ItemTags.SWORDS``` etc.) cover the same cases for any item that
 * actually declares itself sword-like/axe-like/etc. through data, which is the common case.
 */
public class NameHelper {

    /**
     * Possible primary names for helmets.
     */
    private static String[] helms = { "Helmet", "Cap", "Crown", "Great Helm", "Bassinet", "Sallet", "Close Helm", "Barbute" };

    /**
     * Possible primary names for chestplates.
     */
    private static String[] chestplates = { "Chestplate", "Tunic", "Brigandine", "Hauberk", "Cuirass" };

    /**
     * Possible primary names for leggings.
     */
    private static String[] leggings = { "Leggings", "Pants", "Tassets", "Cuisses", "Schynbalds" };

    /**
     * Possible primary names for boots.
     */
    private static String[] boots = { "Boots", "Shoes", "Greaves", "Sabatons", "Sollerets" };

    /**
     * Possible primary names for swords.
     */
    private static String[] swords = { "Sword", "Cutter", "Slicer", "Dicer", "Knife", "Blade", "Machete", "Brand", "Claymore", "Cutlass", "Foil", "Dagger", "Glaive", "Rapier", "Saber", "Scimitar", "Shortsword", "Longsword",
        "Broadsword", "Calibur" };

    /**
     * Possible primary names for axes.
     */
    private static String[] axes = { "Axe", "Chopper", "Hatchet", "Tomahawk", "Cleaver", "Hacker", "Tree-Cutter", "Truncator" };

    /**
     * Possible primary names for pickaxes.
     */
    private static String[] pickaxes = { "Pickaxe", "Pick", "Mattock", "Rock-Smasher", "Miner" };

    /**
     * Possible primary names for shovels.
     */
    private static String[] shovels = { "Shovel", "Spade", "Digger", "Excavator", "Trowel", "Scoop" };

    /**
     * Possible primary names for bows.
     */
    private static String[] bows = { "Bow", "Shortbow", "Longbow", "Flatbow", "Recurve Bow", "Reflex Bow", "Self Bow", "Composite Bow", "Arrow-Flinger" };

    /**
     * Possible primary names for shields.
     */
    private static String[] shields = { "Shield", "Buckler", "Targe", "Greatshield", "Blockade", "Bulwark", "Tower Shield", "Protector", "Aegis" };

    /**
     * Ordered map of material path-fragment → prefix name candidates. Fragments are matched
     * by {@code path.contains(fragment)} and the first matching entry wins, so longer/more
     * specific keys must appear before any shorter keys they would otherwise collide with.
     */
    private static Map<String, String[]> materialNames = new LinkedHashMap<>();
    static {
        materialNames.put("netherite", new String[] { "Burnt", "Embered", "Fiery", "Hellborn", "Flameforged" });
        materialNames.put("diamond", new String[] { "Diamond", "Zircon", "Gemstone", "Jewel", "Crystal" });
        materialNames.put("chainmail", new String[] { "Chainmail", "Chain", "Chain Link", "Scale" });
        materialNames.put("ironwood", new String[] { "Ironwood", "Earthbound", "Oaken", "Ironcapped" });
        materialNames.put("knightmetal", new String[] { "Knightmetal", "Knightly", "Phantom-Forged" });
        materialNames.put("steeleaf", new String[] { "Steeleaf", "Organic", "Natural", "Cobaltstem", "Tungstenpetal" });
        materialNames.put("leather", new String[] { "Leather", "Rawhide", "Lamellar", "Cow Skin" });
        materialNames.put("golden", new String[] { "Golden", "Gold", "Gilt", "Auric", "Ornate" });
        materialNames.put("wooden", new String[] { "Wooden", "Wood", "Hardwood", "Balsa Wood", "Mahogany", "Plywood" });
        materialNames.put("turtle", new String[] { "Tortollan", "Very Tragic", "Environmental", "Organic" });
        materialNames.put("stone", new String[] { "Stone", "Rock", "Marble", "Cobblestone" });
        materialNames.put("fiery", new String[] { "Fiery", "Flaming", "Hydra-Infused", "Infernal" });
        materialNames.put("iron", new String[] { "Iron", "Steel", "Ferrous", "Rusty", "Wrought Iron" });
    }

    /**
     * Applies a random name to an itemstack based on the item itself. An additional prefix is selected
     * from the item's material, detected by scanning the registry path for a recognizable material
     * fragment (e.g. {@code netherite_sword} → {@code netherite}).
     *
     * @param stack The stack to be named.
     * @return The name of the item, without the owning prefix of the boss's name.
     */
    public static Component setItemName(RandomSource random, ItemStack stack) {
        MutableComponent name = (MutableComponent) stack.getItem().getName(stack);
        String baseName = name.getString();
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        Tool tool = stack.get(DataComponents.TOOL);
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);

        if (tool != null) {
            name = buildMaterialPrefix(random, path, baseName);

            String[] type = { "Tool" };
            if (stack.is(ItemTags.SWORDS)) {
                type = swords;
            }
            else if (stack.is(ItemTags.AXES)) {
                type = axes;
            }
            else if (stack.is(ItemTags.PICKAXES)) {
                type = pickaxes;
            }
            else if (stack.is(ItemTags.SHOVELS)) {
                type = shovels;
            }
            else if (stack.has(DataComponents.BLOCKS_ATTACKS)) {
                type = shields;
            }
            name.append(type[random.nextInt(type.length)]);
        }
        else if (stack.getItem() instanceof ProjectileWeaponItem) {
            name = Component.literal(bows[random.nextInt(bows.length)]);
        }
        else if (equippable != null && equippable.slot() != EquipmentSlot.BODY) {
            name = buildMaterialPrefix(random, path, baseName);

            String[] type = { "Armor" };
            EquipmentSlot slot = equippable.slot();
            if (slot == EquipmentSlot.HEAD) {
                type = helms;
            }
            else if (slot == EquipmentSlot.CHEST) {
                type = chestplates;
            }
            else if (slot == EquipmentSlot.LEGS) {
                type = leggings;
            }
            else if (slot == EquipmentSlot.FEET) {
                type = boots;
            }
            name.append(type[random.nextInt(type.length)]);
        }

        stack.set(DataComponents.CUSTOM_NAME, name.withStyle(name.getStyle().withItalic(false)));
        return name;
    }

    private static MutableComponent buildMaterialPrefix(RandomSource random, String path, String baseName) {
        String[] matNames = findMaterialNames(path);
        if (matNames.length == 0) {
            return Component.literal(stripLastToken(baseName));
        }
        return Component.literal(matNames[random.nextInt(matNames.length)] + " ");
    }

    private static String[] findMaterialNames(String path) {
        for (Map.Entry<String, String[]> entry : materialNames.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new String[0];
    }

    private static String stripLastToken(String baseName) {
        String[] split = baseName.split(" ");
        if (split.length <= 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            sb.append(split[i]).append(' ');
        }
        return sb.toString();
    }

}
