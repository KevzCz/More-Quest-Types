package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.morequesttypes.api.ITaskDungeonDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskDynamicDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskPlayerSpellsExtension;
import net.pixeldreamstudios.morequesttypes.compat.DungeonDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.compat.DynamicDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;
import net.pixeldreamstudios.morequesttypes.config.MultiSelectListConfig;
import net.pixeldreamstudios.morequesttypes.tasks.AdvancedKillTask;
import net.pixeldreamstudios.morequesttypes.tasks.CastSpellTask;
import net.pixeldreamstudios.morequesttypes.tasks.DamageTask;
import net.pixeldreamstudios.morequesttypes.tasks.FindEntityTask;
import net.pixeldreamstudios.morequesttypes.tasks.InteractEntityTask;
import net.pixeldreamstudios.morequesttypes.tasks.ReceiveDamageTask;
import net.pixeldreamstudios.morequesttypes.tasks.SpellEquippedTask;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;
import net.pixeldreamstudios.morequesttypes.util.SpellDisplayHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Task.class, remap = false)
public abstract class TaskExtensions implements ITaskDynamicDifficultyExtension, ITaskDungeonDifficultyExtension, ITaskPlayerSpellsExtension {

    // Dynamic Difficulty fields
    @Unique
    private boolean mqt$checkDynamicDifficulty = false;
    @Unique
    private ComparisonMode mqt$dynamicDifficultyComparison = ComparisonMode.EQUALS;
    @Unique
    private int mqt$dynamicDifficultyFirst = 1;
    @Unique
    private int mqt$dynamicDifficultySecond = 1;

    // Dungeon Difficulty fields
    @Unique
    private boolean mqt$checkDungeonDifficulty = false;
    @Unique
    private ComparisonMode mqt$dungeonDifficultyComparison = ComparisonMode.EQUALS;
    @Unique
    private int mqt$dungeonDifficultyFirst = 1;
    @Unique
    private int mqt$dungeonDifficultySecond = 1;

    // Player Spells fields
    @Unique
    private boolean mqt$checkPlayerSpells = false;
    @Unique
    private ITaskPlayerSpellsExtension.MatchMode mqt$playerSpellsMatchMode = ITaskPlayerSpellsExtension.MatchMode.ALL;
    @Unique
    private final List<String> mqt$requiredPlayerSpells = new ArrayList<>();
    @Unique
    private long mqt$playerSpellsRequiredCount = 0;

    // Dynamic Difficulty implementation
    @Override
    public boolean shouldCheckDynamicDifficultyLevel() {
        return mqt$checkDynamicDifficulty;
    }

    @Override
    public void setShouldCheckDynamicDifficultyLevel(boolean check) {
        this.mqt$checkDynamicDifficulty = check;
    }

    @Override
    public ComparisonMode getDynamicDifficultyComparison() {
        return mqt$dynamicDifficultyComparison;
    }

    @Override
    public void setDynamicDifficultyComparison(ComparisonMode mode) {
        this.mqt$dynamicDifficultyComparison = mode;
    }

    @Override
    public int getDynamicDifficultyFirst() {
        return mqt$dynamicDifficultyFirst;
    }

    @Override
    public void setDynamicDifficultyFirst(int level) {
        this.mqt$dynamicDifficultyFirst = level;
    }

    @Override
    public int getDynamicDifficultySecond() {
        return mqt$dynamicDifficultySecond;
    }

    @Override
    public void setDynamicDifficultySecond(int level) {
        this.mqt$dynamicDifficultySecond = level;
    }

    // Dungeon Difficulty implementation
    @Override
    public boolean shouldCheckDungeonDifficultyLevel() {
        return mqt$checkDungeonDifficulty;
    }

    @Override
    public void setShouldCheckDungeonDifficultyLevel(boolean check) {
        this.mqt$checkDungeonDifficulty = check;
    }

    @Override
    public ComparisonMode getDungeonDifficultyComparison() {
        return mqt$dungeonDifficultyComparison;
    }

    @Override
    public void setDungeonDifficultyComparison(ComparisonMode mode) {
        this.mqt$dungeonDifficultyComparison = mode;
    }

    @Override
    public int getDungeonDifficultyFirst() {
        return mqt$dungeonDifficultyFirst;
    }

    @Override
    public void setDungeonDifficultyFirst(int level) {
        this.mqt$dungeonDifficultyFirst = level;
    }

    @Override
    public int getDungeonDifficultySecond() {
        return mqt$dungeonDifficultySecond;
    }

    @Override
    public void setDungeonDifficultySecond(int level) {
        this.mqt$dungeonDifficultySecond = level;
    }

