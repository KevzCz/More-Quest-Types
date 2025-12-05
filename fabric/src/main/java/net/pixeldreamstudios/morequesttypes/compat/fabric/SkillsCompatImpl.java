package net.pixeldreamstudios.morequesttypes.compat.fabric;

import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public final class SkillsCompatImpl {
    private static final String MOD_ID = "puffish_skills";

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

    public static Collection<ResourceLocation> getCategories(boolean onlyWithExperience) {
        if (!isLoaded()) return java.util.List.of();

        return SkillsMod.getInstance().getCategories(onlyWithExperience)
                .stream().collect(Collectors.toUnmodifiableList());
    }

    public static Map<String, String> getCategoryIconData(ServerPlayer player) {
        Map<String, String> result = new java.util.HashMap<>();
        if (! isLoaded()) return result;

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