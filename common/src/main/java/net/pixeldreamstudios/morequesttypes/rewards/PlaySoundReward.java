package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

public final class PlaySoundReward extends Reward {
    private static final List<String> KNOWN_SOUNDS = new ArrayList<>();

    public static void syncKnownSoundList(List<String> data) {
        KNOWN_SOUNDS.clear();
        KNOWN_SOUNDS.addAll(data);
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
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (KNOWN_SOUNDS.isEmpty()) {
            dev.architectury.networking.NetworkManager
                    .sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTSoundsRequest());
        }

        var choices = new ArrayList<String>();
        if (KNOWN_SOUNDS.isEmpty()) {
            choices.add("minecraft:entity.player.levelup");
            choices.add("minecraft:ui.button.click");
            choices.add("minecraft:block.note_block.harp");
        } else {
            choices.addAll(KNOWN_SOUNDS);
        }

        var SOUNDS = NameMap.of(soundId, choices)
                .name(s -> Component.literal((s == null || s.isEmpty()) ? "?" : s))
                .create();

        config.addEnum("sound", soundId, v -> soundId = v, SOUNDS)
                .setNameKey("ftbquests.reward.play_sound.sound");

        var CATS = NameMap.of(SoundSource.PLAYERS, SoundSource.values()).create();
        config.addEnum("category", category, v -> category = v, CATS)
                .setNameKey("ftbquests.reward.play_sound.category");

        config.addDouble("volume", volume, v -> volume = (float) Math.max(0.0, v), 1.0, 0.0, 10.0)
                .setNameKey("ftbquests.reward.play_sound.volume");
        config.addDouble("pitch",  pitch,  v -> pitch  = (float) Math.max(0.1, v), 1.0, 0.1, 3.0)
                .setNameKey("ftbquests.reward.play_sound.pitch");
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
