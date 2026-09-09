package dev.shadowsoffire.apotheosis.net;

import java.util.Optional;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseMenu;
import dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseScreen;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Communicates the selected gem in the Gem Safe between client and server.
 * <p>
 * When the client makes a selection, the client sends this payload to the server. The server will reply with the same payload if it accepts the change.
 */
public record GemCaseSelectPayload(DynamicHolder<Gem> gem) implements CustomPacketPayload {

    public static final Type<GemCaseSelectPayload> TYPE = new Type<>(Apotheosis.loc("gem_case_select"));

    public static final StreamCodec<ByteBuf, GemCaseSelectPayload> CODEC = GemRegistry.INSTANCE.holderStreamCodec().map(GemCaseSelectPayload::new, GemCaseSelectPayload::gem);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<GemCaseSelectPayload> {

        @Override
        public Type<GemCaseSelectPayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, GemCaseSelectPayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handleClient(GemCaseSelectPayload msg, ClientPlayNetworking.Context ctx) {
            GemCaseScreen.handleSelectedGem(msg.gem());
        }

        @Override
        public void handleServer(GemCaseSelectPayload msg, ServerPlayNetworking.Context ctx) {
            if (ctx.player().containerMenu instanceof GemCaseMenu menu) {
                menu.setSelectedGem(msg.gem());
            }
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.empty();
        }

    }

}
