package dev.shadowsoffire.apotheosis.net;

import java.util.Optional;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.effect.AttributeToggleAffix;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by the client's "Toggle Attribute Bonuses" key (upstream 9.1.0): the server flips every attribute the worn boots'
 * {@link AttributeToggleAffix}es provide.
 * <p>
 * Port note (NeoForge -> Fabric): upstream's payload goes both ways; the new state comes back through the
 * {@code ATTRIBUTE_TOGGLES} attachment's own sync here, so this one only goes to the server and carries nothing.
 */
public record AttributeTogglesPayload() implements CustomPacketPayload {

    public static final Type<AttributeTogglesPayload> TYPE = new Type<>(Apotheosis.loc("attribute_toggles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttributeTogglesPayload> CODEC = StreamCodec.unit(new AttributeTogglesPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<AttributeTogglesPayload> {

        @Override
        public Type<AttributeTogglesPayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, AttributeTogglesPayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handleServer(AttributeTogglesPayload msg, ServerPlayNetworking.Context ctx) {
            AttributeToggleAffix.handleToggle(ctx.player());
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.SERVERBOUND);
        }
    }
}
