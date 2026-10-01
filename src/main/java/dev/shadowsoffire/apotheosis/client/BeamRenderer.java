package dev.shadowsoffire.apotheosis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.shadowsoffire.apotheosis.Apotheosis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * The loot beam: a camera-facing strip with a fainter, wider bloom strip and a flame-shaped cap, after Loot Beams
 * Refork (github.com/TUsama/Loot-Beams-Refork, CC0; textures from there too). Apotheosis's own beam (four rotating
 * beacon segments) didn't suit a beam this thin: its textures postdate upstream's asset license split, and vanilla's
 * beacon texture breaks into dashes at this width.
 */
public class BeamRenderer {

    public static final Identifier BEAM_TOP = Apotheosis.loc("textures/rarity/beam_top.png");

    /**
     * @param yaw        rotation (radians, around Y) that turns the strip to face the camera
     * @param height     beam height in blocks (already scaled by the grow-in progress)
     * @param halfWidth  half the main strip's width
     * @param bloomWidth half the bloom strip's width, 0 for none
     * @param color      RGB of the rarity
     * @param alpha      0-255 opacity of the main strip (bloom is 40% of it)
     */
    public static void render(PoseStack pose, SubmitNodeCollector collector, Identifier beamTexture, Identifier bloomTexture, float yaw, float height,
        float halfWidth, float bloomWidth, int color, int alpha, boolean shaderPack) {
        if (height <= 0 || alpha <= 0) {
            return;
        }
        float capTop = height * 1.25F;
        int main = ARGB.color(alpha, color);
        int bloom = ARGB.color((int) (alpha * 0.4F), color);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(yaw));
        collector.submitCustomGeometry(pose, ApothRenderTypes.affixBeam(beamTexture, shaderPack), (p, buf) -> quad(p, buf, main, -halfWidth, halfWidth, 0, height, shaderPack));
        if (bloomWidth > 0) {
            collector.submitCustomGeometry(pose, ApothRenderTypes.affixBeam(bloomTexture, shaderPack), (p, buf) -> quad(p, buf, bloom, -bloomWidth, bloomWidth, 0, height, shaderPack));
        }
        collector.submitCustomGeometry(pose, ApothRenderTypes.affixBeam(BEAM_TOP, shaderPack), (p, buf) -> {
            quad(p, buf, main, -halfWidth, halfWidth, height, capTop, shaderPack);
            if (bloomWidth > 0) {
                quad(p, buf, bloom, -bloomWidth, bloomWidth, height, capTop, shaderPack);
            }
        });
        pose.popPose();
    }

    /** A vertical quad in the strip's plane; the texture's top row is drawn at the top. */
    private static void quad(PoseStack.Pose pose, VertexConsumer buf, int color, float minX, float maxX, float minY, float maxY, boolean shaderPack) {
        vertex(pose, buf, color, minX, minY, 0, 1, shaderPack);
        vertex(pose, buf, color, minX, maxY, 0, 0, shaderPack);
        vertex(pose, buf, color, maxX, maxY, 1, 0, shaderPack);
        vertex(pose, buf, color, maxX, minY, 1, 1, shaderPack);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buf, int color, float x, float y, float u, float v, boolean shaderPack) {
        buf.addVertex(pose, x, y, 0).setUv(u, v).setColor(color).setLight(15728880);
        if (!shaderPack) {
            buf.setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0.0F, 0.0F, 1.0F);
        }
    }
}
