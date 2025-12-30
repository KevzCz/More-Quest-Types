package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.OriginsCompat;

import java.util.ArrayList;
import java.util.List;

public class OriginTask extends Task {
    private ResourceLocation originLayer = ResourceLocation.withDefaultNamespace("empty");
    private ResourceLocation origin = ResourceLocation.withDefaultNamespace("empty");

    public OriginTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.ORIGIN;
    }

    @Override
    public long getMaxProgress() {
        return 1L;
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("origin_layer", originLayer.toString());
        nbt.putString("origin", origin.toString());
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        originLayer = ResourceLocation.tryParse(nbt.getString("origin_layer"));
        origin = ResourceLocation.tryParse(nbt.getString("origin"));

        if (originLayer == null) originLayer = ResourceLocation.withDefaultNamespace("empty");
        if (origin == null) origin = ResourceLocation.withDefaultNamespace("empty");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(originLayer);
        buffer.writeResourceLocation(origin);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        originLayer = buffer.readResourceLocation();
        origin = buffer.readResourceLocation();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        List<ResourceLocation> layers = new ArrayList<>();
        layers.add(ResourceLocation.withDefaultNamespace("empty"));
        layers.addAll(OriginsCompat.getAvailableLayers());

        var LAYER_MAP = NameMap.of(originLayer, layers.toArray(ResourceLocation[]::new))
                .name(layer -> {
                    if (layer.equals(ResourceLocation.withDefaultNamespace("empty"))) {
                        return Component.literal("None");
                    }
                    return Component.literal(layer.toString());
                })
                .create();

        config.addEnum("origin_layer", originLayer, v -> {
            originLayer = v;
        }, LAYER_MAP).setNameKey("morequesttypes.task.origins.layer");

        List<ResourceLocation> origins = new ArrayList<>();
        origins.add(ResourceLocation.withDefaultNamespace("empty"));
        if (!originLayer.equals(ResourceLocation.withDefaultNamespace("empty"))) {
            origins.addAll(OriginsCompat.getOriginsForLayer(originLayer));
        }

        var ORIGIN_MAP = NameMap.of(origin, origins.toArray(ResourceLocation[]::new))
                .name(o -> {
                    if (o.equals(ResourceLocation.withDefaultNamespace("empty"))) {
                        return Component.literal("None");
                    }
                    return Component.literal(o.toString());
                })
                .create();

        config.addEnum("origin", origin, v -> {
            origin = v;
        }, ORIGIN_MAP).setNameKey("morequesttypes.task.origins.origin");
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (! checkTaskSequence(teamData)) return;
        if (! OriginsCompat.isLoaded()) return;

        if (originLayer.equals(ResourceLocation.withDefaultNamespace("empty")) ||
                origin.equals(ResourceLocation.withDefaultNamespace("empty"))) {
            return;
        }

        if (OriginsCompat.hasOrigin(player, originLayer, origin)) {
            teamData.setProgress(this, 1L);
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        return Component.translatable("morequesttypes.task.origins.title",
                originLayer.toString(), origin.toString());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon(ResourceLocation.fromNamespaceAndPath("origins", "icon.png"));
    }
}