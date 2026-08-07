package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.pixeldreamstudios.morequesttypes.api.IQuestFileExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseQuestFile.class, remap = false)
public abstract class QuestFileExtensions implements IQuestFileExtension {
    @Unique
    private boolean mqt$autoRefreshTaskCache = true;

    @Override
    public boolean mqt$isAutoRefreshTaskCache() {
        return mqt$autoRefreshTaskCache;
    }

    @Override
    public void mqt$setAutoRefreshTaskCache(boolean value) {
        mqt$autoRefreshTaskCache = value;
    }

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void mqt$writeCustomData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$autoRefreshTaskCache) {
            nbt.putBoolean("mqt_auto_refresh_task_cache", false);
        }
    }

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void mqt$readCustomData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        mqt$autoRefreshTaskCache = !nbt.contains("mqt_auto_refresh_task_cache")
                || nbt.getBoolean("mqt_auto_refresh_task_cache");
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void mqt$writeCustomNetData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeBoolean(mqt$autoRefreshTaskCache);
    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void mqt$readCustomNetData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        mqt$autoRefreshTaskCache = buffer.readBoolean();
    }

    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void mqt$fillConfigGroup(ConfigGroup config, CallbackInfo ci) {
        config.addBool("mqt_auto_refresh_task_cache", mqt$autoRefreshTaskCache,
                        v -> mqt$autoRefreshTaskCache = v, true)
                .setNameKey("morequesttypes.file.auto_refresh_task_cache");
    }
}
