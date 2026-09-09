package dev.shadowsoffire.apotheosis.tiers;

import java.util.Arrays;
import java.util.Map;
import java.util.function.IntFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment.Target;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugmentRegistry;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;

/**
 * World Tiers for Apothic content, each increasing the quality of loot received and the overall strength of the monsters in the world.
 * <p>
 * The final tier, Apotheosis, allows for endlessly increasing the difficulty in exchange for additional rewards, but does not
 * adjust weights or availability of content.
 * <p>
 * Port note (NeoForge -> Fabric): the player's current tier is now stored in
 * {@link WorldTierComponent} (a Cardinal Components API player component,
 * {@link WorldTierComponents} registers it) instead of a NeoForge data attachment, and
 * synced via the component's own {@code AutoSyncedComponent} machinery instead of a
 * hand-rolled {@code WorldTierPayload}. {@link #getTier} no longer has NeoForge's
 * {@code FakePlayer} special-casing (resolving a fake player's tier from the real player
 * with the same UUID) — Fabric has no equivalent concept, and no widely-used library fills
 * the gap; a fake player here just gets its own (default HAVEN) component like any entity.
 */
public enum WorldTier implements StringRepresentable {
    HAVEN("haven"),
    FRONTIER("frontier"),
    ASCENT("ascent"),
    SUMMIT("summit"),
    PINNACLE("pinnacle");

    public static final IntFunction<WorldTier> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final Codec<WorldTier> CODEC = StringRepresentable.fromValues(WorldTier::values);
    public static final StreamCodec<ByteBuf, WorldTier> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private String name;

    private WorldTier(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public MutableComponent toComponent() {
        return Apotheosis.lang("text", "world_tier." + this.getSerializedName());
    }

    public Identifier getUnlockAdvancement() {
        return switch (this) {
            case HAVEN -> Apoth.Advancements.WORLD_TIER_HAVEN;
            case FRONTIER -> Apoth.Advancements.WORLD_TIER_FRONTIER;
            case ASCENT -> Apoth.Advancements.WORLD_TIER_ASCENT;
            case SUMMIT -> Apoth.Advancements.WORLD_TIER_SUMMIT;
            case PINNACLE -> Apoth.Advancements.WORLD_TIER_PINNACLE;
        };
    }

    /**
     * Returns the current world tier for a player.
     */
    public static WorldTier getTier(Player player) {
        return WorldTierComponent.get(player).getTier();
    }

    public static void setTier(Player player, WorldTier tier) {
        WorldTierComponent comp = WorldTierComponent.get(player);
        WorldTier oldTier = comp.getTier();
        if (oldTier == tier && !isTutorialActive(player)) {
            return;
        }

        comp.setTier(tier);
        WorldTierComponent.KEY.sync(player);

        if (player instanceof ServerPlayer sp) {
            for (TierAugment aug : TierAugmentRegistry.getAugments(oldTier, Target.PLAYERS)) {
                aug.remove(sp.level(), player);
            }

            for (TierAugment aug : TierAugmentRegistry.getAugments(tier, Target.PLAYERS)) {
                aug.apply(sp.level(), player);
            }

            comp.setTierAugmentsApplied(true);
            player.awardStat(Apoth.Stats.WORLD_TIERS_ACTIVATED);
        }
    }

    /**
     * Port note: gated behind the real progression advancement chain
     * (`data/apotheosis/advancement/progression/*.json`), same as upstream. Was briefly
     * stubbed to always return {@code true} while only the Tier Select screen was being
     * stood up (see git history) — now that {@code EquippedItemTrigger},
     * {@code AffixItemPredicate}, and {@code RarityItemPredicate} are ported and wired up
     * (see their own javadocs), the real check is back in effect.
     * <p>
     * One deliberate deviation from upstream: the `ascent` tier's kill requirement used
     * NeoForge's {@code apotheosis:is_invader} predicate (checks for a Gateways-spawned
     * boss), which needs the {@code mobs}/{@code spawner} package — out of this port's
     * scope entirely. Swapped for a "kill any monster" requirement
     * ({@code apotheosis:is_monster}, already ported for the gem-drop loot modifiers) in
     * the advancement JSON instead of leaving the tier unearnable.
     */
    public static boolean isUnlocked(Player player, WorldTier tier) {
        return ApothMiscUtil.hasAdvancement(player, tier.getUnlockAdvancement());
    }

    /**
     * Checks if the World Tier tutorial is active. The tutorial is active if the player is in Haven (the default), and has never clicked the "activate" button.
     * <p>
     * The tutorial being active has the following side effects:
     * <ul>
     * <li>Affix items have their name set to "Unidentified %s" instead of the real affix name</li>
     * <li>Affix items have their affix descriptions removed, and replaced with text directing the player to open the Tier Select screen</li>
     * <li>Upon opening the Tier Select screen, the screen will immediately open the World Tier Tutorial GUI Layer</li>
     * </ul>
     */
    public static boolean isTutorialActive(Player player) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && player.level().isClientSide()) {
            return ClientAccess.isTutorialActive(player);
        }
        return getTier(player) == WorldTier.HAVEN && ((ServerPlayer) player).getStats().getValue(Stats.CUSTOM.get(Apoth.Stats.WORLD_TIERS_ACTIVATED)) == 0;
    }

    public static <T> MapCodec<Map<WorldTier, T>> mapCodec(Codec<T> elementCodec) {
        return Codec.simpleMap(WorldTier.CODEC, elementCodec,
            Keyable.forStrings(() -> Arrays.stream(WorldTier.values()).map(StringRepresentable::getSerializedName)));
    }

    private static class ClientAccess {
        private static boolean isTutorialActive(Player player) {
            return getTier(player) == WorldTier.HAVEN && Minecraft.getInstance().player.getStats().getValue(Stats.CUSTOM.get(Apoth.Stats.WORLD_TIERS_ACTIVATED)) == 0;
        }
    }
}