    // Player Spells implementation
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

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void mqt$writeDifficultyData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (mqt$shouldSerialize()) {
            // Dynamic Difficulty
            if (DynamicDifficultyCompat.isLoaded()) {
                CompoundTag ddTag = new CompoundTag();
                ddTag.putBoolean("check", mqt$checkDynamicDifficulty);
                ddTag.putString("comparison", mqt$dynamicDifficultyComparison.name());
                ddTag.putInt("level_first", mqt$dynamicDifficultyFirst);
                ddTag.putInt("level_second", mqt$dynamicDifficultySecond);
                nbt.put("DynamicDifficulty", ddTag);
            }

            // Dungeon Difficulty
            if (DungeonDifficultyCompat.isLoaded()) {
                CompoundTag dungeonTag = new CompoundTag();
                dungeonTag.putBoolean("check", mqt$checkDungeonDifficulty);
                dungeonTag.putString("comparison", mqt$dungeonDifficultyComparison.name());
                dungeonTag.putInt("level_first", mqt$dungeonDifficultyFirst);
                dungeonTag.putInt("level_second", mqt$dungeonDifficultySecond);
                nbt.put("DungeonDifficulty", dungeonTag);
            }
        }

        // Player Spells
        if (mqt$shouldSerializePlayerSpells()) {
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
    }

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void mqt$readDifficultyData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (mqt$shouldSerialize()) {
            // Dynamic Difficulty
            if (DynamicDifficultyCompat.isLoaded() && nbt.contains("DynamicDifficulty")) {
                CompoundTag ddTag = nbt.getCompound("DynamicDifficulty");
                mqt$checkDynamicDifficulty = ddTag.getBoolean("check");
                try {
                    mqt$dynamicDifficultyComparison = ComparisonMode.valueOf(ddTag.getString("comparison"));
                } catch (Exception e) {
                    mqt$dynamicDifficultyComparison = ComparisonMode.EQUALS;
                }
                mqt$dynamicDifficultyFirst = ddTag.getInt("level_first");
                mqt$dynamicDifficultySecond = ddTag.getInt("level_second");
            }

            // Dungeon Difficulty
            if (DungeonDifficultyCompat.isLoaded() && nbt.contains("DungeonDifficulty")) {
                CompoundTag dungeonTag = nbt.getCompound("DungeonDifficulty");
                mqt$checkDungeonDifficulty = dungeonTag.getBoolean("check");
                try {
                    mqt$dungeonDifficultyComparison = ComparisonMode.valueOf(dungeonTag.getString("comparison"));
                } catch (Exception e) {
                    mqt$dungeonDifficultyComparison = ComparisonMode.EQUALS;
                }
                mqt$dungeonDifficultyFirst = dungeonTag.getInt("level_first");
                mqt$dungeonDifficultySecond = dungeonTag.getInt("level_second");
            }
        }

