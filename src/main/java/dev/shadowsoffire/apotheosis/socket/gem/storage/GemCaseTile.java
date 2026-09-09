package dev.shadowsoffire.apotheosis.socket.gem.storage;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apotheosis.Apoth.Tiles;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.UnsocketedGem;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.network.VanillaPacketDispatcher;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Port note (NeoForge -> Fabric): drops the inner {@code GemCaseItemHandler} (a NeoForge
 * {@code transfer.ResourceHandler<ItemResource>} exposing this safe's contents to hoppers/pipes)
 * — automation access is out of scope for this port's core gameplay (affixes/rarities/gems/
 * sockets); the direct gameplay API ({@link #depositGem}/{@link #extractGem}/{@link #upgradeGem})
 * used by {@link GemCaseMenu} is unaffected. A Fabric {@code Storage<ItemVariant>} implementation
 * (mirroring the dropped handler's virtual-slot-per-gem-purity design, with
 * {@code SnapshotParticipant} for transactional rollback) could be added back later without
 * touching anything here.
 */
public abstract class GemCaseTile extends BlockEntity implements TickingBlockEntity {

    protected final Object2ObjectMap<DynamicHolder<Gem>, EnumMap<Purity, Integer>> gems = new Object2ObjectLinkedOpenHashMap<>();
    protected final Set<GemCaseMenu> activeContainers = new HashSet<>();
    protected final int maxCount;

    // Client-side only: Animation state for gem position switching
    private GemCaseAnimationState animationState;

    public GemCaseTile(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxCount) {
        super(type, pos, state);
        this.maxCount = maxCount;
    }

    /**
     * Inserts a gem into the safe.
     * Gems beyond the storage limit will be voided.
     */
    public void depositGem(ItemStack stack) {
        UnsocketedGem gem = UnsocketedGem.of(stack);
        if (!gem.isValid()) {
            return;
        }

        Purity purity = gem.purity();
        EnumMap<Purity, Integer> map = this.getGems(gem.gem());
        map.put(purity, Math.min(this.maxCount, map.get(purity) + stack.getCount()));

        if (!this.level.isClientSide()) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }
        this.setChanged();
    }

    /**
     * Extracts a gem from the safe, creating a new ItemStack with the requested gem/purity/count.
     * <p>
     * If the requested count exceeds the stored count, only the stored count will be extracted.
     */
    public ItemStack extractGem(DynamicHolder<Gem> gem, Purity purity, int count) {
        EnumMap<Purity, Integer> map = this.getGems(gem);
        int stored = map.get(purity);
        if (stored < count) {
            count = stored;
        }

        if (count <= 0 || !gem.isBound()) {
            return ItemStack.EMPTY;
        }

        map.put(purity, stored - count);

        ItemStack stack = GemItem.createStack(gem.get(), purity, count);

        if (!this.level.isClientSide()) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }

        this.setChanged();

        return stack;
    }

    public boolean upgradeGem(DynamicHolder<Gem> gem, Purity purity, Container matInv) {
        GemUpgradeMatch match = this.getUpgradeMatch(gem, purity, matInv);
        if (match != null) {
            match.execute(matInv, this.getGems(gem));

            if (!this.level.isClientSide()) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            }

            this.setChanged();
            return true;
        }
        else {
            return false;
        }
    }

    @Nullable
    public GemUpgradeMatch getUpgradeMatch(DynamicHolder<Gem> gem, Purity purity, Container matInv) {
        EnumMap<Purity, Integer> map = this.getGems(gem);
        if (map.get(purity) >= this.maxCount) {
            return null;
        }
        return GemUpgradeMatch.findMatch(this.level, purity, map, matInv);
    }

    public int getCount(DynamicHolder<Gem> gem, Purity purity) {
        return this.getGems(gem).get(purity);
    }

    public int getCount(Gem gem, Purity purity) {
        return this.getCount(GemRegistry.INSTANCE.holder(gem), purity);
    }

    /**
     * Returns the underlying purity-to-count map for the provided gem.
     */
    protected final EnumMap<Purity, Integer> getGems(DynamicHolder<Gem> gem) {
        return this.gems.computeIfAbsent(gem, g -> {
            EnumMap<Purity, Integer> map = new EnumMap<>(Purity.class);
            for (Purity p : Purity.values()) {
                map.put(p, 0);
            }
            return map;
        });
    }

    /**
     * Gets the animation state for this gem case, lazily initializing it on the client.
     * Should only be called on the client side.
     */
    public GemCaseAnimationState getAnimationState() {
        if (this.animationState == null) {
            this.animationState = new GemCaseAnimationState(this.level.getRandom());
        }
        return this.animationState;
    }

    public void saveGemData(CompoundTag tag) {
        CompoundTag gems = new CompoundTag();
        for (DynamicHolder<Gem> gem : this.gems.keySet()) {
            EnumMap<Purity, Integer> map = this.gems.get(gem);
            CompoundTag purityTag = new CompoundTag();
            for (Purity p : Purity.values()) {
                int count = map.get(p);
                if (count > 0) {
                    purityTag.putInt(p.getSerializedName(), count);
                }
            }
            gems.put(gem.getId().toString(), purityTag);
        }
        tag.put("gems", gems);
    }

    public void loadGemData(CompoundTag tag) {
        CompoundTag gems = tag.getCompoundOrEmpty("gems");
        for (String key : gems.keySet()) {
            Identifier res = Identifier.tryParse(key);
            DynamicHolder<Gem> gem = GemRegistry.INSTANCE.holder(res);
            if (!gem.isBound()) {
                continue;
            }
            CompoundTag purityTag = gems.getCompoundOrEmpty(key);
            if (purityTag.isEmpty()) {
                this.gems.remove(gem);
            }
            else {
                EnumMap<Purity, Integer> map = new EnumMap<>(Purity.class);
                for (Purity p : Purity.values()) {
                    map.put(p, purityTag.getIntOr(p.getSerializedName(), 0));
                }
                this.gems.put(gem, map);
            }
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag gemTag = new CompoundTag();
        this.saveGemData(gemTag);
        output.store("gems", CompoundTag.CODEC, gemTag);
    }

    /**
     * Port note (NeoForge -> Fabric): vanilla dropped {@code BlockEntity#onDataPacket} in this
     * version — the client applies a received {@code ClientboundBlockEntityDataPacket} straight
     * through the normal {@code loadAdditional} path, so this override (which just re-parsed the
     * "gems" tag) is redundant with {@link #loadAdditional}; the only extra behavior it had —
     * notifying {@link #activeContainers} of the change — is folded into {@link #loadAdditional}
     * here instead, guarded to the client side.
     */
    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("gems", CompoundTag.CODEC).ifPresent(gems -> {
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("gems", gems);
            this.loadGemData(wrapper);
        });
        if (this.level != null && this.level.isClientSide()) {
            this.activeContainers.forEach(GemCaseMenu::onChanged);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveGemData(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void clientTick(Level level, BlockPos pos, BlockState state) {
        GemCaseAnimationState animState = this.getAnimationState();

        int uniqueGems = 0;
        for (DynamicHolder<Gem> gem : this.gems.keySet()) {
            int count = 0;
            for (Purity p : Purity.ALL_PURITIES) {
                count += this.getCount(gem, p);
            }

            if (count > 0) {
                uniqueGems++;
            }
        }

        Player player = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4, false);

        animState.tick(Math.min(uniqueGems, 16), player != null);
    }

    public void addListener(GemCaseMenu ctr) {
        this.activeContainers.add(ctr);
    }

    public void removeListener(GemCaseMenu ctr) {
        this.activeContainers.remove(ctr);
    }

    public static class BasicGemCaseTile extends GemCaseTile {

        public BasicGemCaseTile(BlockPos pos, BlockState state) {
            super(Tiles.GEM_CASE, pos, state, Short.MAX_VALUE);
        }

    }

    public static class EnderGemCaseTile extends GemCaseTile {

        public EnderGemCaseTile(BlockPos pos, BlockState state) {
            super(Tiles.ENDER_GEM_CASE, pos, state, Integer.MAX_VALUE);
        }

    }

}
