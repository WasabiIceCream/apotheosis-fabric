package dev.shadowsoffire.apotheosis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.util.DeathDropsCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * <ul>
 * <li>Upstream {@code EntityMixin}: a glowing entity without a team glows in its name colour (invaders glow in their
 * rarity colour when they spawn, {@code AdventureConfig.bossGlowOnSpawn}).</li>
 * <li>Death drop collection for {@code LivingEntityAdventureMixin}: every other {@code spawnAtLocation} overload ends in
 * this one, like NeoForge's drop capture.</li>
 * </ul>
 */
@Mixin(Entity.class)
public abstract class EntityAdventureMixin {

    @Shadow
    public abstract Component getCustomName();

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void apoth_getTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 0xFFFFFF) {
            Component name = this.getCustomName();
            if (name != null && name.getStyle().getColor() != null) {
                cir.setReturnValue(name.getStyle().getColor().getValue());
            }
        }
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("RETURN"))
    private void apoth_collectDeathDrop(ServerLevel level, ItemStack stack, Vec3 offset, CallbackInfoReturnable<ItemEntity> cir) {
        if (cir.getReturnValue() != null && this instanceof DeathDropsCollector collector) {
            collector.apoth$collectDrop(cir.getReturnValue());
        }
    }
}
