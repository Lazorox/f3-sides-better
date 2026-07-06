package dev.droc101.f3Sides.client.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.droc101.f3Sides.client.DebugScreenEntryGap;
import dev.droc101.f3Sides.client.DebugScreenEntryListInterface;
import dev.droc101.f3Sides.client.DebugScreenEntrySide;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(DebugScreenEntryList.class)
public abstract class DebugScreenEntryListMixin implements DebugScreenEntryListInterface {

    @Unique
    private final Map<Identifier, DebugScreenEntrySide> allSides = new HashMap<>();

    // Per-entry gap setting (none/above/below). Defaults to NONE (no
    // spacer), so entries pack tightly unless the user opts in.
    @Unique
    private final Map<Identifier, DebugScreenEntryGap> allGaps = new HashMap<>();

    // Records the order entries were explicitly assigned a side in, so
    // rendering can keep earlier-activated entries above later ones instead
    // of relying on Minecraft's own internal ordering or an auto-balance.
    @Unique
    private final Map<Identifier, Integer> activationOrder = new HashMap<>();

    @Unique
    private int nextActivationIndex = 0;

    @Unique
    private File debugSidesFile;

    @Unique
    private Codec<DebugScreenEntryListMixin.SerializedSides> sidesCodec = SerializedSides.CODEC;

    @Inject(method = "<init>", at = @At("CTOR_HEAD"))
    void ctor(File workingDirectory, DataFixer dataFixer, CallbackInfo ci) {
        debugSidesFile = new File(workingDirectory, "debug-sides-profile.json");
    }

    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;rebuildCurrentList()V"))
    void load(CallbackInfo ci) {
        DebugScreenEntryList list = (DebugScreenEntryList) ((Object) this);

        try {
            if (!debugSidesFile.isFile()) {
                return;
            }

            Dynamic<JsonElement> data = new Dynamic<>(JsonOps.INSTANCE, StrictJsonParser.parse(FileUtils.readFileToString(debugSidesFile, StandardCharsets.UTF_8)));
            DebugScreenEntryListMixin.SerializedSides serializedOptions = sidesCodec.parse(data).getOrThrow((error) -> new IOException("Could not parse debug sides profile JSON: " + error));
            resetSides(serializedOptions.custom().orElse(Map.of()), serializedOptions.order().orElse(Map.of()), serializedOptions.nextOrder().orElse(0), serializedOptions.gaps().orElse(Map.of()));
        } catch (JsonSyntaxException | IOException e) {
            DebugScreenEntryList.LOGGER.error("Couldn't read debug sides profile file {}, resetting to default", debugSidesFile, e);
            list.save();
        }

    }

    @Inject(method="save", at=@At("TAIL"))
    void save(CallbackInfo ci) {
        SerializedSides serializedOptions = new SerializedSides(Optional.of(allSides), Optional.of(activationOrder), Optional.of(nextActivationIndex), Optional.of(allGaps));

        try {
            FileUtils.writeStringToFile(debugSidesFile, sidesCodec.encodeStart(JsonOps.INSTANCE, serializedOptions).getOrThrow().toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            DebugScreenEntryList.LOGGER.error("Failed to save debug sides profile file {}", debugSidesFile, e);
        }
    }

    @Environment(EnvType.CLIENT)
    record SerializedSides(Optional<Map<Identifier, DebugScreenEntrySide>> custom, Optional<Map<Identifier, Integer>> order, Optional<Integer> nextOrder, Optional<Map<Identifier, DebugScreenEntryGap>> gaps) {
        private static final Codec<Map<Identifier, DebugScreenEntrySide>> CUSTOM_ENTRIES_CODEC;
        private static final Codec<Map<Identifier, Integer>> ORDER_CODEC;
        private static final Codec<Map<Identifier, DebugScreenEntryGap>> GAPS_CODEC;
        public static final Codec<DebugScreenEntryListMixin.SerializedSides> CODEC;

        static {
            CUSTOM_ENTRIES_CODEC = Codec.unboundedMap(Identifier.CODEC, DebugScreenEntrySide.CODEC);
            ORDER_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.INT);
            GAPS_CODEC = Codec.unboundedMap(Identifier.CODEC, DebugScreenEntryGap.CODEC);
            CODEC = RecordCodecBuilder.create((i) -> i.group(
                    CUSTOM_ENTRIES_CODEC.optionalFieldOf("data").forGetter(DebugScreenEntryListMixin.SerializedSides::custom),
                    ORDER_CODEC.optionalFieldOf("order").forGetter(DebugScreenEntryListMixin.SerializedSides::order),
                    Codec.INT.optionalFieldOf("nextOrder").forGetter(DebugScreenEntryListMixin.SerializedSides::nextOrder),
                    GAPS_CODEC.optionalFieldOf("gaps").forGetter(DebugScreenEntryListMixin.SerializedSides::gaps)
            ).apply(i, DebugScreenEntryListMixin.SerializedSides::new));
        }
    }

    @Override
    public void f3sides$setSide(final Identifier location, final DebugScreenEntrySide side) {
        DebugScreenEntryList list = (DebugScreenEntryList) ((Object) this);
        allSides.put(location, side);
        if (side == DebugScreenEntrySide.AUTO) {
            activationOrder.remove(location);
        } else {
            activationOrder.put(location, nextActivationIndex++);
        }
        list.rebuildCurrentList();
        list.save();
    }

