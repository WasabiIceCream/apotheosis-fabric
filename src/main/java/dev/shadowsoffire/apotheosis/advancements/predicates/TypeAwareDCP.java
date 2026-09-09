package dev.shadowsoffire.apotheosis.advancements.predicates;

import net.minecraft.core.component.predicates.DataComponentPredicate;

/** Port of NeoForge's {@code TypeAwareDCP} — no NeoForge coupling upstream, ported unchanged. */
public interface TypeAwareDCP<T extends DataComponentPredicate> extends DataComponentPredicate {

    DataComponentPredicate.Type<T> type();

}
