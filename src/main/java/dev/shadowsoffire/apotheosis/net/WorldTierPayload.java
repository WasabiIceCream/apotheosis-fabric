package dev.shadowsoffire.apotheosis.net;

import java.util.Optional;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;

/**
 * Port of NeoForge's {@code WorldTierPayload} — client-to-server "please switch my world tier"
 * request, sent from {@link dev.shadowsoffire.apotheosis.client.WorldTierSelectScreen}.
 * <p>
 * Port note (NeoForge -> Fabric): only the serverbound half is ported. Upstream also has a
 * {@code handleClient} that calls {@code WorldTier.setTier} directly, used because NeoForge
 * hand-rolls the client sync via this same payload — but {@code WorldTierComponent} (a Cardinal
 * Components {@code AutoSyncedComponent}) already syncs tier changes to the client for free (see
 * that class's javadoc), so a client-bound half of this payload would be redundant.
 */
public record WorldTierPayload(WorldTier tier) implements CustomPacketPayload {

    public static final Type<WorldTierPayload> TYPE = new Type<>(Apotheosis.loc("world_tier"));

    public static final StreamCodec<ByteBuf, WorldTierPayload> CODEC = WorldTier.STREAM_CODEC.map(WorldTierPayload::new, WorldTierPayload::tier);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<WorldTierPayload> {

        @Override
        public Type<WorldTierPayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, WorldTierPayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handleServer(WorldTierPayload msg, ServerPlayNetworking.Context ctx) {
            Player player = ctx.player();
            if (AdventureConfig.enableManualWorldTierChanges) {
                if (WorldTier.isUnlocked(player, msg.tier)) {
                    WorldTier.setTier(player, msg.tier);
                }
            }
            else if (((ServerPlayer) player).getStats().getValue(Stats.CUSTOM.get(Apoth.Stats.WORLD_TIERS_ACTIVATED)) == 0) {
                // The way we actually track if the player has completed the tutorial is through this stat counter.
                // So even when manual tier activation is disabled, we have to permit this to be sent once and disable the tutorial.
                player.awardStat(Apoth.Stats.WORLD_TIERS_ACTIVATED);
            }
            else if (msg.tier() != WorldTier.HAVEN) {
                // Aside from confirming the only allowed tier, the player is sending fraudulent payloads. Deny those.
                ((ServerPlayer) player).connection.disconnect(Apotheosis.lang("disconnect", "tier_changes_disabled"));
            }
            // else: a harmless duplicate tutorial-completion resend (e.g. a client-side UI race
            // re-firing the "activate" button) — the stat's already awarded, ignore silently
            // instead of disconnecting an innocent player for it.
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.SERVERBOUND);
        }

    }

}