    @Override
    public DebugScreenEntrySide f3sides$getSide(final Identifier location) {
        return allSides.getOrDefault(location, DebugScreenEntrySide.AUTO);
    }

    @Override
    public int f3sides$getActivationOrder(final Identifier location) {
        return activationOrder.getOrDefault(location, Integer.MAX_VALUE);
    }

    @Override
    public void f3sides$setGap(final Identifier location, final DebugScreenEntryGap gap) {
        DebugScreenEntryList list = (DebugScreenEntryList) ((Object) this);
        if (gap == DebugScreenEntryGap.NONE) {
            allGaps.remove(location);
        } else {
            allGaps.put(location, gap);
        }
        list.save();
    }

    @Override
    public DebugScreenEntryGap f3sides$getGap(final Identifier location) {
        return allGaps.getOrDefault(location, DebugScreenEntryGap.NONE);
    }

    // Same ordering rule the overlay uses when rendering (explicit order
    // first, then a stable fallback) so that "neighbor" here means the same
    // thing it will mean on screen. The overlay's own natural per-frame
    // order isn't available from the options menu, so ties are broken by
    // identifier instead - this only affects entries that are still on AUTO
    // and haven't been explicitly ordered yet.
    //
    // Only entries that are actually shown (status != NEVER) are included -
    // otherwise a swap could target a hidden entry, which has no visible
    // position at all and would make the move look like it did nothing.
    @Unique
    private List<Identifier> f3sides$sortedEntryOrder() {
        DebugScreenEntryList list = (DebugScreenEntryList) ((Object) this);

        List<Identifier> sorted = new ArrayList<>();
        for (Identifier id : DebugScreenEntries.allEntries().keySet()) {
            if (list.getStatus(id) != DebugScreenEntryStatus.NEVER) {
                sorted.add(id);
            }
        }

        sorted.sort(
                Comparator.<Identifier>comparingInt(id -> activationOrder.getOrDefault(id, Integer.MAX_VALUE))
                        .thenComparing(Identifier::toString)
        );
        return sorted;
    }

    @Override
    public void f3sides$moveUp(final Identifier location) {
        f3sides$move(location, -1);
    }

    @Override
    public void f3sides$moveDown(final Identifier location) {
        f3sides$move(location, 1);
    }

    @Override
    public boolean f3sides$canMoveUp(final Identifier location) {
        int index = f3sides$sortedEntryOrder().indexOf(location);
        return index > 0;
    }

    @Override
    public boolean f3sides$canMoveDown(final Identifier location) {
        List<Identifier> sorted = f3sides$sortedEntryOrder();
        int index = sorted.indexOf(location);
        return index >= 0 && index < sorted.size() - 1;
    }

    @Unique
    private void f3sides$move(final Identifier location, final int direction) {
        DebugScreenEntryList list = (DebugScreenEntryList) ((Object) this);

        List<Identifier> sorted = f3sides$sortedEntryOrder();
        int index = sorted.indexOf(location);
        int neighborIndex = index + direction;
        if (index < 0 || neighborIndex < 0 || neighborIndex >= sorted.size()) {
            return;
        }

        Identifier neighbor = sorted.get(neighborIndex);

        Integer locationOrder = activationOrder.get(location);
        Integer neighborOrder = activationOrder.get(neighbor);

        if (locationOrder == null && neighborOrder == null) {
            // Neither entry has ever been given an explicit order - they're
            // both just tied at "unassigned" and sorting by fallback. A
            // plain swap here would remove-then-remove and change nothing,
            // which is exactly why moves were silently failing. Instead,
            // materialize two fresh, adjacent order values now so the swap
            // actually takes effect, with `location` ending up on the
            // correct side of `neighbor`.
            int lower = nextActivationIndex++;
            int higher = nextActivationIndex++;
            if (direction < 0) {
                activationOrder.put(location, lower);
                activationOrder.put(neighbor, higher);
            } else {
                activationOrder.put(neighbor, lower);
                activationOrder.put(location, higher);
            }
        } else {
            // At least one side already has a real order value - a direct
            // swap correctly moves `location` into `neighbor`'s slot (and
            // vice versa), whether or not the other side has one too.
            if (neighborOrder != null) {
                activationOrder.put(location, neighborOrder);
            } else {
                activationOrder.remove(location);
            }

            if (locationOrder != null) {
                activationOrder.put(neighbor, locationOrder);
            } else {
                activationOrder.remove(neighbor);
            }
        }

        list.rebuildCurrentList();
        list.save();
    }

    @Unique
    public final void resetSides(final Map<Identifier, DebugScreenEntrySide> newEntries, final Map<Identifier, Integer> newOrder, final int newNextIndex, final Map<Identifier, DebugScreenEntryGap> newGaps) {
        allSides.clear();
        allSides.putAll(newEntries);
        activationOrder.clear();
        activationOrder.putAll(newOrder);
        nextActivationIndex = newNextIndex;
        allGaps.clear();
        allGaps.putAll(newGaps);
    }

}
