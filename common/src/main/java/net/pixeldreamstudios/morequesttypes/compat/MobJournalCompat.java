package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.journal.api.MobJournalAPI;
import net.pixeldreamstudios.journal.config.JournalConfig;

import java.util.Collections;
import java.util.Set;

public final class MobJournalCompat {
    private static final String MOD_ID = "journal";

    private MobJournalCompat() {
    }

    public static boolean isLoaded() {
        return Platform.isModLoaded(MobJournalCompat.MOD_ID);
    }

    public static boolean hasDiscovered(Player player, ResourceLocation mobId) {
        if (!MobJournalCompat.isLoaded() || player == null || mobId == null) return false;
        try {
            return MobJournalAPI.hasDiscovered(player, mobId);
        } catch (Throwable t) {
            return false;
        }
    }

    public static Set<ResourceLocation> getDiscoveredMobs(Player player) {
        if (!MobJournalCompat.isLoaded() || player == null) return Collections.emptySet();
        try {
            return MobJournalAPI.getDiscoveredMobs(player);
        } catch (Throwable t) {
            return Collections.emptySet();
        }
    }

    public static boolean isBlacklisted(ResourceLocation mobId) {
        if (!MobJournalCompat.isLoaded() || mobId == null) return false;
        try {
            return JournalConfig.isBlacklisted(mobId);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isDiscoverable(ResourceLocation mobId) {
        if (mobId == null) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(mobId);
        return type != null && type.canSummon() && !MobJournalCompat.isBlacklisted(mobId);
    }
}
