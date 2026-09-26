package dev.shadowsoffire.apotheosis;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

/**
 * Unofficial Fabric port of Apotheosis's Adventure module.
 *
 * Original mod: https://github.com/Shadows-of-Fire/Apotheosis (NeoForge only).
 * This port covers affixes, rarities, gems, and sockets — not enchanting,
 * spawners, or bosses. See mod-dev/apotheosis-fabric/README.md for scope
 * and porting notes.
 */
public class Apotheosis implements ModInitializer {

    public static final String MODID = "apotheosis";
    public static final Logger LOGGER = LoggerFactory.getLogger("Apotheosis");

    /** Whether the (currently unported, TODO-stubbed) Game Stages compat should be active. */
    public static final boolean STAGES_LOADED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("gamestages");

    @Nullable
    private static MinecraftServer currentServer;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
        Apoth.bootstrap();
        // Custom recipe ingredients for the salvaging recipes (upstream: NeoForge ingredient types).
        net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer.register(dev.shadowsoffire.apotheosis.util.AffixItemIngredient.SERIALIZER);
        net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer.register(dev.shadowsoffire.apotheosis.util.GemIngredient.SERIALIZER);
        dev.shadowsoffire.apotheosis.loot.LootRule.initCodecs();
        // Port note: found by actually launching a dev server — this call was missing entirely,
        // so GemBonus.CODEC's CodecMap was empty and every gem's "bonuses" list failed to decode
        // any entry (silently — see Gem.java's promotePartial diagnostic added to catch this),
        // which in turn made every single gem fail its own "no bonuses were provided" precondition.
        dev.shadowsoffire.apotheosis.socket.gem.bonus.GemBonus.initCodecs();
        registerDynamicRegistries();
        registerRadialMiningHook();
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.LinkItemToChatPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.RerollResultPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.GemCaseSelectPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.WorldTierPayload.Provider());
        registerCommands();
        LOGGER.info("Apotheosis (Fabric Adventure port) initializing");
    }

    /**
     * Registers every {@code placebo} {@code DynamicRegistry} in this port to the reload-listener
     * pipeline (via {@code DynamicRegistry#registerToBus()}) — this is what actually makes them
     * load their datapack JSON content on server start / {@code /reload}, rather than sitting
     * permanently empty. Referencing each registry's {@code INSTANCE} field here also forces
     * that class to load, which is what actually runs its constructor (and, for
     * {@code CodecMap}-backed ones, its type registrations).
     */
    private static void registerDynamicRegistries() {
        dev.shadowsoffire.apotheosis.affix.AffixRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.loot.RarityRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.loot.RarityOverrideRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.loot.AffixLootRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.socket.gem.GemRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.socket.gem.PurityWeightsRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.socket.gem.ExtraGemBonusRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.tiers.augments.TierAugmentRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.loot.modifiers.GlobalLootModifierRegistry.INSTANCE.registerToBus();
    }

    /**
     * Port note (NeoForge -> Fabric): replaces {@code RadialAffix}/{@code RadialBonus}'s dropped
     * {@code onBreak(BreakBlockEvent)} static hooks (NeoForge's {@code BreakBlockEvent} has no
     * Fabric equivalent — see those classes' javadoc) with Fabric API's
     * {@code PlayerBlockBreakEvents.AFTER}, which per the plan's research covers this case (unlike
     * break-*speed*, which genuinely has no Fabric event and still needs a mixin — see
     * {@code util.OmneticUtil}'s javadoc, still pending). Guarded to the logical server only, since
     * the event also fires client-side and {@code RadialUtil.breakExtraBlocks} is itself
     * server-authoritative ({@code ServerPlayer#gameMode.destroyBlock}).
     */
    private static void registerRadialMiningHook() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) {
                return;
            }
            var tool = player.getMainHandItem();
            var data = dev.shadowsoffire.apotheosis.affix.effect.RadialAffix.getRadialData(tool);
            if (data == null) {
                data = dev.shadowsoffire.apotheosis.socket.gem.bonus.special.RadialBonus.getRadialData(tool);
            }
            if (data != null && dev.shadowsoffire.apotheosis.util.RadialUtil.RadialState.isRadialMiningEnabled(player)) {
                dev.shadowsoffire.apotheosis.util.RadialUtil.breakExtraBlocks(player, pos, data);
            }
        });
    }

    /**
     * Port note (NeoForge -> Fabric): replaces upstream's {@code ApotheosisCommandEvent} (a
     * NeoForge custom event wrapping a root literal builder — never itself resolved during this
     * port, see the "known coupling point" note in the README) with Fabric API's
     * {@code CommandRegistrationCallback}. Drops {@code BossCommand} (boss scope, excluded).
     */
    private static void registerCommands() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, ctx, environment) -> {
            com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> root = net.minecraft.commands.Commands.literal("apoth");

            dev.shadowsoffire.apotheosis.commands.RarityCommand.register(root);
            dev.shadowsoffire.apotheosis.commands.CategoryCheckCommand.register(root);
            dev.shadowsoffire.apotheosis.commands.ReforgeCommand.register(root);
            dev.shadowsoffire.apotheosis.commands.GemCommand.register(root);
            dev.shadowsoffire.apotheosis.commands.SocketCommand.register(root);
            dev.shadowsoffire.apotheosis.commands.AffixCommand.register(root);
            dev.shadowsoffire.apotheosis.commands.WorldTierCommand.register(root);

            com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> debug = net.minecraft.commands.Commands.literal("debug").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS));
            dev.shadowsoffire.apotheosis.commands.DebugWeightCommand.register(debug, ctx);
            root.then(debug);

            dispatcher.register(root);
        });
    }

    /**
     * The currently-running integrated/dedicated server, or null if none is running.
     * <p>
     * Port note (NeoForge -> Fabric): replaces NeoForge's {@code CommonHooks.resolveLookup},
     * which lets mods reach the "current" registry access outside of an explicit context
     * (e.g. a fallback/dummy value with no live player or level to hand). Fabric has no
     * equivalent convenience, so this tracks it directly via server lifecycle events.
     */
    @Nullable
    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    /**
     * Constructs a resource location using {@link Apotheosis#MODID} as the namespace.
     */
    public static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /**
     * Constructs a mutable component with a lang key of the form "type.modid.path", using {@link Apotheosis#MODID}.
     *
     * @param type The type of language key, "misc", "info", "title", etc...
     * @param path The path of the language key.
     * @param args Translation arguments passed to the created translatable component.
     */
    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(langKey(type, path), args);
    }

    public static String langKey(String type, String path) {
        return type + "." + MODID + "." + path;
    }

    public static MutableComponent sysMessageHeader() {
        return Component.translatable("[%s] ", Component.literal("Apoth").withStyle(ChatFormatting.GOLD));
    }
}
