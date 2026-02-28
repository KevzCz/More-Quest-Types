package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.blaze3d.systems.*;
import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.icon.*;
import dev.ftb.mods.ftblibrary.util.*;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.*;
import net.fabricmc.api.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;

import java.util.*;

public final class PotionEffectTask extends Task {
    private ResourceLocation effectId = ResourceLocation.withDefaultNamespace("regeneration");
    private int amplifier = -1;

    public PotionEffectTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.POTION_EFFECT;
    }

    @Override
    public long getMaxProgress() {
        return 1L;
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
        if (!checkTaskSequence(teamData)) return;

        Holder<MobEffect> holder = PotionEffectTask.resolveEffectHolder(player.registryAccess(), effectId);
        if (holder == null) return;

        Collection<ServerPlayer> online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;

        for (ServerPlayer sp : online) {
            MobEffectInstance instance = sp.getEffect(holder);
            if (instance != null) {
                if (amplifier < 0 || instance.getAmplifier() == amplifier) {
                    teamData.setProgress(this, 1L);
                    return;
                }
            }
        }
    }

    private static Holder<MobEffect> resolveEffectHolder(RegistryAccess access, ResourceLocation rl) {
        if (rl == null) return null;
        var reg = access.registryOrThrow(Registries.MOB_EFFECT);
        var key = ResourceKey.create(Registries.MOB_EFFECT, rl);
        return reg.getHolder(key).orElse(null);
    }

    @Environment(EnvType.CLIENT)
    private Component getEffectName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && effectId != null) {
                Holder<MobEffect> holder = PotionEffectTask.resolveEffectHolder(mc.level.registryAccess(), effectId);
                if (holder != null && holder.value() != null) {
                    return holder.value().getDisplayName();
                }
            }
        } catch (Throwable ignored) {
        }
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
                    Holder<MobEffect> holder = PotionEffectTask.resolveEffectHolder(mc.level.registryAccess(), effectId);
                    if (holder != null) {
                        try {
                            var sprite = mc.getMobEffectTextures().get(holder);
                            if (sprite != null) {
                                return new MobEffectSpriteIcon(sprite);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return super.getAltIcon();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        Component effectName = getEffectName();

        if (amplifier < 0) {
            return effectName.copy();
        } else {
            return Component.translatable(
                    "morequesttypes.task.potion_effect.title_with_amp",
                    effectName,
                    amplifier + 1
            );
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
        }, EFFECTS).setNameKey("morequesttypes.task.potion_effect.effect");

        config.addInt("amplifier", amplifier, v -> amplifier = v, -1, -1, 255)
                .setNameKey("morequesttypes.task.potion_effect.amplifier");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (effectId != null) nbt.putString("effect", effectId.toString());
        nbt.putInt("amplifier", amplifier);
    }


    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        effectId = ResourceLocation.tryParse(nbt.getString("effect"));
        amplifier = nbt.contains("amplifier") ? nbt.getInt("amplifier") : -1;
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(effectId == null ? "" : effectId.toString());
        buf.writeInt(amplifier);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        String s = buf.readUtf();
        effectId = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        amplifier = buf.readInt();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        if (effectId != null) {
            Component effectName = getEffectName();

            if (amplifier < 0) {
                list.add(Component.translatable(
                        "morequesttypes.task.potion_effect.tooltip.any_amp",
                        effectName
                ));
            } else {
                list.add(Component.translatable(
                        "morequesttypes.task.potion_effect.tooltip.specific_amp",
                        effectName,
                        amplifier + 1
                ));
            }
        }
    }
}