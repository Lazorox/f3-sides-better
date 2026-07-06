package dev.droc101.f3Sides.client;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

// Controls whether a blank-line spacer is inserted next to this entry's
// lines when it renders. Default is NONE, so entries pack tightly together
// like vanilla. ABOVE inserts a blank line before this entry's lines, BELOW
// inserts one after.
public enum DebugScreenEntryGap implements StringRepresentable {
    NONE("none"),
    ABOVE("above"),
    BELOW("below");

    public static final Codec<DebugScreenEntryGap> CODEC = StringRepresentable.fromEnum(DebugScreenEntryGap::values);
    private final String name;

    DebugScreenEntryGap(final String name) {
        this.name = name;
    }

    public @NonNull String getSerializedName() {
        return this.name;
    }
}
