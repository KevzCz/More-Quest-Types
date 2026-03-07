package net.pixeldreamstudios.morequesttypes.compat.fabric;

import dev.architectury.platform.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.spell_engine.api.spell.container.*;
import net.spell_engine.api.spell.registry.*;
import net.spell_engine.client.util.*;
import net.spell_engine.internals.container.*;

import java.util.*;
import java.util.stream.*;

public final class SpellEngineCompatImpl {
    private static final String MOD_ID = "spell_engine";
    private static DataComponentType<SpellContainer> SPELL_CONTAINER_TYPE = null;

    private SpellEngineCompatImpl() {
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<SpellContainer> getSpellContainerType() {
        if (SpellEngineCompatImpl.SPELL_CONTAINER_TYPE == null && SpellEngineCompatImpl.isLoaded()) {
            try {
                SpellEngineCompatImpl.SPELL_CONTAINER_TYPE = (DataComponentType<SpellContainer>)
                        BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("spell_engine:spell_container"));
            } catch (Throwable ignored) {
            }
        }
        return SpellEngineCompatImpl.SPELL_CONTAINER_TYPE;
    }

    private static SpellContainer.ContentType getContentType(String contentType) {
        try {
            return SpellContainer.ContentType.valueOf(contentType.toUpperCase());
        } catch (Exception e) {
            return SpellContainer.ContentType.MAGIC;
        }
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
                        SpellContainer.ContentType.MAGIC,
                        "",
                        "",
                        "",
                        ids.size(),
                        ids,
                        0
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

    public static List<ResourceLocation> getItemSpells(ItemStack stack) {
        if (!SpellEngineCompatImpl.isLoaded() || stack == null || stack.isEmpty()) return List.of();
        try {
            DataComponentType<SpellContainer> type = SpellEngineCompatImpl.getSpellContainerType();
            if (type == null) return List.of();

            SpellContainer container = stack.get(type);
            if (container == null) return List.of();

            return container.spell_ids().stream()
                    .map(ResourceLocation::parse)
                    .collect(Collectors.toList());
        } catch (Throwable t) {
            return List.of();
        }
    }

    public static void addItemSpell(ItemStack stack, ResourceLocation spellId, String contentType) {
        if (!SpellEngineCompatImpl.isLoaded() || stack == null || stack.isEmpty() || spellId == null) return;
        try {
            DataComponentType<SpellContainer> type = SpellEngineCompatImpl.getSpellContainerType();
            if (type == null) return;

            SpellContainer existing = stack.get(type);
            List<String> spellIds;
            int maxCount;

            if (existing != null) {
                spellIds = new ArrayList<>(existing.spell_ids());
                String spellStr = spellId.toString();
                if (!spellIds.contains(spellStr)) {
                    spellIds.add(spellStr);
                }
                maxCount = existing.max_spell_count();
            } else {
                spellIds = new ArrayList<>();
                spellIds.add(spellId.toString());
                maxCount = 1;
            }

            maxCount = Math.max(maxCount, spellIds.size());

            SpellContainer.ContentType ct = getContentType(contentType);
            SpellContainer newContainer = new SpellContainer(
                    ct,
                    "",
                    "",
                    "",
                    maxCount,
                    spellIds,
                    0
            );

            stack.set(type, newContainer);
        } catch (Throwable ignored) {
        }
    }

    public static void removeItemSpell(ItemStack stack, ResourceLocation spellId) {
        if (!SpellEngineCompatImpl.isLoaded() || stack == null || stack.isEmpty() || spellId == null) return;
        try {
            DataComponentType<SpellContainer> type = SpellEngineCompatImpl.getSpellContainerType();
            if (type == null) return;

            SpellContainer existing = stack.get(type);
            if (existing == null) return;

            List<String> spellIds = new ArrayList<>(existing.spell_ids());
            spellIds.remove(spellId.toString());

            if (spellIds.isEmpty()) {
                stack.remove(type);
            } else {
                SpellContainer newContainer = new SpellContainer(
                        existing.access(),
                        existing.access_param(),
                        existing.pool(),
                        existing.slot(),
                        existing.max_spell_count(),
                        spellIds,
                        existing.extra_tier_binding()
                );
                stack.set(type, newContainer);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setItemSpells(ItemStack stack, List<ResourceLocation> spellIds, String contentType) {
        if (!SpellEngineCompatImpl.isLoaded() || stack == null || stack.isEmpty() || spellIds == null) return;
        try {
            DataComponentType<SpellContainer> type = SpellEngineCompatImpl.getSpellContainerType();
            if (type == null) return;

            if (spellIds.isEmpty()) {
                stack.remove(type);
                return;
            }

            List<String> spellStrs = spellIds.stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toList());

            SpellContainer existing = stack.get(type);
            SpellContainer.ContentType ct = getContentType(contentType);

            SpellContainer newContainer = new SpellContainer(
                    existing != null ? existing.access() : ct,
                    existing != null ? existing.access_param() : "",
                    existing != null ? existing.pool() : "",
                    existing != null ? existing.slot() : "",
                    spellStrs.size(),
                    spellStrs,
                    existing != null ? existing.extra_tier_binding() : 0
            );

            stack.set(type, newContainer);
        } catch (Throwable ignored) {
        }
    }

    public static boolean hasItemSpell(ItemStack stack, ResourceLocation spellId) {
        if (!SpellEngineCompatImpl.isLoaded() || stack == null || stack.isEmpty() || spellId == null) return false;
        try {
            DataComponentType<SpellContainer> type = SpellEngineCompatImpl.getSpellContainerType();
            if (type == null) return false;

            SpellContainer container = stack.get(type);
            if (container == null) return false;

            return container.spell_ids().contains(spellId.toString());
        } catch (Throwable t) {
            return false;
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
