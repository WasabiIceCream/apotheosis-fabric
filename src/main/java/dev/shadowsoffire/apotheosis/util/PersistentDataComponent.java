package dev.shadowsoffire.apotheosis.util;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v8.component.CardinalComponent;

import dev.shadowsoffire.apotheosis.Apotheosis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A general-purpose, arbitrary {@link CompoundTag} store attached to any {@link Entity} —
 * replaces NeoForge's {@code Entity#getPersistentData()} (itself a NeoForge-only patch, not
 * vanilla — confirmed absent from the plain vanilla jar), which several ported affix call
 * sites use for small bits of scratch data (a projectile's source weapon, a player's reforge
 * seed). Not synced — every current use is server-only.
 * <p>
 * {@link #get(Entity)} returns the live, mutable tag, matching {@code getPersistentData()}'s
 * original call shape ({@code get(entity).put(...)}, {@code .getIntOr(...)}, etc.) so ported
 * call sites need minimal changes.
 */
public class PersistentDataComponent implements CardinalComponent {

    public static final ComponentKey<PersistentDataComponent> KEY = ComponentRegistry.getOrCreate(Apotheosis.loc("persistent_data"), PersistentDataComponent.class);

    private final CompoundTag tag = new CompoundTag();

    public static CompoundTag get(Entity entity) {
        return KEY.get(entity).tag;
    }

    @Override
    public void readData(ValueInput in) {
        in.read("data", CompoundTag.CODEC).ifPresent(this.tag::merge);
    }

    @Override
    public void writeData(ValueOutput out) {
        if (!this.tag.isEmpty()) {
            out.store("data", CompoundTag.CODEC, this.tag);
        }
    }

}
