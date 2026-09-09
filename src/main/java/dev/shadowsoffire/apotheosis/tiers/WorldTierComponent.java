package dev.shadowsoffire.apotheosis.tiers;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import dev.shadowsoffire.apotheosis.Apotheosis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Player component tracking the player's current {@link WorldTier} and whether the current
 * tier's {@link dev.shadowsoffire.apotheosis.tiers.augments.TierAugment}s have been applied.
 * <p>
 * Port note (NeoForge -> Fabric): replaces two separate NeoForge data attachments
 * ({@code Apoth.Attachments.WORLD_TIER}, {@code TIER_AUGMENTS_APPLIED}) with a single
 * Cardinal Components API player component — combining them is a deliberate simplification
 * (they always change together in practice), not a forced port. {@link AutoSyncedComponent}
 * gives free client sync with no hand-rolled packet needed (replacing the original's
 * {@code WorldTierPayload} + {@code PacketDistributor.sendToPlayer} call in
 * {@code WorldTier.setTier}). {@link RespawnableComponent} with
 * {@code RespawnCopyStrategy.ALWAYS_COPY} (wired in {@link ApothComponents}) replaces
 * NeoForge's {@code .copyOnDeath()} attachment builder flag.
 */
public class WorldTierComponent implements RespawnableComponent<WorldTierComponent>, AutoSyncedComponent {

    public static final ComponentKey<WorldTierComponent> KEY = ComponentRegistry.getOrCreate(Apotheosis.loc("world_tier"), WorldTierComponent.class);

    private WorldTier tier = WorldTier.HAVEN;
    private boolean tierAugmentsApplied = false;

    public WorldTier getTier() {
        return this.tier;
    }

    public void setTier(WorldTier tier) {
        this.tier = tier;
    }

    public boolean isTierAugmentsApplied() {
        return this.tierAugmentsApplied;
    }

    public void setTierAugmentsApplied(boolean applied) {
        this.tierAugmentsApplied = applied;
    }

    @Override
    public void readData(ValueInput in) {
        this.tier = in.read("tier", WorldTier.CODEC).orElse(WorldTier.HAVEN);
        this.tierAugmentsApplied = in.getBooleanOr("applied", false);
    }

    @Override
    public void writeData(ValueOutput out) {
        out.store("tier", WorldTier.CODEC, this.tier);
        out.putBoolean("applied", this.tierAugmentsApplied);
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        WorldTier.STREAM_CODEC.encode(buf, this.tier);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        this.tier = WorldTier.STREAM_CODEC.decode(buf);
    }

    @Override
    public void copyFrom(WorldTierComponent other, HolderLookup.Provider registries) {
        this.tier = other.tier;
        // tierAugmentsApplied is intentionally NOT copied — matches the original's inclusion
        // note ("If this is not set, they will be applied the next time the entity joins the
        // level"), i.e. a fresh player-instance (post-death) should re-apply augments.
    }

    public static WorldTierComponent get(Player player) {
        return KEY.get(player);
    }

}
