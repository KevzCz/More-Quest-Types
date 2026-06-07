package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class MultiSelectListConfig extends ConfigValue<List<String>> {
    private final Supplier<List<String>> choicesSupplier;
    private final String titleKey;
    private final Function<String, Component> labelProvider;

    public MultiSelectListConfig(Supplier<List<String>> choicesSupplier, String titleKey) {
        this(choicesSupplier, titleKey, Component::literal);
    }

    public MultiSelectListConfig(Supplier<List<String>> choicesSupplier, String titleKey, Function<String, Component> labelProvider) {
        this.choicesSupplier = choicesSupplier;
        this.titleKey = titleKey;
        this.labelProvider = labelProvider;
    }

    @Override
    public Color4I getColor(@Nullable List<String> v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable List<String> v) {
        int count = v == null ? 0 : v.size();
        return Component.literal(count + " selected");
    }

    @Override
    public Icon getIcon(@Nullable List<String> v) {
        return Icons.ACCEPT;
    }

    @Override
    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        List<String> choices = new ArrayList<>(new LinkedHashSet<>(choicesSupplier.get()));
        Set<String> selected = new HashSet<>(getValue() != null ? getValue() : List.of());

        ConfigGroup group = new ConfigGroup("multi_select", accepted -> {
            if (accepted) {
                List<String> result = new ArrayList<>();
                for (String choice : choices) {
                    if (selected.contains(choice)) {
                        result.add(choice);
                    }
                }
                setValue(result);
                callback.save(true);
            } else {
                callback.save(false);
            }
        });
        group.setNameKey(titleKey);

        for (String choice : choices) {
            Component label = labelProvider.apply(choice);
            group.addBool(ConfigLabelUtil.safeRowId(choice), selected.contains(choice), v -> {
                        if (v) {
                            selected.add(choice);
                        } else {
                            selected.remove(choice);
                        }
                    }, false)
                    .setNameKey(ConfigLabelUtil.nameKey(label));
        }

        new EditConfigScreen(group).openGui();
    }

    @Override
    public void addInfo(TooltipList list) {
    }
}
