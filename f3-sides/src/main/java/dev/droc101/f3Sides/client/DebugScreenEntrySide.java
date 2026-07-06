package dev.droc101.f3Sides.client;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum DebugScreenEntrySide implements StringRepresentable {
    AUTO("auto"),
    LEFT("left"),
    RIGHT("right");

    public static final Codec<DebugScreenEntrySide> CODEC = StringRepresentable.fromEnum(DebugScreenEntrySide::values);
    private final String name;

    DebugScreenEntrySide(final String name) {
        this.name = name;
    }

    public @NonNull String getSerializedName() {
        return this.name;
    }
}
