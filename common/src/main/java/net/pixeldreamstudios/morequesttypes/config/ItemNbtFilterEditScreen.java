package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterData;
import net.pixeldreamstudios.morequesttypes.util.NbtPathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ItemNbtFilterEditScreen extends AbstractThreePanelScreen<ItemNbtFilterEditScreen.FilterContentPanel> {
    private static final int ROW = 22;
    private static final int PATH_ROW = 30;
    private static final int SECTION = 24;
    private static final int PAD = 8;
    private static final int GAP = 4;
    private static final int SECTION_GAP = 10;
    private static final int MAX_PATHS = 100;
    private static final int ACCENT = 4;
    private static final int BOTTOM_SCROLL_PAD = 48;
    private static final int FOOTER_BUTTON_ROW = 27;

    private final Component title;
    private final Supplier<ItemStack> previewSupplier;
    private final ItemNbtFilterEditScreens.FilterEditState state;
    private final ConfigCallback callback;

    private TextBox pathBox;
    private TextBox matchValueBox;
    private TextBox snbtBox;
    private BooleanToggleButton booleanToggle;
    private WrappedLabelWidget valuePreviewLabel;
    private WrappedLabelWidget rawPreviewLabel;
    private WrappedLabelWidget encodedPreviewLabel;
    private PreviewBlockWidget previewBlock;
    private final List<PathSelectButton> pathButtons = new ArrayList<>();
    private FilterContentPanel contentPanel;

    public ItemNbtFilterEditScreen(
            Supplier<ItemStack> previewSupplier,
            ItemNbtFilterEditScreens.FilterEditState state,
            ConfigCallback callback
    ) {
        this.previewSupplier = previewSupplier;
        this.state = state;
        this.callback = callback;
        this.title = Component.translatable("morequesttypes.config.item_nbt_filter_entry.title");
    }

    private ItemStack previewStack() {
        ItemStack stack = previewSupplier.get();
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public Component getTitle() {
        return title;
    }

    @Override
    public boolean onInit() {
        if (state.entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT) {
            if (!state.snbtManual) {
                ItemNbtFilterEditScreens.syncFromPath(previewStack(), state, false);
            }
        } else if (state.entry.type != ItemNbtFilterData.FilterEntry.Type.KEY_EXISTS
                && state.matchValue.isBlank()
                && !state.entry.path.isBlank()) {
            ItemNbtFilterEditScreens.syncFromPath(previewStack(), state, false);
        }
        showScrollBar(true);
        return setSizeProportional(0.56f, 0.88f);
    }

    @Override
    protected int getTopPanelHeight() {
        return 26;
    }

    @Override
    protected Panel createTopPanel() {
        return new TitleTopPanel(this, title);
    }

    @Override
    protected int getBottomPanelHeight() {
        return 52;
    }

    @Override
    protected Panel createBottomPanel() {
        return new FilterFooterPanel(this);
    }

    @Override
    protected FilterContentPanel createMainPanel() {
        contentPanel = new FilterContentPanel(this);
        return contentPanel;
    }

    @Override
    protected void doAccept() {
        readFields();
        ItemNbtFilterEditScreens.finalizeState(previewStack(), state);
        callback.save(true);
        closeGui(true);
    }

    @Override
    protected void doCancel() {
        callback.save(false);
        closeGui(true);
    }

    void readFields() {
        if (pathBox != null) {
            state.entry.path = pathBox.getText().trim();
        }
        if (matchValueBox != null) {
            state.matchValue = matchValueBox.getText();
        }
        if (booleanToggle != null) {
            state.matchValue = booleanToggle.isTrueValue() ? "true" : "false";
        }
        if (snbtBox != null) {
            state.snbtPreview = snbtBox.getText();
            if (state.entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT) {
                state.entry.value = snbtBox.getText();
            }
        }
    }

    void rebuild() {
        readFields();
        refreshWidgets();
    }

    void selectPath(String path) {
        state.entry.path = path;
        state.snbtManual = false;
        ItemNbtFilterEditScreens.syncFromPath(previewStack(), state, true);
        ItemNbtFilterEditScreens.finalizeState(previewStack(), state);

        if (pathBox != null) {
            pathBox.setText(state.entry.path);
        }
        applyMatchValueToWidgets();
        updatePreviewLabels();
        refreshPathHighlights();
    }

    void resetMatchValueFromItem() {
        if (state.entry.path.isBlank()) {
            return;
        }
        state.snbtManual = false;
        state.matchValue = NbtPathUtil.defaultEditableValue(previewStack(), state.entry.path);
        state.snbtPreview = NbtPathUtil.snbtForPathWithValue(previewStack(), state.entry.path, state.matchValue);
        applyMatchValueToWidgets();
        updatePreviewLabels();
    }

    void applyMatchValueToWidgets() {
        if (matchValueBox != null) {
            matchValueBox.setText(state.matchValue);
        }
        if (booleanToggle != null) {
            booleanToggle.setTrueValue(parseBooleanValue(state.matchValue));
        }
        if (snbtBox != null) {
            snbtBox.setText(state.snbtPreview);
        }
    }

    void onMatchValueEdited() {
        if (state.entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT && !state.snbtManual) {
            state.snbtPreview = NbtPathUtil.snbtForPathWithValue(previewStack(), state.entry.path, state.matchValue);
            if (snbtBox != null) {
                snbtBox.setText(state.snbtPreview);
            }
        }
        updatePreviewLabels();
    }

    void refreshPathHighlights() {
        for (PathSelectButton button : pathButtons) {
            button.refreshHighlight();
        }
    }

    void updatePreviewLabels() {
        ItemNbtFilterEditScreens.finalizeState(previewStack(), state);
        Tag tag = state.entry.path.isBlank() ? null : NbtPathUtil.getTagAtPath(previewStack(), state.entry.path);
        if (valuePreviewLabel != null) {
            valuePreviewLabel.setText(previewLine(
                    "morequesttypes.config.item_nbt_matching.value_preview", NbtPathUtil.formatTagValue(tag)));
        }
        if (rawPreviewLabel != null) {
            rawPreviewLabel.setText(previewLine(
                    "morequesttypes.config.item_nbt_matching.raw_preview",
                    tag != null ? tag.toString() : "(no path selected)"));
        }
        if (encodedPreviewLabel != null) {
            String encoded = state.entry.encode().isBlank() ? "(incomplete)" : state.entry.encode();
            encodedPreviewLabel.setText(previewLine("morequesttypes.config.item_nbt_matching.encoded_preview", encoded));
        }
        refreshWidgets();
    }

    @Override
    public boolean mouseScrolled(double scroll) {
        FilterContentPanel main = contentPanel;
        if (main != null && main.scrollPanel(scroll)) {
            return true;
        }
        return super.mouseScrolled(scroll);
    }

    private static boolean parseBooleanValue(String value) {
        return value != null && !value.equalsIgnoreCase("false") && !value.equals("0");
    }

    private static Component previewLine(String key, String value) {
        return Component.translatable(key)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.YELLOW));
    }

    private static Component enumLine(String fieldKey, String valueKey) {
        return Component.translatable(fieldKey)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(valueKey).withStyle(ChatFormatting.AQUA));
    }

    private boolean showsMatchValueField() {
        return state.entry.type != ItemNbtFilterData.FilterEntry.Type.KEY_EXISTS;
    }

    private boolean usesBooleanToggle() {
        return state.entry.type == ItemNbtFilterData.FilterEntry.Type.BOOLEAN_VALUE;
    }

    private boolean needsSnbtField() {
        return state.entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT;
    }

    private String matchValueLabelKey() {
        return switch (state.entry.type) {
            case SNBT -> "morequesttypes.config.item_nbt_matching.edit_value";
            case VALUE_EQUALS -> "morequesttypes.config.item_nbt_matching.expected_value";
            case VALUE_CONTAINS -> "morequesttypes.config.item_nbt_matching.contains_text";
            case BOOLEAN_VALUE -> "morequesttypes.config.item_nbt_matching.boolean_value";
            case NUMERIC_COMPARE -> "morequesttypes.config.item_nbt_matching.numeric_value";
            case KEY_EXISTS -> "morequesttypes.config.item_nbt_matching.match_value";
        };
    }

    class FilterContentPanel extends Panel {
        private final List<LayoutEntry> layout = new ArrayList<>();

        FilterContentPanel(ItemNbtFilterEditScreen screen) {
            super(screen);
        }

        @Override
        public void addWidgets() {
            setOffset(true);
            layout.clear();
            pathButtons.clear();
            booleanToggle = null;
            previewBlock = null;

            layout.add(section("morequesttypes.config.item_nbt_matching.section_filter"));

            layout.add(entry(new EnumCycleButton(
                    this,
                    enumLine("morequesttypes.config.item_nbt_matching.filter_kind",
                            "morequesttypes.config.item_nbt_matching.kind." + state.entry.kind.name().toLowerCase()),
                    () -> {
                        ItemNbtFilterData.FilterEntry.Kind[] values = ItemNbtFilterData.FilterEntry.Kind.values();
                        state.entry.kind = values[(state.entry.kind.ordinal() + 1) % values.length];
                    }
            )));

            layout.add(entry(new EnumCycleButton(
                    this,
                    enumLine("morequesttypes.config.item_nbt_matching.filter_type",
                            "morequesttypes.config.item_nbt_matching.type." + state.entry.type.name().toLowerCase()),
                    () -> {
                        ItemNbtFilterData.FilterEntry.Type[] values = ItemNbtFilterData.FilterEntry.Type.values();
                        state.entry.type = values[(state.entry.type.ordinal() + 1) % values.length];
                        state.snbtManual = false;
                        ItemNbtFilterEditScreens.syncFromPath(previewStack(), state, true);
                    }
            )));

            if (state.entry.type == ItemNbtFilterData.FilterEntry.Type.NUMERIC_COMPARE) {
                layout.add(entry(new EnumCycleButton(
                        this,
                        enumLine("morequesttypes.config.item_nbt_matching.numeric_operator",
                                "morequesttypes.config.item_nbt_matching.numeric_op."
                                        + state.entry.numericOperator.name().toLowerCase()),
                        () -> {
                            ItemNbtFilterData.FilterEntry.NumericOperator[] values =
                                    ItemNbtFilterData.FilterEntry.NumericOperator.values();
                            state.entry.numericOperator = values[(state.entry.numericOperator.ordinal() + 1) % values.length];
                        }
                )));
            }

            layout.add(gap());
            layout.add(section("morequesttypes.config.item_nbt_matching.section_path"));

            if (!previewStack().isEmpty()) {
                List<String> paths = NbtPathUtil.extractDisplayPaths(previewStack());
                if (!paths.isEmpty()) {
                    layout.add(entry(new WrappedHintLabel(this,
                            Component.translatable("morequesttypes.config.item_nbt_matching.path_browser_hint"))));

                    int count = Math.min(paths.size(), MAX_PATHS);
                    for (int i = 0; i < count; i++) {
                        PathSelectButton button = new PathSelectButton(this, paths.get(i));
                        pathButtons.add(button);
                        layout.add(entry(button, PATH_ROW));
                    }

                    if (paths.size() > MAX_PATHS) {
                        layout.add(entry(new WrappedHintLabel(this, Component.translatable(
                                "morequesttypes.config.item_nbt_matching.more_paths",
                                paths.size() - MAX_PATHS))));
                    }
                }
            } else {
                layout.add(entry(new WrappedHintLabel(this,
                        Component.translatable("morequesttypes.config.item_nbt_matching.no_preview_item"))));
            }

            pathBox = decorateTextBox(new TextBox(this) {
                @Override
                public void onTextChanged() {
                    state.entry.path = getText().trim();
                    updatePreviewLabels();
                    refreshPathHighlights();
                }
            }, Component.translatable("morequesttypes.config.item_nbt_matching.filter_path"), "e.g. tag.display.Name");
            pathBox.setText(state.entry.path);
            pathBox.setMaxLength(512);
            layout.add(entry(pathBox, ROW + 2));

            layout.add(gap());
            layout.add(section("morequesttypes.config.item_nbt_matching.section_value"));

            if (showsMatchValueField()) {
                if (usesBooleanToggle()) {
                    booleanToggle = new BooleanToggleButton(this, parseBooleanValue(state.matchValue));
                    layout.add(entry(booleanToggle));
                    layout.add(entry(new WrappedHintLabel(this,
                            Component.translatable("morequesttypes.config.item_nbt_matching.boolean_toggle_hint"))));
                } else {
                    matchValueBox = decorateTextBox(new TextBox(this) {
                        @Override
                        public void onTextChanged() {
                            state.matchValue = getText();
                            state.snbtManual = false;
                            onMatchValueEdited();
                        }
                    }, Component.translatable(matchValueLabelKey()),
                            Component.translatable("morequesttypes.config.item_nbt_matching.match_value_placeholder").getString());
                    matchValueBox.setText(state.matchValue);
                    matchValueBox.setMaxLength(2048);
                    layout.add(entry(matchValueBox, ROW + 2));

                    if (needsSnbtField()) {
                        layout.add(entry(new WrappedHintLabel(this,
                                Component.translatable("morequesttypes.config.item_nbt_matching.snbt_value_hint"))));
                    }

                    layout.add(entry(new SimpleActionButton(this,
                            Component.translatable("morequesttypes.config.item_nbt_matching.reset_from_item"),
                            () -> resetMatchValueFromItem())));
                }
            } else {
                matchValueBox = null;
                layout.add(entry(new WrappedHintLabel(this,
                        Component.translatable("morequesttypes.config.item_nbt_matching.key_exists_hint"))));
            }

            if (needsSnbtField()) {
                snbtBox = decorateTextBox(new TextBox(this) {
                    @Override
                    public void onTextChanged() {
                        state.snbtPreview = getText();
                        state.entry.value = getText();
                        state.snbtManual = true;
                        updatePreviewLabels();
                    }
                }, Component.translatable("morequesttypes.config.item_nbt_matching.filter_snbt"),
                        Component.translatable("morequesttypes.config.item_nbt_matching.snbt_placeholder").getString());
                snbtBox.setText(state.snbtPreview);
                snbtBox.setMaxLength(4096);
                layout.add(entry(snbtBox, ROW * 2 + 6));
            } else {
                snbtBox = null;
            }

            layout.add(gap());
            layout.add(section("morequesttypes.config.item_nbt_matching.section_preview"));

            ItemNbtFilterEditScreens.finalizeState(previewStack(), state);
            Tag tag = state.entry.path.isBlank() ? null : NbtPathUtil.getTagAtPath(previewStack(), state.entry.path);
            valuePreviewLabel = new WrappedLabelWidget(this, previewLine(
                    "morequesttypes.config.item_nbt_matching.value_preview", NbtPathUtil.formatTagValue(tag)));
            rawPreviewLabel = new WrappedLabelWidget(this, previewLine(
                    "morequesttypes.config.item_nbt_matching.raw_preview",
                    tag != null ? tag.toString() : "(no path selected)"));
            String encoded = state.entry.encode().isBlank() ? "(incomplete)" : state.entry.encode();
            encodedPreviewLabel = new WrappedLabelWidget(this, previewLine(
                    "morequesttypes.config.item_nbt_matching.encoded_preview", encoded));

            previewBlock = new PreviewBlockWidget(this, valuePreviewLabel, rawPreviewLabel, encodedPreviewLabel);
            layout.add(entry(previewBlock, ROW));

            layout.add(gap());
            layout.add(new LayoutEntry(null, BOTTOM_SCROLL_PAD));

            for (LayoutEntry entry : layout) {
                if (entry.widget != null) {
                    add(entry.widget);
                }
            }
        }

        @Override
        public void alignWidgets() {
            int innerW = Math.max(220, getWidth() - PAD * 2);
            Theme theme = getTheme();
            int y = PAD;
            for (LayoutEntry entry : layout) {
                int height = entry.height;
                if (entry.widget instanceof DynamicHeightWidget dynamic) {
                    height = Math.max(height, dynamic.computeHeight(theme, innerW));
                }
                if (entry.widget != null) {
                    entry.widget.setPosAndSize(PAD, y, innerW, height);
                }
                y += height + GAP;
            }
        }

        @Override
        public boolean mouseScrolled(double scroll) {
            if (scrollPanel(scroll)) {
                return true;
            }
            return super.mouseScrolled(scroll);
        }

        private LayoutEntry section(String key) {
            return new LayoutEntry(new SectionHeader(this, Component.translatable(key)), SECTION);
        }

        private LayoutEntry gap() {
            return new LayoutEntry(null, SECTION_GAP);
        }

        private LayoutEntry entry(Widget widget) {
            return new LayoutEntry(widget, ROW);
        }

        private LayoutEntry entry(Widget widget, int height) {
            return new LayoutEntry(widget, height);
        }
    }

    private static TextBox decorateTextBox(TextBox box, Component label, String ghost) {
        box.setLabel(label);
        box.setLabelColor(Color4I.rgb(170, 170, 170));
        box.ghostText = ghost;
        box.textColor = Color4I.WHITE;
        return box;
    }

    private interface DynamicHeightWidget {
        int computeHeight(Theme theme, int width);
    }

    private record LayoutEntry(Widget widget, int height) {
    }

    private final class FilterFooterPanel extends Panel {
        private final Button buttonAccept;
        private final Button buttonCancel;

        FilterFooterPanel(ItemNbtFilterEditScreen screen) {
            super(screen);
            buttonAccept = SimpleTextButton.accept(this, btn -> screen.doAccept(),
                    TextComponentUtils.hotkeyTooltip("Shift + Enter"));
            buttonCancel = SimpleTextButton.cancel(this, btn -> screen.doCancel(),
                    TextComponentUtils.hotkeyTooltip("ESC"));
        }

        @Override
        public void addWidgets() {
            add(buttonAccept);
            add(buttonCancel);
        }

        @Override
        public void alignWidgets() {
            int buttonY = getHeight() - FOOTER_BUTTON_ROW + 2;
            buttonCancel.setPos(getWidth() - buttonCancel.width - 5, buttonY);
            buttonAccept.setPos(buttonCancel.posX - buttonAccept.width - 5, buttonY);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawPanelBackground(graphics, x, y, w, h);
            Color4I.GRAY.withAlpha(64).draw(graphics, x, y, w, 1);

            Component hint = Component.translatable("morequesttypes.config.item_nbt_matching.placeholders_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            int hintMaxW = w - 16;
            int hintH = h - FOOTER_BUTTON_ROW;
            Color4I.rgb(22, 22, 28).draw(graphics, x + 4, y + 3, w - 8, hintH - 2);
            UiTextWrap.draw(graphics, theme, hint, x + 10, y + 5, hintMaxW - 12, Color4I.rgb(165, 175, 190));
        }
    }

    private static final class TitleTopPanel extends Panel {
        private final Component title;

        TitleTopPanel(Panel parent, Component title) {
            super(parent);
            this.title = title;
        }

        @Override
        public void addWidgets() {
        }

        @Override
        public void alignWidgets() {
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            drawBackground(graphics, theme, x, y, w, h);
            theme.drawString(graphics, title, x + 8, y + 8, Color4I.WHITE, 0);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawPanelBackground(graphics, x, y, w, h);
            Color4I.BLACK.withAlpha(80).draw(graphics, x, y + h - 1, w, 1);
        }
    }

    private final class PathSelectButton extends Button {
        private final String path;

        PathSelectButton(Panel panel, String path) {
            super(panel, Component.literal(path), Icons.BOOK);
            this.path = path;
            setTitle(Component.literal(path));
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            String value = NbtPathUtil.pathValueLabel(previewStack(), path);
            list.add(Component.literal(path));
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.select_path_hint")
                    .withStyle(ChatFormatting.GRAY));
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.value_preview")
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(value).withStyle(ChatFormatting.YELLOW)));
        }

        void refreshHighlight() {
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            selectPath(path);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean selected = path.equals(state.entry.path);
            Color4I bg = selected
                    ? Color4I.rgb(28, 48, 28)
                    : (isMouseOver() ? Color4I.rgb(48, 48, 56) : Color4I.rgb(24, 24, 28));
            bg.draw(graphics, x, y, w, h);
            if (selected) {
                Color4I.rgb(80, 200, 80).draw(graphics, x, y, ACCENT, h);
            }
            Color4I.rgb(60, 60, 68).draw(graphics, x, y + h - 1, w, 1);

            String value = NbtPathUtil.pathValueLabel(previewStack(), path);
            int valueWidth = Math.min(theme.getStringWidth(value), w / 3);
            int pathMax = w - valueWidth - 20;
            String pathText = truncate(theme, path, pathMax);

            theme.drawString(graphics, Component.literal(pathText).withStyle(
                            selected ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                    x + 8, y + 10, Color4I.WHITE, 0);

            String valueText = truncate(theme, value, w / 3);
            int drawnValueWidth = theme.getStringWidth(valueText);
            theme.drawString(graphics, Component.literal(valueText).withStyle(ChatFormatting.YELLOW),
                    x + w - drawnValueWidth - 8, y + 10, Color4I.rgb(255, 220, 100), 0);
        }
    }

    private static String truncate(Theme theme, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (theme.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        for (int end = text.length() - 1; end > 0; end--) {
            if (theme.getStringWidth(text.substring(0, end) + ellipsis) <= maxWidth) {
                return text.substring(0, end) + ellipsis;
            }
        }
        return ellipsis;
    }

    private static final class SectionHeader extends Widget {
        private final Component text;

        SectionHeader(Panel panel, Component text) {
            super(panel);
            this.text = text;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I.rgb(18, 18, 24).draw(graphics, x, y + 4, w, h - 4);
            Color4I.rgb(64, 140, 200).draw(graphics, x, y + 4, 3, h - 4);
            theme.drawString(graphics, text.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    x + 10, y + 10, Color4I.WHITE, 0);
        }
    }

    private static final class WrappedHintLabel extends Widget implements DynamicHeightWidget {
        private final Component text;

        WrappedHintLabel(Panel panel, Component text) {
            super(panel);
            this.text = text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        }

        @Override
        public int computeHeight(Theme theme, int width) {
            return UiTextWrap.height(theme, text, width - 4);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            UiTextWrap.draw(graphics, theme, text, x + 2, y, w - 4, Color4I.rgb(150, 150, 158));
        }
    }

    private static final class WrappedLabelWidget extends Widget implements DynamicHeightWidget {
        private Component text;

        WrappedLabelWidget(Panel panel, Component text) {
            super(panel);
            this.text = text;
        }

        void setText(Component text) {
            this.text = text;
        }

        @Override
        public int computeHeight(Theme theme, int width) {
            return UiTextWrap.height(theme, text, width - 12);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            UiTextWrap.draw(graphics, theme, text, x + 6, y, w - 12, Color4I.WHITE);
        }
    }

    private final class PreviewBlockWidget extends Widget implements DynamicHeightWidget {
        private final WrappedLabelWidget valueLabel;
        private final WrappedLabelWidget rawLabel;
        private final WrappedLabelWidget encodedLabel;

        PreviewBlockWidget(Panel panel, WrappedLabelWidget valueLabel, WrappedLabelWidget rawLabel, WrappedLabelWidget encodedLabel) {
            super(panel);
            this.valueLabel = valueLabel;
            this.rawLabel = rawLabel;
            this.encodedLabel = encodedLabel;
        }

        void refreshBounds() {
            ItemNbtFilterEditScreen screen = (ItemNbtFilterEditScreen) getGui();
            if (screen != null) {
                screen.refreshWidgets();
            }
        }

        @Override
        public int computeHeight(Theme theme, int width) {
            int innerW = width - PAD * 2;
            return PAD * 2
                    + valueLabel.computeHeight(theme, innerW)
                    + rawLabel.computeHeight(theme, innerW)
                    + encodedLabel.computeHeight(theme, innerW);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I.rgb(16, 16, 20).draw(graphics, x, y, w, h);
            Color4I.rgb(48, 48, 56).draw(graphics, x, y, w, 1);
            Color4I.rgb(48, 48, 56).draw(graphics, x, y + h - 1, w, 1);
            Color4I.rgb(48, 48, 56).draw(graphics, x, y, 1, h);
            Color4I.rgb(48, 48, 56).draw(graphics, x + w - 1, y, 1, h);

            int lineY = y + PAD;
            int innerW = w - PAD * 2;
            int valueH = valueLabel.computeHeight(theme, innerW);
            valueLabel.setPosAndSize(x + PAD, lineY, innerW, valueH);
            valueLabel.draw(graphics, theme, x + PAD, lineY, innerW, valueH);
            lineY += valueH;

            int rawH = rawLabel.computeHeight(theme, innerW);
            rawLabel.setPosAndSize(x + PAD, lineY, innerW, rawH);
            rawLabel.draw(graphics, theme, x + PAD, lineY, innerW, rawH);
            lineY += rawH;

            int encodedH = encodedLabel.computeHeight(theme, innerW);
            encodedLabel.setPosAndSize(x + PAD, lineY, innerW, encodedH);
            encodedLabel.draw(graphics, theme, x + PAD, lineY, innerW, encodedH);
        }
    }

    private final class BooleanToggleButton extends SimpleTextButton {
        private boolean trueValue;

        BooleanToggleButton(Panel panel, boolean trueValue) {
            super(panel, label(trueValue), Icons.ACCEPT);
            this.trueValue = trueValue;
        }

        boolean isTrueValue() {
            return trueValue;
        }

        void setTrueValue(boolean trueValue) {
            this.trueValue = trueValue;
            setTitle(label(trueValue));
        }

        private static Component label(boolean trueValue) {
            return Component.translatable(trueValue
                    ? "morequesttypes.config.item_nbt_matching.boolean_true"
                    : "morequesttypes.config.item_nbt_matching.boolean_false");
        }

        @Override
        public boolean hasIcon() {
            return false;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            trueValue = !trueValue;
            setTitle(label(trueValue));
            state.matchValue = trueValue ? "true" : "false";
            onMatchValueEdited();
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I bg = trueValue ? Color4I.rgb(32, 72, 32) : Color4I.rgb(72, 32, 32);
            bg.draw(graphics, x, y, w, h);
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(35).draw(graphics, x, y, w, h);
            }
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }
    }

    private final class SimpleActionButton extends SimpleTextButton {
        private final Runnable action;

        SimpleActionButton(Panel panel, Component label, Runnable action) {
            super(panel, label, Icons.REFRESH);
            this.action = action;
        }

        @Override
        public boolean hasIcon() {
            return false;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            action.run();
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I.rgb(36, 36, 44).draw(graphics, x, y, w, h);
            if (isMouseOver()) {
                Color4I.rgb(56, 56, 68).draw(graphics, x, y, w, h);
            }
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }
    }

    private final class EnumCycleButton extends SimpleTextButton {
        private final Runnable onCycle;

        EnumCycleButton(Panel panel, Component label, Runnable onCycle) {
            super(panel, label, Icons.SETTINGS);
            this.onCycle = onCycle;
        }

        @Override
        public boolean hasIcon() {
            return false;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            onCycle.run();
            rebuild();
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I.rgb(36, 36, 44).draw(graphics, x, y, w, h);
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(30).draw(graphics, x, y, w, h);
            }
            Color4I.rgb(64, 140, 200).draw(graphics, x, y, 3, h);
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }
    }
}
