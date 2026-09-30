package dev.shadowsoffire.apotheosis.net;

import java.util.Optional;

import org.apache.commons.lang3.mutable.MutableInt;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.client.BossSpawnEffects;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client notice that an invader spawned nearby: the client plays the rarity's invader sound and draws a
 * rarity-coloured beacon beam at the position for 20 seconds.
 * <p>
 * Port note (NeoForge -> Fabric): same payload as upstream; the client handler lives in {@link BossSpawnEffects}
 * instead of {@code AdventureModuleClient}. NeoForge's protocol/version metadata has no Fabric equivalent (see
 * {@link PayloadProvider}).
 */
public record BossSpawnPayload(BlockPos pos, DynamicHolder<LootRarity> rarity) implements CustomPacketPayload {

    public static final Type<BossSpawnPayload> TYPE = new Type<>(Apotheosis.loc("boss_spawn"));

    public static final StreamCodec<ByteBuf, BossSpawnPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BossSpawnPayload::pos,
        RarityRegistry.INSTANCE.holderStreamCodec(), BossSpawnPayload::rarity,
        BossSpawnPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<BossSpawnPayload> {

        @Override
        public Type<BossSpawnPayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, BossSpawnPayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handleClient(BossSpawnPayload msg, ClientPlayNetworking.Context ctx) {
            BossSpawnEffects.onBossSpawn(msg.pos, msg.rarity);
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.CLIENTBOUND);
        }

    }

    public static record BossSpawnData(BlockPos pos, LootRarity rarity, MutableInt ticks) {

    }

}