        // Player Spells
        mqt$requiredPlayerSpells.clear();
        if (mqt$shouldSerializePlayerSpells() && nbt.contains("PlayerSpells")) {
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
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void mqt$writeNetDifficultyData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (mqt$shouldSerialize()) {
            // Dynamic Difficulty
            boolean hasDynamic = DynamicDifficultyCompat.isLoaded();
            buffer.writeBoolean(hasDynamic);
            if (hasDynamic) {
                buffer.writeBoolean(mqt$checkDynamicDifficulty);
                buffer.writeEnum(mqt$dynamicDifficultyComparison);
                buffer.writeVarInt(mqt$dynamicDifficultyFirst);
                buffer.writeVarInt(mqt$dynamicDifficultySecond);
            }

            // Dungeon Difficulty
            boolean hasDungeon = DungeonDifficultyCompat.isLoaded();
            buffer.writeBoolean(hasDungeon);
            if (hasDungeon) {
                buffer.writeBoolean(mqt$checkDungeonDifficulty);
                buffer.writeEnum(mqt$dungeonDifficultyComparison);
                buffer.writeVarInt(mqt$dungeonDifficultyFirst);
                buffer.writeVarInt(mqt$dungeonDifficultySecond);
            }
        }

        // Player Spells
        boolean hasSpells = mqt$shouldSerializePlayerSpells();
        buffer.writeBoolean(hasSpells);
        if (hasSpells) {
            buffer.writeBoolean(mqt$checkPlayerSpells);
            buffer.writeEnum(mqt$playerSpellsMatchMode);
            buffer.writeVarLong(mqt$playerSpellsRequiredCount);
            buffer.writeVarInt(mqt$requiredPlayerSpells.size());
            for (String spell : mqt$requiredPlayerSpells) {
                buffer.writeUtf(spell);
            }
        }
    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void mqt$readNetDifficultyData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (mqt$shouldSerialize()) {
            // Dynamic Difficulty
            boolean hasDynamic = buffer.readBoolean();
            if (hasDynamic) {
                mqt$checkDynamicDifficulty = buffer.readBoolean();
                mqt$dynamicDifficultyComparison = buffer.readEnum(ComparisonMode.class);
                mqt$dynamicDifficultyFirst = buffer.readVarInt();
                mqt$dynamicDifficultySecond = buffer.readVarInt();
            }

            // Dungeon Difficulty
            boolean hasDungeon = buffer.readBoolean();
            if (hasDungeon) {
                mqt$checkDungeonDifficulty = buffer.readBoolean();
                mqt$dungeonDifficultyComparison = buffer.readEnum(ComparisonMode.class);
                mqt$dungeonDifficultyFirst = buffer.readVarInt();
                mqt$dungeonDifficultySecond = buffer.readVarInt();
            }
        }

        // Player Spells
        boolean hasSpells = buffer.readBoolean();
        if (hasSpells) {
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
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void mqt$addDifficultyConfig(ConfigGroup config, CallbackInfo ci) {
        if (mqt$shouldSerialize()) {
            var COMPARISON_MAP = NameMap.of(ComparisonMode.EQUALS, ComparisonMode.values())
                    .name(mode -> Component.translatable(mode.getTranslationKey()))
                    .create();

            // Dynamic Difficulty Group
            if (DynamicDifficultyCompat.isLoaded()) {
                ConfigGroup ddGroup = config.getOrCreateSubgroup("dynamic_difficulty");
                ddGroup.setNameKey("morequesttypes.config.group.dynamic_difficulty");

                ddGroup.addBool("check_level", mqt$checkDynamicDifficulty,
                                v -> mqt$checkDynamicDifficulty = v, false)
                        .setNameKey("morequesttypes.task.dynamic_difficulty.check_level");

                ddGroup.addEnum("comparison", mqt$dynamicDifficultyComparison,
                                v -> mqt$dynamicDifficultyComparison = v, COMPARISON_MAP)
                        .setNameKey("morequesttypes.task.dynamic_difficulty.comparison");

                ddGroup.addInt("level_first", mqt$dynamicDifficultyFirst,
                                v -> mqt$dynamicDifficultyFirst = Math.max(1, v), 1, 1, 1000)
                        .setNameKey("morequesttypes.task.dynamic_difficulty.level_first");

                ddGroup.addInt("level_second", mqt$dynamicDifficultySecond,
                                v -> mqt$dynamicDifficultySecond = Math.max(mqt$dynamicDifficultyFirst + 1, v),
                                mqt$dynamicDifficultyFirst + 1, mqt$dynamicDifficultyFirst + 1, 1000)
                        .setNameKey("morequesttypes.task.dynamic_difficulty.level_second");
            }

            // Dungeon Difficulty Group
            if (DungeonDifficultyCompat.isLoaded()) {
                ConfigGroup dungeonGroup = config.getOrCreateSubgroup("dungeon_difficulty");
                dungeonGroup.setNameKey("morequesttypes.config.group.dungeon_difficulty");

                dungeonGroup.addBool("check_level", mqt$checkDungeonDifficulty,
                                v -> mqt$checkDungeonDifficulty = v, false)
                        .setNameKey("morequesttypes.task.dungeon_difficulty.check_level");

                dungeonGroup.addEnum("comparison", mqt$dungeonDifficultyComparison,
                                v -> mqt$dungeonDifficultyComparison = v, COMPARISON_MAP)
                        .setNameKey("morequesttypes.task.dungeon_difficulty.comparison");

                dungeonGroup.addInt("level_first", mqt$dungeonDifficultyFirst,
                                v -> mqt$dungeonDifficultyFirst = Math.max(1, v), 1, 1, 1000)
                        .setNameKey("morequesttypes.task.dungeon_difficulty.level_first");

                dungeonGroup.addInt("level_second", mqt$dungeonDifficultySecond,
                                v -> mqt$dungeonDifficultySecond = Math.max(mqt$dungeonDifficultyFirst + 1, v),
                                mqt$dungeonDifficultyFirst + 1, mqt$dungeonDifficultyFirst + 1, 1000)
                        .setNameKey("morequesttypes.task.dungeon_difficulty.level_second");
            }
        }

        // Player Spells Group
        if (mqt$shouldSerializePlayerSpells()) {
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
                                    SpellDisplayHelper::availableSpellIdsOnClient,
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

    @Unique
    private boolean mqt$shouldSerialize() {
        Task self = (Task) (Object) this;
        return self instanceof AdvancedKillTask
                || self instanceof FindEntityTask
                || self instanceof InteractEntityTask
                || self instanceof DamageTask
                || self instanceof ReceiveDamageTask
                || self instanceof CastSpellTask;
    }

    @Unique
    private boolean mqt$shouldSerializePlayerSpells() {
        Task self = (Task) (Object) this;
        if (self instanceof CastSpellTask || self instanceof SpellEquippedTask) {
            return false;
        }
        return SpellEngineCompat.isLoaded();
    }
}
