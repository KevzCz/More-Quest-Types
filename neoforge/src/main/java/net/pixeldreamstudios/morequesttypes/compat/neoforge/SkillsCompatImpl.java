package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SkillsCompatImpl {
    private static final String MOD_ID = "puffish_skills";

    private SkillsCompatImpl() {}

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static int getTotalLevel(ServerPlayer player) {
        if (!isLoaded()) return 0;
        var api = SkillsMod.getInstance();
        return api.getCategories(true).stream()
                .map(id -> api.getCurrentLevel(player, id).orElse(0))
                .reduce(0, Integer::sum);
    }

    public static int getCategoryLevel(ServerPlayer player, ResourceLocation categoryId) {
        if (! isLoaded()) return 0;
        return SkillsMod.getInstance().getCurrentLevel(player, categoryId).orElse(0);
    }

    public static int getCategoryExperience(ServerPlayer player, ResourceLocation categoryId) {
        if (!isLoaded()) return 0;
        return SkillsMod.getInstance().getExperience(player, categoryId).orElse(0);
    }

    public static int getCategoryPoints(ServerPlayer player, ResourceLocation categoryId, ResourceLocation source) {
        if (!isLoaded()) return 0;
        return SkillsMod.getInstance().getPoints(player, categoryId, source).orElse(0);
    }

    public static void addCategoryExperience(ServerPlayer player, ResourceLocation categoryId, int amount) {
        if (!isLoaded()) return;
        SkillsMod.getInstance().addExperience(player, categoryId, amount);
    }

    public static void setCategoryExperience(ServerPlayer player, ResourceLocation categoryId, int amount) {
        if (!isLoaded()) return;
        SkillsMod.getInstance().setExperience(player, categoryId, amount);
    }

    public static void addCategoryPoints(ServerPlayer player, ResourceLocation categoryId, ResourceLocation source, int amount) {
        if (!isLoaded()) return;
        SkillsMod.getInstance().addPoints(player, categoryId, source, amount, false);
    }

    public static void setCategoryPoints(ServerPlayer player, ResourceLocation categoryId, ResourceLocation source, int amount) {
        if (!isLoaded()) return;
        SkillsMod.getInstance().setPoints(player, categoryId, source, amount, false);
    }

    public static Collection<ResourceLocation> getCategories(boolean onlyWithExperience) {
        if (!isLoaded()) return List.of();
        return SkillsMod.getInstance().getCategories(onlyWithExperience)
                .stream()
                .collect(Collectors.toUnmodifiableList());
    }

    public static Map<String, String> getCategoryIconData(ServerPlayer player) {
        Map<String, String> result = new java.util.HashMap<>();
        if (!isLoaded()) return result;

        try {
            var skillsMod = SkillsMod.getInstance();
            var categories = skillsMod.getCategories(false);

            for (ResourceLocation catId : categories) {
                result.put(catId.toString(), "LOOKUP:" + catId.toString());
            }
        } catch (Exception e) {
        }

        return result;
    }
}