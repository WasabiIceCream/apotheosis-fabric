package dev.shadowsoffire.apotheosis.util;

import java.util.ArrayDeque;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

/**
 * Port note: vanilla's {@code LootContext} no longer tracks "which loot table id is currently
 * being queried" at all in this version — confirmed via javap ({@code getQueriedLootTableId()}
 * and everything like it is simply gone from {@code LootContext}/{@code LootParams}/
 * {@code LootTable}). Loot tables are registry entries now ({@code LootTable.CODEC} is a
 * {@code Codec<Holder<LootTable>>}), but a {@code LootTable} instance carries no self-reference
 * to its own key, so there's no direct way to ask "what's my id" from inside one either.
 * <p>
 * This is a small thread-local stack, pushed/popped by
 * {@code mixin.LootTableGlobalModifiersMixin} around the outermost roll of each real
 * drop/chest-fill/trade query (using a reverse {@code Registry<LootTable>#getKey} lookup to
 * resolve the id once, at that single point), and read by anything that used to call
 * {@code LootContext#getQueriedLootTableId()} — {@code util.LootPatternMatcher} and the
 * {@code loot.modifiers.*} classes.
 */
public final class LootTableQueryTracker {

    private static final ThreadLocal<ArrayDeque<Identifier>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Sentinel pushed in place of a null id. {@code java.util.ArrayDeque} rejects null elements
     * outright (throws NPE from {@code addFirst}), but callers here legitimately need to push
     * "unresolved" (e.g. a synthetic loot table from {@code /loot} or a datapack function, which
     * has no registry key to reverse-lookup) — found live when every loot roll on the server
     * started throwing NPE the moment one of those ran. Translated back to null at the
     * {@link #current()} boundary so external behavior is unchanged.
     */
    private static final Identifier UNRESOLVED = Identifier.fromNamespaceAndPath("apotheosis", "unresolved_loot_table_query");

    private LootTableQueryTracker() {}

    public static void push(@Nullable Identifier id) {
        STACK.get().push(id == null ? UNRESOLVED : id);
    }

    public static void pop() {
        ArrayDeque<Identifier> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /**
     * @return The id of the loot table at the top of the outermost currently-in-progress query on
     *         this thread, or null if none is in progress (or its id could not be resolved).
     */
    @Nullable
    public static Identifier current() {
        Identifier id = STACK.get().peek();
        return id == UNRESOLVED ? null : id;
    }

}
