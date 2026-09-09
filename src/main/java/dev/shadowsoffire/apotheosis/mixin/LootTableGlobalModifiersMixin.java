package dev.shadowsoffire.apotheosis.mixin;

import java.util.ArrayDeque;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.loot.modifiers.GlobalLootModifier;
import dev.shadowsoffire.apotheosis.loot.modifiers.GlobalLootModifierRegistry;
import dev.shadowsoffire.apotheosis.util.LootTableQueryTracker;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Fabric-native replacement for NeoForge's {@code IGlobalLootModifier} application hook — see
 * {@code loot.modifiers.GlobalLootModifier}'s javadoc for the full "why."
 * <p>
 * Targets {@code LootTable#getRandomItemsRaw(LootContext, Consumer)} — confirmed via bytecode
 * inspection to be the single method every other public roll entry point
 * ({@code getRandomItemsRaw(LootParams, Consumer)}, all {@code getRandomItems} overloads)
 * ultimately funnels through exactly once per real drop/chest-fill/trade query. It is also the
 * method used internally for loot-table <em>references</em> (one table's pool entry pointing at
 * another table), which reuse the caller's existing {@code LootContext} directly — meaning a
 * single top-level query can recurse into this method multiple times. Global loot modifiers
 * must only see the complete, fully-assembled item list for the outermost query, not run once
 * per referenced sub-table, so a per-thread depth counter tracks reentrancy: only the depth-0
 * (outermost) invocation buffers items and applies modifiers; nested calls pass their consumer
 * straight through unmodified. Because {@code LootItemFunction#decorate} composes consumers by
 * delegation, items produced by a referenced sub-table still ultimately flow into the depth-0
 * buffer — so the buffer genuinely accumulates the full result set for the whole query,
 * sub-tables included, ready for modifiers to see it exactly once.
 * <p>
 * Also resolves and pushes the queried table's id (via {@link LootTableQueryTracker}) for the
 * duration of the outermost call — see that class's javadoc for why this is needed at all
 * (vanilla's own {@code LootContext#getQueriedLootTableId()} no longer exists in this version).
 * The id is resolved via a reverse {@code Registry<LootTable>#getKey} lookup, since a
 * {@code LootTable} instance carries no self-reference to its own registry key.
 */
@Mixin(LootTable.class)
public class LootTableGlobalModifiersMixin {

    @Unique
    private static final ThreadLocal<Integer> apoth$depth = ThreadLocal.withInitial(() -> 0);

    @Unique
    private static final ThreadLocal<ArrayDeque<Object[]>> apoth$frames = ThreadLocal.withInitial(ArrayDeque::new);

    @ModifyVariable(
        method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"), argsOnly = true, index = 2)
    private Consumer<ItemStack> apoth$wrapConsumer(Consumer<ItemStack> original, LootContext ctx) {
        int depth = apoth$depth.get();
        apoth$depth.set(depth + 1);
        if (depth != 0) {
            return original;
        }
        ObjectArrayList<ItemStack> buffer = new ObjectArrayList<>();
        apoth$frames.get().push(new Object[] { buffer, original });
        LootTableQueryTracker.push(apoth$resolveTableId(ctx));
        return buffer::add;
    }

    @Inject(
        method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
        at = @At("RETURN"))
    private void apoth$flushAndApplyModifiers(LootContext ctx, Consumer<ItemStack> consumer, CallbackInfo ci) {
        int depth = apoth$depth.get() - 1;
        apoth$depth.set(depth);
        if (depth != 0) {
            return;
        }
        Object[] frame = apoth$frames.get().pop();
        @SuppressWarnings("unchecked")
        ObjectArrayList<ItemStack> buffer = (ObjectArrayList<ItemStack>) frame[0];
        @SuppressWarnings("unchecked")
        Consumer<ItemStack> original = (Consumer<ItemStack>) frame[1];

        try {
            ObjectArrayList<ItemStack> result = buffer;
            for (GlobalLootModifier modifier : GlobalLootModifierRegistry.INSTANCE.getOrdered()) {
                result = modifier.apply(result, ctx);
            }
            result.forEach(original);
        }
        finally {
            LootTableQueryTracker.pop();
        }
    }

    /**
     * Port bug found via live runtime testing: {@code Level#registryAccess()} is the level's
     * static/datapack {@code RegistryAccess} — loot tables are not part of that tree at all in
     * this version. They live in {@code MinecraftServer#reloadableRegistries()}'s own separate
     * {@code HolderLookup.Provider} (confirmed by decompiling vanilla's own
     * {@code RandomizableContainer#unpackLootTable}, which fetches loot tables via
     * {@code server.reloadableRegistries().getLootTable(key)}, never via
     * {@code level.registryAccess()}). The original code compiled fine and silently returned
     * null from every single query — including chest opens — because the lookup it queried was
     * simply the wrong one (empty of loot tables), not because the target table was missing or
     * the cast/logic was broken. Confirmed live: added temporary diagnostic logging and every
     * single {@code getRandomItemsRaw} invocation during real gameplay (chest opens included)
     * resolved to {@code queriedTable=null}, silently disabling every {@code GlobalLootModifier}
     * — including the one injecting affixed gear into chest loot — for the entire session.
     * <p>
     * {@code HolderLookup.RegistryLookup} (what {@code reloadableRegistries().lookup()} actually
     * returns per-registry) has no {@code getKey(T)}-style reverse lookup either — unlike a
     * classic {@code Registry<T>}, it only supports forward lookup by key. So this still has to
     * scan {@link HolderLookup#listElements()} for the {@link Holder.Reference} whose value is
     * this exact instance. Loot table counts are in the low hundreds, and this only runs once
     * per outermost (depth-0) query, so the linear scan is cheap enough for gameplay use.
     */
    @Unique
    private Identifier apoth$resolveTableId(LootContext ctx) {
        try {
            MinecraftServer server = ctx.getLevel().getServer();
            HolderLookup.RegistryLookup<LootTable> lookup = server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE);
            LootTable self = (LootTable) (Object) this;
            return lookup.listElements()
                .filter(ref -> ref.value() == self)
                .findFirst()
                .map(ref -> ref.key().identifier())
                .orElse(null);
        }
        catch (Exception e) {
            return null;
        }
    }

}
