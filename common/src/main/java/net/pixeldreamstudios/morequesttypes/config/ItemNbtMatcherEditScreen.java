package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ItemStackConfig;
import dev.ftb.mods.ftblibrary.config.ui.resource.SelectItemStackScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterData;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterRepair;
import net.pixeldreamstudios.morequesttypes.util.NbtPathUtil;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ItemNbtMatcherEditScreen extends AbstractThreePanelScreen<ItemNbtMatcherEditScreen.MatcherContentPanel> {
    private static final int ROW = 22;
    private static final int PATH_ROW = 30;
    private static final int SECTION = 24;
    private static final int PAD = 8;
    private static final int GAP = 4;
    private static final int SECTION_GAP = 10;
    private static final int MAX_PATHS = 80;
    private static final int BOTTOM_SCROLL_PAD = 52;
    private static final int FOOTER_BUTTON_ROW = 27;

    private final Component title;
    private final ItemNbtFilterData data;
    private final ItemStack initialPreview;
    private final ConfigCallback callback;
    private MatcherContentPanel contentPanel;

    public ItemNbtMatcherEditScreen(ItemNbtFilterData data, ItemStack initialPreview, ConfigCallback callback) {
        this.data = data;
        this.initialPreview = initialPreview == null || initialPreview.isEmpty()
                ? ItemStack.EMPTY
                : initialPreview.copyWithCount(1);
        this.callback = callback;
        this.title = Component.translatable("morequesttypes.config.item_nbt_matching");
    }

    @Override
    public boolean onInit() {
        showScrollBar(true);
        return setSizeProportional(0.5f, 0.82f);
    }

    @Override
    protected int getTopPanelHeight() {
        return 26;
    }

    @Override
    protected Panel createTopPanel() {
        return new TitlePanel(this, title);
    }

    @Override
    protected int getBottomPanelHeight() {
        return 52;
    }

    @Override
    protected Panel createBottomPanel() {
        return new FooterPanel(this);
    }

    @Override
    protected MatcherContentPanel createMainPanel() {
        contentPanel = new MatcherContentPanel(this);
        return contentPanel;
    }

    @Override
    public boolean mouseScrolled(double scroll) {
        if (contentPanel != null && contentPanel.scrollPanel(0, scroll)) {
            return true;
        }
        return super.mouseScrolled(scroll);
    }

    void rebuild() {
        refreshWidgets();
    }

    ItemStack getPreviewStack() {
        if (!data.previewItem.isEmpty()) {
            return data.previewItem.copy();
        }
        if (!initialPreview.isEmpty()) {
            return initialPreview.copy();
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack held = main.isEmpty() ? player.getOffhandItem() : main;
        return held.isEmpty() ? ItemStack.EMPTY : held.copy();
    }

    void setPreviewItem(ItemStack stack) {
        data.previewItem = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!data.previewItem.isEmpty()) {
            data.previewItem.setCount(1);
        }
        repairFilters();
        rebuild();
    }

    void repairFilters() {
        ItemStack preview = getPreviewStack();
        for (ItemNbtFilterData.FilterEntry entry : data.filters) {
            ItemNbtFilterRepair.repair(entry, preview);
        }
    }

    void openFilterEditor(int index, boolean isNew) {
        ItemNbtFilterData.FilterEntry entry = isNew
                ? new ItemNbtFilterData.FilterEntry()
                : data.filters.get(index).copy();
        ItemNbtFilterEditScreens.FilterEditState state = new ItemNbtFilterEditScreens.FilterEditState(entry);

        ItemNbtFilterEditScreens.open(this::getPreviewStack, state, accepted -> {
            if (accepted) {
                ItemStack preview = getPreviewStack();
                ItemNbtFilterEditScreens.finalizeState(preview, state);
                ItemNbtFilterRepair.repair(state.entry, preview);
                if (isNew) {
                    data.filters.add(state.entry.copy());
                } else {
                    data.filters.set(index, state.entry.copy());
                }
            }
            rebuild();
        });
    }

    void removeFilter(int index) {
        if (index >= 0 && index < data.filters.size()) {
            data.filters.remove(index);
            rebuild();
        }
    }

    void addIgnorePath(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        String trimmed = path.trim();
        if (!data.ignorePaths.contains(trimmed)) {
            data.ignorePaths.add(trimmed);
            rebuild();
        }
    }

    void removeIgnorePath(int index) {
        if (index >= 0 && index < data.ignorePaths.size()) {
            data.ignorePaths.remove(index);
            rebuild();
        }
    }

    @Override
    protected void doAccept() {
        callback.save(true);
        closeGui(true);
    }

    @Override
    protected void doCancel() {
        callback.save(false);
        closeGui(true);
    }

    static String filterSummary(ItemNbtFilterData.FilterEntry entry) {
        if (entry == null || entry.encode().isBlank()) {
            return "(Empty filter)";
        }
        String prefix = switch (entry.kind) {
            case EXCLUDE -> "NOT ";
            case OR -> "OR ";
            case REQUIRE -> "";
        };
        return prefix + abbreviate(entry.encode(), 56);
    }

    private static String abbreviate(String value, int max) {
        String compact = value.replace('\n', ' ').trim();
        if (compact.length() <= max) {
            return compact;
        }
        return compact.substring(0, max - 3) + "...";
    }

    class MatcherContentPanel extends Panel {
        private final List<LayoutEntry> layout = new ArrayList<>();

        MatcherContentPanel(ItemNbtMatcherEditScreen screen) {
            super(screen);
        }

        @Override
        public void addWidgets() {
            setOffset(true);
            layout.clear();

            layout.add(section("morequesttypes.config.item_nbt_matching.preview"));
            layout.add(entry(new PreviewItemButton(this)));
            layout.add(entry(new WrappedHint(this,
                    Component.translatable("morequesttypes.config.item_nbt_matching.preview_item.tooltip"))));

            layout.add(gap());
            layout.add(section("morequesttypes.config.item_nbt_matching.filters"));
            layout.add(entry(new AddFilterButton(this)));
            for (int i = 0; i < data.filters.size(); i++) {
                layout.add(entry(new FilterRuleButton(this, i)));
            }
            if (data.filters.isEmpty()) {
                layout.add(entry(new WrappedHint(this,
                        Component.translatable("morequesttypes.config.item_nbt_matching.filter_entry.info"))));
            }

            layout.add(gap());
            layout.add(section("morequesttypes.config.item_nbt_matching.ignore"));

            ItemStack preview = getPreviewStack();
            if (!preview.isEmpty()) {
                List<String> paths = NbtPathUtil.extractDisplayPaths(preview);
                if (!paths.isEmpty()) {
                    layout.add(entry(new WrappedHint(this,
                            Component.translatable("morequesttypes.config.item_nbt_matching.path_browser_hint"))));
                    int count = Math.min(paths.size(), MAX_PATHS);
                    for (int i = 0; i < count; i++) {
                        layout.add(entry(new IgnorePathAddButton(this, paths.get(i)), PATH_ROW));
                    }
                    if (paths.size() > MAX_PATHS) {
                        layout.add(entry(new WrappedHint(this, Component.translatable(
                                "morequesttypes.config.item_nbt_matching.more_paths",
                                paths.size() - MAX_PATHS))));
                    }
                }
            } else {
                layout.add(entry(new WrappedHint(this,
                        Component.translatable("morequesttypes.config.item_nbt_matching.no_preview_item"))));
            }

            layout.add(entry(new IgnorePathTextBox(this), ROW + 2));

            for (int i = 0; i < data.ignorePaths.size(); i++) {
                layout.add(entry(new IgnorePathRowButton(this, i), PATH_ROW));
            }

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
            if (scrollPanel(0, scroll)) {
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

    private interface DynamicHeightWidget {
        int computeHeight(Theme theme, int width);
    }

    private record LayoutEntry(Widget widget, int height) {
    }

    private static final class TitlePanel extends Panel {
        private final Component title;

        TitlePanel(Panel parent, Component title) {
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

    private final class FooterPanel extends Panel {
        private final Button accept;
        private final Button cancel;

        FooterPanel(ItemNbtMatcherEditScreen screen) {
            super(screen);
            accept = SimpleTextButton.accept(this, btn -> screen.doAccept(),
                    TextComponentUtils.hotkeyTooltip("Shift + Enter"));
            cancel = SimpleTextButton.cancel(this, btn -> screen.doCancel(),
                    TextComponentUtils.hotkeyTooltip("ESC"));
        }

        @Override
        public void addWidgets() {
            add(accept);
            add(cancel);
        }

        @Override
        public void alignWidgets() {
            int buttonY = getHeight() - FOOTER_BUTTON_ROW + 2;
            cancel.setPos(getWidth() - cancel.width - 5, buttonY);
            accept.setPos(cancel.posX - accept.width - 5, buttonY);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawPanelBackground(graphics, x, y, w, h);
            Color4I.GRAY.withAlpha(64).draw(graphics, x, y, w, 1);
        }
    }

    private final class PreviewItemButton extends Button {
        PreviewItemButton(Panel panel) {
            super(panel, Component.translatable("morequesttypes.config.item_nbt_matching.preview_item"), Icons.ART);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            ItemNbtMatcherEditScreen matcher = matcherScreen();
            var tempConfig = new ItemStackConfig(false, true);
            tempConfig.setValue(matcher.getPreviewStack());
            matcher.closeGui(false);
            new SelectItemStackScreen(tempConfig, accepted -> {
                if (accepted) {
                    ItemStack selected = tempConfig.getValue();
                    matcher.setPreviewItem(selected == null ? ItemStack.EMPTY : selected);
                }
                matcher.openGui();
            }).openGui();
        }

        private ItemNbtMatcherEditScreen matcherScreen() {
            return (ItemNbtMatcherEditScreen) getGui();
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            ItemStack stack = getPreviewStack();
            if (stack.isEmpty()) {
                list.add(Component.translatable("morequesttypes.config.item_nbt_matching.preview_item.tooltip"));
                return;
            }
            list.add(stack.getHoverName());
            list.add(Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                    .withStyle(ChatFormatting.GRAY));
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.preview_item.tooltip"));
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I.rgb(28, 28, 34).draw(graphics, x, y, w, h);
            if (isMouseOver()) {
                Color4I.rgb(40, 40, 50).draw(graphics, x, y, w, h);
            }
            theme.drawString(graphics, getTitle(), x + 8, y + 7, Color4I.WHITE, 0);

            ItemStack stack = getPreviewStack();
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String text = abbreviate(id, 44);
                theme.drawString(graphics, Component.literal(text).withStyle(ChatFormatting.YELLOW),
                        x + w - theme.getStringWidth(text) - 36, y + 7, Color4I.rgb(255, 220, 100), 0);
                drawItem(graphics, theme, stack, x + w - 28, y + 4);
            } else {
                theme.drawString(graphics, Component.literal("(none)").withStyle(ChatFormatting.GRAY),
                        x + w - 60, y + 7, Color4I.GRAY, 0);
            }
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }

        private void drawItem(GuiGraphics graphics, Theme theme, ItemStack stack, int x, int y) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(theme.getFont(), stack, x, y);
        }
    }

    private final class AddFilterButton extends SimpleTextButton {
        AddFilterButton(Panel panel) {
            super(panel, Component.translatable("morequesttypes.config.item_nbt_matching.add_filter"), Icons.ADD);
        }

        @Override
        public boolean hasIcon() {
            return false;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            openFilterEditor(data.filters.size(), true);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I.rgb(32, 48, 32).draw(graphics, x, y, w, h);
            if (isMouseOver()) {
                Color4I.rgb(42, 62, 42).draw(graphics, x, y, w, h);
            }
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }
    }

    private final class FilterRuleButton extends Button {
        private final int index;

        FilterRuleButton(Panel panel, int index) {
            super(panel, Component.literal(filterSummary(data.filters.get(index))), Icons.BOOK);
            this.index = index;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            if (button == MouseButton.RIGHT) {
                removeFilter(index);
            } else {
                openFilterEditor(index, false);
            }
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(getTitle());
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.filter_entry.info")
                    .withStyle(ChatFormatting.GRAY));
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.remove_filter_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I bg = isMouseOver() ? Color4I.rgb(48, 48, 56) : Color4I.rgb(24, 24, 28);
            bg.draw(graphics, x, y, w, h);
            theme.drawString(graphics, getTitle(), x + 8, y + 10, Color4I.WHITE, 0);
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }
    }

    private final class IgnorePathAddButton extends Button {
        private final String path;

        IgnorePathAddButton(Panel panel, String path) {
            super(panel, Component.literal(path), Icons.ADD);
            this.path = path;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            addIgnorePath(path);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(Component.literal(path));
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.add_ignore_path_hint")
                    .withStyle(ChatFormatting.GRAY));
            String value = NbtPathUtil.pathValueLabel(getPreviewStack(), path);
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.value_preview")
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(value).withStyle(ChatFormatting.YELLOW)));
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean alreadyIgnored = data.ignorePaths.contains(path);
            Color4I bg = alreadyIgnored
                    ? Color4I.rgb(36, 36, 28)
                    : (isMouseOver() ? Color4I.rgb(40, 52, 40) : Color4I.rgb(22, 28, 22));
            bg.draw(graphics, x, y, w, h);
            String value = NbtPathUtil.pathValueLabel(getPreviewStack(), path);
            theme.drawString(graphics, Component.literal(path).withStyle(
                            alreadyIgnored ? ChatFormatting.GRAY : ChatFormatting.GREEN),
                    x + 8, y + 10, Color4I.WHITE, 0);
            String valueText = abbreviate(value, 24);
            theme.drawString(graphics, Component.literal(valueText).withStyle(ChatFormatting.YELLOW),
                    x + w - theme.getStringWidth(valueText) - 8, y + 10, Color4I.rgb(255, 220, 100), 0);
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }
    }

    private final class IgnorePathRowButton extends Button {
        private final int index;

        IgnorePathRowButton(Panel panel, int index) {
            super(panel, Component.literal(data.ignorePaths.get(index)), Icons.BOOK);
            this.index = index;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            removeIgnorePath(index);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(getTitle());
            list.add(Component.translatable("morequesttypes.config.item_nbt_matching.remove_ignore_path_hint")
                    .withStyle(ChatFormatting.GRAY));
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            Color4I.rgb(32, 28, 28).draw(graphics, x, y, w, h);
            if (isMouseOver()) {
                Color4I.rgb(52, 36, 36).draw(graphics, x, y, w, h);
            }
            theme.drawString(graphics, getTitle(), x + 8, y + 10, Color4I.rgb(255, 180, 120), 0);
            theme.drawString(graphics, Component.literal("×").withStyle(ChatFormatting.RED),
                    x + w - 16, y + 8, Color4I.RED, 0);
            Color4I.GRAY.withAlpha(110).draw(graphics, x, y + h - 1, w, 1);
        }
    }

    private final class IgnorePathTextBox extends TextBox {
        IgnorePathTextBox(Panel panel) {
            super(panel);
            setLabel(Component.translatable("morequesttypes.config.item_nbt_matching.ignore_path_manual"));
            setLabelColor(Color4I.rgb(170, 170, 170));
            ghostText = "e.g. minecraft:custom_data.owner (press Enter)";
            textColor = Color4I.WHITE;
            setMaxLength(512);
        }

        @Override
        public void onEnterPressed() {
            String text = getText().trim();
            if (!text.isEmpty()) {
                addIgnorePath(text);
                setText("");
            }
        }
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

    private static final class WrappedHint extends Widget implements DynamicHeightWidget {
        private final Component text;

        WrappedHint(Panel panel, Component text) {
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
}
