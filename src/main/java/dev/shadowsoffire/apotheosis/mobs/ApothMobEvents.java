package dev.shadowsoffire.apotheosis.mobs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.Apoth.Attachments;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.compat.GameStagesCompat;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.AugmentRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.EliteRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.InvaderRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.InvaderSpawnRulesRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Augmentation;
import dev.shadowsoffire.apotheosis.mobs.types.Elite;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.mobs.util.SurfaceType;
import dev.shadowsoffire.apotheosis.net.BossSpawnPayload;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment.Target;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugmentRegistry;
import dev.shadowsoffire.apotheosis.util.PersistentDataComponent;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

/**
 * This file contains Apotheosis's mob processing events.
 * <p>
 * These events execute the following steps in sequential order:
 * <ol>
 * <li>Checks if the mob spawn can be consumed to spawn an {@link Invader}, aborting the mob spawn.</li>
 * <li>Applies the world tier's monster {@link TierAugment}s, then checks if a mob should be {@linkplain Augmentation augmented},
 * and attempts to run the augments (e.g. monsters wearing a random affixed item).</li>
 * <li>Attempts to mark the entity to become an {@link Elite}; the transformation itself runs once it has joined the level.</li>
 * </ol>
 * <p>
 * Port notes (NeoForge -> Fabric), the hook points replacing upstream's events:
 * <ul>
 * <li>{@code FinalizeSpawnEvent} (fired by NeoForge before {@code Mob#finalizeSpawn}, cancellable) is split in two.
 * Step 1 only ever applies to {@code NATURAL}/{@code CHUNK_GENERATION} spawns, which both come from
 * {@code NaturalSpawner}, so {@code NaturalSpawnerInvaderMixin} wraps exactly those two {@code finalizeSpawn} calls: when
 * {@link #tryReplaceWithInvader} succeeds, the original mob's {@code finalizeSpawn} is skipped (no jockeys, chickens or
 * other side effects) and the following {@code addFreshEntityWithPassengers} is skipped too, so the replaced mob never
 * enters the world. Steps 2-3 run for every spawn reason, at the head of {@code Mob#finalizeSpawn}
 * ({@code MobFinalizeSpawnMixin}), which every subclass reaches through {@code super}.</li>
 * <li>Upstream lets an elite with {@code "finalize": false} cancel the mob's whole {@code finalizeSpawn}. A hook inside
 * {@code Mob#finalizeSpawn} can't stop a subclass's own override, so here such elites keep whatever vanilla
 * randomisation their type applies (e.g. default equipment in slots their gear set doesn't cover).</li>
 * <li>{@code EntityJoinLevelEvent} is Fabric's {@code ServerEntityEvents.ENTITY_LOAD}. The elite transformation is
 * queued and run at the end of the server tick: it adds supporting entities, and adding entities from inside the
 * entity manager's tracking callback can modify the section being iterated.</li>
 * <li>Player/mob attachments are Fabric data attachments ({@link Attachments}); {@code getPersistentData()} is
 * {@link PersistentDataComponent}; {@code PacketDistributor} is {@code ServerPlayNetworking}.</li>
 * <li>Chunk-generation spawns run on world-generation threads. The cooldown check-and-set is synchronized, the
 * spawn announcement is handed to the server thread, and the generation context reads the biome from the
 * generating region instead of the live level.</li>
 * </ul>
 * Pre-existing port deviation kept: tier augments and the join-level fallback only apply to {@link Monster}s (upstream:
 * every {@link Mob}), matching what this port has always done.
 */
public class ApothMobEvents {

    public static final String APOTH_MINIBOSS = "apoth.miniboss";
    public static final String APOTH_MINIBOSS_PLAYER = APOTH_MINIBOSS + ".player";

    /**
     * Mobs marked as elites that have joined a level and are waiting for {@link #processPendingElites}.
     */
    private static final Deque<Mob> PENDING_ELITES = new ArrayDeque<>();

    private static final Object INVADER_LOCK = new Object();

