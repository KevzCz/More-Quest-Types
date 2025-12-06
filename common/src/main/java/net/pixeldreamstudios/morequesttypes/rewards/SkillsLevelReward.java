package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillsLevelReward extends Reward {
    public enum Kind { EXPERIENCE, POINTS }
    public enum OperationType { ADD, SET, REDUCE }

    private String categoryId = "";
    private Kind kind = Kind.EXPERIENCE;
    private OperationType operationType = OperationType.ADD;
    private int amount = 0;
    private String pointSource = "more_quest_types:reward";

    private static final Map<String, String> CATEGORY_ICONS = new ConcurrentHashMap<>();

    public static void syncCategoryIcons(Map<String, String> icons) {
        CATEGORY_ICONS.clear();
        CATEGORY_ICONS.putAll(icons);
    }

    public SkillsLevelReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.SKILLS_LEVEL;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (! SkillsCompat.isLoaded()) return;
        final ResourceLocation cat = parse(categoryId);
        if (cat == null) return;
        if (amount == 0 && operationType != OperationType.SET) return;

        final ResourceLocation source = ResourceLocation.tryParse(pointSource);
        if (source == null) return;

        switch (kind) {
            case EXPERIENCE -> {
                switch (operationType) {
                    case ADD -> SkillsCompat.addCategoryExperience(player, cat, amount);
                    case SET -> SkillsCompat.setCategoryExperience(player, cat, amount);
                    case REDUCE -> {
                        // FIXED: Get current EXPERIENCE (not level) and subtract
                        int currentXp = SkillsCompat.getCategoryExperience(player, cat);
                        int newXp = Math.max(0, currentXp - amount);
                        SkillsCompat.setCategoryExperience(player, cat, newXp);
                    }
                }
            }
            case POINTS -> {
                switch (operationType) {
                    case ADD -> SkillsCompat.addCategoryPoints(player, cat, source, amount);
                    case SET -> SkillsCompat.setCategoryPoints(player, cat, source, amount);
                    case REDUCE -> {
                        int current = SkillsCompat.getCategoryPoints(player, cat, source);
                        int newAmount = current - amount; // Points can go negative
                        SkillsCompat.setCategoryPoints(player, cat, source, newAmount);
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        String cat = categoryId.isBlank() ? "?" : categoryId;
        String kindStr = kind.name().toLowerCase(Locale.ROOT);
        String opStr = operationType.name().toLowerCase(Locale.ROOT);

        return Component.translatable(
                "morequesttypes.reward.skills_level.title",
                opStr,
                amount,
                kindStr,
                cat
        );
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (! categoryId.isBlank()) nbt.putString("category", categoryId);
        nbt.putString("kind", kind.name());
        nbt.putString("operation_type", operationType.name());
        nbt.putInt("amount", amount);
        nbt.putString("point_source", pointSource);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        categoryId = nbt.getString("category");
        try { kind = Kind.valueOf(nbt.getString("kind")); } catch (Throwable ignored) { kind = Kind.EXPERIENCE; }
        try { operationType = OperationType.valueOf(nbt.getString("operation_type")); } catch (Throwable ignored) { operationType = OperationType.ADD; }
        amount = nbt.getInt("amount");
        pointSource = nbt.contains("point_source") ? nbt.getString("point_source") : "more_quest_types:reward";
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(categoryId);
        buf.writeEnum(kind);
        buf.writeEnum(operationType);
        buf.writeVarInt(amount);
        buf.writeUtf(pointSource);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        categoryId = buf.readUtf();
        kind = buf.readEnum(Kind.class);
        operationType = buf.readEnum(OperationType.class);
        amount = buf.readVarInt();
        pointSource = buf.readUtf();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        // Kind selector
        var KINDS = NameMap.of(Kind.EXPERIENCE, Kind.values()).create();
        config.addEnum("kind", kind, v -> kind = v, KINDS)
                .setNameKey("morequesttypes.reward.skills_level.kind");

        // Operation type selector
        var OPERATIONS = NameMap.of(OperationType.ADD, OperationType.values()).create();
        config.addEnum("operation_type", operationType, v -> operationType = v, OPERATIONS)
                .setNameKey("morequesttypes.reward.skills_level.operation_type");

        // Amount input
        config.addInt("amount", amount, v -> amount = v, 0, 0, 1_000_000)
                .setNameKey("morequesttypes.reward.skills_level.amount");

        // Category selector
        final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("none");

        ArrayList<ResourceLocation> cats = new ArrayList<>();
        if (SkillsCompat.isLoaded()) {
            cats.addAll(SkillsCompat.getCategories(true));
        }
        cats.add(0, NONE);

        ResourceLocation current = Objects.requireNonNullElse(
                parse(categoryId),
                (cats.size() > 1 ?  cats.get(1) : NONE)
        );
        if (current.equals(NONE) && cats.size() > 1) {
            current = cats.get(1);
        }

        var CAT_MAP = NameMap
                .of(current, cats.toArray(ResourceLocation[]::new))
                .name(rl -> rl.equals(NONE)
                        ? Component.literal("None")
                        : Component.literal(rl.toString()))
                .create();

        config.addEnum("category", current, rl -> {
            categoryId = rl.equals(NONE) ? "" : rl.toString();
        }, CAT_MAP).setNameKey("morequesttypes.reward.skills_level.category");

        config.addString("point_source", pointSource, v -> pointSource = v, "more_quest_types:reward")
                .setNameKey("morequesttypes.reward.skills_level.point_source");
    }

    private static ResourceLocation parse(String s) {
        if (s == null || s.isBlank()) return null;
        return ResourceLocation.tryParse(s);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (categoryId.isEmpty()) {
            return getType().getIconSupplier();
        }

        String iconData = CATEGORY_ICONS.get(categoryId);
        if (iconData != null && iconData.startsWith("LOOKUP:")) {
            try {
                String catIdStr = iconData.substring(7);
                ResourceLocation catId = ResourceLocation.tryParse(catIdStr);
                if (catId != null && SkillsCompat.isLoaded()) {
                    var clientMod = net.puffish.skillsmod.client.SkillsClientMod.getInstance();
                    var clientModAccessor = (net.pixeldreamstudios.morequesttypes.mixin.accessor.SkillsClientModAccessor) clientMod;
                    var screenData = clientModAccessor.mqt$getScreenData();
                    var screenDataAccessor = (net.pixeldreamstudios.morequesttypes.mixin.accessor.ClientSkillScreenDataAccessor) screenData;
                    var categoryData = screenDataAccessor.mqt$getCategory(catId);

                    if (categoryData.isPresent()) {
                        var config = categoryData.get().getConfig();
                        var iconConfig = config.icon();

                        if (iconConfig instanceof net.puffish.skillsmod.client.config.ClientIconConfig.ItemIconConfig itemIcon) {
                            return dev.ftb.mods.ftblibrary.icon.ItemIcon.getItemIcon(itemIcon.item());
                        } else if (iconConfig instanceof net.puffish.skillsmod.client.config.ClientIconConfig.TextureIconConfig textureIcon) {
                            return Icon.getIcon(textureIcon.texture());
                        }
                    }
                }
            } catch (Exception e) {

            }
        }

        return getType().getIconSupplier();
    }
}