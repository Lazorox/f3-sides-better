package dev.droc101.f3Sides.client.mixin;

import dev.droc101.f3Sides.client.DebugScreenEntryGap;
import dev.droc101.f3Sides.client.DebugScreenEntryListInterface;
import dev.droc101.f3Sides.client.DebugScreenEntrySide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(targets = "net.minecraft.client.gui.screens.debug.DebugOptionsScreen$OptionEntry")
public abstract class OptionEntryMixin {
    // Small square reorder buttons - fixed width, no need to measure text.
    @Unique
    private static final int ARROW_WIDTH = 14;

    // Per-button horizontal padding added on top of the widest label text
    // among that button's possible values, and a floor so tiny labels
    // (e.g. "Auto") don't produce a cramped-looking button.
    @Unique
    private static final int CYCLE_BUTTON_PADDING = 16;

    @Unique
    private static final int CYCLE_BUTTON_MIN_WIDTH = 40;

    @Unique
    private CycleButton<DebugScreenEntryStatus> statusButton;

    @Unique
    private CycleButton<DebugScreenEntrySide> sideButton;

    @Unique
    private CycleButton<DebugScreenEntryGap> gapButton;

    @Unique
    private Button upButton;

    @Unique
    private Button downButton;

    @Unique
    private static Component GetStatusText(DebugScreenEntryStatus status) {
        switch (status) {
            case ALWAYS_ON -> {
                return Component.translatable("debug.entry.always");
            }
            case IN_OVERLAY -> {
                return Component.translatable("debug.entry.overlay");
            }
            case NEVER -> {
                return Component.translatable("debug.entry.never");
            }
        }
        return Component.literal(status.getSerializedName());
    }

    @Unique
    private static Component GetSideText(DebugScreenEntrySide side) {
        switch (side) {
            case AUTO -> {
                return Component.translatable("debug.entry.auto");
            }
            case LEFT -> {
                return Component.translatable("debug.entry.left");
            }
            case RIGHT -> {
                return Component.translatable("debug.entry.right");
            }
        }
        return Component.literal(side.getSerializedName());
    }

    @Unique
    private static Component GetGapText(DebugScreenEntryGap gap) {
        switch (gap) {
            case NONE -> {
                return Component.translatable("debug.entry.gap.none");
            }
            case ABOVE -> {
                return Component.translatable("debug.entry.gap.above");
            }
            case BELOW -> {
                return Component.translatable("debug.entry.gap.below");
            }
        }
        return Component.literal(gap.getSerializedName());
    }

    @Unique
    private void SetNextStatus(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryStatus status = Minecraft.getInstance().debugEntries.getStatus(entry.location);
        switch (status) {
            case ALWAYS_ON -> entry.setValue(entry.location, DebugScreenEntryStatus.NEVER);
            case IN_OVERLAY -> entry.setValue(entry.location, DebugScreenEntryStatus.ALWAYS_ON);
            case NEVER -> entry.setValue(entry.location, DebugScreenEntryStatus.IN_OVERLAY);
        }
    }

    @Unique
    private void SetNextSide(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryListInterface entries = (DebugScreenEntryListInterface)(Minecraft.getInstance().debugEntries);
        DebugScreenEntrySide side = entries.f3sides$getSide(entry.location);
        switch (side) {
            case AUTO -> setSide(entry.location, DebugScreenEntrySide.LEFT);
            case LEFT -> setSide(entry.location, DebugScreenEntrySide.RIGHT);
            case RIGHT -> setSide(entry.location, DebugScreenEntrySide.AUTO);
        }
    }

    @Unique
    public final void setSide(final Identifier location, final DebugScreenEntrySide side) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        ((DebugScreenEntryListInterface)entries).f3sides$setSide(location, side);

