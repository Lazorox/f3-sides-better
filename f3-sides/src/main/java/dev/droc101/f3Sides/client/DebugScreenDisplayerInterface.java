package dev.droc101.f3Sides.client;

import net.minecraft.resources.Identifier;

public interface DebugScreenDisplayerInterface {

    default void f3_sides$setNextIdentifier(Identifier identifier) {
        throw new AssertionError();
    }

}
