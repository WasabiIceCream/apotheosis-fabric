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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Generates flavor names for loot-generated items, based on their type/material.
 * <p>
 * Port note: upstream loads every name pool below from a Placebo {@code Configuration} file
 * ({@code apotheosis/names.cfg}); that {@code load(Configuration)} method isn't ported, so the pools
 * are upstream's defaults. The boss-name half ({@link #setEntityName}, used by invaders and elites
 * with {@code "name": "use_name_generation"}) was added with the mob features in 0.4.0.
 * <p>
 * Also drops the {@code canPerformAction(ItemAbilities....)} fallback checks upstream layers on
 * top of its item-tag checks (e.g. {@code stack.canPerformAction(ItemAbilities.SWORD_SWEEP)}) —
 * {@code ItemAbilities} is a NeoForge-only tool-ability API with no Fabric equivalent; the
 * vanilla tag checks alone (```ItemTags.SWORDS``` etc.) cover the same cases for any item that
 * actually declares itself sword-like/axe-like/etc. through data, which is the common case.
 */
public class NameHelper {

    /**
     * List of all possible full names.
     */
    private static String[] names = { "Biscuit", "Elisande", "Willow",
        "Bippy", "Butto", "Prim", "Tyrael", "Bajorno", "Michael Morbius", "Morbius", "Arun", "Panez", "Doomsday", "Vanamar", "WhatTheDrunk",
        "Lothrazar", "Chelly", "Chelicia", "Darsh", "Dariush", "Cheese E Piloza", "Bing", "Royal", "NoWayHere", "SwankyStella", "Isosahedron",
        "Asfalis", "Biz", "Icicle", "Darko", "Shadows", "Katarina", "Faellynna", "Diliviel", "Jank", "Albert", "Andrew", "Anderson", "Andy", "Allan",
        "Arthur", "Aaron", "Allison", "Arielle", "Amanda", "Anne", "Annie", "Amy", "Alana", "Brandon", "Brady", "Bernard", "Ben", "Benjamin", "Bob",
        "Bobette", "Brooke", "Brandy", "Beatrice", "Bea", "Bella", "Becky", "Carlton", "Carl", "Calvin", "Cameron", "Carson", "Chase", "Cassandra",
        "Cassie", "Cas", "Carol", "Carly", "Cherise", "Charlotte", "Cheryl", "Chasity", "Danny", "Drake", "Daniel", "Derrel", "David", "Dave", "Donovan",
        "Don", "Donald", "Drew", "Derrick", "Darla", "Donna", "Dora", "Danielle", "Edward", "Elliot", "Ed", "Edson", "Elton", "Eddison", "Earl", "Eric",
        "Ericson", "Eddie", "Ediovany", "Emma", "Elizabeth", "Eliza", "Esperanza", "Esper", "Esmeralda", "Emi", "Emily", "Elaine", "Fernando", "Ferdinand",
        "Fred", "Feddie", "Fredward", "Frank", "Franklin", "Felix", "Felicia", "Fran", "Greg", "Gregory", "George", "Gerald", "Gina", "Geraldine", "Gabby",
        "Hendrix", "Henry", "Hobbes", "Herbert", "Heath", "Henderson", "Helga", "Hera", "Helen", "Helena", "Hannah", "Ike", "Issac", "Israel", "Ismael", "Irlanda",
        "Isabelle", "Irene", "Irenia", "Jimmy", "Jim", "Justin", "Jacob", "Jake", "Jon", "Johnson", "Jonny", "Jonathan", "Josh", "Joshua", "Julian", "Jesus",
        "Jericho", "Jeb", "Jess", "Joan", "Jill", "Jillian", "Jessica", "Jennifer", "Jenny", "Jen", "Judy", "Kenneth", "Kenny", "Ken", "Keith", "Kevin", "Karen",
        "Kassandra", "Kassie", "Leonard", "Leo", "Leroy", "Lee", "Lenny", "Luke", "Lucas", "Liam", "Lorraine", "Latasha", "Lauren", "Laquisha", "Livia",
        "Lydia", "Lila", "Lilly", "Lillian", "Lilith", "Lana", "Mason", "Mike", "Mickey", "Mario", "Manny", "Mark", "Marcus", "Martin", "Marty", "Matthew",
        "Matt", "Max", "Maximillian", "Marth", "Mia", "Marriah", "Maddison", "Maddie", "Marissa", "Miranda", "Mary", "Martha", "Melonie", "Melody", "Mel",
        "Minnie", "Nathan", "Nathaniel", "Nate", "Ned", "Nick", "Norman", "Nicholas", "Natasha", "Nicki", "Nora", "Nelly", "Nina", "Orville", "Oliver",
        "Orlando", "Owen", "Olsen", "Odin", "Olaf", "Ortega", "Olivia", "Patrick", "Pat", "Paul", "Perry", "Pinnochio", "Patrice", "Patricia", "Pennie",
        "Petunia", "Patti", "Pernelle", "Quade", "Quincy", "Quentin", "Quinn", "Roberto", "Robbie", "Rob", "Robert", "Roy", "Roland", "Ronald", "Richard",
        "Rick", "Ricky", "Rose", "Rosa", "Rhonda", "Rebecca", "Roberta", "Sparky", "Shiloh", "Stephen", "Steve", "Saul", "Sheen", "Shane", "Sean", "Sampson",
        "Samuel", "Sammy", "Stefan", "Sasha", "Sam", "Susan", "Suzy", "Shelby", "Samantha", "Sheila", "Sharon", "Sally", "Stephanie", "Sandra", "Sandy",
        "Sage", "Tim", "Thomas", "Thompson", "Tyson", "Tyler", "Tom", "Tyrone", "Timmothy", "Tamara", "Tabby", "Tabitha", "Tessa", "Tiara", "Tyra", "Uriel",
        "Ursala", "Uma", "Victor", "Vincent", "Vince", "Vance", "Vinny", "Velma", "Victoria", "Veronica", "Wilson", "Wally", "Wallace", "Will", "Wilard",
        "William", "Wilhelm", "Xavier", "Xandra", "Young", "Yvonne", "Yolanda", "Zach", "Zachary" };

