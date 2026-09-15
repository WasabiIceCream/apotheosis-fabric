package dev.shadowsoffire.apotheosis.loot.modifiers;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.util.LootPatternMatcher;
import dev.shadowsoffire.apotheosis.util.LootTableQueryTracker;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A server-specific (non-upstream) global loot modifier: gives already-generated, still-unenchanted
 * items in a matching loot table an independent chance to roll a random enchantment — including
 * vanilla's "treasure" enchantments (Curse of Binding, Curse of Vanishing, Frost Walker, Mending),
 * which normally only come from villager trading. Added because this server disables villagers
 * entirely (see {@code docs/current-state.md}'s 2026-09-15 "no villagers, period" entries), which
 * otherwise makes those four enchantments unobtainable — Mending/Frost Walker already have other
 * sources in this modpack, but Curse of Binding/Vanishing genuinely didn't.
 * <p>
 * Deliberately reuses vanilla's own {@link EnchantRandomlyFunction} (via
 * {@link EnchantRandomlyFunction#randomApplicableEnchantment}, the same helper vanilla's own loot
 * tables use for "any enchantment appropriate for this item") rather than reimplementing
 * enchantment-compatibility/leveling logic — this means the enchantment pool is whatever's tagged
 * {@code #minecraft:on_random_loot} in the live registry at generation time, so any enchantment any
 * currently-installed mod adds and tags accordingly is automatically eligible too, with zero
 * per-mod special-casing. Only ever rolls on items {@link ItemStack#isEnchantable()} reports true
 * for (has an {@code ENCHANTABLE} component and no existing vanilla enchantments yet) — an
 * Apotheosis-affixed item is untouched by this check since affixes are a separate data component,
 * not a vanilla enchantment.
 */
public class RandomEnchantLootModifier extends ContextualLootModifier {

    public static final MapCodec<RandomEnchantLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
        .and(TableEntry.CODEC.listOf().fieldOf("entries").forGetter(g -> g.entries))
        .apply(inst, RandomEnchantLootModifier::new));

    protected final List<TableEntry> entries;

    public RandomEnchantLootModifier(LootItemCondition[] conditions, int priority, List<TableEntry> entries) {
        super(conditions, priority);
        this.entries = entries;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext ctx, GenContext gCtx) {
        Identifier queriedTable = LootTableQueryTracker.current();
        if (queriedTable == null) {
            return generatedLoot;
        }
        for (TableEntry entry : this.entries) {
            if (entry.pattern().matches(queriedTable)) {
                LootItemFunction fn = EnchantRandomlyFunction.randomApplicableEnchantment(ctx.getLevel().registryAccess()).build();
                for (int i = 0; i < generatedLoot.size(); i++) {
                    ItemStack stack = generatedLoot.get(i);
                    if (stack.isEnchantable() && ctx.getRandom().nextFloat() <= entry.chance()) {
                        generatedLoot.set(i, fn.apply(stack, ctx));
                    }
                }
                break;
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends GlobalLootModifier> getCodec() {
        return CODEC.codec();
    }

    /**
     * @param pattern The loot pattern matcher that determines which tables this entry applies to.
     * @param chance  The independent, per-eligible-item chance of rolling a random enchantment, when a table is matched.
     */
    public static record TableEntry(LootPatternMatcher pattern, float chance) {

        public static final Codec<TableEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            LootPatternMatcher.CODEC.fieldOf("pattern").forGetter(TableEntry::pattern),
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(TableEntry::chance))
            .apply(inst, TableEntry::new));

    }
}
