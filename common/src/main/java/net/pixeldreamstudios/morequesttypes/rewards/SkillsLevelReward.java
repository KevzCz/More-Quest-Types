package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.icon.*;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.*;
import net.fabricmc.api.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.pixeldreamstudios.morequesttypes.compat.*;
import net.pixeldreamstudios.morequesttypes.mixin.accessor.*;
import net.puffish.skillsmod.client.*;
import net.puffish.skillsmod.client.config.*;

import java.util.*;
import java.util.concurrent.*;

public final class SkillsLevelReward extends Reward {
    public enum Kind {EXPERIENCE, POINTS}

    public enum OperationType {ADD, SET, REDUCE}

    private String categoryId = "";
    private Kind kind = Kind.EXPERIENCE;
    private OperationType operationType = OperationType.ADD;
    private int amount = 0;
    private String pointSource = "more_quest_types:reward";

    private static final Map<String, String> CATEGORY_ICONS = new ConcurrentHashMap<>();

    public static void syncCategoryIcons(Map<String, String> icons) {
        SkillsLevelReward.CATEGORY_ICONS.clear();
        SkillsLevelReward.CATEGORY_ICONS.putAll(icons);
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
        if (!SkillsCompat.isLoaded()) return;
        final ResourceLocation cat = SkillsLevelReward.parse(categoryId);
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
        if (!categoryId.isBlank()) nbt.putString("category", categoryId);
        nbt.putString("kind", kind.name());
        nbt.putString("operation_type", operationType.name());
        nbt.putInt("amount", amount);
        nbt.putString("point_source", pointSource);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        categoryId = nbt.getString("category");
        try {
            kind = Kind.valueOf(nbt.getString("kind"));
        } catch (Throwable ignored) {
            kind = Kind.EXPERIENCE;
        }
        try {
            operationType = OperationType.valueOf(nbt.getString("operation_type"));
        } catch (Throwable ignored) {
            operationType = OperationType.ADD;
        }
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

        var KINDS = NameMap.of(Kind.EXPERIENCE, Kind.values()).create();
        config.addEnum("kind", kind, v -> kind = v, KINDS)
                .setNameKey("morequesttypes.reward.skills_level.kind");

        var OPERATIONS = NameMap.of(OperationType.ADD, OperationType.values()).create();
        config.addEnum("operation_type", operationType, v -> operationType = v, OPERATIONS)
                .setNameKey("morequesttypes.reward.skills_level.operation_type");

        config.addInt("amount", amount, v -> amount = v, 0, 0, 1_000_000)
                .setNameKey("morequesttypes.reward.skills_level.amount");

        final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("none");

        ArrayList<ResourceLocation> cats = new ArrayList<>();
        if (SkillsCompat.isLoaded()) {
            cats.addAll(SkillsCompat.getCategories(true));
        }
        cats.add(0, NONE);

        ResourceLocation current = Objects.requireNonNullElse(
                SkillsLevelReward.parse(categoryId),
                (cats.size() > 1 ? cats.get(1) : NONE)
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

        String iconData = SkillsLevelReward.CATEGORY_ICONS.get(categoryId);
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

                        if (iconConfig instanceof ClientIconConfig.ItemIconConfig itemIcon) {
                            return ItemIcon.getItemIcon(itemIcon.item());
                        } else if (iconConfig instanceof ClientIconConfig.TextureIconConfig textureIcon) {
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