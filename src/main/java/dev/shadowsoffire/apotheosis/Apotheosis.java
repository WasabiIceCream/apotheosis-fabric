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
 * This port covers affixes, rarities, gems, sockets, and (since 0.4.0) the
 * mob features: invaders, elites and augmentations, and (since 0.4.1) the rogue
 * spawner and boss dungeon worldgen. Not enchanting, Apothic Spawners or
 * gateways. See
 * mod-dev/apotheosis-fabric/README.md for scope and porting notes.
 */
public class Apotheosis implements ModInitializer {

    public static final String MODID = "apotheosis";
    public static final Logger LOGGER = LoggerFactory.getLogger("Apotheosis");

    /**
     * Set the environment variable {@code APOTH_DEBUG_MOBS=on} to log every step of the mob spawn processing
     * (invaders, augmentations, elites). Port note: logged at INFO here (upstream: DEBUG), since the flag is already opt-in.
     */
    public static final boolean DEBUG_MOBS = "on".equalsIgnoreCase(System.getenv("APOTH_DEBUG_MOBS"));

    /**
     * Set the environment variable {@code APOTH_DEBUG_WORLDGEN=on} to log the position of every rogue spawner and boss dungeon
     * as it generates (same flag as upstream).
     */
    public static final boolean DEBUG_WORLDGEN = "on".equalsIgnoreCase(System.getenv("APOTH_DEBUG_WORLDGEN"));

    /** Whether the (currently unported, TODO-stubbed) Game Stages compat should be active. */
    public static final boolean STAGES_LOADED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("gamestages");

    @Nullable
    private static MinecraftServer currentServer;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Gameoverse: which items the spell weapon category picked up (checked against the RPG Series configs).
            var ids = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i -> {
                try {
                    return Apoth.LootCategories.SPELL_WEAPON.isValid(i.getDefaultInstance());
                }
                catch (RuntimeException e) {
                    return false;
                }
            })
                .map(i -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).toString()).sorted().toList();
            LOGGER.info("Spell weapons ({}): {}", ids.size(), ids);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
        Apoth.bootstrap();
        // 26.1 no longer sends recipes to clients. The salvaging, gem cutting and reforging
        // screens match recipes client-side, so opt their serializers into Fabric's recipe
        // sync; ApotheosisClient fills the client caches when they arrive.
        for (var serializer : java.util.List.of(Apoth.RecipeSerializers.SALVAGING, Apoth.RecipeSerializers.REFORGING,
            Apoth.RecipeSerializers.BASIC_GEM_CUTTING, Apoth.RecipeSerializers.PURITY_UPGRADE)) {
            net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(serializer.value());
        }
        // Custom recipe ingredients for the salvaging recipes (upstream: NeoForge ingredient types).
        net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer.register(dev.shadowsoffire.apotheosis.util.AffixItemIngredient.SERIALIZER);
        net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer.register(dev.shadowsoffire.apotheosis.util.GemIngredient.SERIALIZER);
        dev.shadowsoffire.apotheosis.loot.LootRule.initCodecs();
        // Port note: found by actually launching a dev server — this call was missing entirely,
        // so GemBonus.CODEC's CodecMap was empty and every gem's "bonuses" list failed to decode
        // any entry (silently — see Gem.java's promotePartial diagnostic added to catch this),
        // which in turn made every single gem fail its own "no bonuses were provided" precondition.
        dev.shadowsoffire.apotheosis.socket.gem.bonus.GemBonus.initCodecs();
        dev.shadowsoffire.apotheosis.mobs.util.SpawnCondition.initCodecs();
        dev.shadowsoffire.apotheosis.mobs.util.EntityModifier.initCodecs();
        registerDynamicRegistries();
        AdventureEvents.register(); // before the radial mining hook, as upstream's priorities
        registerRadialMiningHook();
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.LinkItemToChatPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.RerollResultPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.GemCaseSelectPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.WorldTierPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.BossSpawnPayload.Provider());
        dev.shadowsoffire.placebo.network.PayloadHelper.registerPayload(new dev.shadowsoffire.apotheosis.net.RadialStatePayload.Provider());
        registerMobHooks();
        dev.shadowsoffire.apotheosis.gen.ApothWorldgen.init();
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
        dev.shadowsoffire.apotheosis.mobs.registries.InvaderRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.mobs.registries.EliteRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.mobs.registries.AugmentRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.mobs.registries.InvaderSpawnRulesRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.spawner.RogueSpawnerRegistry.INSTANCE.registerToBus();
        dev.shadowsoffire.apotheosis.loot.modifiers.GlobalLootModifierRegistry.INSTANCE.registerToBus();
    }

    /**
     * Fabric event wiring for the mob features in {@code mobs.ApothMobEvents} (upstream: NeoForge event subscribers in
     * {@code ApothMobEvents} and {@code AdventureEvents}). The spawn hooks themselves are mixins
     * ({@code MobFinalizeSpawnMixin}, {@code NaturalSpawnerInvaderMixin}, {@code MobBossMixin}).
     */
    private static void registerMobHooks() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(dev.shadowsoffire.apotheosis.mobs.ApothMobEvents::onEntityLoad);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(dev.shadowsoffire.apotheosis.mobs.ApothMobEvents::processPendingElites);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> dev.shadowsoffire.apotheosis.mobs.ApothMobEvents.clearPending());
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(dev.shadowsoffire.apotheosis.mobs.ApothMobEvents::allowDamage);
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(dev.shadowsoffire.apotheosis.mobs.ApothMobEvents::afterDeath);
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
     * {@code CommandRegistrationCallback}.
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
            dev.shadowsoffire.apotheosis.commands.BossCommand.register(root);

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

    public static void debugLog(net.minecraft.core.BlockPos pos, String name) {
        if (DEBUG_WORLDGEN) {
            LOGGER.info("Generated a {} at {} {} {}", name, pos.getX(), pos.getY(), pos.getZ());
        }
    }

}
