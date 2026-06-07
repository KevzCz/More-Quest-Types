package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.api.ITaskPlayerSpellsExtension;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;
import net.pixeldreamstudios.morequesttypes.config.MultiSelectListConfig;
import net.pixeldreamstudios.morequesttypes.tasks.CastSpellTask;
import net.pixeldreamstudios.morequesttypes.tasks.SpellEquippedTask;
import net.pixeldreamstudios.morequesttypes.util.SpellDisplayHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Task.class, remap = false)
public abstract class TaskExtensionsSpellEngine implements ITaskPlayerSpellsExtension {
    @Unique
    private boolean mqt$checkPlayerSpells = false;
    @Unique
    private ITaskPlayerSpellsExtension.MatchMode mqt$playerSpellsMatchMode = ITaskPlayerSpellsExtension.MatchMode.ALL;
    @Unique
    private final List<String> mqt$requiredPlayerSpells = new ArrayList<>();
    @Unique
    private long mqt$playerSpellsRequiredCount = 0;

    @Override
    public boolean shouldCheckPlayerSpells() {
        return mqt$checkPlayerSpells;
    }

    @Override
    public void setShouldCheckPlayerSpells(boolean check) {
        mqt$checkPlayerSpells = check;
    }

    @Override
    public MatchMode getPlayerSpellsMatchMode() {
        return mqt$playerSpellsMatchMode;
    }

    @Override
    public void setPlayerSpellsMatchMode(MatchMode mode) {
        mqt$playerSpellsMatchMode = mode != null ? mode : MatchMode.ALL;
    }

    @Override
    public List<String> getRequiredPlayerSpells() {
        return mqt$requiredPlayerSpells;
    }

    @Override
    public void setRequiredPlayerSpells(List<String> spells) {
        mqt$requiredPlayerSpells.clear();
        if (spells != null) {
            mqt$requiredPlayerSpells.addAll(spells);
        }
    }

    @Override
    public long getPlayerSpellsRequiredCount() {
        return mqt$playerSpellsRequiredCount;
    }

    @Override
    public void setPlayerSpellsRequiredCount(long count) {
        mqt$playerSpellsRequiredCount = Math.max(0, count);
    }

    @Unique
    private boolean mqt$shouldSerializePlayerSpells() {
        if (!SpellEngineCompat.isLoaded()) {
            return false;
        }
        Task self = (Task) (Object) this;
        return self != null
                && !(self instanceof CastSpellTask)
                && !(self instanceof SpellEquippedTask);
    }

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void mqt$writePlayerSpellsData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldSerializePlayerSpells()) {
            return;
        }

        CompoundTag spellsTag = new CompoundTag();
        spellsTag.putBoolean("check", mqt$checkPlayerSpells);
        spellsTag.putString("mode", mqt$playerSpellsMatchMode.name());
        spellsTag.putLong("required", mqt$playerSpellsRequiredCount);
        ListTag list = new ListTag();
        for (String spell : mqt$requiredPlayerSpells) {
            if (spell != null && !spell.isBlank()) {
                list.add(StringTag.valueOf(spell));
            }
        }
        spellsTag.put("spells", list);
        nbt.put("PlayerSpells", spellsTag);
    }

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void mqt$readPlayerSpellsData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldSerializePlayerSpells()) {
            return;
        }

        mqt$requiredPlayerSpells.clear();
        if (!nbt.contains("PlayerSpells")) {
            return;
        }

        CompoundTag spellsTag = nbt.getCompound("PlayerSpells");
        mqt$checkPlayerSpells = spellsTag.getBoolean("check");
        try {
            mqt$playerSpellsMatchMode = MatchMode.valueOf(spellsTag.getString("mode"));
        } catch (Exception ignored) {
            mqt$playerSpellsMatchMode = MatchMode.ALL;
        }
        mqt$playerSpellsRequiredCount = Math.max(0, spellsTag.getLong("required"));

        ListTag list = spellsTag.getList("spells", 8);
        for (int i = 0; i < list.size(); i++) {
            String spell = list.getString(i);
            if (!spell.isBlank()) {
                mqt$requiredPlayerSpells.add(spell);
            }
        }
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void mqt$writeNetPlayerSpellsData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        boolean hasSpells = mqt$shouldSerializePlayerSpells();
        buffer.writeBoolean(hasSpells);
        if (!hasSpells) {
            return;
        }

        buffer.writeBoolean(mqt$checkPlayerSpells);
        buffer.writeEnum(mqt$playerSpellsMatchMode);
        buffer.writeVarLong(mqt$playerSpellsRequiredCount);
        buffer.writeVarInt(mqt$requiredPlayerSpells.size());
        for (String spell : mqt$requiredPlayerSpells) {
            buffer.writeUtf(spell);
        }
    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void mqt$readNetPlayerSpellsData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        boolean hasSpells = buffer.readBoolean();
        if (!hasSpells) {
            return;
        }

        mqt$checkPlayerSpells = buffer.readBoolean();
        mqt$playerSpellsMatchMode = buffer.readEnum(MatchMode.class);
        mqt$playerSpellsRequiredCount = buffer.readVarLong();
        mqt$requiredPlayerSpells.clear();
        int count = buffer.readVarInt();
        for (int i = 0; i < count; i++) {
            String spell = buffer.readUtf();
            if (!spell.isBlank()) {
                mqt$requiredPlayerSpells.add(spell);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void mqt$addPlayerSpellsConfig(ConfigGroup config, CallbackInfo ci) {
        if (!mqt$shouldSerializePlayerSpells()) {
            return;
        }

        ConfigGroup spellsGroup = config.getOrCreateSubgroup("player_spells");
        spellsGroup.setNameKey("morequesttypes.config.group.player_spells");

        spellsGroup.addBool("check", mqt$checkPlayerSpells, v -> mqt$checkPlayerSpells = v, false)
                .setNameKey("morequesttypes.task.player_spells.check");

        var MODE_MAP = NameMap.of(mqt$playerSpellsMatchMode, MatchMode.values())
                .name(mode -> Component.translatable("morequesttypes.task.player_spells.mode." + mode.name().toLowerCase()))
                .create();
        spellsGroup.addEnum("mode", mqt$playerSpellsMatchMode, v -> mqt$playerSpellsMatchMode = v, MODE_MAP)
                .setNameKey("morequesttypes.task.player_spells.mode");

        spellsGroup.addLong("required", mqt$playerSpellsRequiredCount, v -> mqt$playerSpellsRequiredCount = Math.max(0, v), 0L, 0L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.player_spells.required");

        spellsGroup.add("required_spells", new MultiSelectListConfig(
                                () -> {
                                    try {
                                        var minecraft = Minecraft.getInstance();
                                        if (minecraft.level != null) {
                                            return SpellEngineCompat.getAllSpells(minecraft.level).stream()
                                                    .map(ResourceLocation::toString)
                                                    .sorted()
                                                    .toList();
                                        }
                                    } catch (Throwable ignored) {
                                    }
                                    return List.of();
                                },
                                "morequesttypes.task.player_spells.required_spells",
                                SpellDisplayHelper::spellName),
                        new ArrayList<>(mqt$requiredPlayerSpells),
                        v -> {
                            mqt$requiredPlayerSpells.clear();
                            mqt$requiredPlayerSpells.addAll(v);
                        },
                        new ArrayList<>())
                .setNameKey("morequesttypes.task.player_spells.required_spells");
    }
}
