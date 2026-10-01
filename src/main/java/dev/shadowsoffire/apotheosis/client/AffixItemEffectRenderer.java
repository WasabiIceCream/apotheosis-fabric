package dev.shadowsoffire.apotheosis.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRenderData;
import dev.shadowsoffire.apotheosis.loot.RarityRenderData.ShadowData;
import dev.shadowsoffire.apotheosis.particle.RarityParticleData;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Loot beams: a rarity-coloured beam, a tinted shadow and (epic and up) rising particles on dropped items with a
 * rarity, as configured by each rarity's {@code render_data}.
 * <p>
 * Port note (NeoForge -> Fabric): upstream's {@code AffixItemEffectRenderer}. Beams and shadows are submitted from
 * Fabric's {@code LevelRenderEvents.COLLECT_SUBMITS} (upstream: {@code SubmitCustomGeometryEvent}), particles spawn on
 * {@code ClientTickEvents.END_CLIENT_TICK} (upstream: {@code ClientTickEvent.Post}), and the three per-item values
 * upstream keeps in NeoForge data attachments live in a client-side weak map. Upstream's beam, glow, shadow and
 * particle textures postdate its asset license split, so the rarities point at vanilla's beacon beam and shadow and
 * the particle uses vanilla's glow sprite.
 * <p>
 * Gameoverse: the beam itself, the textures, the drop chime and the shader-pack mode are Loot Beams Refork's
 * (github.com/TUsama/Loot-Beams-Refork, CC0), see {@link BeamRenderer}. The rarity's {@code render_data} still
 * decides which items get a beam, its height, whether it has a bloom ({@code glow_radius}), the shadow and particles.
 */
public class AffixItemEffectRenderer {

    private static final float GROW_IN_TICKS = 15F;

    /** Beams fade out beyond this distance (blocks) from the camera, as in Loot Beams Refork. */
    private static final float FADE_DISTANCE = 15F;
    private static final int BEAM_ALPHA = (int) (0.75F * 255);

