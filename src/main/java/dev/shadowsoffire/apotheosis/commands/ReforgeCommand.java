package dev.shadowsoffire.apotheosis.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ReforgeCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_RARITY = RarityCommand.SUGGEST_RARITY;

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reforge").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("rarity", IdentifierArgument.id()).suggests(SUGGEST_RARITY).executes(c -> {
            // Port addition: any living entity can run it (execute as <mob>), so it's testable without a player; a
            // non-player source uses a Pinnacle-tier context and gets the rolled affixes back as command feedback.
            net.minecraft.world.entity.Entity src = c.getSource().getEntityOrException();
            if (!(src instanceof net.minecraft.world.entity.LivingEntity le)) {
                throw net.minecraft.commands.CommandSourceStack.ERROR_NOT_PLAYER.create();
            }
            GenContext ctx = le instanceof Player p ? GenContext.forPlayer(p)
                : GenContext.standalone(le.getRandom(), dev.shadowsoffire.apotheosis.tiers.WorldTier.PINNACLE, 0, (net.minecraft.server.level.ServerLevel) le.level(), le.blockPosition());
            LootRarity rarity = RarityRegistry.INSTANCE.getValue(IdentifierArgument.getId(c, "rarity"));
            ItemStack stack = le.getMainHandItem();
            AffixHelper.setAffixes(stack, ItemAffixes.EMPTY);
            LootController.createLootItem(stack, rarity, ctx);
            if (!(le instanceof Player)) {
                String affixes = AffixHelper.getAffixes(stack).keySet().stream().map(h -> h.getId().toString()).sorted().collect(java.util.stream.Collectors.joining(", "));
                c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(stack.getItem() + " -> " + affixes), false);
            }
            return 0;
        })));
    }

}
