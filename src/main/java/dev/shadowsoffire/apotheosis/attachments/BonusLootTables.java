package dev.shadowsoffire.apotheosis.attachments;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Bonus loot tables can be attached to a {@link Mob} and will be rolled when the mob is killed.
 * <p>
 * Port note: this record itself is vanilla-only, ported verbatim. Its only real consumers
 * upstream (boss data, Twilight Forest treasure goblin compat) are boss/spawner-related and
 * out of this port's Adventure-module scope, so the NeoForge attachment-registration plumbing
 * ({@code Apoth.Attachments.BONUS_LOOT_TABLES}, the {@code MobMixin} death hook that calls
 * {@link #drop}) is intentionally not ported yet. When bosses are ever in scope, that plumbing
 * should use Cardinal Components API (already installed on the target server) instead of a
 * NeoForge-style attachment, and the death hook should use
 * {@code net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH} (verified
 * during initial research to cover NeoForge's {@code LivingDeathEvent} for this exact purpose).
 */
public record BonusLootTables(List<ResourceKey<LootTable>> tables) {

    public static final BonusLootTables EMPTY = new BonusLootTables(List.of());

    public static final Codec<BonusLootTables> CODEC = ResourceKey.codec(Registries.LOOT_TABLE).listOf().xmap(BonusLootTables::new, BonusLootTables::tables);

    /**
     * Rolls all additional loot tables contained in this attachment and spawns the items via {@link Mob#spawnAtLocation}.
     */
    public void drop(Mob owner, DamageSource source, boolean hitByPlayer) {
        ServerLevel serverLevel = (ServerLevel) owner.level();
        for (ResourceKey<LootTable> key : this.tables) {
            // This is a copy of LivingEntity#dropLootFromLootTable - we can't invoke it directly since Mob overrides it
            LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(key);
            if (table == LootTable.EMPTY) {
                continue;
            }

            LootParams.Builder lootparams$builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, owner)
                .withParameter(LootContextParams.ORIGIN, owner.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
            net.minecraft.world.entity.player.Player lastHurtByPlayer = owner.getLastHurtByPlayer();
            if (hitByPlayer && lastHurtByPlayer != null) {
                lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, lastHurtByPlayer)
                    .withLuck(lastHurtByPlayer.getLuck());
            }

            LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
            table.getRandomItems(lootparams, owner.getLootTableSeed(), stack -> owner.spawnAtLocation(serverLevel, stack));
        }
    }

    public boolean isEmpty() {
        return this.tables.isEmpty();
    }

    public BonusLootTables mergeWith(BonusLootTables other) {
        List<ResourceKey<LootTable>> combined = new ArrayList<>();
        combined.addAll(this.tables);
        combined.addAll(other.tables);
        return new BonusLootTables(combined);
    }

}
