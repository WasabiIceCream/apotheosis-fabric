package dev.shadowsoffire.apotheosis.commands;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.ApothMobEvents;
import dev.shadowsoffire.apotheosis.mobs.registries.EliteRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.InvaderRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Elite;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.util.PersistentDataComponent;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /apoth spawn_boss} (upstream) and {@code /apoth spawn_elite} (port addition).
 * <p>
 * <pre>
 * /apoth spawn_boss &lt;pos|entity&gt; [invader] [rarity] [send_notification]
 * /apoth spawn_elite &lt;pos|entity&gt; [elite]
 * </pre>
 * Both use the executing player (or the nearest player within 64 blocks of the position) for the generation context.
 * Port notes: with no player in reach (e.g. from the server console or RCON with nobody nearby) they fall back to a
 * Haven, zero-luck context instead of failing, and say so. Upstream's entity branch read the {@code pos} argument when
 * {@code send_notification} was given (a crash); fixed here. {@code spawn_elite} picks a random entity type from the
 * elite's {@code entities} set, runs its normal {@code finalizeSpawn} (spawn reason {@code COMMAND}), then transforms it
 * immediately instead of waiting for the join-level queue, so it works without a player online.
 */
public class BossCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_BOSS = (ctx, builder) -> SharedSuggestionProvider.suggest(InvaderRegistry.INSTANCE.getKeys().stream().map(Identifier::toString), builder);

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_ELITE = (ctx, builder) -> SharedSuggestionProvider.suggest(EliteRegistry.INSTANCE.getKeys().stream().map(Identifier::toString), builder);

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("spawn_boss").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        // Brigadier doesn't really do branching commands very well.
        builder.then(
            Commands.argument("pos", Vec3Argument.vec3())
                .then(Commands.argument("boss", IdentifierArgument.id()).suggests(SUGGEST_BOSS)
                    .then(Commands.argument("rarity", IdentifierArgument.id()).suggests(RarityCommand.SUGGEST_RARITY)
                        .then(Commands.argument("send_notification", BoolArgumentType.bool())
                            .executes(c -> spawnBoss(c, Vec3Argument.getVec3(c, "pos"), IdentifierArgument.getId(c, "boss"), IdentifierArgument.getId(c, "rarity"), BoolArgumentType.getBool(c, "send_notification"))))
                        .executes(c -> spawnBoss(c, Vec3Argument.getVec3(c, "pos"), IdentifierArgument.getId(c, "boss"), IdentifierArgument.getId(c, "rarity"))))
                    .executes(c -> spawnBoss(c, Vec3Argument.getVec3(c, "pos"), IdentifierArgument.getId(c, "boss"), null)))
                .executes(c -> spawnBoss(c, Vec3Argument.getVec3(c, "pos"), null, null)));

        builder.then(
            Commands.argument("entity", EntityArgument.entity())
                .then(Commands.argument("boss", IdentifierArgument.id()).suggests(SUGGEST_BOSS)
                    .then(Commands.argument("rarity", IdentifierArgument.id()).suggests(RarityCommand.SUGGEST_RARITY)
                        .then(Commands.argument("send_notification", BoolArgumentType.bool())
                            .executes(c -> spawnBoss(c, EntityArgument.getEntity(c, "entity").position(), IdentifierArgument.getId(c, "boss"), IdentifierArgument.getId(c, "rarity"), BoolArgumentType.getBool(c, "send_notification"))))
                        .executes(c -> spawnBoss(c, EntityArgument.getEntity(c, "entity").position(), IdentifierArgument.getId(c, "boss"), IdentifierArgument.getId(c, "rarity"))))
                    .executes(c -> spawnBoss(c, EntityArgument.getEntity(c, "entity").position(), IdentifierArgument.getId(c, "boss"), null)))
                .executes(c -> spawnBoss(c, EntityArgument.getEntity(c, "entity").position(), null, null)));

        root.then(builder);

        LiteralArgumentBuilder<CommandSourceStack> elite = Commands.literal("spawn_elite").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        elite.then(Commands.argument("pos", Vec3Argument.vec3())
            .then(Commands.argument("elite", IdentifierArgument.id()).suggests(SUGGEST_ELITE)
                .executes(c -> spawnElite(c, Vec3Argument.getVec3(c, "pos"), IdentifierArgument.getId(c, "elite"))))
            .executes(c -> spawnElite(c, Vec3Argument.getVec3(c, "pos"), null)));
        elite.then(Commands.argument("entity", EntityArgument.entity())
            .then(Commands.argument("elite", IdentifierArgument.id()).suggests(SUGGEST_ELITE)
                .executes(c -> spawnElite(c, EntityArgument.getEntity(c, "entity").position(), IdentifierArgument.getId(c, "elite"))))
            .executes(c -> spawnElite(c, EntityArgument.getEntity(c, "entity").position(), null)));
        root.then(elite);
    }

    public static int spawnBoss(CommandContext<CommandSourceStack> c, Vec3 pos, @Nullable Identifier bossId, @Nullable Identifier rarityId) {
        return spawnBoss(c, pos, bossId, rarityId, false);
    }

    public static int spawnBoss(CommandContext<CommandSourceStack> c, Vec3 pos, @Nullable Identifier bossId, @Nullable Identifier rarityId, boolean sendNotification) {
        ServerLevel level = c.getSource().getLevel();
        GenContext ctx = context(c, pos);

        Invader boss = bossId == null ? InvaderRegistry.INSTANCE.getRandomItem(ctx) : InvaderRegistry.INSTANCE.getValue(bossId);
        if (boss == null) {
            if (bossId != null) {
                c.getSource().sendFailure(Component.literal("Unknown boss: " + bossId));
            }
            else {
                c.getSource().sendFailure(Component.literal("No bosses available for the current context!"));
            }
            return -2;
        }

        Mob bossEntity;

        if (rarityId != null) {
            DynamicHolder<LootRarity> rarity = RarityRegistry.INSTANCE.holder(rarityId);
            if (!rarity.isBound()) {
                c.getSource().sendFailure(Component.literal("Unknown rarity: " + rarityId));
                return -3;
            }
            bossEntity = boss.createBoss(level, BlockPos.containing(pos), ctx, rarity.get());
        }
        else {
            bossEntity = boss.createBoss(level, BlockPos.containing(pos), ctx);
        }

        level.addFreshEntityWithPassengers(bossEntity);

        if (sendNotification) {
            ApothMobEvents.sendInvaderSpawnNotification(level, bossEntity);
        }

        Identifier id = InvaderRegistry.INSTANCE.getKey(boss);
        c.getSource().sendSuccess(() -> Component.literal("Spawned invader " + id + " (" + bossEntity.getUUID() + ") at " + bossEntity.blockPosition().toShortString() + ", tier " + ctx.tier().getSerializedName()), true);
        return 1;
    }

    public static int spawnElite(CommandContext<CommandSourceStack> c, Vec3 pos, @Nullable Identifier eliteId) {
        ServerLevel level = c.getSource().getLevel();
        GenContext ctx = context(c, pos);

        Elite elite = eliteId == null ? EliteRegistry.INSTANCE.getRandomItem(ctx) : EliteRegistry.INSTANCE.getValue(eliteId);
        if (elite == null) {
            c.getSource().sendFailure(Component.literal(eliteId != null ? "Unknown elite: " + eliteId : "No elites available for the current context!"));
            return -2;
        }

        List<Holder<EntityType<?>>> types = elite.getEntities().stream().toList();
        if (types.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Elite " + EliteRegistry.INSTANCE.getKey(elite) + " matches every entity type; summon a mob and let it roll naturally instead."));
            return -3;
        }

        EntityType<?> type = types.get(ctx.rand().nextInt(types.size())).value();
        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Mob mob)) {
            c.getSource().sendFailure(Component.literal("Could not create a mob of type " + EntityType.getKey(type)));
            return -4;
        }

        mob.snapTo(pos.x, pos.y, pos.z, ctx.rand().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);
        // finalizeSpawn may have rolled (and marked) a different elite; the command's choice wins.
        PersistentDataComponent.get(mob).remove(Elite.MINIBOSS_KEY);
        PersistentDataComponent.get(mob).remove(Elite.PLAYER_KEY);
        elite.transformMiniboss(level, mob, ctx);
        level.addFreshEntityWithPassengers(mob);

        Identifier id = EliteRegistry.INSTANCE.getKey(elite);
        c.getSource().sendSuccess(() -> Component.literal("Spawned elite " + id + " as " + EntityType.getKey(type) + " (" + mob.getUUID() + ") at " + mob.blockPosition().toShortString() + ", tier " + ctx.tier().getSerializedName()), true);
        return 1;
    }

    private static GenContext context(CommandContext<CommandSourceStack> c, Vec3 pos) {
        Entity source = c.getSource().getEntity();
        ServerLevel level = c.getSource().getLevel();
        Player summoner = source instanceof Player p ? p : level.getNearestPlayer(pos.x(), pos.y(), pos.z(), 64, false);
        if (summoner != null) {
            return GenContext.forPlayerAtPos(summoner.getRandom(), summoner, BlockPos.containing(pos));
        }
        c.getSource().sendSystemMessage(Component.literal("No player within 64 blocks; using a Haven, zero-luck generation context."));
        return GenContext.standalone(level.getRandom(), WorldTier.HAVEN, 0, level, BlockPos.containing(pos));
    }

}
