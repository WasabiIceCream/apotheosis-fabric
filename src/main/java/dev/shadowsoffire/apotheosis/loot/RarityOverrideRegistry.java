package dev.shadowsoffire.apotheosis.loot;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;

import dev.shadowsoffire.apotheosis.Apoth.BuiltInRegs;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.placebo.dynreg.DynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;
import net.minecraft.resources.Identifier;

public class RarityOverrideRegistry extends DynamicRegistry<RarityOverride> {

    public static final RarityOverrideRegistry INSTANCE = new RarityOverrideRegistry();

    protected Map<LootCategory, RarityOverride> byCategory = new HashMap<>();

    public RarityOverrideRegistry() {
        super(Apotheosis.LOGGER, Apotheosis.loc("rarity_override"), RegistrySerializer.synced(RarityOverride.CODEC));
    }

    // Port note: found by actually launching a dev server — same class of bug as AffixRegistry's
    // override (see its javadoc). RarityOverride entries reference DynamicHolder<LootRarity>
    // directly, so this must wait for RarityRegistry to finish applying first.
    @Override
    public java.util.Collection<net.minecraft.resources.Identifier> getFabricDependencies() {
        java.util.Set<net.minecraft.resources.Identifier> deps = new java.util.HashSet<>(super.getFabricDependencies());
        deps.add(RarityRegistry.INSTANCE.getFabricId());
        return deps;
    }

    @Nullable
    public RarityOverride getOverride(LootCategory category) {
        return this.byCategory.get(category);
    }

    @Override
    protected void validateItem(Identifier key, RarityOverride value) {
        String path = key.getPath().replace('/', ':');
        Identifier cat = Identifier.tryParse(path);
        Preconditions.checkNotNull(cat, "Invalid category path: " + path);
        LootCategory category = BuiltInRegs.LOOT_CATEGORY.getValue(cat);
        Preconditions.checkArgument(category != null && !category.isNone(), "Category not found: " + cat);
        Preconditions.checkArgument(value.category() == category, "Category mismatch: " + value.category() + " != " + category);
    }

    // Port note: found live (same class of bug as AffixRegistry — see its onReload() javadoc).
    // No longer resets byCategory in beginReload(); rebuilds off to the side and atomically
    // swaps at the end of onReload() instead, so a concurrent getOverride() read never sees an
    // empty/partially-rebuilt map mid-reload.
    @Override
    protected void onReload(ReloadType type) {
        super.onReload(type);
        Map<LootCategory, RarityOverride> rebuilt = new HashMap<>();
        this.registry.forEach((key, value) -> {
            String path = key.getPath().replace('/', ':');
            Identifier cat = Identifier.tryParse(path);
            LootCategory category = BuiltInRegs.LOOT_CATEGORY.getValue(cat);
            RarityOverride old = rebuilt.put(category, value);
            if (old != null) {
                this.logger.warn("Duplicate rarity override for category {}: Old: {}, New: {}", path, this.getKey(old), key);
            }
        });
        this.byCategory = rebuilt;
    }

}
