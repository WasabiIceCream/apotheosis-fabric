package dev.shadowsoffire.apotheosis.util;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.util.RadialUtil.RadialState;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Player component tracking the player's {@link RadialState} preference.
 * <p>
 * Port note (NeoForge -> Fabric): replaces {@code Apoth.Attachments.RADIAL_MINING_MODE} (a
 * NeoForge data attachment) — same pattern as {@code tiers.WorldTierComponent}. Also replaces
 * the original's hand-rolled {@code RadialStatePayload}/{@code PacketDistributor.sendToPlayer}
 * sync in {@code RadialUtil.toggleRadialState} with {@link AutoSyncedComponent}'s built-in sync.
 */
public class RadialMiningComponent implements RespawnableComponent<RadialMiningComponent>, AutoSyncedComponent {

    public static final ComponentKey<RadialMiningComponent> KEY = ComponentRegistry.getOrCreate(Apotheosis.loc("radial_mining_mode"), RadialMiningComponent.class);

    private RadialState state = RadialState.REQUIRE_NOT_SNEAKING;

    public RadialState getState() {
        return this.state;
    }

    public void setState(RadialState state) {
        this.state = state;
    }

    @Override
    public void readData(ValueInput in) {
        this.state = in.read("mode", RadialState.CODEC).orElse(RadialState.REQUIRE_NOT_SNEAKING);
    }

    @Override
    public void writeData(ValueOutput out) {
        out.store("mode", RadialState.CODEC, this.state);
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        RadialState.STREAM_CODEC.encode(buf, this.state);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        this.state = RadialState.STREAM_CODEC.decode(buf);
    }

    @Override
    public void copyFrom(RadialMiningComponent other, HolderLookup.Provider registries) {
        this.state = other.state;
    }

    public static RadialMiningComponent get(Player player) {
        return KEY.get(player);
    }

}