    /**
     * List of all name parts.
     */
    private static String[] nameParts = { "Prim", "Morb", "Ius", "Kat", "Chel", "Bing", "Darsh", "Jank", "Dark", "Osto", "Grab", "Thar",
        "Ger", "Ald", "Mas", "On", "O", "Din", "Thor", "Jon", "Ath", "Burb", "En", "A", "E", "I", "U", "Hab", "Bloo", "Ena",
        "Dit", "Aph", "Ern", "Bor", "Dav", "Id", "Toast", "Son", "For", "Wen", "Lob", "Van", "Zap", "Ear", "Ben", "Don", "Bran",
        "Gro", "Jen", "Bob", "Ette", "Ere", "Man", "Qua", "Bro", "Cree", "Per", "Skel", "Ton", "Zom", "Bie", "Wolf", "End", "Er",
        "Pig", "Sil", "Ver", "Fish", "Cow", "Chic", "Ken", "Sheep", "Squid", "Hell", "Dra", "Gor", "Nyx", "Fae", "Lux", "Vex",
        "Hex", "Rune", "Frost", "Flame", "Storm", "Shade", "Dawn", "Dusk", "Ash", "Mist", "Might", "Fury", "Rage", "Doom", "Grim",
        "Void", "Rend", "Slay", "Ar", "Or", "Ur", "El", "Al", "Im", "Un", "En", "Ix", "Ox", "Dire", "Dark", "Bright", "Swift", "Glow",
        "Shine", "Gleam", "Spark"
    };

    /**
     * List of prefixes, that are optionally applied to names.
     */
    private static String[] prefixes = { "Dr. Michael", "Sir", "Mister", "Madam", "Doctor", "Father", "Mother", "Poppa", "Lord", "Lady", "Overseer", "Professor",
        "Mr.", "Mr. President", "Duke", "Duchess", "Dame", "The Honorable", "Chancellor", "Vice-Chancellor", "His Holiness", "Reverend", "Count", "Viscount",
        "Earl", "Captain", "Major", "General", "Senpai", "Discount" };

