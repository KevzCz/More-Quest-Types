package net.pixeldreamstudios.morequesttypes.config;



import dev.ftb.mods.ftblibrary.config.ConfigCallback;

import net.fabricmc.api.EnvType;

import net.fabricmc.api.Environment;

import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterData;

import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterRepair;

import net.pixeldreamstudios.morequesttypes.util.NbtPathUtil;



@Environment(EnvType.CLIENT)

public final class ItemNbtFilterEditScreens {

    private ItemNbtFilterEditScreens() {

    }



    public static void open(ItemStack previewStack, FilterEditState state, ConfigCallback callback) {

        restoreFromPersisted(previewStack, state);

        new ItemNbtFilterEditScreen(previewStack, state, callback).openGui();

    }



    public static void restoreFromPersisted(ItemStack previewStack, FilterEditState state) {

        ItemNbtFilterRepair.repair(state.entry, previewStack);

        applyEntryToEditorState(state);

    }



    public static void applyEntryToEditorState(FilterEditState state) {

        ItemNbtFilterData.FilterEntry entry = state.entry;



        if (entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT) {

            state.snbtPreview = entry.value;

            state.snbtManual = !entry.value.isBlank();

            state.matchValue = "";

            if (!entry.path.isBlank() && !entry.value.isBlank()) {

                String extracted = NbtPathUtil.editableValueFromSnbt(entry.path, entry.value);

                if (!extracted.isBlank()) {

                    state.matchValue = extracted;

                }

            }

        } else if (entry.type == ItemNbtFilterData.FilterEntry.Type.KEY_EXISTS) {

            state.matchValue = "";

            state.snbtPreview = "";

            state.snbtManual = false;

        } else {

            state.matchValue = entry.value;

            state.snbtPreview = "";

            state.snbtManual = false;

        }

    }



    public static void syncFromPath(ItemStack previewStack, FilterEditState state, boolean force) {

        if (state.entry.path.isBlank()) {

            return;

        }



        switch (state.entry.type) {

            case SNBT -> {

                if (force || state.matchValue.isBlank()) {

                    state.matchValue = NbtPathUtil.defaultEditableValue(previewStack, state.entry.path);

                }

                if (!state.snbtManual || force) {

                    state.snbtPreview = NbtPathUtil.snbtForPathWithValue(previewStack, state.entry.path, state.matchValue);

                }

            }

            case KEY_EXISTS -> state.matchValue = "";

            case VALUE_EQUALS, VALUE_CONTAINS, BOOLEAN_VALUE, NUMERIC_COMPARE -> {

                if (force || state.matchValue.isBlank()) {

                    state.matchValue = NbtPathUtil.defaultEditableValue(previewStack, state.entry.path);

                }

            }

        }

    }



    public static void finalizeState(ItemStack previewStack, FilterEditState state) {

        ItemNbtFilterRepair.repairColonSplitPattern(state.entry);



        if (state.entry.path.isBlank()) {

            return;

        }



        switch (state.entry.type) {

            case SNBT -> {

                if (!state.snbtManual || state.entry.value.isBlank()) {

                    state.entry.value = NbtPathUtil.snbtForPathWithValue(previewStack, state.entry.path, state.matchValue);

                    state.snbtPreview = state.entry.value;

                } else {

                    state.entry.value = state.snbtPreview;

                }

            }

            case KEY_EXISTS -> {

                state.entry.value = "";

                state.matchValue = "";

            }

            case VALUE_EQUALS, VALUE_CONTAINS, BOOLEAN_VALUE, NUMERIC_COMPARE -> {

                state.entry.value = state.matchValue.trim();

            }

        }

    }



    public static final class FilterEditState {

        public final ItemNbtFilterData.FilterEntry entry;

        public String matchValue = "";

        public String snbtPreview = "";

        public boolean snbtManual = false;



        public FilterEditState(ItemNbtFilterData.FilterEntry entry) {

            this.entry = entry;

            applyEntryToEditorState(this);

        }

    }

}


