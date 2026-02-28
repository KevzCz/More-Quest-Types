package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.util.SpellRender;
import net.spell_engine.internals.container.SpellContainerSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SpellEngineCompatImpl {
    private static final String MOD_ID = "spell_engine";

    private SpellEngineCompatImpl() {}

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static Collection<ResourceLocation> getAllSpells(Level level) {
        if (!isLoaded() || level == null) return List.of();
        try {
            return SpellRegistry.stream(level)
                    .map(holderRef -> holderRef.unwrapKey().map(k -> k.location()).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableList());
        } catch (Throwable t) {
            return List.of();
        }
    }

    public static void installSpells(ServerPlayer player, String baseKey, Collection<ResourceLocation> spells) {
        if (!isLoaded() || player == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
            serverSide.keySet().removeIf(k -> k.startsWith(baseKey + "/"));

            if (spells != null && !spells.isEmpty()) {
                List<String> ids = spells.stream().map(ResourceLocation::toString).collect(Collectors.toList());
                SpellContainer container = new SpellContainer(SpellContainer.ContentType.ANY, true, "", 0, ids);
                serverSide.put(baseKey + "/any", container);
            }

            SpellContainerSource.setDirtyServerSide(player);
            SpellContainerSource.syncServerSideContainers(player);
        } catch (Throwable ignored) {}
    }

    public static void uninstallSpells(ServerPlayer player, String baseKey) {
        if (!isLoaded() || player == null) return;
        try {
            Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
            serverSide.keySet().removeIf(k -> k.startsWith(baseKey + "/"));
            SpellContainerSource.setDirtyServerSide(player);
            SpellContainerSource.syncServerSideContainers(player);
        } catch (Throwable ignored) {}
    }
    public static Collection<String> getInstalledSpellKeys(ServerPlayer player) {
        if (!isLoaded() || player == null) return List.of();
        try {
            @SuppressWarnings("unchecked")
            Map<String, SpellContainer> serverSide =
                    ((SpellContainerSource.Owner) player).serverSideSpellContainers();
            return List.copyOf(serverSide.keySet());
        } catch (Throwable t) {
            return List.of();
        }
    }

    public static void removeInstalledSpellKeys(ServerPlayer player, Collection<String> keys) {
        if (!isLoaded() || player == null || keys == null || keys.isEmpty()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, SpellContainer> serverSide =
                    ((SpellContainerSource.Owner) player).serverSideSpellContainers();
            for (String k : keys) serverSide.remove(k);
            SpellContainerSource.setDirtyServerSide(player);
            SpellContainerSource.syncServerSideContainers(player);
        } catch (Throwable ignored) {}
    }
    public static ResourceLocation getSpellIconTexture(ResourceLocation spellId) {
        if (!isLoaded() || spellId == null) return null;
        try {
            // SpellRender helper you showed maps spell id -> texture path
            return SpellRender.iconTexture(spellId);
        } catch (Throwable t) {
            return null;
        }
    }
}
