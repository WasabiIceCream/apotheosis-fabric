package dev.shadowsoffire.apotheosis.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.util.ItemLinking;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Port note (NeoForge -> Fabric): only {@code LINK_ITEM_TO_CHAT} is ported so far —
 * {@code TOGGLE_RADIAL} was already replaced by {@code util.RadialMiningComponent}'s built-in
 * sync (no keybind-driven packet needed), and {@code OPEN_WORLD_TIER_SELECT}/
 * {@code COMPARE_EQUIPMENT} need client screens (world-tier select, equipment comparison
 * tooltip) that aren't ported yet.
 * <p>
 * Also drops NeoForge's {@code KeyConflictContext.GUI}/{@code KeyModifier.SHIFT} (vanilla
 * {@link KeyMapping} has no modifier-key concept at all) in favor of binding plain T and
 * checking the raw shift key state via {@link InputConstants#isKeyDown} at trigger time —
 * {@code Screen.hasShiftDown()}, the usual shortcut for this, no longer exists in this version
 * (confirmed via javap) — same "shift+T" behavior, checked a different way.
 */
public class AdventureKeys {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Apotheosis.loc("keys"));

    public static final KeyMapping LINK_ITEM_TO_CHAT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        Apotheosis.langKey("key", "link_item_to_chat"),
        Type.KEYSYM, GLFW.GLFW_KEY_T, CATEGORY));

    public static final KeyMapping OPEN_WORLD_TIER_SELECT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        Apotheosis.langKey("key", "open_world_tier_select"),
        Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY));

    /**
     * No-op call target used solely to force this class's static initializer (and thus the
     * {@link KeyMappingHelper#registerKeyMapping} calls above) to run during
     * {@code ClientModInitializer#onInitializeClient}, rather than lazily on first use.
     * <p>
     * Port bug found via live runtime testing: this class was previously only ever referenced
     * from inside the {@code ClientTickEvents.END_CLIENT_TICK} lambda registered in
     * {@code ApotheosisClient}. Registering that lambda does not execute it, so the class — and
     * its key-mapping registration — didn't actually load until the first client tick fired,
     * which is well after {@code GameOptions} has already been constructed. Fabric's
     * {@code KeyMappingRegistryImpl} throws {@code IllegalStateException} if registration happens
     * after that point, which permanently failed this class's static init (a Java class that
     * throws during `<clinit>` becomes unusable forever — every later access rethrows as
     * {@code NoClassDefFoundError}), crash-looping the client on every subsequent tick.
     */
    public static void init() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        while (LINK_ITEM_TO_CHAT.consumeClick()) {
            boolean shiftDown = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (shiftDown) {
                ItemLinking.sendHoveredItem();
            }
        }
        while (OPEN_WORLD_TIER_SELECT.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new dev.shadowsoffire.apotheosis.client.WorldTierSelectScreen());
            }
        }
    }

}
