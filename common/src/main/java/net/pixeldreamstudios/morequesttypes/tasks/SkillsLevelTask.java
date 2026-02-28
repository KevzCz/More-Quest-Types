package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.icon.*;
import dev.ftb.mods.ftblibrary.util.*;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.*;
import net.fabricmc.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.pixeldreamstudios.morequesttypes.compat.*;
import net.pixeldreamstudios.morequesttypes.mixin.accessor.ClientSkillScreenDataAccessor;
import net.pixeldreamstudios.morequesttypes.mixin.accessor.SkillsClientModAccessor;
import net.pixeldreamstudios.morequesttypes.util.*;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.config.ClientIconConfig.ItemIconConfig;
import net.puffish.skillsmod.client.config.ClientIconConfig.TextureIconConfig;

import java.util.*;
import java.util.concurrent.*;

public class SkillsLevelTask extends Task {
    public enum Mode { TOTAL_LEVEL, CATEGORY_LEVEL }

    private Mode mode = Mode.TOTAL_LEVEL;
    private ComparisonMode comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
    private int firstNumber = 10;
    private int secondNumber = 20;
    private String categoryId = "";

    private static final Map<String, String> CATEGORY_ICONS = new ConcurrentHashMap<>();

    public SkillsLevelTask(long id, Quest quest) {
        super(id, quest);
    }

    public static void syncCategoryIcons(Map<String, String> icons) {
        CATEGORY_ICONS.clear();
        CATEGORY_ICONS.putAll(icons);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.SKILLS_LEVEL;
    }

    @Override
    public long getMaxProgress() {
        return ComparisonManager.getMaxProgress(comparisonMode, firstNumber, secondNumber);
    }

    @Override
    public boolean hideProgressNumbers() {
        return comparisonMode.isRange();
    }

