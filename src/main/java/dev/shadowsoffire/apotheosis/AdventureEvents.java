package dev.shadowsoffire.apotheosis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.LivingDrops;
import dev.shadowsoffire.apotheosis.affix.effect.OmneticAffix;
import dev.shadowsoffire.apotheosis.affix.effect.TelepathicAffix;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.OmneticBonus;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.tiers.WorldTierComponent;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugment.Target;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugmentRegistry;
import dev.shadowsoffire.apotheosis.util.OmneticUtil;
import dev.shadowsoffire.apotheosis.util.OmneticUtil.OmneticData;
import dev.shadowsoffire.apotheosis.util.PersistentDataComponent;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of upstream's {@code AdventureEvents} handlers that 0.4.4 connected (the ones ported earlier live in
 * {@link Apotheosis}, {@code mobs.ApothMobEvents}, {@code affix.ProjectileHooks} and the mixins; the full map is in
 * {@code DEVLOG.md}, 0.4.4). NeoForge events are replaced by Fabric API events where one exists and by mixins that
 * call the static methods here otherwise.
 */
public final class AdventureEvents {

    private AdventureEvents() {}

    public static void register() {
        // Upstream: blockBreak (BreakBlockEvent, normal priority). Registered before the radial mining hook (low priority upstream).
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> blockBreak(player, level, pos, state));
        // Upstream: clone (PlayerEvent.Clone), the reforge seed survives death so dying can't reroll the reforging table.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            int oldSeed = PersistentDataComponent.get(oldPlayer).getIntOr(AffixHelper.REFORGE_SEED, 0);
            PersistentDataComponent.get(newPlayer).putInt(AffixHelper.REFORGE_SEED, oldSeed);
        });
        // Upstream: applyMissedTierAugments (EntityJoinLevelEvent), player half. The "applied" flag is not copied on death,
        // so the World Tier's player augments (luck, experience) come back after a respawn.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> applyMissedTierAugments(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> applyMissedTierAugments(newPlayer));
    }

    /**
     * Upstream {@code onDamage} ({@code LivingIncomingDamageEvent}, low priority): gem and affix {@code onHurt} for every
     * equipped item of the entity being hurt (Blast-Forged, Runed and the other damage reduction affixes, the damage
     * reduction and Mageslayer gem bonuses). Called from {@code mixin.LivingEntityHooksMixin} right after Apothic
     * Attributes' incoming damage handling (projectile damage, crits, dodge), like upstream's priorities.
     */
    public static float onHurt(LivingEntity ent, DamageSource src, float amount) {
        // World Tier damage reduction augments (upstream 9.1.0), before the equipment's own reductions.
        dev.shadowsoffire.apotheosis.attachments.DamageReductions reductions = ent.getAttached(dev.shadowsoffire.apotheosis.Apoth.Attachments.DAMAGE_REDUCTIONS);
        if (reductions != null) {
            amount = reductions.applyReductions(src, amount);
        }
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack s = ent.getItemBySlot(slot);
            if (s.isEmpty()) {
                continue;
            }
            amount = SocketHelper.getGems(s).onHurt(src, ent, amount);

            for (AffixInstance inst : AffixHelper.getAffixes(s).values()) {
                amount = inst.onHurt(src, ent, amount);
            }
        }
        return amount;
    }

    /**
     * Upstream {@code shieldBlock} ({@code LivingShieldBlockEvent}): gem and affix {@code onShieldBlock} for the item being
     * used to block. Returns the damage actually blocked.
     */
    public static float onShieldBlock(LivingEntity ent, DamageSource src, float blocked) {
        ItemStack stack = ent.getUseItem();
        if (stack.isEmpty() || blocked <= 0) {
            return blocked;
        }
        blocked = SocketHelper.getGems(stack).onShieldBlock(ent, src, blocked);

        for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
            blocked = inst.onShieldBlock(ent, src, blocked);
        }
        return blocked;
    }

    /**
     * Upstream {@code blockBreak} ({@code BreakBlockEvent}): gem and affix {@code onBlockBreak} for the main hand item.
     * Port note: runs after the block is broken ({@code PlayerBlockBreakEvents.AFTER}), so a break another mod cancels
     * (claims) doesn't trigger it.
     */
    public static void blockBreak(Player player, Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        SocketHelper.getGems(stack).onBlockBreak(player, level, pos, state);
        AffixHelper.streamAffixes(stack).forEach(inst -> inst.onBlockBreak(player, level, pos, state));
    }

    /**
     * Upstream {@code drops} (low priority, affix {@code modifyEntityLoot} for the killer's main hand: Festive) and
     * {@code dropsLowest} (lowest priority, Telepathic), on the drops collected by {@code mixin.LivingEntityDropsMixin}.
     * Item entities a hook added to the list are added to the level at the end.
     */
    public static void onEntityDrops(LivingEntity dead, DamageSource source, List<ItemEntity> collected, Set<ItemEntity> fromLoot) {
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        Set<ItemEntity> existing = Collections.newSetFromMap(new IdentityHashMap<>());
        existing.addAll(collected);
        LivingDrops drops = new LivingDrops(dead, source, new ArrayList<>(collected), fromLoot);

        if (source.getEntity() instanceof Player p) {
            AffixHelper.streamAffixes(p.getMainHandItem()).forEach(a -> a.modifyEntityLoot(drops));
        }
        TelepathicAffix.drops(drops);

        for (ItemEntity item : drops.drops()) {
            if (!existing.contains(item)) {
                level.addFreshEntity(item);
            }
        }
    }

    /**
     * Upstream {@code speed} ({@code PlayerEvent.BreakSpeed}, highest priority): Omnetic affixes and gems mine at the speed of
     * the best tool they stand for.
     */
    public static float omneticSpeed(Player player, BlockState state, float speed) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return speed;
        }
        OmneticData affixData = OmneticAffix.getData(stack);
        if (affixData != null) {
            speed = OmneticUtil.applyOmneticSpeed(player, state, speed, affixData);
        }
        OmneticData gemData = OmneticBonus.getData(stack);
        if (gemData != null) {
            speed = OmneticUtil.applyOmneticSpeed(player, state, speed, gemData);
        }
        return speed;
    }

    /**
     * Upstream {@code harvest} ({@code PlayerEvent.HarvestCheck}): Omnetic affixes and gems can harvest what their tools can.
     */
    public static boolean omneticHarvest(Player player, BlockState state) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        OmneticData affixData = OmneticAffix.getData(stack);
        if (affixData != null && OmneticUtil.canOmneticHarvest(state, affixData)) {
            return true;
        }
        OmneticData gemData = OmneticBonus.getData(stack);
        return gemData != null && OmneticUtil.canOmneticHarvest(state, gemData);
    }

    /**
     * Upstream {@code stackedOnOther} ({@code ItemStackedOnOtherEvent}): right-clicking a gem onto an item in an inventory
     * sockets it, if the item has a free socket that accepts the gem. Called from {@code mixin.ItemStackMixin}
     * ({@code ItemStack#overrideOtherStackedOnMe}). Returns true if the gem was socketed.
     */
    public static boolean stackedOnOther(ItemStack stack, ItemStack gemStack, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && gemStack.is(Apoth.Items.GEM) && slot.allowModification(player)) {
            ItemStack socketed = SocketHelper.socketGemInItem(stack, gemStack);
            if (!socketed.isEmpty()) {
                slot.set(socketed);
                access.set(gemStack.copyWithCount(gemStack.getCount() - 1));
                player.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1, 1.5F + 0.35F * (1 - 2 * player.getRandom().nextFloat()));
                return true;
            }
        }
        return false;
    }

    /**
     * Upstream {@code applyMissedTierAugments}, player half (the mob half is in {@code ApothMobEvents}).
     */
    public static void applyMissedTierAugments(ServerPlayer player) {
        WorldTierComponent comp = WorldTierComponent.get(player);
        if (comp.isTierAugmentsApplied()) {
            return;
        }
        WorldTier tier = WorldTier.getTier(player);
        for (TierAugment aug : TierAugmentRegistry.getAugments(tier, Target.PLAYERS)) {
            aug.apply(player.level(), player);
        }
        comp.setTierAugmentsApplied(true);
    }
}
