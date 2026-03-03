package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.platform.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.spell_engine.api.spell.container.*;
import net.spell_engine.api.spell.registry.*;
import net.spell_engine.client.util.*;
import net.spell_engine.internals.container.*;

import java.util.*;
import java.util.stream.*;

public final class SpellEngineCompatImpl {
    private static final String MOD_ID = "spell_engine";

    private SpellEngineCompatImpl() {
    }

    public static boolean isLoaded() {
        return Platform.isModLoaded(SpellEngineCompatImpl.MOD_ID);
    }

    public static Collection<ResourceLocation> getAllSpells(Level level) {
        if (!SpellEngineCompatImpl.isLoaded() || level == null) return List.of();
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
        if (!SpellEngineCompatImpl.isLoaded() || player == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
            serverSide.keySet().removeIf(k -> k.startsWith(baseKey + "/"));

            if (spells != null && !spells.isEmpty()) {
                List<String> ids = spells.stream().map(ResourceLocation::toString).collect(Collectors.toList());
                SpellContainer container = new SpellContainer(
                        SpellContainer.ContentType.MAGIC, // access (ContentType)
                        "",                                // access_param
                        "",                                // pool
                        "",                                // slot
                        ids.size(),                        // max_spell_count
                        ids,                               // spell_ids
                        0                                  // extra_tier_binding
                );
                serverSide.put(baseKey + "/magic", container);
            }

            SpellContainerSource.setDirtyServerSide(player);
            SpellContainerSource.syncServerSideContainers(player);
        } catch (Throwable ignored) {
        }
    }

    public static void uninstallSpells(ServerPlayer player, String baseKey) {
        if (!SpellEngineCompatImpl.isLoaded() || player == null) return;
        try {
            Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
            serverSide.keySet().removeIf(k -> k.startsWith(baseKey + "/"));
            SpellContainerSource.setDirtyServerSide(player);
            SpellContainerSource.syncServerSideContainers(player);
        } catch (Throwable ignored) {
        }
    }

    public static Collection<String> getInstalledSpellKeys(ServerPlayer player) {
        if (!SpellEngineCompatImpl.isLoaded() || player == null) return List.of();
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
        if (!SpellEngineCompatImpl.isLoaded() || player == null || keys == null || keys.isEmpty()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, SpellContainer> serverSide =
                    ((SpellContainerSource.Owner) player).serverSideSpellContainers();
            for (String k : keys) serverSide.remove(k);
            SpellContainerSource.setDirtyServerSide(player);
            SpellContainerSource.syncServerSideContainers(player);
        } catch (Throwable ignored) {
        }
    }

    public static ResourceLocation getSpellIconTexture(ResourceLocation spellId) {
        if (!SpellEngineCompatImpl.isLoaded() || spellId == null) return null;
        try {
            return SpellRender.iconTexture(spellId);
        } catch (Throwable t) {
            return null;
        }
    }
}
