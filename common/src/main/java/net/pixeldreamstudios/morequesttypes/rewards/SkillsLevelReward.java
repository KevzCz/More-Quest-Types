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

    private String categoryId = "";
    private Kind kind = Kind.EXPERIENCE;
    private int amount = 0;
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
        if (!SkillsCompat.isLoaded()) return;
        final ResourceLocation cat = parse(categoryId);
        if (cat == null) return;

        switch (kind) {
            case EXPERIENCE -> {
                if (amount != 0) {
                    net.puffish.skillsmod.SkillsMod.getInstance().addExperience(player, cat, amount);
                }
            }
            case POINTS -> {
                if (amount != 0) {
                    final ResourceLocation SRC = ResourceLocation.fromNamespaceAndPath("more_quest_types", "reward");
                    net.puffish.skillsmod.SkillsMod.getInstance().addPoints(player, cat, SRC, amount, false);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        String cat = categoryId.isBlank() ? "?" : categoryId;
        String mode = Integer.toString(amount);
        return Component.translatable(
                "morequesttypes.reward.skills_level.title",
                kind.name().toLowerCase(Locale.ROOT),
                cat,
                mode
        );
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!categoryId.isBlank()) nbt.putString("category", categoryId);
        nbt.putString("kind", kind.name());
        nbt.putInt("amount", amount);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        categoryId = nbt.getString("category");
        try { kind = Kind.valueOf(nbt.getString("kind")); } catch (Throwable ignored) { kind = Kind.EXPERIENCE; }
        amount = nbt.getInt("amount");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(categoryId);
        buf.writeEnum(kind);
        buf.writeVarInt(amount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        categoryId = buf.readUtf();
        kind = buf.readEnum(Kind.class);
        amount = buf.readVarInt();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var KINDS = NameMap.of(Kind.EXPERIENCE, Kind.values()).create();
        config.addEnum("kind", kind, v -> kind = v, KINDS)
                .setNameKey("morequesttypes.reward.skills_level.kind");

        config.addInt("amount", amount, v -> amount = v, 0, 0, 1_000_000)
                .setNameKey("morequesttypes.reward.skills_level.amount");

        final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("none");

        ArrayList<ResourceLocation> cats = new ArrayList<>();
        if (SkillsCompat.isLoaded()) {
            cats.addAll(SkillsCompat.getCategories(true));
        }
        cats.add(0, NONE);

        ResourceLocation current = Objects.requireNonNullElse(
                parse(categoryId),
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
