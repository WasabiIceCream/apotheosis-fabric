package dev.shadowsoffire.apotheosis.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Port note (NeoForge/Apothic-Attributes -> Fabric): {@code cat.getSlots()} now returns vanilla
 * {@link EquipmentSlotGroup} instead of Apothic-Attributes' {@code EntitySlotGroup} — see
 * {@code Apoth.LootCategories}'s javadoc. {@code EquipmentSlotGroup} has no {@code .id()}, so
 * this reports {@code getSerializedName()} instead.
 */
public class CategoryCheckCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("loot_category").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            // Port addition: works for any living entity (execute as <mob>) and reports as command feedback.
            net.minecraft.world.entity.Entity src = c.getSource().getEntityOrException();
            if (!(src instanceof net.minecraft.world.entity.LivingEntity le)) {
                throw net.minecraft.commands.CommandSourceStack.ERROR_NOT_PLAYER.create();
            }
            ItemStack stack = le.getMainHandItem();
            LootCategory cat = LootCategory.forItem(stack);
            EquipmentSlotGroup slots = cat.isNone() ? null : cat.getSlots();
            c.getSource().sendSuccess(() -> Component.literal("Loot Category - " + (cat.isNone() ? "none" : cat.getKey())), false);
            c.getSource().sendSuccess(() -> Component.literal("Equipment Slot - " + (slots == null ? "null" : slots.getSerializedName())), false);
            return 0;
        }));
    }

}