    /**
     * List of suffixes, that are optionally applied to names. A suffix will always be preceeded by "the"
     * That is, selecting "Mighty" from this list would incur the addition of "The Mighty" to the name.
     */
    private static String[] suffixes = { "Morbius", "Dragonborn", "Rejected", "Mighty", "Supreme", "Superior", "Ultimate", "Lame", "Wimpy", "Curious", "Sneaky",
        "Pathetic", "Crying", "Eagle", "Errant", "Unholy", "Questionable", "Mean", "Hungry", "Thirsty", "Feeble", "Wise", "Sage", "Magical", "Mythical",
        "Legendary", "Not Very Nice", "Jerk", "Doctor", "Misunderstood", "Angry", "Knight", "Bishop", "Godly", "Special", "Toasty", "Shiny", "Shimmering",
        "Light", "Dark", "Odd-Smelling", "Funky", "Rock Smasher", "Son of Herobrine", "Cracked", "Sticky", "\u00a7kAlien\u00a7r", "Baby", "Manly", "Rough",
        "Scary", "Undoubtable", "Honest", "Non-Suspicious", "Boring", "Odd", "Lazy", "Super", "Nifty", "Ogre Slayer", "Pig Thief", "Dirt Digger", "Really Cool",
        "Doominator", "... Something", "Extra-Fishy", "Gorilla Slaughterer", "Marbles Winner", "AC Rizzlord", "President", "Burger Chef", "Professional Animator",
        "Cheese Sprayer", "Happiness Advocate", "Ghost Hunter", "Head of Potatoes", "Ninja", "Warrior", "Pyromancer", "Trombone Player", "Airport Technician",
        "Grand Magistrix", "Starved", "Terrifying", "Expert Cloud Watcher", "Cookie Enthusiast", "Grass Toucher", "Coffee Addict", "Mildly Confused"
    };

    /*
     * Item name pools below.
     */

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

    public static String suffixFormat = "%s the %s";
    public static String ownershipFormat = "%s's";
    public static String chainFormat = "%s %s";

    /**
     * Makes a name using {@link NameHelper#nameParts}.
     * The name is made out of a random value from name parts, combined with up to two more values from the array.
     * The selected values are not unique, and may overlap.
     */
    public static String nameFromParts(RandomSource random) {
        String name = NameHelper.nameParts[random.nextInt(NameHelper.nameParts.length)] + NameHelper.nameParts[random.nextInt(NameHelper.nameParts.length)].toLowerCase();
        if (random.nextFloat() < 0.4F) {
            name += NameHelper.nameParts[random.nextInt(NameHelper.nameParts.length)].toLowerCase();
        }
        if (random.nextFloat() < 0.15F) {
            name += NameHelper.nameParts[random.nextInt(NameHelper.nameParts.length)].toLowerCase();
        }
        return name;
    }

    /**
     * Applies a random name to an entity.
     * The root name is either randomly selected from {@link NameHelper#names} or generated by {@link NameHelper#nameFromParts(RandomSource)}
     * There is a 50% chance for a prefix to be selected from {@link NameHelper#prefixes}
     * There is a 80% chance for a suffix to be selected from {@link NameHelper#suffixes}
     *
     * @return The root name of the entity, without any prefixes or suffixes.
     */
    public static String setEntityName(RandomSource rand, Mob entity) {
        String root;

        if (names.length > 0 && nameParts.length > 0) {
            root = rand.nextFloat() < 0.45F ? NameHelper.names[rand.nextInt(NameHelper.names.length)] : NameHelper.nameFromParts(rand);
        }
        else if (names.length > 0) {
            root = NameHelper.names[rand.nextInt(NameHelper.names.length)];
        }
        else {
            root = NameHelper.nameFromParts(rand);
        }

        String name = root;
        if (rand.nextFloat() < 0.3F && prefixes.length > 0) {
            name = NameHelper.prefixes[rand.nextInt(NameHelper.prefixes.length)] + " " + name;
        }
        if (rand.nextFloat() < 0.8F && suffixes.length > 0) {
            name = String.format(suffixFormat, name, NameHelper.suffixes[rand.nextInt(NameHelper.suffixes.length)]);
        }
        entity.setCustomName(Component.literal(name));
        entity.setCustomNameVisible(true);
        return root;
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
