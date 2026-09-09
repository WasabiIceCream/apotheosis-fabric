package dev.shadowsoffire.apotheosis.loot.modifiers;

import dev.shadowsoffire.apotheosis.tiers.GenContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class ContextualLootModifier extends GlobalLootModifier {

    protected ContextualLootModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected final ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        var gCtx = GenContext.forLoot(ctx);
        if (gCtx != null) {
            return this.doApply(loot, ctx, gCtx);
        }
        return loot;
    }

    protected abstract ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx, GenContext gCtx);

}
