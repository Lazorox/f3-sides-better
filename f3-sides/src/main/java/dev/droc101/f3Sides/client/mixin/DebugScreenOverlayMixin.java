package dev.droc101.f3Sides.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.droc101.f3Sides.client.DebugScreenDisplayerInterface;
import dev.droc101.f3Sides.client.DebugScreenEntryGap;
import dev.droc101.f3Sides.client.DebugScreenEntryListInterface;
import dev.droc101.f3Sides.client.DebugScreenEntrySide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {

    // Tracks the order identifiers are naturally encountered in each frame,
    // used only as a tiebreaker for entries that were never explicitly
    // assigned a side.
    @Unique
    private final Map<Identifier, Integer> f3sides$naturalOrder = new LinkedHashMap<>();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    void resetOrder(CallbackInfo ci) {
        f3sides$naturalOrder.clear();
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;display(Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
    void setNextId(GuiGraphicsExtractor graphics, CallbackInfo ci, @Local(name = "id") Identifier id, @Local(name = "displayer") DebugScreenDisplayer displayer) {
        ((DebugScreenDisplayerInterface) displayer).f3_sides$setNextIdentifier(id);
        f3sides$naturalOrder.putIfAbsent(id, f3sides$naturalOrder.size());
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 3))
    boolean addGroupsCorrectly(List<String> instance, @Local(name = "groups") Map<Identifier, Collection<String>> groups, @Local(name = "leftLines") List<String> leftLines, @Local(name = "rightLines") List<String> rightLines) {
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        DebugScreenEntryListInterface entriesInterface = (DebugScreenEntryListInterface) entries;

        // Sort primarily by when each entry was explicitly assigned a side
        // (earlier activation = stays higher up). Entries that are still on
        // AUTO have no activation order, so they fall back to the natural
        // per-frame order and sort after every manually-placed entry.
        List<Identifier> sortedKeys = new ArrayList<>(groups.keySet());
        sortedKeys.sort(
                Comparator.<Identifier>comparingInt(entriesInterface::f3sides$getActivationOrder)
                        .thenComparingInt(id -> f3sides$naturalOrder.getOrDefault(id, Integer.MAX_VALUE))
        );

        for (Identifier key : sortedKeys) {
            Collection<String> lines = groups.get(key);
            if (lines == null || lines.isEmpty()) {
                continue;
            }

            DebugScreenEntrySide side = entriesInterface.f3sides$getSide(key);
            if (side == DebugScreenEntrySide.AUTO) {
                if (leftLines.size() > rightLines.size()) {
                    side = DebugScreenEntrySide.RIGHT;
                } else {
                    side = DebugScreenEntrySide.LEFT;
                }
            }

            List<String> target = (side == DebugScreenEntrySide.LEFT) ? leftLines : rightLines;

            // Default is no spacer at all, so entries pack tightly like
            // vanilla. A spacer is only inserted when this entry explicitly
            // requests one via its Gap setting. "Above" is skipped if this
            // is the first entry on its side (nothing to separate from).
            DebugScreenEntryGap gap = entriesInterface.f3sides$getGap(key);
            if (gap == DebugScreenEntryGap.ABOVE && !target.isEmpty()) {
                target.add("");
            }

            target.addAll(lines);

            if (gap == DebugScreenEntryGap.BELOW) {
                target.add("");
            }
        }

        return true;
    }

}
