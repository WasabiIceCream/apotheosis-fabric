package dev.shadowsoffire.apotheosis.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingScreen;
import dev.shadowsoffire.apotheosis.affix.reforging.ReforgingScreen;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingScreen;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingScreen;
import dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseScreen;
import dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseTileRenderer;
import dev.shadowsoffire.placebo.network.PayloadHelper;

public class ApotheosisClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PayloadHelper.registerClientHandlers();
        // Must happen here, synchronously during client mod init — not lazily on first tick.
        // See AdventureKeys#init's javadoc for why (registering a KeyMapping after GameOptions
        // exists throws, permanently breaking the class and crash-looping the client).
        AdventureKeys.init();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> AdventureKeys.tick());
        registerScreens();
        registerTileRenderers();
        AdventureTooltips.register();

        // Port note (bugfix): unlike WorldTierComponent (Cardinal Components syncs it to the
        // client automatically on join), vanilla stats are NOT pushed to the client on login —
        // the client's local Stats cache starts empty and only fills in as stats are earned
        // during play, or on an explicit REQUEST_STATS request (e.g. opening the Statistics
        // screen). WorldTier.isTutorialActive's client-side half reads that local cache to
        // decide whether to show the World Tier tutorial, so on every fresh join it saw
        // apotheosis:world_tiers_activated as 0 (not yet synced) even when the server's real,
        // persisted value was already 1 — reopening the tutorial every login despite it having
        // already been completed. Request stats immediately on join so the cache is populated
        // well before the player could plausibly open the Tier Select screen.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            handler.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        });

        Apotheosis.LOGGER.info("Apotheosis client (Fabric Adventure port) initializing");
    }

    private static void registerScreens() {
        MenuScreens.register(Apoth.Menus.GEM_CUTTING, GemCuttingScreen::new);
        MenuScreens.register(Apoth.Menus.GEM_CASE, GemCaseScreen::new);
        MenuScreens.register(Apoth.Menus.REFORGING, ReforgingScreen::new);
        MenuScreens.register(Apoth.Menus.SALVAGE, SalvagingScreen::new);
        MenuScreens.register(Apoth.Menus.AUGMENTING, AugmentingScreen::new);
    }

    /**
     * Port note: only the Gem Case's tile renderer is ported. Upstream's
     * {@code ReforgingTableTileRenderer}/{@code AugmentingTableTileRenderer} render a floating
     * animated hammer/star-cube model via NeoForge's {@code ModelEvent.RegisterStandalone} +
     * {@code StandaloneModelKey} system (registered in the not-ported {@code AdventureModuleClient}),
     * for which no Fabric equivalent standalone-model registration path exists, and no source for the
     * hammer/star_cube 3D geometry itself was found in either Zenith Renewed or the CurseForge legacy
     * pack (both are 2D-texture-only sources). Left as a known cosmetic gap — the tables function
     * fully, they just don't show the floating animated prop above them.
     */
    private static void registerTileRenderers() {
        BlockEntityRenderers.register(Apoth.Tiles.GEM_CASE, GemCaseTileRenderer::new);
        BlockEntityRenderers.register(Apoth.Tiles.ENDER_GEM_CASE, GemCaseTileRenderer::new);
    }
}
