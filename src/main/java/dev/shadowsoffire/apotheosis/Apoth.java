package dev.shadowsoffire.apotheosis;

import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.mojang.serialization.Codec;

import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Object Holder Class. For the main mod class, see {@link Apotheosis}.
 * <p>
 * Port note: upstream's {@code Apoth} is a ~740-line object-holder class spanning both the
 * Adventure module (in scope here) and boss/spawner/enchanting content (out of scope — see
 * {@code README.md}). Rather than port it wholesale, it's being built up incrementally,
 * nested class by nested class, only as each is actually needed by in-scope code. Expect this
 * file to grow substantially as {@code affix}/{@code socket}/{@code item} are ported.
 */
public class Apoth {

    public static final DeferredHelper R = DeferredHelper.create(Apotheosis.MODID);

    public static final class BuiltInRegs {

        public static final Registry<LootCategory> LOOT_CATEGORY = R.registry("loot_category", Apotheosis.loc("none"));

        public static final com.mojang.serialization.MapCodec<dev.shadowsoffire.apotheosis.loot.conditions.KilledByRealPlayerCondition> KILLED_BY_REAL_PLAYER = R.lootCondition("killed_by_real_player", dev.shadowsoffire.apotheosis.loot.conditions.KilledByRealPlayerCondition.CODEC);

        public static final com.mojang.serialization.MapCodec<dev.shadowsoffire.apotheosis.advancements.predicates.MonsterPredicate> IS_MONSTER = R.custom("is_monster", net.minecraft.core.registries.BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, dev.shadowsoffire.apotheosis.advancements.predicates.MonsterPredicate.CODEC);

        private static void bootstrap() {}
    }

    public static final class Components {

        public static final DataComponentType<ItemAffixes> AFFIXES = R.component("affixes", b -> b.persistent(ItemAffixes.CODEC).networkSynchronized(ItemAffixes.STREAM_CODEC));

        public static final DataComponentType<DynamicHolder<dev.shadowsoffire.apotheosis.loot.LootRarity>> RARITY = R.component("rarity", b -> b.persistent(RarityRegistry.INSTANCE.holderCodec()).networkSynchronized(RarityRegistry.INSTANCE.holderStreamCodec()));

        public static final DataComponentType<net.minecraft.network.chat.Component> AFFIX_NAME = R.component("affix_name", b -> b.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.TRUSTED_STREAM_CODEC));

