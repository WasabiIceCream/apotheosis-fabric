package dev.shadowsoffire.apotheosis.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apoth.LootCategories;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.IdentifierException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;

/**
 * Port note (NeoForge -> Fabric): {@link #getSlots()} now returns vanilla's
 * {@link EquipmentSlotGroup} instead of Apothic-Attributes' {@code EntitySlotGroup} — see
 * {@code Apoth.LootCategories}'s javadoc. {@link #forItem} drops the
 * {@code Apoth.DataMaps.LOOT_CATEGORY_OVERRIDES} per-item override lookup (NeoForge Data Maps
 * have no Fabric equivalent, and no DataMaps system is ported yet at all) — TODO: revisit if
 * a per-item category override is ever actually needed; nothing in the ported scope uses it
 * yet.
 */
public final class LootCategory {

    public static final Codec<LootCategory> CODEC = Codec.lazyInitialized(() -> Apoth.BuiltInRegs.LOOT_CATEGORY.byNameCodec());
    public static final Codec<Set<LootCategory>> SET_CODEC = PlaceboCodecs.setOf(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, LootCategory> STREAM_CODEC = ByteBufCodecs.registry(Apoth.BuiltInRegs.LOOT_CATEGORY.key());

    private static List<LootCategory> sortedCategories = new ArrayList<>();

    private final Predicate<ItemStack> validator;
    private final EquipmentSlotGroup slots;
    private final int priority;

    @Nullable
    private String descId;

    public LootCategory(Predicate<ItemStack> validator, EquipmentSlotGroup slots, int priority) {
        this.validator = Preconditions.checkNotNull(validator);
        this.slots = Preconditions.checkNotNull(slots);
        this.priority = priority;
    }

    public LootCategory(Predicate<ItemStack> validator, EquipmentSlotGroup slots) {
        this(validator, slots, 1000);
    }

    public String getDescId() {
        return this.getOrCreateDescriptionId();
    }

    public String getDescIdPlural() {
        return this.getDescId() + ".plural";
    }

    public Identifier getKey() {
        return Apoth.BuiltInRegs.LOOT_CATEGORY.getKey(this);
    }

    public int priority() {
        return this.priority;
    }

    /**
     * Returns the relevant equipment slot group for this item.
     * The passed item should be of the type this category represents.
     */
    public EquipmentSlotGroup getSlots() {
        return this.slots;
    }

    public boolean isValid(ItemStack stack) {
        return this.validator.test(stack);
    }

    @Deprecated(forRemoval = true)
    public boolean isArmor() {
        return this == LootCategories.HELMET || this == LootCategories.CHESTPLATE || this == LootCategories.LEGGINGS || this == LootCategories.BOOTS;
    }

    @Deprecated(forRemoval = true)
    public boolean isBreaker() {
        return this == LootCategories.BREAKER;
    }

    @Deprecated(forRemoval = true)
    public boolean isRanged() {
        return this == LootCategories.BOW || this == LootCategories.TRIDENT;
    }

    @Deprecated(forRemoval = true)
    public boolean isDefensive() {
        return this.isArmor() || this == LootCategories.SHIELD;
    }

    @Deprecated(forRemoval = true)
    public boolean isMelee() {
        return this == LootCategories.MELEE_WEAPON || this == LootCategories.TRIDENT;
    }

    @Deprecated(forRemoval = true)
    public boolean isMeleeOrShield() {
        return this.isMelee() || this == LootCategories.SHIELD;
    }

    public boolean isNone() {
        return this == LootCategories.NONE;
    }

    @Override
    public String toString() {
        return String.format("LootCategory[%s]", this.getKey());
    }

    protected String getOrCreateDescriptionId() {
        if (this.descId == null) {
            this.descId = Util.makeDescriptionId("loot_category", this.getKey());
        }

        return this.descId;
    }

    public static <T> MapCodec<Map<LootCategory, T>> mapCodec(Codec<T> codec) {
        return Codec.simpleMap(LootCategory.CODEC, codec, Apoth.BuiltInRegs.LOOT_CATEGORY::keys);
    }

    /**
     * Determines the loot category for an item, by iterating all the categories and selecting the first matching one.
     * <p>
     * TODO: Cache this result as a CachedObject sensitive to all component changes.
     *
     * @param stack The item to find the category for.
     * @return The first valid loot category, or {@link LootCategories#NONE} if no categories were valid.
     */
    public static LootCategory forItem(ItemStack stack) {
        if (sortedCategories.isEmpty()) {
            throw new UnsupportedOperationException("Attempted to resolve the loot category for an item before loot categories were registered!");
        }

        if (stack.isEmpty()) {
            return LootCategories.NONE;
        }

        for (LootCategory c : sortedCategories) {
            if (c.isValid(stack)) {
                return c;
            }
        }
        return LootCategories.NONE;
    }

    @Nullable
    private static Identifier readLocWithApothNamespace(String path) {
        try {
            return path.contains(":") ? Identifier.parse(path) : Apotheosis.loc(path);
        }
        catch (IdentifierException resourcelocationexception) {
            return null;
        }
    }

    @ApiStatus.Internal
    public static class Inner {

        public static void rebuildSortedValueList() {
            var list = new ArrayList<LootCategory>();
            for (LootCategory cat : Apoth.BuiltInRegs.LOOT_CATEGORY) {
                list.add(cat);
            }
            Collections.sort(list, Comparator.comparing(LootCategory::priority));
            LootCategory.sortedCategories = list;
        }
    }
}
