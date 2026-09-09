package dev.shadowsoffire.apotheosis.tiers.augments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment.Target;
import dev.shadowsoffire.placebo.dynreg.DynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;
import dev.shadowsoffire.placebo.dynreg.SubtypedSerializer;

public final class TierAugmentRegistry extends DynamicRegistry<TierAugment> {

    /**
     * Public serializer so external mods can register additional TierAugment subtypes during their setup phase.
     */
    public static final SubtypedSerializer<TierAugment> SERIALIZER = RegistrySerializer.<TierAugment>subtypedSynced("tier_augments")
        .register(Apotheosis.loc("attribute"), AttributeAugment.CODEC);

    public static TierAugmentRegistry INSTANCE = new TierAugmentRegistry();

    protected Map<Key, List<TierAugment>> augmentsPerTier = new HashMap<>();

    private TierAugmentRegistry() {
        super(Apotheosis.LOGGER, Apotheosis.loc("tier_augments"), SERIALIZER);
    }

    // Port note: found live (same class of bug as AffixRegistry — see its onReload() javadoc).
    // This used to .clear() augmentsPerTier in beginReload() and then rebuild it in place via
    // computeIfAbsent() in onReload(), leaving a window (and, worse, a non-thread-safe in-place
    // HashMap mutation) where a concurrent getAugments() read — e.g. from MobSpawnAugmentMixin on
    // a mob-spawn thread — could see a partially-empty or mid-mutation map. Fixed the same way:
    // build the new map fully off to the side, then atomically swap the field in one assignment.
    @Override
    protected void onReload(ReloadType type) {
        super.onReload(type);
        Map<Key, List<TierAugment>> rebuilt = new HashMap<>();
        for (TierAugment aug : this.registry.values()) {
            rebuilt.computeIfAbsent(new Key(aug.tier(), aug.target()), t -> new ArrayList<>()).add(aug);
        }
        for (List<TierAugment> augList : rebuilt.values()) {
            augList.sort(Comparator.comparing(TierAugment::sortIndex));
        }
        this.augmentsPerTier = rebuilt;
    }

    /**
     * Returns a list of all augments for the target tier.
     * <p>
     * This list may be empty.
     */
    public static List<TierAugment> getAugments(WorldTier tier, Target target) {
        Key key = new Key(tier, target);
        return Collections.unmodifiableList(INSTANCE.augmentsPerTier.getOrDefault(key, List.of()));
    }

    private record Key(WorldTier tier, Target target) {}

}