    @Override
    public String formatMaxProgress() {
        if (comparisonMode.isRange()) {
            return "1";
        }
        return Long.toString(getMaxProgress());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public String formatProgress(TeamData teamData, long progress) {
        if (comparisonMode.isRange()) {
            return progress >= 1 ? "1" : "0";
        }
        long shown = Math.max(0, Math.min(progress, getMaxProgress()));
        return Long.toString(shown);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getButtonText() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return Component.literal("?  / " + getMaxProgress());

        long p = TeamData.get(player).getProgress(this);

        if (comparisonMode.isRange()) {
            return Component.literal(p >= 1 ? "✓" : "✗");
        }

        long shown = Math.max(0, Math.min(p, getMaxProgress()));
        return Component.literal(shown + " / " + getMaxProgress());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (mode == Mode.TOTAL_LEVEL || categoryId.isEmpty()) {
            return getType().getIconSupplier();
        }

        String iconData = CATEGORY_ICONS.get(categoryId);
        if (iconData != null && iconData.startsWith("LOOKUP:")) {
            try {
                String catIdStr = iconData.substring(7);
                ResourceLocation catId = ResourceLocation.tryParse(catIdStr);
                if (catId != null && SkillsCompat.isLoaded()) {
                    var clientMod = SkillsClientMod.getInstance();
                    var clientModAccessor = (SkillsClientModAccessor) clientMod;
                    var screenData = clientModAccessor.mqt$getScreenData();
                    var screenDataAccessor = (ClientSkillScreenDataAccessor) screenData;
                    var categoryData = screenDataAccessor.mqt$getCategory(catId);

                    if (categoryData.isPresent()) {
                        var config = categoryData.get().getConfig();
                        var iconConfig = config.icon();

                        if (iconConfig instanceof ItemIconConfig itemIcon) {
                            return ItemIcon.getItemIcon(itemIcon.item());
                        } else if (iconConfig instanceof TextureIconConfig textureIcon) {
                            return Icon.getIcon(textureIcon.texture());
                        }
                    }
                }
            } catch (Exception e) {

            }
        }

        return getType().getIconSupplier();
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODE_MAP = NameMap.of(Mode.TOTAL_LEVEL, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODE_MAP)
                .setNameKey("morequesttypes.task.skills_level.mode");

        var COMPARISON_MAP = NameMap.of(ComparisonMode.GREATER_OR_EQUAL, ComparisonMode.values())
                .name(cm -> Component.translatable(cm.getTranslationKey()))
                .create();
        config.addEnum("comparison_mode", comparisonMode, v -> {
            comparisonMode = v;
            if (v.isRange() && secondNumber <= firstNumber) {
                secondNumber = firstNumber + 10;
            }
        }, COMPARISON_MAP).setNameKey("morequesttypes.task.comparison_mode");

        config.addInt("first_number", firstNumber, v -> {
            firstNumber = Math.max(0, v);
            if (comparisonMode.isRange() && secondNumber <= firstNumber) {
                secondNumber = firstNumber + 10;
            }
        }, 10, 0, 100000).setNameKey("morequesttypes.task.first_number");

        config.addInt("second_number", secondNumber, v -> {
            if (comparisonMode.isRange() && v <= firstNumber) {
                secondNumber = firstNumber + 10;
            } else {
                secondNumber = Math.max(0, v);
            }
        }, 20, 0, 100000).setNameKey("morequesttypes.task.second_number");

        final var NONE = ResourceLocation.withDefaultNamespace("none");

        ArrayList<ResourceLocation> cats = new ArrayList<>();
        if (SkillsCompat.isLoaded()) {
            cats.addAll(SkillsCompat.getCategories(true));
        }
        cats.add(0, NONE);

        ResourceLocation current = (mode == Mode.TOTAL_LEVEL)
                ? NONE
                : Objects.requireNonNullElse(ResourceLocation.tryParse(categoryId),
                (cats.size() > 1 ? cats.get(1) : NONE));

        if (current.equals(NONE) && cats.size() > 1) current = cats.get(1);

        var CAT_MAP = NameMap
                .of(current, cats.toArray(ResourceLocation[]::new))
                .name(rl -> {
                    if (rl == null) return Component.literal("Unknown");
                    return rl.equals(NONE) ? Component.literal("None") : Component.literal(rl.toString());
                })
                .create();

        config.addEnum("category", current, rl -> {
            categoryId = rl.equals(NONE) ? "" : rl.toString();
        }, CAT_MAP).setNameKey("morequesttypes.task.skills_level.category");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putString("comparison_mode", comparisonMode.name());
        nbt.putInt("first_number", firstNumber);
        nbt.putInt("second_number", secondNumber);
        if (!  categoryId.isBlank()) nbt.putString("category", categoryId);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("mode")) {
            try { mode = Mode.valueOf(nbt.getString("mode")); }
            catch (IllegalArgumentException ignored) { mode = Mode.TOTAL_LEVEL; }
        }
        if (nbt.contains("comparison_mode")) {
            try { comparisonMode = ComparisonMode.valueOf(nbt.getString("comparison_mode")); }
            catch (IllegalArgumentException ignored) { comparisonMode = ComparisonMode.GREATER_OR_EQUAL; }
        }
        firstNumber = Math.max(0, nbt.getInt("first_number"));
        secondNumber = Math.max(0, nbt.getInt("second_number"));
        categoryId = nbt.getString("category");

        if (comparisonMode.isRange() && secondNumber <= firstNumber) {
            secondNumber = firstNumber + 10;
        }

        if (nbt.contains("required_level") && !  nbt.contains("first_number")) {
            firstNumber = Math.max(1, nbt.getInt("required_level"));
            comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(mode);
        buf.writeEnum(comparisonMode);
        buf.writeVarInt(firstNumber);
        buf.writeVarInt(secondNumber);
        buf.writeUtf(categoryId);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        mode = buf.readEnum(Mode.class);
        comparisonMode = buf.readEnum(ComparisonMode.class);
        firstNumber = Math.max(0, buf.readVarInt());
        secondNumber = Math.max(0, buf.readVarInt());
        categoryId = buf.readUtf();

        if (comparisonMode.isRange() && secondNumber <= firstNumber) {
            secondNumber = firstNumber + 10;
        }
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!  checkTaskSequence(teamData)) return;
        if (! SkillsCompat.isLoaded()) return;

        Collection<ServerPlayer> online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;
        if (! online.iterator().next().getUUID().equals(player.getUUID())) return;

        int best = 0;
        if (mode == Mode.TOTAL_LEVEL) {
            for (ServerPlayer p : online) {
                best = Math.max(best, SkillsCompat.getTotalLevel(p));
            }
        } else {
            var cat = parseCategoryOrNull(categoryId);
            if (cat == null) return;
            for (ServerPlayer p : online) {
                best = Math.max(best, SkillsCompat.getCategoryLevel(p, cat));
            }
        }

        long current = teamData.getProgress(this);
        long target;

        if (comparisonMode.isRange()) {
            target = ComparisonManager.compare(best, comparisonMode, firstNumber, secondNumber) ? 1 : 0;
        } else {
            target = Math.max(0, Math.min(getMaxProgress(), best));
        }

        if (target != current) {
            teamData.setProgress(this, target);
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        String compDesc = ComparisonManager.getDescription(comparisonMode, firstNumber, secondNumber);
        if (mode == Mode.TOTAL_LEVEL) {
            list.add(Component.translatable("morequesttypes.task.skills_level.tooltip.total_comparison", compDesc));
        } else {
            var shown = categoryId.isEmpty() ? "?" : categoryId;
            list.add(Component.translatable("morequesttypes.task.skills_level.tooltip.category_comparison", shown, compDesc));
        }
    }

    private static ResourceLocation parseCategoryOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return ResourceLocation.tryParse(s);
    }
}