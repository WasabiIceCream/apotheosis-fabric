package dev.shadowsoffire.apotheosis.loot.modifiers;

import java.util.Arrays;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.CodecProvider;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Port note (NeoForge -> Fabric): Fabric ships no equivalent of NeoForge's
 * {@code IGlobalLootModifier}/{@code LootModifier} (a post-generation hook that mutates the
 * complete, already-rolled item list of an arbitrary loot table, filtered by json-configured
 * conditions — <i>not</i> the same thing as the "Loot Table Modifier" mod dependency used
 * elsewhere in this port, which edits a table's pool/entry structure at build time, before any
 * roll happens). This is a from-scratch replacement, mirroring NeoForge's own shape closely
 * enough that {@code loot/modifiers/*} port with only their {@code codec()} return type and
 * import changed:
 * <ul>
 * <li>Entries are loaded from {@code data/<ns>/loot_modifier/*.json} via
 * {@link GlobalLootModifierRegistry}, a {@code placebo} {@code DynamicRegistry} using a
 * type-keyed {@code SubtypedSerializer} (the same polymorphic-codec pattern already used for
 * {@code Affix}/{@code GemBonus}), instead of NeoForge's {@code neoforge:loot_modifier_type}
 * registry.</li>
 * <li>Application happens via {@code mixin.LootTableGlobalModifiersMixin}, which wraps the
 * item consumer of {@code LootTable#getRandomItemsRaw(LootParams, Consumer)} — the single
 * entry point that every real drop/chest-fill/trade call funnels through exactly once per
 * query, with recursive loot-table references (which reuse the same {@code LootContext}
 * overload) excluded via a depth-tracked reentrancy guard so modifiers apply once per query,
 * not once per referenced sub-table. See that mixin's javadoc for the full reasoning.</li>
 * </ul>
 */
public abstract class GlobalLootModifier implements CodecProvider<GlobalLootModifier> {

    protected final LootItemCondition[] conditions;
    protected final int priority;

    protected GlobalLootModifier(LootItemCondition[] conditions, int priority) {
        this.conditions = conditions;
        this.priority = priority;
    }

    /**
     * Tests this modifier's conditions and, if they pass, applies it to the loot list.
     */
    public final ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(ctx)) {
                return loot;
            }
        }
        return this.doApply(loot, ctx);
    }

    protected abstract ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx);

    public final int priority() {
        return this.priority;
    }

    public abstract Codec<? extends GlobalLootModifier> getCodec();

    /**
     * Shared (conditions, priority) codec fields, for a subclass to build on via
     * {@code codecStart(inst).and(extraFields).apply(inst, Ctor::new)} — same idiom as
     * upstream's own {@code LootModifier.codecStart}.
     */
    protected static <T extends GlobalLootModifier> Products.P2<RecordCodecBuilder.Mu<T>, LootItemCondition[], Integer> codecStart(RecordCodecBuilder.Instance<T> inst) {
        return inst.group(
            LootItemCondition.DIRECT_CODEC.listOf()
                .xmap(l -> l.toArray(new LootItemCondition[0]), Arrays::asList)
                .optionalFieldOf("conditions", new LootItemCondition[0])
                .forGetter(m -> m.conditions),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(m -> m.priority));
    }

}
