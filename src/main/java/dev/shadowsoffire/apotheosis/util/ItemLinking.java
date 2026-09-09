package dev.shadowsoffire.apotheosis.util;

import java.util.UUID;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.net.LinkItemToChatPayload;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Port note (NeoForge -> Fabric): the client-side trigger ({@code Client.sendHoveredItem}) is
 * left in this same file (rather than split into a client-only class) since it's a single small
 * method guarded by a runtime environment check — same approach as vanilla/Fabric code that
 * needs one client-only call from otherwise-common code. Uses Fabric's
 * {@code ClientPlayNetworking.send} instead of NeoForge's {@code ClientPacketDistributor}.
 */
public class ItemLinking {

    /**
     * Cooldown tracker map from player id -> game time of last link.
     */
    private static final Object2LongMap<UUID> LAST_LINK_TIMES = new Object2LongOpenHashMap<>();

    public static void sendHoveredItem() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            Client.sendHoveredItem();
        }
    }

    public static boolean isOnCooldown(UUID id, long gameTime) {
        return LAST_LINK_TIMES.getOrDefault(id, -AdventureConfig.itemLinkingCooldown) + AdventureConfig.itemLinkingCooldown >= gameTime;
    }

    public static void startCooldown(UUID id, long gameTime) {
        LAST_LINK_TIMES.put(id, gameTime);
    }

    private static class Client {
        public static void sendHoveredItem() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scn) {
                net.minecraft.world.inventory.Slot slot = scn.hoveredSlot;
                if (slot != null && slot.hasItem()) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new LinkItemToChatPayload(scn.getMenu().containerId, slot.index, slot.getItem().getItem()));
                }
            }
        }
    }

}
