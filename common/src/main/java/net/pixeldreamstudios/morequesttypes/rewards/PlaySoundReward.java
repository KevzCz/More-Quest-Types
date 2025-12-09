package net.pixeldreamstudios.morequesttypes.rewards;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class PlaySoundReward extends Reward {
    private static final List<String> SERVER_SOUNDS = new ArrayList<>();
    private static long lastSyncTime = 0;
    private static final long SYNC_COOLDOWN_MS = 500;

    public static void syncKnownSoundList(List<String> data) {
        SERVER_SOUNDS.clear();
        SERVER_SOUNDS.addAll(data);
        lastSyncTime = System.currentTimeMillis();
    }

    private String soundId = "minecraft:entity.player.levelup";
    private SoundSource category = SoundSource.PLAYERS;
    private float volume = 1.0f;
    private float pitch  = 1.0f;

    public PlaySoundReward(long id, dev.ftb.mods.ftbquests.quest.Quest q) { super(id, q); }
    @Override public RewardType getType() { return MoreRewardTypes.PLAY_SOUND; }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        ResourceLocation rl = ResourceLocation.tryParse(soundId);
        if (rl == null) return;

        Holder.Reference<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.getHolder(rl).orElse(null);
        if (holder != null) {
            player.playNotifySound(holder.value(), category,
                    Math.max(0f, volume), Math.max(0.1f, pitch));
            return;
        }

        SoundEvent ev = BuiltInRegistries.SOUND_EVENT.get(rl);
        if (ev != null) {
            player.playNotifySound(ev, category,
                    Math.max(0f, volume), Math.max(0.1f, pitch));
            return;
        }

        try {
            var soundPacket = new ClientboundSoundPacket(
                    Holder.direct(SoundEvent.createVariableRangeEvent(rl)),
                    category,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    Math.max(0f, volume),
                    Math.max(0.1f, pitch),
                    player.getRandom().nextLong()
            );
            player.connection.send(soundPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Environment(EnvType.CLIENT)
    private static List<String> collectClientSounds() {
        var soundSet = new LinkedHashSet<String>();

        BuiltInRegistries.SOUND_EVENT.keySet().stream()
                .map(ResourceLocation::toString)
                .forEach(soundSet::add);

        try {
            var minecraft = Minecraft.getInstance();
            var resourceManager = minecraft.getResourceManager();

            var namespaces = resourceManager.getNamespaces();

            for (String namespace : namespaces) {
                ResourceLocation soundsJsonLocation = ResourceLocation.fromNamespaceAndPath(namespace, "sounds.json");

                try {
                    var resources = resourceManager.getResourceStack(soundsJsonLocation);
                    for (Resource resource : resources) {
                        try (var reader = new InputStreamReader(resource.open())) {
                            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                            for (String soundEvent : json.keySet()) {
                                soundSet.add(namespace + ":" + soundEvent);
                            }
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(soundSet);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (SERVER_SOUNDS.isEmpty() || (System.currentTimeMillis() - lastSyncTime) > SYNC_COOLDOWN_MS) {
            dev.architectury.networking.NetworkManager
                    .sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTSoundsRequest());
        }

        var choices = collectClientSounds();

        for (String serverSound : SERVER_SOUNDS) {
            if (!choices.contains(serverSound)) {
                choices.add(serverSound);
            }
        }

        if (choices.isEmpty()) {
            choices.add("minecraft:entity.player.levelup");
            choices.add("minecraft:ui.button.click");
            choices.add("minecraft:block.note_block.harp");
        }

        if (! soundId.isEmpty() && !choices.contains(soundId)) {
            choices.add(soundId);
        }

        choices.sort(String::compareTo);

        var SOUNDS = NameMap.of(soundId, choices)
                .name(s -> Component.literal((s == null || s.isEmpty()) ? "?" : s))
                .create();

        config.addEnum("sound", soundId, v -> soundId = v, SOUNDS)
                .setNameKey("morequesttypes.reward.play_sound.sound");

        var CATS = NameMap.of(SoundSource.PLAYERS, SoundSource.values()).create();
        config.addEnum("category", category, v -> category = v, CATS)
                .setNameKey("morequesttypes.reward.play_sound.category");

        config.addDouble("volume", volume, v -> volume = (float) Math.max(0.0, v), 1.0, 0.0, 10.0)
                .setNameKey("morequesttypes.reward.play_sound.volume");
        config.addDouble("pitch",  pitch,  v -> pitch  = (float) Math.max(0.1, v), 1.0, 0.1, 3.0)
                .setNameKey("morequesttypes.reward.play_sound.pitch");
    }

    @Override
    public void writeData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("sound", soundId);
        nbt.putString("category", category.getName());
        nbt.putFloat("volume", volume);
        nbt.putFloat("pitch", pitch);
    }

    @Override
    public void readData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        soundId = nbt.getString("sound");
        try {
            category = SoundSource.valueOf(nbt.getString("category").toUpperCase(java.util.Locale.ROOT));
        } catch (Throwable ignored) {
            category = SoundSource.PLAYERS;
        }
        volume = nbt.contains("volume") ? nbt.getFloat("volume") : 1.0f;
        pitch  = nbt.contains("pitch")  ? nbt.getFloat("pitch")  : 1.0f;
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(soundId);
        buffer.writeEnum(category);
        buffer.writeFloat(volume);
        buffer.writeFloat(pitch);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        soundId  = buffer.readUtf();
        category = buffer.readEnum(SoundSource.class);
        volume   = buffer.readFloat();
        pitch    = buffer.readFloat();
    }

    @Override public boolean getExcludeFromClaimAll() { return false; }
}