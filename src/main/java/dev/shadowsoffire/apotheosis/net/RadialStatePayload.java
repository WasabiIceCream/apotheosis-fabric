package dev.shadowsoffire.apotheosis.net;

import java.util.Optional;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.util.RadialUtil;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by the client's "Change Radial Mining Mode" key: the server steps the player's radial mining mode to the next one and
 * tells them.
 * <p>
 * Port note (NeoForge -> Fabric): upstream's payload goes both ways; the server-to-client half (the new state) is
 * {@code util.RadialMiningComponent}'s Cardinal Components sync here, so this one only goes to the server and carries nothing.
 */
public record RadialStatePayload() implements CustomPacketPayload {

    public static final Type<RadialStatePayload> TYPE = new Type<>(Apotheosis.loc("radial_state_change"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadialStatePayload> CODEC = StreamCodec.unit(new RadialStatePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<RadialStatePayload> {

        @Override
        public Type<RadialStatePayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, RadialStatePayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handleServer(RadialStatePayload msg, ServerPlayNetworking.Context ctx) {
            RadialUtil.toggleRadialState(ctx.player());
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.SERVERBOUND);
        }
    }
}
