package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.pixeldreamstudios.morequesttypes.api.IRewardDynamicDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.compat.DynamicDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.rewards.SummonReward;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Reward.class, remap = false)
public abstract class RewardExtensions implements IRewardDynamicDifficultyExtension {

    @Unique
    private boolean mqt$setMobLevel = false;
    @Unique
    private int mqt$mobLevel = 1;
    @Override
    public boolean shouldSetMobLevel() {
        return mqt$setMobLevel;
    }

    @Override
    public void setShouldSetMobLevel(boolean set) {
        this.mqt$setMobLevel = set;
    }

    @Override
    public int getMobLevel() {
        return mqt$mobLevel;
    }

    @Override
    public void setMobLevel(int level) {
        this.mqt$mobLevel = level;
    }

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void mqt$writeDifficultyData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

        if (DynamicDifficultyCompat.isLoaded()) {
            CompoundTag ddTag = new CompoundTag();
            ddTag.putBoolean("set_level", mqt$setMobLevel);
            ddTag.putInt("level", mqt$mobLevel);
            nbt.put("DynamicDifficulty", ddTag);
        }

    }

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void mqt$readDifficultyData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

        if (DynamicDifficultyCompat.isLoaded() && nbt.contains("DynamicDifficulty")) {
            CompoundTag ddTag = nbt.getCompound("DynamicDifficulty");
            mqt$setMobLevel = ddTag.getBoolean("set_level");
            mqt$mobLevel = ddTag.getInt("level");
        }
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void mqt$writeNetDifficultyData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

        boolean hasDynamic = DynamicDifficultyCompat.isLoaded();
        buffer.writeBoolean(hasDynamic);
        if (hasDynamic) {
            buffer.writeBoolean(mqt$setMobLevel);
            buffer.writeVarInt(mqt$mobLevel);
        }

    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void mqt$readNetDifficultyData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

        boolean hasDynamic = buffer.readBoolean();
        if (hasDynamic) {
            mqt$setMobLevel = buffer.readBoolean();
            mqt$mobLevel = buffer.readVarInt();
        }
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void mqt$addDifficultyConfig(ConfigGroup config, CallbackInfo ci) {
        if (!mqt$shouldSerialize()) return;

        if (DynamicDifficultyCompat.isLoaded()) {
            ConfigGroup ddGroup = config.getOrCreateSubgroup("dynamic_difficulty");
            ddGroup.setNameKey("morequesttypes.config.group.dynamic_difficulty");

            ddGroup.addBool("set_mob_level", mqt$setMobLevel,
                            v -> mqt$setMobLevel = v, false)
                    .setNameKey("morequesttypes.reward.summon.set_mob_level");

            ddGroup.addInt("mob_level", mqt$mobLevel,
                            v -> mqt$mobLevel = Math.max(1, v), 1, 1, 1000)
                    .setNameKey("morequesttypes.reward.summon.mob_level");
        }

    }

    @Unique
    private boolean mqt$shouldSerialize() {
        Reward self = (Reward) (Object) this;
        return self instanceof SummonReward;
    }
}