        public static final DataComponentType<Integer> SOCKETS = R.component("sockets", b -> b.persistent(Codec.intRange(0, 16)).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<ItemContainerContents> SOCKETED_GEMS = R.component("socketed_gems", b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<Float> DURABILITY_BONUS = R.component("durability_bonus", b -> b.persistent(Codec.floatRange(0, 1)).networkSynchronized(ByteBufCodecs.FLOAT));

        public static final DataComponentType<net.minecraft.world.level.block.Block> STONEFORMING_TARGET = R.component("stoneforming_target", b -> b.persistent(net.minecraft.core.registries.BuiltInRegistries.BLOCK.byNameCodec()).networkSynchronized(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK)));

        public static final DataComponentType<Boolean> FROM_CHEST = R.component("from_chest", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> FROM_TRADER = R.component("from_trader", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> FROM_BOSS = R.component("from_boss", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> FROM_MOB = R.component("from_mob", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> CHARM_ENABLED = R.component("charm_enabled", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> MALICE_MARKER = R.component("malice_marker", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Boolean> TOUCHED_BY_MALICE = R.component("touched_by_malice", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Float> RENDER_ALPHA = R.component("render_alpha", b -> b.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

        public static final DataComponentType<DynamicHolder<dev.shadowsoffire.apotheosis.socket.gem.Gem>> GEM = R.component("gem", b -> b.persistent(dev.shadowsoffire.apotheosis.socket.gem.GemRegistry.INSTANCE.holderCodec()).networkSynchronized(dev.shadowsoffire.apotheosis.socket.gem.GemRegistry.INSTANCE.holderStreamCodec()));

        public static final DataComponentType<dev.shadowsoffire.apotheosis.socket.gem.Purity> PURITY = R.component("purity", b -> b.persistent(dev.shadowsoffire.apotheosis.socket.gem.Purity.CODEC).networkSynchronized(dev.shadowsoffire.apotheosis.socket.gem.Purity.STREAM_CODEC));

        private static void bootstrap() {}

    }

    /**
     * Port note (NeoForge -> Fabric): {@code IS_MAGIC} replaces NeoForge's own convention tag
     * {@code neoforge:is_magic} (no Fabric equivalent) with an {@code apotheosis:is_magic}
     * damage-type tag, pre-populated with vanilla's two built-in magic damage types
     * ({@code minecraft:magic}/{@code indirect_magic}) — see
     * {@code data/apotheosis/tags/damage_type/is_magic.json}.
     */
    public static final class Tags {
        public static final net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> IS_MAGIC = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Apotheosis.loc("is_magic"));

        /**
         * Effects on this tag get a longer "top up before it runs out" window in
         * {@code item.PotionCharmItem} (210 ticks instead of the default 5). No entries are
         * pre-populated — same as upstream, which leaves this tag empty by default too.
         */
        public static final net.minecraft.tags.TagKey<net.minecraft.world.effect.MobEffect> EXTENDED_CHARM_DURATION = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT, Apotheosis.loc("extended_charm_duration"));

        /**
         * Potions on this tag may not be converted into a Potion Charm via {@code item.PotionCharmItem#isValidPotion}.
         * No entries are pre-populated — same as upstream.
         */
        public static final net.minecraft.tags.TagKey<net.minecraft.world.item.alchemy.Potion> POTION_CHARM_BLACKLIST = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.POTION, Apotheosis.loc("potion_charm_blacklist"));
    }

    public static final class DamageTypes {
        public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> EXECUTE = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Apotheosis.loc("execute"));
        public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> PSYCHIC = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Apotheosis.loc("psychic"));

        /**
         * Port note (NeoForge -> Fabric): upstream's {@code ThunderstruckAffix} dynamically tags
         * an ad-hoc {@code mobAttack} damage source with {@code is_lightning}/{@code bypasses_armor}
         * at hit time, via NeoForge's {@code DamageSourceExtension} (a NeoForge-only interface
         * allowing mutable damage tags after creation — no Fabric/vanilla equivalent). Since damage
         * type tags are just datapack tag-file membership, this port instead declares a real,
         * static "thunderstruck" damage type that's always tagged both ways (see
         * {@code data/apotheosis/damage_type/thunderstruck.json} and the two
         * {@code data/minecraft/tags/damage_type/*.json} additions) — same effect on damage
         * calculation, at the cost of a fixed death-message id instead of borrowing mob_attack's.
         */
        public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> THUNDERSTRUCK = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Apotheosis.loc("thunderstruck"));
    }

    public static final class Advancements {
        public static final Identifier WORLD_TIER_HAVEN = Apotheosis.loc("progression/haven");
        public static final Identifier WORLD_TIER_FRONTIER = Apotheosis.loc("progression/frontier");
        public static final Identifier WORLD_TIER_ASCENT = Apotheosis.loc("progression/ascent");
        public static final Identifier WORLD_TIER_SUMMIT = Apotheosis.loc("progression/summit");
        public static final Identifier WORLD_TIER_PINNACLE = Apotheosis.loc("progression/pinnacle");
    }

    public static final class Stats {
        public static final Identifier WORLD_TIERS_ACTIVATED = R.customStat("world_tiers_activated", StatFormatter.DEFAULT);

        private static void bootstrap() {}
    }

    public static final class Triggers {
        public static final dev.shadowsoffire.apotheosis.advancements.EquippedItemTrigger EQUIPPED_ITEM = R.criteriaTrigger("equipped_item", new dev.shadowsoffire.apotheosis.advancements.EquippedItemTrigger());

        private static void bootstrap() {}
    }

    public static final class DataComponentPredicates {
        public static final net.minecraft.core.component.predicates.DataComponentPredicate.Type<dev.shadowsoffire.apotheosis.advancements.predicates.AffixItemPredicate> AFFIXED_ITEM = R.componentPredicate("affixed_item", dev.shadowsoffire.apotheosis.advancements.predicates.AffixItemPredicate.CODEC);
        public static final net.minecraft.core.component.predicates.DataComponentPredicate.Type<dev.shadowsoffire.apotheosis.advancements.predicates.RarityItemPredicate> ITEM_WITH_RARITY = R.componentPredicate("item_with_rarity", dev.shadowsoffire.apotheosis.advancements.predicates.RarityItemPredicate.CODEC);

        private static void bootstrap() {}
    }

    /**
     * Port note (Apothic-Attributes -> Fabric): Apothic-Attributes itself is out of this port's
     * scope, but a handful of upstream {@code tier_augments} entries reference three of its
     * custom attributes ({@code experience_gained}, {@code armor_pierce}, {@code prot_pierce}).
     * Rather than leave those augments permanently broken (unknown registry key) or drop them
     * entirely, these are re-registered here under our own namespace with the same mechanical
     * effect, restored via mixins:
     * <ul>
     * <li>{@code EXPERIENCE_GAINED}: {@code PlayerExperienceMixin} multiplies XP awarded to a
     * player by {@code (1 + attributeValue)}.</li>
     * <li>{@code ARMOR_PIERCE}: {@code CombatRulesMixin} reduces the defender's effective armor
     * value by the attacker's attribute value before the standard armor-damage-reduction
     * formula runs.</li>
     * <li>{@code PROT_PIERCE}: {@code EnchantmentHelperMixin} reduces the defender's aggregate
     * Protection-family enchantment reduction by the attacker's attribute value.</li>
     * </ul>
     * All three are added to every {@link net.minecraft.world.entity.LivingEntity}'s default
     * attribute map via {@code LivingEntityAttributesMixin} (into
     * {@code LivingEntity#createLivingAttributes}), since Fabric API has no event for
     * retrofitting a new default attribute onto existing entity types.
     * <p>
     * The fourth out-of-scope attribute referenced upstream, Apothic-Enchanting's
     * {@code max_eterna}, is NOT re-created here — it's tied to that mod's "Eternal" enchantment
     * mechanic (a whole unported enchantment, not a simple numeric buff), so those 4
     * {@code tier_augments} entries were dropped entirely rather than faked.
     * <p>
     * Port note (NeoForge -> Fabric, added later): the same treatment applies to
     * {@code armor/attribute/unbound}'s {@code neoforge:creative_flight} — a NeoForge-core
     * attribute (not Apothic-Attributes'), meaningless on Fabric since NeoForge itself patches
     * player-ability-tick code to read it. Re-registered here as {@code CREATIVE_FLIGHT} (0.0-1.0,
     * boolean-style: >= 0.5 means "grant flight") with the actual behavior restored via
     * {@code ServerPlayerCreativeFlightMixin}, which mirrors vanilla's own
     * {@code ServerPlayer#updatePlayerAttributes()} pattern (reading
     * {@code Attributes.BLOCK_INTERACTION_RANGE} every tick to toggle creative-only reach) but for
     * this attribute instead, toggling {@code Abilities#mayfly} for non-creative, non-spectator
     * players only, and only ever revoking a grant it made itself.
     */
    public static final class CustomAttributes {
        public static final Holder<net.minecraft.world.entity.ai.attributes.Attribute> EXPERIENCE_GAINED = R.rangedAttribute("experience_gained", 0.0, 0.0, 1024.0);
        public static final Holder<net.minecraft.world.entity.ai.attributes.Attribute> ARMOR_PIERCE = R.rangedAttribute("armor_pierce", 0.0, 0.0, 1024.0);
        public static final Holder<net.minecraft.world.entity.ai.attributes.Attribute> PROT_PIERCE = R.rangedAttribute("prot_pierce", 0.0, 0.0, 1024.0);
        public static final Holder<net.minecraft.world.entity.ai.attributes.Attribute> CREATIVE_FLIGHT = R.rangedAttribute("creative_flight", 0.0, 0.0, 1.0);

        private static void bootstrap() {}
    }

    /**
     * Port note (NeoForge/Apothic-Attributes -> Fabric): the original uses Apothic-Attributes'
     * {@code EntitySlotGroup} (a richer slot-group type) for {@code LootCategory}'s slots.
     * Apothic-Attributes isn't a dependency of this port; vanilla's own
     * {@link EquipmentSlotGroup} covers every case actually used here (ANY, HAND, MAINHAND,
     * HEAD/CHEST/LEGS/FEET), so it's used directly instead — no wrapper type needed.
     * <p>
     * Also drops the {@code // TODO https://github.com/neoforged/NeoForge/issues/3112} comment
     * about re-wiring {@code BREAKER} against dig-ability tags — not our issue to track.
     */
    public static final class LootCategories {

        public static final LootCategory BOW = register("bow", s -> s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem, EquipmentSlotGroup.HAND);
        public static final LootCategory BREAKER = register("breaker", s -> s.is(ItemTags.PICKAXES) || s.is(ItemTags.SHOVELS), EquipmentSlotGroup.MAINHAND);
        public static final LootCategory HELMET = register("helmet", armorSlot(EquipmentSlot.HEAD), EquipmentSlotGroup.HEAD);
        public static final LootCategory CHESTPLATE = register("chestplate", armorSlot(EquipmentSlot.CHEST), EquipmentSlotGroup.CHEST);
        public static final LootCategory LEGGINGS = register("leggings", armorSlot(EquipmentSlot.LEGS), EquipmentSlotGroup.LEGS);
        public static final LootCategory BOOTS = register("boots", armorSlot(EquipmentSlot.FEET), EquipmentSlotGroup.FEET);
        public static final LootCategory SHIELD = register("shield", s -> s.getItem() instanceof ShieldItem, EquipmentSlotGroup.HAND);
        public static final LootCategory TRIDENT = register("trident", s -> s.getItem() instanceof TridentItem, EquipmentSlotGroup.MAINHAND);
        public static final LootCategory MELEE_WEAPON = register("melee_weapon", s -> s.is(ItemTags.SWORDS) || getDefaultModifiers(s).compute(Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND) > 1,
            EquipmentSlotGroup.MAINHAND, 2000);
        public static final LootCategory SHEARS = register("shears", s -> s.getItem() instanceof ShearsItem, EquipmentSlotGroup.MAINHAND, 2500);
        /**
         * Port note (Curios -> Fabric): upstream defines this in {@code compat/curios/CuriosCompat}
         * as "whatever's tagged {@code curios:charm}", gated behind Curios' own custom equipment-slot
         * system — out of scope, and unnecessary here: {@code PotionCharmItem}'s
         * {@code AdventureConfig.charmsInCuriosOnly} already defaults to {@code false} (upstream's
         * own default too), so Potion Charms work from any inventory slot without needing an
         * equip-slot system at all. This category exists purely so affixes like {@code lucky} can
         * target Potion Charms — matched directly by item instance instead of a Curios/Trinkets tag.
         */
        public static final LootCategory CHARM = register("charm", s -> s.getItem() instanceof dev.shadowsoffire.apotheosis.item.PotionCharmItem, EquipmentSlotGroup.ANY, 2600);
        public static final LootCategory NONE = register("none", Predicates.alwaysFalse(), EquipmentSlotGroup.ANY, Integer.MAX_VALUE);

        private static LootCategory register(String path, Predicate<ItemStack> filter, EquipmentSlotGroup slots, int priority) {
            return R.custom(path, BuiltInRegs.LOOT_CATEGORY, new LootCategory(filter, slots, priority));
        }

        private static LootCategory register(String path, Predicate<ItemStack> filter, EquipmentSlotGroup slots) {
            return register(path, filter, slots, 1000);
        }

        private static Predicate<ItemStack> armorSlot(EquipmentSlot slot) {
            return stack -> {
                if (stack.is(net.minecraft.world.item.Items.CARVED_PUMPKIN) || stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AbstractSkullBlock) {
                    return false;
                }

                EquipmentSlot itemSlot = null;
                Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
                if (equippable != null) {
                    itemSlot = equippable.slot();
                }

                return itemSlot == slot;
            };
        }

        /**
         * Port note: NeoForge's {@code Item.getDefaultAttributeModifiers(ItemStack)} no longer
         * exists (nor does any vanilla equivalent) — since 1.20.5+, an item's inherent default
         * attribute modifiers are already baked into its default {@code ATTRIBUTE_MODIFIERS}
         * component at registration time, so a plain component read already reflects them; no
         * separate fallback lookup is needed.
         */
        private static ItemAttributeModifiers getDefaultModifiers(ItemStack stack) {
            return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        }

        private static void bootstrap() {
            // Entries are plain Java constants registered above, once, at class-init time — this
            // just rebuilds LootCategory's priority-sorted lookup list after they're all in.
            // Port note: replaces the original's onBake(BakeCallback) registry hook — see
            // DeferredHelper.registry()'s javadoc for why that's unnecessary here.
            LootCategory.Inner.rebuildSortedValueList();
        }
    }

    /**
     * Port note: upstream's {@code Items} extends vanilla's own {@code net.minecraft.world.item.Items}
     * so both can be referenced together; kept that way here. Only {@link #GEM} is registered so
     * far — the rest (rarity materials, sigils, reforging/salvaging/cutting/augmenting table
     * items, potion charm, etc.) will be added once the {@code item} package is reached.
     */
    public static final class Items extends net.minecraft.world.item.Items {

        public static final Holder<net.minecraft.world.item.Item> GEM = R.item("gem", dev.shadowsoffire.apotheosis.socket.gem.GemItem::new);

        /**
         * Port note: only the subset of upstream's item list buildable from the ported
         * {@code item} package (5 files, minus boss-scoped {@code BossSummonerItem}) is
         * registered here. Not yet added: the rarity-tier salvage materials (need
         * {@code SalvageItem}, unported), spawner runes and {@code BOSS_SUMMONER} (boss/spawner
         * scope, excluded), and the smithing-template icon-list items (need
         * {@code SmithingTemplateItem} factory helpers, unported). The sigils below need no
         * dedicated behavior class upstream either — they're plain {@code TooltipItem}s, same as here.
         */
        public static final Holder<net.minecraft.world.item.Item> GOD_FUSED_PEARL = R.item("god_fused_pearl", dev.shadowsoffire.apotheosis.item.GlowyItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.EPIC));

        public static final Holder<net.minecraft.world.item.Item> GEM_DUST = R.item("gem_dust", net.minecraft.world.item.Item::new);

        public static final Holder<net.minecraft.world.item.Item> GEM_FUSED_SLATE = R.item("gem_fused_slate", net.minecraft.world.item.Item::new);

        public static final Holder<net.minecraft.world.item.Item> SIGIL_OF_SOCKETING = R.item("sigil_of_socketing", dev.shadowsoffire.apotheosis.item.TooltipItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

        public static final Holder<net.minecraft.world.item.Item> SIGIL_OF_WITHDRAWAL = R.item("sigil_of_withdrawal", dev.shadowsoffire.apotheosis.item.TooltipItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

        public static final Holder<net.minecraft.world.item.Item> SIGIL_OF_REBIRTH = R.item("sigil_of_rebirth", dev.shadowsoffire.apotheosis.item.TooltipItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

        public static final Holder<net.minecraft.world.item.Item> SIGIL_OF_ENHANCEMENT = R.item("sigil_of_enhancement", dev.shadowsoffire.apotheosis.item.TooltipItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

        public static final Holder<net.minecraft.world.item.Item> SIGIL_OF_UNNAMING = R.item("sigil_of_unnaming", dev.shadowsoffire.apotheosis.item.TooltipItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

        public static final Holder<net.minecraft.world.item.Item> SIGIL_OF_MALICE = R.item("sigil_of_malice", dev.shadowsoffire.apotheosis.item.TooltipItem::new, p -> p
            .component(DataComponents.ITEM_NAME, Apotheosis.lang("item", "sigil_of_malice").withStyle(net.minecraft.ChatFormatting.RED)));

        public static final Holder<net.minecraft.world.item.Item> SIGIL_OF_SUPREMACY = R.item("sigil_of_supremacy", dev.shadowsoffire.apotheosis.item.TooltipItem::new, p -> p
            .component(DataComponents.ITEM_NAME, Apotheosis.lang("item", "sigil_of_supremacy").withStyle(net.minecraft.ChatFormatting.GOLD)));

        public static final Holder<net.minecraft.world.item.Item> POTION_CHARM = R.item("potion_charm", dev.shadowsoffire.apotheosis.item.PotionCharmItem::new);

        /**
         * Port note: rarity-tier salvage materials, given out by the reforging/salvaging tables.
         * Keyed to rarities by id string (e.g. {@code "common"}) via {@code RarityRegistry} —
         * requires the matching rarity to actually exist in datapack JSON at runtime; the holder
         * itself resolves lazily so registration order doesn't matter.
         */
        public static final Holder<net.minecraft.world.item.Item> MYSTERIOUS_SCRAP_METAL = rarityMat("mysterious_scrap_metal", "common");

        public static final Holder<net.minecraft.world.item.Item> TIMEWORN_FABRIC = rarityMat("timeworn_fabric", "uncommon");

        public static final Holder<net.minecraft.world.item.Item> LUMINOUS_CRYSTAL_SHARD = rarityMat("luminous_crystal_shard", "rare");

        public static final Holder<net.minecraft.world.item.Item> ARCANE_SANDS = rarityMat("arcane_sands", "epic");

        public static final Holder<net.minecraft.world.item.Item> GODFORGED_PEARL = rarityMat("godforged_pearl", "mythic");

        private static Holder<net.minecraft.world.item.Item> rarityMat(String registryName, String rarityId) {
            return R.item(registryName, p -> new dev.shadowsoffire.apotheosis.affix.salvaging.SalvageItem(RarityRegistry.INSTANCE.holder(Apotheosis.loc(rarityId)), p));
        }

        /**
         * Port note: upstream uses {@code TooltipBlockItem} for these too (same placeholder-swap
         * note as {@code GEM_CUTTING_TABLE} above).
         */
        public static final Holder<net.minecraft.world.item.Item> SIMPLE_REFORGING_TABLE = R.blockItem("simple_reforging_table", Blocks.SIMPLE_REFORGING_TABLE, dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableBlockItem::new, java.util.function.UnaryOperator.identity());

        public static final Holder<net.minecraft.world.item.Item> REFORGING_TABLE = R.blockItem("reforging_table", Blocks.REFORGING_TABLE, dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableBlockItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.EPIC));

        public static final Holder<net.minecraft.world.item.Item> SALVAGING_TABLE = R.blockItem("salvaging_table", Blocks.SALVAGING_TABLE, java.util.function.UnaryOperator.identity());

        public static final Holder<net.minecraft.world.item.Item> AUGMENTING_TABLE = R.blockItem("augmenting_table", Blocks.AUGMENTING_TABLE, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

        /**
         * Port note: upstream uses a {@code TooltipBlockItem} (an {@code item} package class not
         * yet ported — see the {@code item} package's own status). Plain {@link net.minecraft.world.item.BlockItem}
         * is used here as a placeholder; swap once {@code TooltipBlockItem} exists.
         */
        public static final Holder<net.minecraft.world.item.Item> GEM_CUTTING_TABLE = R.blockItem("gem_cutting_table", Blocks.GEM_CUTTING_TABLE, java.util.function.UnaryOperator.identity());

        public static final Holder<net.minecraft.world.item.Item> GEM_CASE = R.blockItem("gem_case", Blocks.GEM_CASE, dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseBlockItem::new, java.util.function.UnaryOperator.identity());

        public static final Holder<net.minecraft.world.item.Item> ENDER_GEM_CASE = R.blockItem("ender_gem_case", Blocks.ENDER_GEM_CASE, dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseBlockItem::new, java.util.function.UnaryOperator.identity());

        private static void bootstrap() {}
    }

    /**
     * Port note: only {@link #GEM_CUTTING_TABLE} is registered so far, for the {@code socket.gem.cutting}
     * package. The remaining tables ({@code reforging}, {@code salvaging}, {@code augmenting}) and
     * the {@code gem_case}/{@code ender_gem_case} storage blocks belong to packages not yet ported.
     * <p>
     * Note: this block has no blockstate/model/loot-table/texture assets yet — those need to be
     * authored (or copied from upstream's resource pack, if license permits) before it can
     * actually be placed and rendered in-world. The Java-side logic is complete and compiles.
     */
    public static final class Blocks {

        public static final Holder<net.minecraft.world.level.block.Block> GEM_CUTTING_TABLE = R.block("gem_cutting_table", dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingBlock::new,
            p -> p.sound(net.minecraft.world.level.block.SoundType.WOOD).strength(2.5F));

        public static final Holder<net.minecraft.world.level.block.Block> GEM_CASE = R.block("gem_case", p -> new dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseBlock(dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.BasicGemCaseTile::new, p, Short.MAX_VALUE),
            p -> p.requiresCorrectToolForDrops().strength(5, 1200F).sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion().lightLevel(s -> 2));

        public static final Holder<net.minecraft.world.level.block.Block> ENDER_GEM_CASE = R.block("ender_gem_case", p -> new dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseBlock(dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.EnderGemCaseTile::new, p, Integer.MAX_VALUE),
            p -> p.requiresCorrectToolForDrops().strength(5, 1200F).sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion().lightLevel(s -> 2));

        public static final Holder<net.minecraft.world.level.block.Block> SIMPLE_REFORGING_TABLE = R.block("simple_reforging_table", dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableBlock::new, p -> p.requiresCorrectToolForDrops().strength(2, 20F));

        public static final Holder<net.minecraft.world.level.block.Block> REFORGING_TABLE = R.block("reforging_table", dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableBlock::new, p -> p.requiresCorrectToolForDrops().strength(4, 1000F));

        public static final Holder<net.minecraft.world.level.block.Block> SALVAGING_TABLE = R.block("salvaging_table", dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingTableBlock::new, p -> p.sound(net.minecraft.world.level.block.SoundType.WOOD).strength(2.5F));

        public static final Holder<net.minecraft.world.level.block.Block> AUGMENTING_TABLE = R.block("augmenting_table", dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingTableBlock::new, p -> p.requiresCorrectToolForDrops().strength(4, 1000F));

        private static void bootstrap() {}
    }

    public static final class Tiles {

        public static final net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.BasicGemCaseTile> GEM_CASE =
            (net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.BasicGemCaseTile>) (Object)
                R.blockEntity("gem_case", (pos, state) -> new dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.BasicGemCaseTile(pos, state), Blocks.GEM_CASE).value();

        public static final net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.EnderGemCaseTile> ENDER_GEM_CASE =
            (net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.EnderGemCaseTile>) (Object)
                R.blockEntity("ender_gem_case", (pos, state) -> new dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTile.EnderGemCaseTile(pos, state), Blocks.ENDER_GEM_CASE).value();

        public static final net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableTile> REFORGING_TABLE =
            (net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableTile>) (Object)
                R.blockEntity("reforging_table", (pos, state) -> new dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableTile(pos, state), Blocks.SIMPLE_REFORGING_TABLE, Blocks.REFORGING_TABLE).value();

        public static final net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingTableTile> SALVAGING_TABLE =
            (net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingTableTile>) (Object)
                R.blockEntity("salvaging_table", (pos, state) -> new dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingTableTile(pos, state), Blocks.SALVAGING_TABLE).value();

        public static final net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingTableTile> AUGMENTING_TABLE =
            (net.minecraft.world.level.block.entity.BlockEntityType<dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingTableTile>) (Object)
                R.blockEntity("augmenting_table", (pos, state) -> new dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingTableTile(pos, state), Blocks.AUGMENTING_TABLE).value();

        private static void bootstrap() {}
    }

    public static final class Menus {

        public static final net.minecraft.world.inventory.MenuType<dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingMenu> GEM_CUTTING = R.menu("gem_cutting", dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingMenu::new);

        public static final net.minecraft.world.inventory.MenuType<dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseMenu> GEM_CASE = R.menuWithPos("gem_case", dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseMenu::new);

        public static final net.minecraft.world.inventory.MenuType<dev.shadowsoffire.apotheosis.affix.reforging.ReforgingMenu> REFORGING = R.menuWithPos("reforging", dev.shadowsoffire.apotheosis.affix.reforging.ReforgingMenu::new);

        public static final net.minecraft.world.inventory.MenuType<dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu> SALVAGE = R.menuWithPos("salvage", dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu::new);

        public static final net.minecraft.world.inventory.MenuType<dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingMenu> AUGMENTING = R.menuWithPos("augmenting", dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingMenu::new);

        private static void bootstrap() {}
    }

    public static final class RecipeTypes {

        public static final net.minecraft.world.item.crafting.RecipeType<dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingRecipe> GEM_CUTTING = R.recipe("gem_cutting");

        public static final net.minecraft.world.item.crafting.RecipeType<dev.shadowsoffire.apotheosis.affix.reforging.ReforgingRecipe> REFORGING = R.recipe("reforging");

        public static final net.minecraft.world.item.crafting.RecipeType<dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingRecipe> SALVAGING = R.recipe("salvaging");

        private static void bootstrap() {}
    }

    public static final class Sounds {

        public static final net.minecraft.sounds.SoundEvent REFORGE_ITEM_REFORGED = R.sound("reforge_item_reforged");

        public static final net.minecraft.sounds.SoundEvent REFORGE_ITEM_PLACED = R.sound("reforge_item_placed");

        public static final net.minecraft.sounds.SoundEvent MALICE = R.sound("malice");

        // Port note: found via actually launching a dev server — data/apotheosis/apotheosis/rarities/*.json
        // (ported verbatim from upstream) references these 4 by id in their (non-optional-in-practice,
        // strictly-decoded-when-present) "invader_sound" field. Without the SoundEvent itself registered,
        // the whole rarity tier fails to parse (registry-key resolution, not a missing-audio-file issue —
        // same as the other custom sounds above, no .ogg asset exists yet, that's cosmetic-only).
        public static final net.minecraft.sounds.SoundEvent INVADER_UNCOMMON = R.sound("invader_uncommon");

        public static final net.minecraft.sounds.SoundEvent INVADER_RARE = R.sound("invader_rare");

        public static final net.minecraft.sounds.SoundEvent INVADER_EPIC = R.sound("invader_epic");

        public static final net.minecraft.sounds.SoundEvent INVADER_MYTHIC = R.sound("invader_mythic");

        private static void bootstrap() {}
    }

    public static final class RecipeSerializers {

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> BASIC_GEM_CUTTING = R.recipeSerializer("basic_gem_cutting", () -> dev.shadowsoffire.apotheosis.socket.gem.cutting.BasicGemCuttingRecipe.SERIALIZER);

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> REFORGING = R.recipeSerializer("reforging", () -> dev.shadowsoffire.apotheosis.affix.reforging.ReforgingRecipe.SERIALIZER);

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> SALVAGING = R.recipeSerializer("salvaging", () -> dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingRecipe.SERIALIZER);

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> PURITY_UPGRADE = R.recipeSerializer("purity_upgrade", () -> dev.shadowsoffire.apotheosis.socket.gem.cutting.PurityUpgradeRecipe.SERIALIZER);

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> ADD_SOCKETS = R.recipeSerializer("add_sockets", () -> dev.shadowsoffire.apotheosis.socket.AddSocketsRecipe.SERIALIZER);

        /**
         * Port note: {@code SocketingRecipe} carries no per-instance data (the base/addition
         * matching is hardcoded), so both codec halves are {@code .unit(...)} — same shape as
         * upstream's own use of a stateless single-instance recipe serializer.
         */
        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> SOCKETING = R.recipeSerializer("socketing", () -> new net.minecraft.world.item.crafting.RecipeSerializer<>(
            com.mojang.serialization.MapCodec.unit(dev.shadowsoffire.apotheosis.socket.SocketingRecipe::new),
            net.minecraft.network.codec.StreamCodec.unit(new dev.shadowsoffire.apotheosis.socket.SocketingRecipe())));

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> WITHDRAWAL = R.recipeSerializer("withdrawal", () -> new net.minecraft.world.item.crafting.RecipeSerializer<>(
            com.mojang.serialization.MapCodec.unit(dev.shadowsoffire.apotheosis.socket.WithdrawalRecipe::new),
            net.minecraft.network.codec.StreamCodec.unit(new dev.shadowsoffire.apotheosis.socket.WithdrawalRecipe())));

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> MALICE = R.recipeSerializer("malice", () -> new net.minecraft.world.item.crafting.RecipeSerializer<>(
            com.mojang.serialization.MapCodec.unit(dev.shadowsoffire.apotheosis.recipe.MaliceRecipe::new),
            net.minecraft.network.codec.StreamCodec.unit(new dev.shadowsoffire.apotheosis.recipe.MaliceRecipe())));

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> SUPREMACY = R.recipeSerializer("supremacy", () -> new net.minecraft.world.item.crafting.RecipeSerializer<>(
            com.mojang.serialization.MapCodec.unit(dev.shadowsoffire.apotheosis.recipe.SupremacyRecipe::new),
            net.minecraft.network.codec.StreamCodec.unit(new dev.shadowsoffire.apotheosis.recipe.SupremacyRecipe())));

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> POTION_CHARM_CRAFTING = R.recipeSerializer("potion_charm_crafting", () -> dev.shadowsoffire.apotheosis.recipe.PotionCharmRecipe.SERIALIZER);

        public static final Holder<net.minecraft.world.item.crafting.RecipeSerializer<?>> UNNAMING = R.recipeSerializer("unnaming", () -> new net.minecraft.world.item.crafting.RecipeSerializer<>(
            com.mojang.serialization.MapCodec.unit(dev.shadowsoffire.apotheosis.affix.UnnamingRecipe::new),
            net.minecraft.network.codec.StreamCodec.unit(new dev.shadowsoffire.apotheosis.affix.UnnamingRecipe())));

        private static void bootstrap() {}
    }

    public static void bootstrap() {
        BuiltInRegs.bootstrap();
        LootCategories.bootstrap();
        Components.bootstrap();
        Items.bootstrap();
        Blocks.bootstrap();
        Tiles.bootstrap();
        Menus.bootstrap();
        RecipeTypes.bootstrap();
        RecipeSerializers.bootstrap();
        Sounds.bootstrap();
        Stats.bootstrap();
        Triggers.bootstrap();
        DataComponentPredicates.bootstrap();
        CustomAttributes.bootstrap();
    }

}
