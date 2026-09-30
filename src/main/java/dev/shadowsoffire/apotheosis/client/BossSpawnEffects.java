package dev.shadowsoffire.apotheosis.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.net.BossSpawnPayload.BossSpawnData;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the invader spawn announcement: the rarity's invader sound, and a rarity-coloured beacon beam at the
 * spawn position for 400 ticks.
 * <p>
 * Port note (NeoForge -> Fabric): upstream's {@code AdventureModuleClient#onBossSpawn}, {@code renderBossBeams}
 * ({@code SubmitCustomGeometryEvent}) and {@code time} ({@code ClientTickEvent.Post}). Here the beams are submitted from
 * Fabric's {@code LevelRenderEvents.COLLECT_SUBMITS}, which hands over the same submit collector, pose stack and level
 * render state, and the timer runs on {@code ClientTickEvents.END_CLIENT_TICK}.
 */
public class BossSpawnEffects {

    private static final List<BossSpawnData> BOSS_SPAWNS = new ArrayList<>();

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(BossSpawnEffects::renderBossBeams);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BOSS_SPAWNS.clear());
    }

    public static void onBossSpawn(BlockPos pos, DynamicHolder<LootRarity> rarityHolder) {
        if (rarityHolder.isBound()) {
            LootRarity rarity = rarityHolder.get();
            BOSS_SPAWNS.add(new BossSpawnData(pos, rarity, new MutableInt()));
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.getSoundManager().play(new SimpleSoundInstance(rarity.invaderSound(), SoundSource.HOSTILE, AdventureConfig.bossAnnounceRange / 16F, 1.0F, mc.player.getRandom(), pos));
            }
        }
    }

    private static void renderBossBeams(LevelRenderContext ctx) {
        if (BOSS_SPAWNS.isEmpty()) {
            return;
        }

        Vec3 camPos = ctx.levelState().cameraRenderState.pos;
        PoseStack poseStack = ctx.poseStack();
        float animTime = ctx.levelState().gameTime;

        for (BossSpawnData data : BOSS_SPAWNS) {
            BlockPos pos = data.pos();
            int color = 0xFF000000 | data.rarity().color().getValue();

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
            BeaconRenderer.submitBeaconBeam(poseStack, ctx.submitNodeCollector(), BeaconRenderer.BEAM_LOCATION, 1.0F, animTime, 0, 1024, color, 0.2F, 0.25F);
            poseStack.popPose();
        }
    }

    private static void tick() {
        for (int i = 0; i < BOSS_SPAWNS.size(); i++) {
            BossSpawnData data = BOSS_SPAWNS.get(i);
            if (data.ticks().getAndIncrement() > 400) {
                BOSS_SPAWNS.remove(i--);
            }
        }
    }

}