    /**
     * Step 1. Called from {@code NaturalSpawnerInvaderMixin} in place of the mob's {@code finalizeSpawn}.
     *
     * @return True if an invader was spawned, in which case the caller must not finalize or add the original mob.
     */
    public static boolean tryReplaceWithInvader(ServerLevelAccessor level, Mob mob, EntitySpawnReason reason) {
        // Invaders can only trigger off of natural spawns (chunk generation is considered "natural")
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION || !(mob instanceof Monster)) {
            debugLog("[Invaders]: Failed invader preconditions for {} ({}).", mob.getName().getString(), reason);
            return false;
        }

        try {
            Player player = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), -1, false);
            if (player == null) {
                debugLog("[Invaders]: Discarding due to lack of player context.");
                return false;
            }

            GenContext ctx = contextFor(level, player, mob.blockPosition());
            synchronized (INVADER_LOCK) {
                return trySpawnInvader(level, mob, reason, ctx, player);
            }
        }
        catch (Exception ex) {
            // Never let an invader roll break natural spawning or world generation.
            Apotheosis.LOGGER.error("Failure while attempting to spawn an Apothic Invader in place of {}", mob, ex);
            return false;
        }
    }

    /**
     * Steps 2 and 3. Called from the head of {@code Mob#finalizeSpawn} for every spawn reason.
     */
    public static void onFinalizeSpawn(ServerLevelAccessor level, Mob mob, EntitySpawnReason reason) {
        debugLog("Finalizing spawn for: {}", mob.getName().getString());
        Player player = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), -1, false);
        if (player == null) {
            debugLog("Discarding due to lack of player context.");
            return; // Spawns require player context
        }

        GenContext ctx = contextFor(level, player, mob.blockPosition());
        tryAugmentations(level, mob, reason, ctx);
        trySpawnElite(level, mob, reason, ctx, player);
    }

    private static boolean trySpawnInvader(ServerLevelAccessor sLevel, Mob mob, EntitySpawnReason reason, GenContext ctx, Player player) {
        long gameTime = sLevel.getGameTime();

        if (player.getAttachedOrElse(Attachments.INVADER_COOLDOWN, 0L) > gameTime) {
            debugLog("[Invaders]: Spawn cooldown is active for the context player {}.", player.getName().getString());
            return false;
        }

        ResourceKey<DimensionType> dimId = sLevel.getLevel().dimensionTypeRegistration().unwrapKey().orElse(null);
        InvaderSpawnRules rules = dimId == null ? null : InvaderSpawnRulesRegistry.INSTANCE.getRules(dimId);
        if (rules == null) {
            debugLog("[Invaders]: No invader spawn rules present for dimension {}", dimId);
            return false;
        }

        float chance = rules.spawnChances().get(ctx.tier());
        SurfaceType surface = rules.surfaceType();

        if (ctx.rand().nextFloat() > chance) {
            debugLog("[Invaders]: Failed random chance roll.");
            return false;
        }

        if (surface.test(sLevel, mob.blockPosition())) {
            debugLog("[Invaders]: Succeeded at random chance roll and surface test.");
            Invader item = InvaderRegistry.INSTANCE.getRandomItem(ctx);
            if (item == null) {
                Apotheosis.LOGGER.error("Attempted to spawn an Invader in dimension {} using configured spawn rules {} but no bosses were made available.", dimId, rules);
                return false;
            }

            if (!item.basicData().canSpawn(mob, sLevel, reason)) {
                debugLog("[Invaders]: Failed invader spawn conditions.");
                return false;
            }

            Mob boss = item.createBoss(sLevel, BlockPos.containing(mob.getX() - 0.5, mob.getY(), mob.getZ() - 0.5), ctx);
            if (AdventureConfig.bossAutoAggro && !player.isCreative()) {
                boss.setTarget(player);
            }

            if (canSpawn(sLevel, boss, player.distanceToSqr(boss))) {
                sLevel.addFreshEntityWithPassengers(boss);

                ServerLevel level = sLevel.getLevel();
                MinecraftServer server = level.getServer();
                if (server.isSameThread()) {
                    sendInvaderSpawnNotification(level, boss);
                }
                else {
                    // Chunk generation runs off-thread; announce from the server thread.
                    server.execute(() -> sendInvaderSpawnNotification(level, boss));
                }

                long end = gameTime + rules.cooldown().orElse(AdventureConfig.bossSpawnCooldown);
                applyClusteredCooldown(level, player, ctx.tier(), boss, end);
                debugLog("[Invaders]: Successfully spawned an invader {} at {}", boss.getName().getString(), boss.blockPosition());
                return true;
            }
            else {
                debugLog("Failed entity spawn checks.");
            }
        }
        else {
            debugLog("[Invaders]: Failed surface test " + surface);
        }

        return false;
    }

    public static void sendInvaderSpawnNotification(ServerLevel sLevel, Mob invader) {
        Component name = getName(invader);
        DynamicHolder<LootRarity> rarity = getRarity(invader);

        if (name == null || !rarity.isBound()) {
            Apotheosis.LOGGER.warn("An Invader {} ({}) has spawned without a name ({}) or rarity ({})!", invader.getName().getString(), EntityType.getKey(invader.getType()), name, rarity);
        }
        else {
            sLevel.players().forEach(p -> {
                if (isWithinAnnounceRange(p, invader)) {
                    p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("info.apotheosis.boss_spawn", name, (int) invader.getX(), (int) invader.getY())));
                    if (ServerPlayNetworking.canSend(p, BossSpawnPayload.TYPE)) {
                        ServerPlayNetworking.send(p, new BossSpawnPayload(invader.blockPosition(), rarity));
                    }
                }
            });
        }
    }

    /**
     * Applies the invader spawn cooldown to the triggering player and all clustered players.
     * <p>
     * The cluster is every player in the level whose world tier matches the tier the spawn was rolled against
     * and who is within {@link AdventureConfig#bossAnnounceRange} of the spawned boss.
     *
     * @param end The game time at which the affected players may next trigger an invader spawn.
     */
    private static void applyClusteredCooldown(ServerLevel level, Player trigger, WorldTier tier, Mob boss, long end) {
        applyCooldown(trigger, end);
        int clustered = 0;
        for (ServerPlayer p : level.players()) {
            if (p != trigger && WorldTier.getTier(p) == tier && isWithinAnnounceRange(p, boss)) {
                applyCooldown(p, end);
                clustered++;
            }
        }
        debugLog("[Invaders]: Applied spawn cooldown ending at {} to {} and {} clustered player(s).", end, trigger.getName().getString(), clustered);
    }

    /**
     * Sets a player's invader spawn cooldown to the given end time, unless their current cooldown ends later.
     */
    private static void applyCooldown(Player player, long end) {
        if (end > player.getAttachedOrElse(Attachments.INVADER_COOLDOWN, 0L)) {
            player.setAttached(Attachments.INVADER_COOLDOWN, end);
        }
    }

    /**
     * Checks if the player is within {@link AdventureConfig#bossAnnounceRange} blocks of the target entity, ignoring Y-level.
     * <p>
     * Used both for the invader spawn announcement and for clustering the invader spawn cooldown, so the two stay in lockstep.
     */
    private static boolean isWithinAnnounceRange(Player player, Entity target) {
        Vec3 tPos = new Vec3(target.getX(), player.getY(), target.getZ());
        return player.distanceToSqr(tPos) <= AdventureConfig.bossAnnounceRange * AdventureConfig.bossAnnounceRange;
    }

    /**
     * Applies all active {@link TierAugment}s to the mob, then rolls each {@link Augmentation}'s chance for the context tier.
     */
    private static void tryAugmentations(ServerLevelAccessor level, Mob mob, EntitySpawnReason type, GenContext ctx) {
        float healthPct = mob.getHealth() / mob.getMaxHealth();

        if (mob instanceof Monster) {
            for (TierAugment aug : TierAugmentRegistry.getAugments(ctx.tier(), Target.MONSTERS)) {
                aug.apply(level, mob);
            }
            mob.setAttached(Attachments.TIER_AUGMENTS_APPLIED, true);
        }

        for (Augmentation aug : AugmentRegistry.getAll()) {
            if (aug.canApply(level, mob, type, ctx)) {
                if (ctx.rand().nextFloat() < aug.chance(ctx.tier())) {
                    debugLog("Applying augmentation {}", AugmentRegistry.INSTANCE.getKey(aug));
                    aug.apply(mob, ctx);
                }
                else {
                    debugLog("Roll failed for augmentation {}", AugmentRegistry.INSTANCE.getKey(aug));
                }
            }
            else {
                debugLog("Skipped augmentation {}", AugmentRegistry.INSTANCE.getKey(aug));
            }
        }

        // Since Tier Augments or Augmentations may apply max health, we need to update the mob's current HP.
        mob.setHealth(healthPct * mob.getMaxHealth());
    }

    private static boolean trySpawnElite(ServerLevelAccessor sLevel, Mob mob, EntitySpawnReason reason, GenContext ctx, Player player) {
        Elite item = EliteRegistry.INSTANCE.getRandomItem(ctx, mob);
        if (item == null) {
            debugLog("No Elites were available for {} and {}", ctx, mob);
            return false;
        }

        if (!item.basicData().canSpawn(mob, sLevel, reason)) {
            debugLog("The elite {} was selected but could not spawn based on spawn conditions.", EliteRegistry.INSTANCE.getKey(item));
            return false;
        }

        if (ctx.rand().nextFloat() <= item.getChance()) {
            markElite(mob, item, player);
            debugLog("Successfully spawned the elite {} at {}", EliteRegistry.INSTANCE.getKey(item), mob.blockPosition());
            return true;
        }

        return false;
    }

    /**
     * Marks a mob to be transformed into the given elite once it joins the level, using the player for the generation context.
     */
    public static void markElite(Mob mob, Elite elite, Player player) {
        CompoundTag data = PersistentDataComponent.get(mob);
        data.putString(Elite.MINIBOSS_KEY, EliteRegistry.INSTANCE.getKey(elite).toString());
        data.putString(Elite.PLAYER_KEY, player.getUUID().toString());
    }

    /**
     * {@code ServerEntityEvents.ENTITY_LOAD}: fires for freshly added entities and for entities loaded from disk.
     */
    public static void onEntityLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        applyMissedTierAugments(level, mob);

        CompoundTag data = PersistentDataComponent.get(mob);
        if (data.contains(Elite.PLAYER_KEY) && data.getString(Elite.MINIBOSS_KEY).isPresent()) {
            PENDING_ELITES.add(mob);
        }
    }

    /**
     * Upstream {@code AdventureEvents#applyMissedTierAugments}, mob half: monsters that never went through
     * {@code finalizeSpawn} (invaders, structure/NBT-placed mobs, mobs saved before this feature) get the nearest player's
     * monster tier augments the first time they join a level with a player online.
     */
    private static void applyMissedTierAugments(ServerLevel level, Mob mob) {
        if (!(mob instanceof Monster) || mob.getAttachedOrElse(Attachments.TIER_AUGMENTS_APPLIED, false)) {
            return;
        }
        Player player = level.getNearestPlayer(mob, -1);
        if (player != null) {
            float healthPct = mob.getHealth() / mob.getMaxHealth();
            WorldTier tier = WorldTier.getTier(player);
            for (TierAugment aug : TierAugmentRegistry.getAugments(tier, Target.MONSTERS)) {
                aug.apply(level, mob);
            }
            mob.setAttached(Attachments.TIER_AUGMENTS_APPLIED, true);
            mob.setHealth(healthPct * mob.getMaxHealth());
        }
    }

    /**
     * Runs at the end of every server tick. Transforms elites queued by {@link #onEntityLoad}.
     * <p>
     * Upstream runs this from {@code EntityJoinLevelEvent} directly; see the class javadoc for why it's deferred here.
     */
    public static void processPendingElites(MinecraftServer server) {
        int count = PENDING_ELITES.size();
        for (int i = 0; i < count && !PENDING_ELITES.isEmpty(); i++) {
            Mob mob = PENDING_ELITES.poll();
            if (mob.isRemoved() || !(mob.level() instanceof ServerLevel level)) {
                continue;
            }
            transformElite(level, mob);
        }
    }

    public static void clearPending() {
        PENDING_ELITES.clear();
    }

    private static void transformElite(ServerLevel level, Mob mob) {
        CompoundTag data = PersistentDataComponent.get(mob);
        if (!data.contains(Elite.PLAYER_KEY)) {
            return; // Already transformed (queued twice).
        }
        String key = data.getString(Elite.MINIBOSS_KEY).orElse("");
        try {
            UUID playerId = UUID.fromString(data.getString(Elite.PLAYER_KEY).orElseThrow());
            Player player = level.getPlayerByUUID(playerId);
            if (player == null) {
                player = level.getNearestPlayer(mob, -1);
            }

            if (player != null) {
                GenContext ctx = GenContext.forPlayerAtPos(level.getRandom(), player, mob.blockPosition());
                Elite item = EliteRegistry.INSTANCE.getValue(Identifier.tryParse(key));
                if (item != null) {
                    item.transformMiniboss(level, mob, ctx);
                }
                else {
                    // Unknown elite (removed by a datapack): drop the marker so we don't retry forever.
                    data.remove(Elite.MINIBOSS_KEY);
                    data.remove(Elite.PLAYER_KEY);
                }
            }
            // No player online: leave the markers, the transformation retries next time the mob joins a level.
        }
        catch (Exception ex) {
            data.remove(Elite.PLAYER_KEY);
            Apotheosis.LOGGER.error("Failure while initializing the Apothic Elite " + key, ex);
        }
    }

    /**
     * Upstream {@code AdventureEvents#preventBossSuffocate}: invaders are immune to suffocation. {@code ServerLivingEntityEvents.ALLOW_DAMAGE}.
     */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        return !(source.is(DamageTypes.IN_WALL) && PersistentDataComponent.get(entity).contains(Invader.BOSS_KEY));
    }

    /**
     * Upstream {@code AdventureEvents#removeCloudsOnDeath}: elites that carried effect clouds as passengers take them along on death.
     */
    public static void afterDeath(LivingEntity entity, DamageSource source) {
        if (PersistentDataComponent.get(entity).getBooleanOr(Elite.MINIBOSS_KEY, false)) {
            for (Entity passenger : entity.getPassengers()) {
                if (passenger instanceof AreaEffectCloud cloud) {
                    cloud.discard();
                }
            }
        }
    }

    /**
     * Builds the generation context for a spawn. Same as {@link GenContext#forPlayerAtPos}, but reads the biome from the
     * spawning level accessor, which during chunk generation is the generating region (safe off-thread).
     */
    private static GenContext contextFor(ServerLevelAccessor level, Player player, BlockPos pos) {
        RandomSource rand = level.getRandom();
        return new GenContext(rand, WorldTier.getTier(player), player.getLuck(), level.getLevel().dimension(), level.getBiome(pos), GameStagesCompat.getStages(player));
    }

    private static boolean canSpawn(LevelAccessor world, Mob entity, double playerDist) {
        if (playerDist > entity.getType().getCategory().getDespawnDistance() * entity.getType().getCategory().getDespawnDistance() && entity.removeWhenFarAway(playerDist)) {
            return false;
        }
        else {
            return entity.checkSpawnRules(world, EntitySpawnReason.NATURAL) && entity.checkSpawnObstruction(world);
        }
    }

    @Nullable
    private static Component getName(Mob boss) {
        return boss.getSelfAndPassengers().filter(e -> PersistentDataComponent.get(e).contains(Invader.BOSS_KEY)).findFirst().map(Entity::getCustomName).orElse(null);
    }

    private static DynamicHolder<LootRarity> getRarity(Mob boss) {
        return boss.getSelfAndPassengers()
            .filter(e -> PersistentDataComponent.get(e).contains(Invader.BOSS_KEY))
            .findFirst()
            .map(ApothMobEvents::getRarityHolder)
            .orElse(RarityRegistry.INSTANCE.emptyHolder());
    }

    private static DynamicHolder<LootRarity> getRarityHolder(Entity entity) {
        Identifier id = Identifier.tryParse(PersistentDataComponent.get(entity).getString(Invader.RARITY_KEY).orElse(""));
        return RarityRegistry.INSTANCE.holder(id);
    }

    private static final Marker MARKER = MarkerFactory.getMarker(ApothMobEvents.class.getSimpleName());

    private static void debugLog(String msg, Object... args) {
        if (Apotheosis.DEBUG_MOBS) {
            Apotheosis.LOGGER.info(MARKER, msg, args);
        }
    }

}
