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
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.api.ITaskLevelZExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskReskillableExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskSkillsExtension;
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import net.pixeldreamstudios.morequesttypes.tasks.LevelZTask;
import net.pixeldreamstudios.morequesttypes.tasks.ReskillableTask;
import net.pixeldreamstudios.morequesttypes.tasks.SkillsLevelTask;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = Task.class, remap = false)
public abstract class TaskExtensionsPlayerLeveling implements
        ITaskSkillsExtension,
        ITaskLevelZExtension,
        ITaskReskillableExtension {
    @Unique
    private boolean mqt$checkSkills = false;
    @Unique
    private ITaskSkillsExtension.SkillsMode mqt$skillsMode = ITaskSkillsExtension.SkillsMode.TOTAL_LEVEL;
    @Unique
    private ComparisonMode mqt$skillsComparison = ComparisonMode.GREATER_OR_EQUAL;
    @Unique
    private int mqt$skillsFirst = 10;
    @Unique
    private int mqt$skillsSecond = 20;
    @Unique
    private String mqt$skillsCategoryId = "";
    @Unique
    private boolean mqt$checkLevelZ = false;
    @Unique
    private ITaskLevelZExtension.LevelZMode mqt$levelZMode = ITaskLevelZExtension.LevelZMode.LEVEL;
    @Unique
    private ComparisonMode mqt$levelZComparison = ComparisonMode.GREATER_OR_EQUAL;
    @Unique
    private int mqt$levelZFirst = 10;
    @Unique
    private int mqt$levelZSecond = 20;
    @Unique
    private int mqt$levelZSkillId = -1;
    @Unique
    private boolean mqt$checkReskillable = false;
    @Unique
    private ITaskReskillableExtension.ReskillableMode mqt$reskillableMode = ITaskReskillableExtension.ReskillableMode.SKILL_LEVEL;
    @Unique
    private ComparisonMode mqt$reskillableComparison = ComparisonMode.GREATER_OR_EQUAL;
    @Unique
    private int mqt$reskillableFirst = 10;
    @Unique
    private int mqt$reskillableSecond = 20;
    @Unique
    private int mqt$reskillableSkillIndex = 0;
    @Override
    public boolean shouldCheckSkillsLevel() { return mqt$checkSkills; }
    @Override
    public void setShouldCheckSkillsLevel(boolean check) { mqt$checkSkills = check; }
    @Override
    public ITaskSkillsExtension.SkillsMode getSkillsMode() { return mqt$skillsMode; }
    @Override
    public void setSkillsMode(ITaskSkillsExtension.SkillsMode mode) { mqt$skillsMode = mode; }
    @Override
    public ComparisonMode getSkillsComparison() { return mqt$skillsComparison; }
    @Override
    public void setSkillsComparison(ComparisonMode mode) { mqt$skillsComparison = mode; }
    @Override
    public int getSkillsFirstNumber() { return mqt$skillsFirst; }
    @Override
    public void setSkillsFirstNumber(int value) { mqt$skillsFirst = value; }
    @Override
    public int getSkillsSecondNumber() { return mqt$skillsSecond; }
    @Override
    public void setSkillsSecondNumber(int value) { mqt$skillsSecond = value; }
    @Override
    public String getSkillsCategoryId() { return mqt$skillsCategoryId; }
    @Override
    public void setSkillsCategoryId(String categoryId) { mqt$skillsCategoryId = categoryId; }
    @Override
    public boolean shouldCheckLevelZ() { return mqt$checkLevelZ; }
    @Override
    public void setShouldCheckLevelZ(boolean check) { mqt$checkLevelZ = check; }
    @Override
    public ITaskLevelZExtension.LevelZMode getLevelZMode() { return mqt$levelZMode; }
    @Override
    public void setLevelZMode(ITaskLevelZExtension.LevelZMode mode) { mqt$levelZMode = mode; }
    @Override
    public ComparisonMode getLevelZComparison() { return mqt$levelZComparison; }
    @Override
    public void setLevelZComparison(ComparisonMode mode) { mqt$levelZComparison = mode; }
    @Override
    public int getLevelZFirstNumber() { return mqt$levelZFirst; }
    @Override
    public void setLevelZFirstNumber(int value) { mqt$levelZFirst = value; }
    @Override
    public int getLevelZSecondNumber() { return mqt$levelZSecond; }
    @Override
    public void setLevelZSecondNumber(int value) { mqt$levelZSecond = value; }
    @Override
    public int getLevelZSkillId() { return mqt$levelZSkillId; }
    @Override
    public void setLevelZSkillId(int skillId) { mqt$levelZSkillId = skillId; }
    @Override
    public boolean shouldCheckReskillable() { return mqt$checkReskillable; }
    @Override
    public void setShouldCheckReskillable(boolean check) { mqt$checkReskillable = check; }
    @Override
    public ITaskReskillableExtension.ReskillableMode getReskillableMode() { return mqt$reskillableMode; }
    @Override
    public void setReskillableMode(ITaskReskillableExtension.ReskillableMode mode) { mqt$reskillableMode = mode; }
    @Override
    public ComparisonMode getReskillableComparison() { return mqt$reskillableComparison; }
    @Override
    public void setReskillableComparison(ComparisonMode mode) { mqt$reskillableComparison = mode; }
    @Override
    public int getReskillableFirstNumber() { return mqt$reskillableFirst; }
    @Override
    public void setReskillableFirstNumber(int value) { mqt$reskillableFirst = value; }
    @Override
    public int getReskillableSecondNumber() { return mqt$reskillableSecond; }
    @Override
    public void setReskillableSecondNumber(int value) { mqt$reskillableSecond = value; }
    @Override
    public int getReskillableSkillIndex() { return mqt$reskillableSkillIndex; }
    @Override
    public void setReskillableSkillIndex(int skillIndex) { mqt$reskillableSkillIndex = skillIndex; }

    @Unique
    private boolean mqt$shouldSerializePlayerLeveling() {
        Task self = (Task) (Object) this;
        return self != null
                && !(self instanceof SkillsLevelTask)
                && !(self instanceof LevelZTask)
                && !(self instanceof ReskillableTask);
    }
    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void mqt$writePlayerLevelingData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (! mqt$shouldSerializePlayerLeveling()) return;

        // Skills
        if (SkillsCompat.isLoaded()) {
            CompoundTag skillsTag = new CompoundTag();
            skillsTag.putBoolean("check", mqt$checkSkills);
            skillsTag.putString("mode", mqt$skillsMode.name());
            skillsTag.putString("comparison", mqt$skillsComparison.name());
            skillsTag.putInt("first", mqt$skillsFirst);
            skillsTag.putInt("second", mqt$skillsSecond);
            skillsTag.putString("category", mqt$skillsCategoryId);
            nbt.put("SkillsLevel", skillsTag);
        }

        if (LevelZCompat.isLoaded()) {
            CompoundTag levelZTag = new CompoundTag();
            levelZTag.putBoolean("check", mqt$checkLevelZ);
            levelZTag.putString("mode", mqt$levelZMode.name());
            levelZTag.putString("comparison", mqt$levelZComparison.name());
            levelZTag.putInt("first", mqt$levelZFirst);
            levelZTag.putInt("second", mqt$levelZSecond);
            levelZTag.putInt("skill_id", mqt$levelZSkillId);
            nbt.put("LevelZ", levelZTag);
        }

        if (ReskillableCompat.isLoaded()) {
            CompoundTag reskillableTag = new CompoundTag();
            reskillableTag.putBoolean("check", mqt$checkReskillable);
            reskillableTag.putString("mode", mqt$reskillableMode.name());
            reskillableTag.putString("comparison", mqt$reskillableComparison.name());
            reskillableTag.putInt("first", mqt$reskillableFirst);
            reskillableTag.putInt("second", mqt$reskillableSecond);
            reskillableTag.putInt("skill_index", mqt$reskillableSkillIndex);
            nbt.put("Reskillable", reskillableTag);
        }
    }

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void mqt$readPlayerLevelingData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldSerializePlayerLeveling()) return;

        if (SkillsCompat.isLoaded() && nbt.contains("SkillsLevel")) {
            CompoundTag tag = nbt.getCompound("SkillsLevel");
            mqt$checkSkills = tag.getBoolean("check");
            try { mqt$skillsMode = ITaskSkillsExtension.SkillsMode.valueOf(tag.getString("mode")); }
            catch (Exception e) { mqt$skillsMode = ITaskSkillsExtension.SkillsMode.TOTAL_LEVEL; }
            try { mqt$skillsComparison = ComparisonMode.valueOf(tag.getString("comparison")); }
            catch (Exception e) { mqt$skillsComparison = ComparisonMode.GREATER_OR_EQUAL; }
            mqt$skillsFirst = tag.getInt("first");
            mqt$skillsSecond = tag.getInt("second");
            mqt$skillsCategoryId = tag.getString("category");
        }

        if (LevelZCompat.isLoaded() && nbt.contains("LevelZ")) {
            CompoundTag tag = nbt.getCompound("LevelZ");
            mqt$checkLevelZ = tag.getBoolean("check");
            try { mqt$levelZMode = ITaskLevelZExtension.LevelZMode.valueOf(tag.getString("mode")); }
            catch (Exception e) { mqt$levelZMode = ITaskLevelZExtension.LevelZMode.LEVEL; }
            try { mqt$levelZComparison = ComparisonMode.valueOf(tag.getString("comparison")); }
            catch (Exception e) { mqt$levelZComparison = ComparisonMode.GREATER_OR_EQUAL; }
            mqt$levelZFirst = tag.getInt("first");
            mqt$levelZSecond = tag.getInt("second");
            mqt$levelZSkillId = tag.getInt("skill_id");
        }

        if (ReskillableCompat.isLoaded() && nbt.contains("Reskillable")) {
            CompoundTag tag = nbt.getCompound("Reskillable");
            mqt$checkReskillable = tag.getBoolean("check");
            try { mqt$reskillableMode = ITaskReskillableExtension.ReskillableMode.valueOf(tag.getString("mode")); }
            catch (Exception e) { mqt$reskillableMode = ITaskReskillableExtension.ReskillableMode.SKILL_LEVEL; }
            try { mqt$reskillableComparison = ComparisonMode.valueOf(tag.getString("comparison")); }
            catch (Exception e) { mqt$reskillableComparison = ComparisonMode.GREATER_OR_EQUAL; }
            mqt$reskillableFirst = tag.getInt("first");
            mqt$reskillableSecond = tag.getInt("second");
            mqt$reskillableSkillIndex = tag.getInt("skill_index");
        }
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void mqt$writeNetPlayerLevelingData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldSerializePlayerLeveling()) {
            buffer.writeBoolean(false);
            buffer.writeBoolean(false);
            buffer.writeBoolean(false);
            return;
        }

        boolean hasSkills = SkillsCompat.isLoaded();
        buffer.writeBoolean(hasSkills);
        if (hasSkills) {
            buffer.writeBoolean(mqt$checkSkills);
            buffer.writeEnum(mqt$skillsMode);
            buffer.writeEnum(mqt$skillsComparison);
            buffer.writeVarInt(mqt$skillsFirst);
            buffer.writeVarInt(mqt$skillsSecond);
            buffer.writeUtf(mqt$skillsCategoryId);
        }

        boolean hasLevelZ = LevelZCompat.isLoaded();
        buffer.writeBoolean(hasLevelZ);
        if (hasLevelZ) {
            buffer.writeBoolean(mqt$checkLevelZ);
            buffer.writeEnum(mqt$levelZMode);
            buffer.writeEnum(mqt$levelZComparison);
            buffer.writeVarInt(mqt$levelZFirst);
            buffer.writeVarInt(mqt$levelZSecond);
            buffer.writeVarInt(mqt$levelZSkillId);
        }

        boolean hasReskillable = ReskillableCompat.isLoaded();
        buffer.writeBoolean(hasReskillable);
        if (hasReskillable) {
            buffer.writeBoolean(mqt$checkReskillable);
            buffer.writeEnum(mqt$reskillableMode);
            buffer.writeEnum(mqt$reskillableComparison);
            buffer.writeVarInt(mqt$reskillableFirst);
            buffer.writeVarInt(mqt$reskillableSecond);
            buffer.writeVarInt(mqt$reskillableSkillIndex);
        }
    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void mqt$readNetPlayerLevelingData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldSerializePlayerLeveling()) {
            buffer.readBoolean();
            buffer.readBoolean();
            buffer.readBoolean();
            return;
        }

        boolean hasSkills = buffer.readBoolean();
        if (hasSkills) {
            mqt$checkSkills = buffer.readBoolean();
            mqt$skillsMode = buffer.readEnum(ITaskSkillsExtension.SkillsMode.class);
            mqt$skillsComparison = buffer.readEnum(ComparisonMode.class);
            mqt$skillsFirst = buffer.readVarInt();
            mqt$skillsSecond = buffer.readVarInt();
            mqt$skillsCategoryId = buffer.readUtf();
        }

        boolean hasLevelZ = buffer.readBoolean();
        if (hasLevelZ) {
            mqt$checkLevelZ = buffer.readBoolean();
            mqt$levelZMode = buffer.readEnum(ITaskLevelZExtension.LevelZMode.class);
            mqt$levelZComparison = buffer.readEnum(ComparisonMode.class);
            mqt$levelZFirst = buffer.readVarInt();
            mqt$levelZSecond = buffer.readVarInt();
            mqt$levelZSkillId = buffer.readVarInt();
        }

        boolean hasReskillable = buffer.readBoolean();
        if (hasReskillable) {
            mqt$checkReskillable = buffer.readBoolean();
            mqt$reskillableMode = buffer.readEnum(ITaskReskillableExtension.ReskillableMode.class);
            mqt$reskillableComparison = buffer.readEnum(ComparisonMode.class);
            mqt$reskillableFirst = buffer.readVarInt();
            mqt$reskillableSecond = buffer.readVarInt();
            mqt$reskillableSkillIndex = buffer.readVarInt();
        }
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void mqt$addPlayerLevelingConfig(ConfigGroup config, CallbackInfo ci) {
        if (!mqt$shouldSerializePlayerLeveling()) return;

        var COMPARISON_MAP = NameMap.of(ComparisonMode.GREATER_OR_EQUAL, ComparisonMode.values())
                .name(mode -> Component.translatable(mode.getTranslationKey()))
                .create();

        if (SkillsCompat.isLoaded()) {
            ConfigGroup skillsGroup = config.getOrCreateSubgroup("skills_level");
            skillsGroup.setNameKey("morequesttypes.config.group.skills_level");

            skillsGroup.addBool("check", mqt$checkSkills, v -> mqt$checkSkills = v, false)
                    .setNameKey("morequesttypes.task.skills_level.check");

            var SKILLS_MODE_MAP = NameMap.of(ITaskSkillsExtension.SkillsMode.TOTAL_LEVEL,
                    ITaskSkillsExtension.SkillsMode.values()).create();
            skillsGroup.addEnum("mode", mqt$skillsMode, v -> mqt$skillsMode = v, SKILLS_MODE_MAP)
                    .setNameKey("morequesttypes.task.skills_level.mode");

            skillsGroup.addEnum("comparison", mqt$skillsComparison, v -> {
                mqt$skillsComparison = v;
                if (v.isRange() && mqt$skillsSecond <= mqt$skillsFirst) {
                    mqt$skillsSecond = mqt$skillsFirst + 10;
                }
            }, COMPARISON_MAP).setNameKey("morequesttypes.task.comparison_mode");

            skillsGroup.addInt("first_number", mqt$skillsFirst, v -> {
                mqt$skillsFirst = Math.max(0, v);
                if (mqt$skillsComparison.isRange() && mqt$skillsSecond <= mqt$skillsFirst) {
                    mqt$skillsSecond = mqt$skillsFirst + 10;
                }
            }, 10, 0, 100000).setNameKey("morequesttypes.task.first_number");

            skillsGroup.addInt("second_number", mqt$skillsSecond, v -> {
                if (mqt$skillsComparison.isRange() && v <= mqt$skillsFirst) {
                    mqt$skillsSecond = mqt$skillsFirst + 10;
                } else {
                    mqt$skillsSecond = Math.max(0, v);
                }
            }, 20, 0, 100000).setNameKey("morequesttypes.task.second_number");

            final var NONE = ResourceLocation.withDefaultNamespace("none");
            ArrayList<ResourceLocation> cats = new ArrayList<>();
            cats.addAll(SkillsCompat.getCategories(true));
            cats.add(0, NONE);

            ResourceLocation current = ResourceLocation.tryParse(mqt$skillsCategoryId);
            if (current == null || current.equals(NONE)) {
                current = cats.size() > 1 ? cats.get(1) :  NONE;
            }

            var CAT_MAP = NameMap.of(current, cats.toArray(ResourceLocation[]::new))
                    .name(rl -> rl.equals(NONE) ? Component.literal("None") : Component.literal(rl.toString()))
                    .create();

            skillsGroup.addEnum("category", current, rl -> {
                mqt$skillsCategoryId = rl.equals(NONE) ? "" : rl.toString();
            }, CAT_MAP).setNameKey("morequesttypes.task.skills_level.category");
        }

        if (LevelZCompat.isLoaded()) {
            ConfigGroup levelZGroup = config.getOrCreateSubgroup("levelz");
            levelZGroup.setNameKey("morequesttypes.config.group.levelz");

            levelZGroup.addBool("check", mqt$checkLevelZ, v -> mqt$checkLevelZ = v, false)
                    .setNameKey("morequesttypes.task.levelz.check");

            var LEVELZ_MODE_MAP = NameMap.of(ITaskLevelZExtension.LevelZMode.LEVEL,
                    ITaskLevelZExtension.LevelZMode.values()).create();
            levelZGroup.addEnum("mode", mqt$levelZMode, v -> mqt$levelZMode = v, LEVELZ_MODE_MAP)
                    .setNameKey("morequesttypes.task.levelz.mode");

            levelZGroup.addEnum("comparison", mqt$levelZComparison, v -> {
                mqt$levelZComparison = v;
                if (v.isRange() && mqt$levelZSecond <= mqt$levelZFirst) {
                    mqt$levelZSecond = mqt$levelZFirst + 10;
                }
            }, COMPARISON_MAP).setNameKey("morequesttypes.task.comparison_mode");

            levelZGroup.addInt("first_number", mqt$levelZFirst, v -> {
                mqt$levelZFirst = Math.max(0, v);
                if (mqt$levelZComparison.isRange() && mqt$levelZSecond <= mqt$levelZFirst) {
                    mqt$levelZSecond = mqt$levelZFirst + 10;
                }
            }, 10, 0, 100000).setNameKey("morequesttypes.task.first_number");

            levelZGroup.addInt("second_number", mqt$levelZSecond, v -> {
                if (mqt$levelZComparison.isRange() && v <= mqt$levelZFirst) {
                    mqt$levelZSecond = mqt$levelZFirst + 10;
                } else {
                    mqt$levelZSecond = Math.max(0, v);
                }
            }, 20, 0, 100000).setNameKey("morequesttypes.task.second_number");

            Map<Integer, String> skills = new LinkedHashMap<>();
            skills.put(-1, "player_level");
            skills.putAll(LevelZCompat.getAvailableSkills());

            List<Integer> skillIds = new ArrayList<>(skills.keySet());
            Integer currentSkillId = skillIds.contains(mqt$levelZSkillId) ? mqt$levelZSkillId : -1;

            var SKILL_MAP = NameMap.of(currentSkillId, skillIds.toArray(Integer[]::new))
                    .name(id -> {
                        if (id == -1) return Component.literal("Player Level");
                        return Component.literal(skills.getOrDefault(id, "Unknown"));
                    })
                    .create();

            levelZGroup.addEnum("skill", currentSkillId, v -> mqt$levelZSkillId = v, SKILL_MAP)
                    .setNameKey("morequesttypes.task.levelz.skill");
        }

        if (ReskillableCompat.isLoaded()) {
            ConfigGroup reskillableGroup = config.getOrCreateSubgroup("reskillable");
            reskillableGroup.setNameKey("morequesttypes.config.group.reskillable");

            reskillableGroup.addBool("check", mqt$checkReskillable, v -> mqt$checkReskillable = v, false)
                    .setNameKey("morequesttypes.task.reskillable.check");

            var RESK_MODE_MAP = NameMap.of(ITaskReskillableExtension.ReskillableMode.SKILL_LEVEL,
                    ITaskReskillableExtension.ReskillableMode.values()).create();
            reskillableGroup.addEnum("mode", mqt$reskillableMode, v -> mqt$reskillableMode = v, RESK_MODE_MAP)
                    .setNameKey("morequesttypes.task.reskillable.mode");

            reskillableGroup.addEnum("comparison", mqt$reskillableComparison, v -> {
                mqt$reskillableComparison = v;
                if (v.isRange() && mqt$reskillableSecond <= mqt$reskillableFirst) {
                    mqt$reskillableSecond = mqt$reskillableFirst + 10;
                }
            }, COMPARISON_MAP).setNameKey("morequesttypes.task.comparison_mode");

            reskillableGroup.addInt("first_number", mqt$reskillableFirst, v -> {
                mqt$reskillableFirst = Math.max(0, v);
                if (mqt$reskillableComparison.isRange() && mqt$reskillableSecond <= mqt$reskillableFirst) {
                    mqt$reskillableSecond = mqt$reskillableFirst + 10;
                }
            }, 10, 0, 100000).setNameKey("morequesttypes.task.first_number");

            reskillableGroup.addInt("second_number", mqt$reskillableSecond, v -> {
                if (mqt$reskillableComparison.isRange() && v <= mqt$reskillableFirst) {
                    mqt$reskillableSecond = mqt$reskillableFirst + 10;
                } else {
                    mqt$reskillableSecond = Math.max(0, v);
                }
            }, 20, 0, 100000).setNameKey("morequesttypes.task.second_number");

            Map<Integer, String> skills = new LinkedHashMap<>();
            skills.putAll(ReskillableCompat.getAvailableSkills());
            if (skills.isEmpty()) skills.put(0, "No Skills Available");

            List<Integer> skillIds = new ArrayList<>(skills.keySet());
            Integer currentSkillIndex = skillIds.contains(mqt$reskillableSkillIndex) ?
                    mqt$reskillableSkillIndex : skillIds.get(0);

            var SKILL_MAP = NameMap.of(currentSkillIndex, skillIds.toArray(Integer[]::new))
                    .name(id -> Component.literal(skills.getOrDefault(id, "Unknown")))
                    .create();

            reskillableGroup.addEnum("skill", currentSkillIndex, v -> mqt$reskillableSkillIndex = v, SKILL_MAP)
                    .setNameKey("morequesttypes.task.reskillable.skill");
        }
    }
}