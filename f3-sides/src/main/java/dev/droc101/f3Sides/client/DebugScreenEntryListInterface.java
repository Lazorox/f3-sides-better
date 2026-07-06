package dev.droc101.f3Sides.client;

import net.minecraft.resources.Identifier;

public interface DebugScreenEntryListInterface {
    default void f3sides$setSide(final Identifier location, final DebugScreenEntrySide side) {
        throw new AssertionError("implemented in mixin");
    }

    default DebugScreenEntrySide f3sides$getSide(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }

    // Returns the order in which this entry was explicitly assigned a side
    // (lower = assigned earlier). Entries that are on AUTO (never explicitly
    // assigned, or reset back to Auto) return Integer.MAX_VALUE so they sort
    // after every manually-placed entry.
    default int f3sides$getActivationOrder(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }

    default void f3sides$setGap(final Identifier location, final DebugScreenEntryGap gap) {
        throw new AssertionError("implemented in mixin");
    }

    default DebugScreenEntryGap f3sides$getGap(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }

    // Swaps this entry with its immediate neighbor in the current render
    // order (one position up/down). Works regardless of whether either
    // entry already has an explicit order or is still on AUTO.
    default void f3sides$moveUp(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }

    default void f3sides$moveDown(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }

    default boolean f3sides$canMoveUp(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }

    default boolean f3sides$canMoveDown(final Identifier location) {
        throw new AssertionError("implemented in mixin");
    }
}