    /**
     * Per dropped item: [0] tick the beam started growing (-1 while not on the ground), [1] next particle tick,
     * [2] 1 once seen in the air, [3] 1 once the drop chime played.
     */
    private static final Map<ItemEntity, int[]> STATE = new WeakHashMap<>();

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(AffixItemEffectRenderer::submitBeams);
        ClientTickEvents.END_CLIENT_TICK.register(AffixItemEffectRenderer::spawnParticles);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.clear());
    }

    private static int[] state(ItemEntity item) {
        return STATE.computeIfAbsent(item, k -> new int[] { -1, 0, 0, 0 });
    }

    private static java.lang.reflect.Method irisInUse;
    private static Object irisApi;
    private static boolean irisChecked;

    /** True while an Iris shader pack is active (reflection, so Iris stays optional). */
    static boolean shaderPackInUse() {
        if (!irisChecked) {
            irisChecked = true;
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")) {
                try {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    irisApi = api.getMethod("getInstance").invoke(null);
                    irisInUse = api.getMethod("isShaderPackInUse");
                }
                catch (ReflectiveOperationException | LinkageError e) {
                    irisApi = null;
                }
            }
        }
        if (irisApi == null) {
            return false;
        }
        try {
            return (Boolean) irisInUse.invoke(irisApi);
        }
        catch (ReflectiveOperationException e) {
            irisApi = null;
            return false;
        }
    }

    private static void submitBeams(LevelRenderContext ctx) {
        if (!AdventureConfig.enableAffixItemEffects) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Vec3 camPos = ctx.levelState().cameraRenderState.pos;
        PoseStack pose = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        float partials = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean shaderPack = shaderPackInUse();

        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof ItemEntity item)) {
                continue;
            }

            ItemStack stack = item.getItem();
            DynamicHolder<LootRarity> rarityHolder = AffixHelper.getRarity(stack);
            if (!rarityHolder.isBound()) {
                continue;
            }
            if (!item.onGround()) {
                int[] s = state(item);
                s[0] = -1;
                s[2] = 1;
                continue;
            }

            int[] s = state(item);
            if (s[0] < 0) {
                s[0] = item.tickCount;
            }

            LootRarity rarity = rarityHolder.get();
            RarityRenderData renderData = rarity.renderData();
            int color = rarity.color().getValue();

            float progress = Mth.clamp(item.tickCount - s[0] + partials, 0, GROW_IN_TICKS) / GROW_IN_TICKS;

            double x = Mth.lerp(partials, item.xOld, item.getX());
            double y = Mth.lerp(partials, item.yOld, item.getY());
            double z = Mth.lerp(partials, item.zOld, item.getZ());

            // Translate to the item entity in camera space; shadow rendering relies on this outer translation.
            pose.pushPose();
            pose.translate(x - camPos.x, y - camPos.y, z - camPos.z);

            float beamHeight = renderData.beamHeight();
            if (beamHeight > 0) {
                if (s[2] == 1 && s[3] == 0) {
                    // A beamed item seen falling has just landed. Items already lying there when they came into
                    // view stay quiet, so walking into a loot pile doesn't set off a chorus.
                    s[3] = 1;
                    mc.level.playLocalSound(x, y, z, LOOT_DROP, net.minecraft.sounds.SoundSource.AMBIENT, 0.1F, 1.0F, false);
                }
                float dist = (float) Math.sqrt(camPos.distanceToSqr(x, y, z));
                float alpha = BEAM_ALPHA * (dist > FADE_DISTANCE ? 1 / Math.max((dist - FADE_DISTANCE) / FADE_DISTANCE, 1.0F) : 1);
                float yaw = (float) Math.atan2(x - camPos.x, z - camPos.z);
                float halfWidth = Math.max(renderData.beamRadius() * 1.25F, 0.04F);
                float bloomWidth = renderData.glowRadius() > 0 ? halfWidth * 1.35F : 0;
                BeamRenderer.render(pose, collector, renderData.beamTexture(), renderData.glowTexture(), yaw, beamHeight * progress, halfWidth,
                    bloomWidth, color, (int) alpha, shaderPack);
            }

            ShadowData shadow = renderData.shadow();
            ShadowRenderer.renderShadow(pose, collector, item, partials, mc.level, shadow, ARGB.color(shadow.alpha(), color));

            pose.popPose();
        }
    }

    /** Loot Beams Refork's drop chime (three variants), heard within 8 blocks. */
    private static final net.minecraft.sounds.SoundEvent LOOT_DROP = net.minecraft.sounds.SoundEvent.createFixedRangeEvent(
        dev.shadowsoffire.apotheosis.Apotheosis.loc("loot_drop"), 8.0F);

    private static void spawnParticles(Minecraft mc) {
        if (!AdventureConfig.enableAffixItemEffects || mc.level == null || mc.isPaused()) {
            return;
        }

        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof ItemEntity item)) {
                continue;
            }

            ItemStack stack = item.getItem();
            DynamicHolder<LootRarity> rarityHolder = AffixHelper.getRarity(stack);
            if (!rarityHolder.isBound() || !item.onGround()) {
                continue;
            }

            LootRarity rarity = rarityHolder.get();
            if (!rarity.renderData().particle().enabled()) {
                continue;
            }

            int[] s = state(item);
            if (item.tickCount - s[1] <= 0) {
                continue;
            }

            int color = rarity.color().getValue();
            RarityParticleData opt = new RarityParticleData(ARGB.red(color) / 255F, ARGB.green(color) / 255F, ARGB.blue(color) / 255F);
            RandomSource rand = item.getRandom();
            double spread = 0.1;
            mc.level.addParticle(opt,
                item.getX() - spread + rand.nextDouble() * 2 * spread,
                item.getY(),
                item.getZ() - spread + rand.nextDouble() * 2 * spread,
                0, 0.03 + 0.005 * rand.nextGaussian(), 0);
            s[1] = item.tickCount + 10 + rand.nextInt(15);
        }
    }

}
