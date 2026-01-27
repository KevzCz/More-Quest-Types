package net.pixeldreamstudios.morequesttypes.rewards;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Objects;

public final class PotionReward extends Reward {
    private ResourceLocation effectId = ResourceLocation.withDefaultNamespace("regeneration");
    private int amplifier = 0;
    private int durationTicks = 200;
    private boolean showParticles = true;

    public PotionReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.POTION;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        var reg = player.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
        var key = ResourceKey.create(Registries.MOB_EFFECT, effectId);
        var holderOpt = reg.getHolder(key);
        if (holderOpt.isEmpty()) return;

        player.addEffect(new MobEffectInstance(holderOpt.get(), durationTicks, amplifier, false, showParticles));
    }

    @Environment(EnvType.CLIENT)
    private Component getEffectName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && effectId != null) {
                var reg = mc.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
                var key = ResourceKey.create(Registries.MOB_EFFECT, effectId);
                var holder = reg.getHolder(key);
                if (holder.isPresent() && holder.get().value() != null) {
                    return holder.get().value().getDisplayName();
                }
            }
        } catch (Throwable ignored) {}
        return Component.literal(effectId != null ? effectId.toString() : "?");
    }

    @Environment(EnvType.CLIENT)
    private static class MobEffectSpriteIcon extends ImageIcon {
        private final TextureAtlasSprite sprite;

        public MobEffectSpriteIcon(TextureAtlasSprite sprite) {
            super(sprite.atlasLocation());
            this.sprite = sprite;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int w, int h) {
            RenderSystem.setShaderTexture(0, sprite.atlasLocation());
            graphics.blit(x, y, 0, w, h, sprite);
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        try {
            if (effectId != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    var reg = mc.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
                    var key = ResourceKey.create(Registries.MOB_EFFECT, effectId);
                    var holder = reg.getHolder(key);
                    if (holder.isPresent()) {
                        try {
                            var sprite = mc.getMobEffectTextures().get(holder.get());
                            if (sprite != null) {
                                return new MobEffectSpriteIcon(sprite);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}

        return super.getAltIcon();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        Component effectName = getEffectName();

        if (amplifier > 0) {
            return Component.translatable(
                    "morequesttypes.reward.potion.title_with_amp",
                    effectName,
                    amplifier + 1
            );
        } else {
            return effectName.copy();
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var keys = BuiltInRegistries.MOB_EFFECT.keySet().stream()
                .map(Objects::toString)
                .sorted()
                .toList();

        var EFFECTS = NameMap.of(effectId.toString(), keys.toArray(String[]::new))
                .name(Component::literal)
                .create();

        config.addEnum("effect", effectId.toString(), s -> {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) effectId = rl;
        }, EFFECTS).setNameKey("morequesttypes.reward.potion.effect");

        config.addInt("amplifier", amplifier, v -> amplifier = Math.max(0, v), 0, 0, 255)
                .setNameKey("morequesttypes.reward.potion.amplifier");

        config.addInt("duration_ticks", durationTicks, v -> durationTicks = Math.max(1, v), 1, 1, 12000)
                .setNameKey("morequesttypes.reward.potion.duration_ticks");

        config.addBool("show_particles", showParticles, v -> showParticles = v, true)
                .setNameKey("morequesttypes.reward.potion.show_particles");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (effectId != null) nbt.putString("effect", effectId.toString());
        nbt.putInt("amplifier", amplifier);
        nbt.putInt("duration_ticks", durationTicks);
        nbt.putBoolean("show_particles", showParticles);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        effectId = ResourceLocation.tryParse(nbt.getString("effect"));
        amplifier = nbt.contains("amplifier") ? nbt.getInt("amplifier") : 0;
        durationTicks = nbt.contains("duration_ticks") ? nbt.getInt("duration_ticks") : 200;
        showParticles = !nbt.contains("show_particles") || nbt.getBoolean("show_particles");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(effectId == null ? "" : effectId.toString());
        buf.writeInt(amplifier);
        buf.writeInt(durationTicks);
        buf.writeBoolean(showParticles);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        String s = buf.readUtf();
        effectId = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        amplifier = buf.readInt();
        durationTicks = buf.readInt();
        showParticles = buf.readBoolean();
    }

}