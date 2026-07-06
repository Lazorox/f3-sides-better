package dev.droc101.f3Sides.client.mixin;

import dev.droc101.f3Sides.client.DebugScreenDisplayerInterface;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(targets = "net.minecraft.client.gui.components.DebugScreenOverlay$1")
public class DebugScreenDisplayerMixin implements DebugScreenDisplayerInterface {
    @Unique
    Identifier nextId = Identifier.withDefaultNamespace("unknown");

    @Inject(method="addLine", at=@At("HEAD"), cancellable = true)
    void addLine(String line, CallbackInfo ci) {
        ((DebugScreenDisplayer)(Object)this).addToGroup(nextId, line);
        ci.cancel();
    }

    @Inject(method="addPriorityLine", at=@At("HEAD"), cancellable = true)
    void addPriorityLine(String line, CallbackInfo ci) {
        ((DebugScreenDisplayer)(Object)this).addToGroup(nextId, line);
        ci.cancel();
    }

    @ModifyVariable(method = "addToGroup(Lnet/minecraft/resources/Identifier;Ljava/lang/String;)V", at = @At("HEAD"), name = "group", argsOnly = true)
    private Identifier modifyGroupA(Identifier group) {
        return nextId;
    }

    @ModifyVariable(method = "addToGroup(Lnet/minecraft/resources/Identifier;Ljava/util/Collection;)V", at = @At("HEAD"), name = "group", argsOnly = true)
    private Identifier modifyGroupB(Identifier group) {
        return nextId;
    }

    @Override
    public void f3_sides$setNextIdentifier(Identifier identifier) {
        nextId = identifier;
    }
}
