package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.morequesttypes.api.ITaskDungeonDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskDynamicDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.compat.DungeonDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.compat.DynamicDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.tasks.AdvancedKillTask;
import net.pixeldreamstudios.morequesttypes.tasks.DamageTask;
import net.pixeldreamstudios.morequesttypes.tasks.FindEntityTask;
import net.pixeldreamstudios.morequesttypes.tasks.InteractEntityTask;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Task.class, remap = false)
public abstract class TaskExtensions implements ITaskDynamicDifficultyExtension, ITaskDungeonDifficultyExtension {

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

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void mqt$writeDifficultyData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (! mqt$shouldSerialize()) return;

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

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void mqt$readDifficultyData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

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

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void mqt$writeNetDifficultyData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

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

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void mqt$readNetDifficultyData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

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

    @Environment(EnvType.CLIENT)
    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void mqt$addDifficultyConfig(ConfigGroup config, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

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

    @Unique
    private boolean mqt$shouldSerialize() {
        Task self = (Task) (Object) this;
        return self instanceof AdvancedKillTask
                || self instanceof FindEntityTask
                || self instanceof InteractEntityTask
                || self instanceof DamageTask;
    }
}