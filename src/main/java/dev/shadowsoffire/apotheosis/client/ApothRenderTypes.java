package dev.shadowsoffire.apotheosis.client;

import java.util.function.Function;

import dev.shadowsoffire.apotheosis.mixin.RenderTypeInvoker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Render types for the loot beams. Without a shader pack the beam uses vanilla's translucent beacon beam; with one
 * (Iris), the translucent particle pipeline, which shader packs handle cleanly (Loot Beams Refork's approach, CC0).
 * The shadow under the item uses vanilla's entity shadow type with our texture, as upstream does.
 */
public final class ApothRenderTypes {

    private static final Function<Identifier, RenderType> BEAM = Util.memoize(texture -> RenderTypes.beaconBeam(texture, true));

    private static final Function<Identifier, RenderType> BEAM_SHADER = Util.memoize(texture -> RenderTypeInvoker.apoth$create("apotheosis_loot_beam",
        RenderSetup.builder(RenderPipelines.TRANSLUCENT_PARTICLE)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .sortOnUpload()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()));

    private static final Function<Identifier, RenderType> SHADOW = Util.memoize(RenderTypes::entityShadow);

    public static RenderType affixBeam(Identifier location, boolean shaderPack) {
        return shaderPack ? BEAM_SHADER.apply(location) : BEAM.apply(location);
    }

    public static RenderType affixShadow(Identifier location) {
        return SHADOW.apply(location);
    }

    private ApothRenderTypes() {}

}
