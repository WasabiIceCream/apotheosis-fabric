package dev.shadowsoffire.apotheosis.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;

/**
 * Exposes {@link ClientAdvancements}' private {@code progress} map — needed by
 * {@code ApothMiscUtil#hasAdvancment} to check a completed advancement client-side (e.g. for the
 * World Tier Select screen, which reads {@code WorldTier#isUnlocked} entirely client-side).
 * <p>
 * Port bug found via live runtime testing: this field has no public accessor at all in this
 * version — confirmed via javap (only {@code get(Identifier)}, which returns the
 * {@link AdvancementHolder} itself, not its progress; and {@code setListener}, a single-slot
 * callback already owned by the vanilla advancements GUI screen while it's open, not something
 * safe to steal). Without this, the client-side check was stubbed to always return {@code false}
 * (see {@code ApothMiscUtil}'s prior TODO) — meaning a tier's unlock advancement could be
 * genuinely earned server-side and still show as permanently locked in the Tier Select screen,
 * since the screen never learned about it. An accessor mixin, not reflection, matches this
 * codebase's existing convention for this exact class of "private field, no getter" problem
 * (see {@code LivingEntityInvoker}).
 */
@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccessor {

    @Accessor("progress")
    Map<AdvancementHolder, AdvancementProgress> apoth$getProgress();

}