        entry.refreshEntry();
    }

    @Unique
    private void SetNextGap(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryListInterface entries = (DebugScreenEntryListInterface)(Minecraft.getInstance().debugEntries);
        DebugScreenEntryGap gap = entries.f3sides$getGap(entry.location);
        switch (gap) {
            case NONE -> setGap(entry.location, DebugScreenEntryGap.ABOVE);
            case ABOVE -> setGap(entry.location, DebugScreenEntryGap.BELOW);
            case BELOW -> setGap(entry.location, DebugScreenEntryGap.NONE);
        }
    }

    @Unique
    public final void setGap(final Identifier location, final DebugScreenEntryGap gap) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        ((DebugScreenEntryListInterface)entries).f3sides$setGap(location, gap);

        entry.refreshEntry();
    }

    @Unique
    private void MoveUp(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryListInterface entries = (DebugScreenEntryListInterface)(Minecraft.getInstance().debugEntries);
        entries.f3sides$moveUp(entry.location);
        entry.refreshEntry();
    }

    @Unique
    private void MoveDown(DebugOptionsScreen.OptionEntry entry) {
        DebugScreenEntryListInterface entries = (DebugScreenEntryListInterface)(Minecraft.getInstance().debugEntries);
        entries.f3sides$moveDown(entry.location);
        entry.refreshEntry();
    }

    // Vanilla's CycleButton was always created at a flat 80px regardless of
    // what it actually displays, which is what ate into the label's space
    // in the first place - "Auto"/"Left"/"Right" and "No Gap" never needed
    // anywhere near that much room. Sizing each button to its own widest
    // possible value (plus a little padding) reclaims that wasted space for
    // the label instead.
    @Unique
    private static <T> int measureButtonWidth(Font font, T[] values, Function<T, Component> textFor) {
        int widest = 0;
        for (T value : values) {
            widest = Math.max(widest, font.width(textFor.apply(value)));
        }
        return Math.max(CYCLE_BUTTON_MIN_WIDTH, widest + CYCLE_BUTTON_PADDING);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/debug/DebugOptionsScreen$OptionEntry;refreshEntry()V"))
    void CtorInject(CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        Font font = Minecraft.getInstance().font;

        int statusWidth = measureButtonWidth(font, DebugScreenEntryStatus.values(), OptionEntryMixin::GetStatusText);
        int sideWidth = measureButtonWidth(font, DebugScreenEntrySide.values(), OptionEntryMixin::GetSideText);
        int gapWidth = measureButtonWidth(font, DebugScreenEntryGap.values(), OptionEntryMixin::GetGapText);

        statusButton = CycleButton.builder(OptionEntryMixin::GetStatusText, DebugScreenEntryStatus.NEVER)
                .withValues(DebugScreenEntryStatus.values())
                .create(
                        10, 5, statusWidth, 16,
                        Component.translatable("debug.entry.mode"),
                        (button, value) -> SetNextStatus(entry));

        sideButton = CycleButton.builder(OptionEntryMixin::GetSideText, DebugScreenEntrySide.AUTO)
                .withValues(DebugScreenEntrySide.values())
                .create(
                        10, 5, sideWidth, 16,
                        Component.translatable("debug.entry.side"),
                        (button, value) -> SetNextSide(entry));

        gapButton = CycleButton.builder(OptionEntryMixin::GetGapText, DebugScreenEntryGap.NONE)
                .withValues(DebugScreenEntryGap.values())
                .create(
                        10, 5, gapWidth, 16,
                        Component.translatable("debug.entry.gap"),
                        (button, value) -> SetNextGap(entry));

        upButton = Button.builder(Component.literal("▲"), (button) -> MoveUp(entry))
                .tooltip(Tooltip.create(Component.translatable("debug.entry.move.up")))
                .size(ARROW_WIDTH, 16)
                .build();

        downButton = Button.builder(Component.literal("▼"), (button) -> MoveDown(entry))
                .tooltip(Tooltip.create(Component.translatable("debug.entry.move.down")))
                .size(ARROW_WIDTH, 16)
                .build();

        entry.children.clear();
        entry.children.add(upButton);
        entry.children.add(downButton);
        entry.children.add(statusButton);
        entry.children.add(sideButton);
        entry.children.add(gapButton);
    }

    // Total horizontal space consumed by the reorder arrows plus the three
    // mode/side/gap buttons, including the small gaps between them. Shared
    // between the label-truncation calc and the button-positioning calc so
    // the two can never disagree about where the buttons actually start.
    @Unique
    private int reservedButtonsWidth() {
        int arrows = upButton.getWidth() + 1 + downButton.getWidth();
        int mainButtons = statusButton.getWidth() + 2 + sideButton.getWidth() + 2 + gapButton.getWidth();
        return arrows + 4 + mainButtons;
    }

    // Vanilla draws the entry label at full width with no truncation at
    // all - it was only ever designed with room for a single button on the
    // right. Now that the reorder arrows and three cycle buttons take up
    // more space, long entry names (mod ids, biome ids, etc.) would run
    // straight under them. This redirects that draw call so the label is
    // shortened with "..." only when it would actually overlap the buttons,
    // using each button's real (now text-sized, not flat 80px) width.
    @Redirect(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    void truncateLabel(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);

        int available = entry.getContentWidth() - reservedButtonsWidth() - 4;

        String toDraw = text;
        if (available > 0 && font.width(text) > available) {
            int ellipsisWidth = font.width("...");
            toDraw = font.plainSubstrByWidth(text, Math.max(0, available - ellipsisWidth)) + "...";
        }

        graphics.text(font, toDraw, x, y, color);
    }

    @Inject(method="extractContent", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CycleButton;setX(I)V"), cancellable = true)
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);

        int buttonsStartX = entry.getContentX() + entry.getContentWidth() - reservedButtonsWidth();

        upButton.setX(buttonsStartX);
        downButton.setX(upButton.getX() + upButton.getWidth() + 1);
        statusButton.setX(downButton.getX() + downButton.getWidth() + 4);
        sideButton.setX(statusButton.getX() + statusButton.getWidth() + 2);
        gapButton.setX(sideButton.getX() + sideButton.getWidth() + 2);

        upButton.setY(entry.getContentY());
        downButton.setY(entry.getContentY());
        statusButton.setY(entry.getContentY());
        sideButton.setY(entry.getContentY());
        gapButton.setY(entry.getContentY());

        upButton.extractRenderState(graphics, mouseX, mouseY, a);
        downButton.extractRenderState(graphics, mouseX, mouseY, a);
        statusButton.extractRenderState(graphics, mouseX, mouseY, a);
        sideButton.extractRenderState(graphics, mouseX, mouseY, a);
        gapButton.extractRenderState(graphics, mouseX, mouseY, a);

        ci.cancel();
    }

    @Inject(method="refreshEntry", at=@At("TAIL"))
    void refreshEntry(CallbackInfo ci) {
        DebugOptionsScreen.OptionEntry entry = (DebugOptionsScreen.OptionEntry) ((Object) this);
        DebugScreenEntryList entries = Minecraft.getInstance().debugEntries;
        DebugScreenEntryListInterface entriesInterface = (DebugScreenEntryListInterface) entries;

        DebugScreenEntrySide side = entriesInterface.f3sides$getSide(entry.location);
        sideButton.setValue(side);
        DebugScreenEntryGap gap = entriesInterface.f3sides$getGap(entry.location);
        gapButton.setValue(gap);
        DebugScreenEntryStatus statusValue = entries.getStatus(entry.location);
        statusButton.setValue(statusValue);

        upButton.active = entriesInterface.f3sides$canMoveUp(entry.location);
        downButton.active = entriesInterface.f3sides$canMoveDown(entry.location);

        boolean isNoop = DebugScreenEntries.getEntry(entry.location) instanceof DebugEntryNoop;
        if (isNoop) {
            sideButton.active = false;
            gapButton.active = false;
            upButton.active = false;
            downButton.active = false;
        }
    }

}
