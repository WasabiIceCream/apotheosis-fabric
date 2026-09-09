package dev.shadowsoffire.apotheosis.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.tiers.TieredDynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Handles loading the configurable portion of rarities.
 */
public class RarityRegistry extends TieredDynamicRegistry<LootRarity> {

    public static final RarityRegistry INSTANCE = new RarityRegistry();

    protected BiMap<Item, DynamicHolder<LootRarity>> materialMap = HashBiMap.create();
    protected List<LootRarity> sorted = new ArrayList<>();

    private RarityRegistry() {
        super(Apotheosis.LOGGER, Apotheosis.loc("rarities"), RegistrySerializer.synced(LootRarity.LOAD_CODEC));
    }

    /**
     * Checks if a given item is a rarity material.
     *
     * @param stack The item being checked.
     * @return True if the item is a rarity material.
     */
    public static boolean isMaterial(Item item) {
        return getMaterialRarity(item).isBound();
    }

    /**
     * Returns the rarity associated with the passed rarity material.
     * <p>
     * May be unbound.
     */
    public static DynamicHolder<LootRarity> getMaterialRarity(Item item) {
        return INSTANCE.materialMap.getOrDefault(item, INSTANCE.emptyHolder());
    }

    public static List<LootRarity> getSortedRarities() {
        return Collections.unmodifiableList(INSTANCE.sorted);
    }

    // Port note: found live (same class of bug as AffixRegistry — see its onReload() javadoc).
    // No longer resets materialMap/sorted in beginReload(); rebuilds off to the side and
    // atomically swaps both at the end of onReload() instead, so a concurrent
    // isMaterial()/getMaterialRarity()/getSortedRarities() read never sees an empty or
    // partially-rebuilt state mid-reload.
    @Override
    protected void onReload(ReloadType type) {
        super.onReload(type);
        BiMap<Item, DynamicHolder<LootRarity>> rebuiltMaterialMap = HashBiMap.create();
        List<LootRarity> rebuiltSorted = new ArrayList<>();
        for (LootRarity r : this.getValues()) {
            DynamicHolder<LootRarity> old = rebuiltMaterialMap.put(r.getMaterial(), this.holder(r));
            if (old != null) {
                throw new RuntimeException("Two rarities may not share the same rarity material: " + this.getKey(r) + " conflicts with " + old.getId());
            }
            rebuiltSorted.add(r);
        }
        rebuiltSorted.sort(Comparator.comparing(LootRarity::sortIndex));
        this.materialMap = rebuiltMaterialMap;
        this.sorted = rebuiltSorted;
    }

    @Override
    protected void validateItem(Identifier key, LootRarity item) {
        super.validateItem(key, item);
        Preconditions.checkNotNull(item.color());
        Preconditions.checkArgument(item.getMaterial() != null && item.getMaterial() != Items.AIR);
        Preconditions.checkArgument(!item.rules().isEmpty(), "A rarity must provide base rules.");
    }
}
