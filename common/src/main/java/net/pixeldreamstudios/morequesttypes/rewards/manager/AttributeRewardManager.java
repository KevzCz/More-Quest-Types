package net.pixeldreamstudios.morequesttypes.rewards.manager;

import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.pixeldreamstudios.morequesttypes.rewards.AttributeReward;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class AttributeRewardManager {
    private AttributeRewardManager() {}

    public static void syncForPlayer(ServerPlayer player) {
        TeamData teamData = TeamData.get(player);
        var file = teamData.getFile();
        if (file == null) return;

        Collection<? extends QuestObjectBase> all = file.getAllObjects();
        if (all != null) {
            for (QuestObjectBase obj : all) {
                if (!(obj instanceof Reward r)) continue;
                if (!(r instanceof AttributeReward ar)) continue;

                boolean claimed = teamData.isRewardClaimed(player.getUUID(), ar);
                if (claimed) {
                    try { ar.applyToPlayer(player); } catch (Throwable ignored) {}
                } else {
                    try { ar.removeFromPlayer(player); } catch (Throwable ignored) {}
                }
            }
        }

        try {
            Set<ResourceLocation> attrsInRewards = new HashSet<>();
            if (all != null) {
                for (QuestObjectBase obj : all) {
                    if (!(obj instanceof Reward r)) continue;
                    if (!(r instanceof AttributeReward ar)) continue;
                    ResourceLocation attrId = ar.getAttributeId();
                    if (attrId != null) attrsInRewards.add(attrId);
                }
            }

            if (attrsInRewards.isEmpty()) return;

            RegistryAccess ra = player.server.registryAccess();
            var attrRegistry = ra.registryOrThrow(Registries.ATTRIBUTE);

            for (ResourceLocation attrKey : attrsInRewards) {
                try {
                    var key = ResourceLocation.tryParse(attrKey.toString());
                    if (key == null) continue;
                    var holderOpt = attrRegistry.getHolder(key);
                    if (holderOpt.isEmpty()) continue;
                    Holder<Attribute> holder = holderOpt.get();

                    AttributeInstance inst = player.getAttribute(holder);
                    if (inst == null) continue;

                    try {
                        for (AttributeModifier mod : inst.getModifiers()) {
                            if (mod == null) continue;
                            ResourceLocation modId;
                            try {
                                modId = mod.id();
                            } catch (Throwable t) {
                                continue;
                            }
                            if (!"morequesttypes".equals(modId.getNamespace())) continue;

                            String path = modId.getPath();
                            if (!path.startsWith("reward_")) continue;

                            String idPart = path.substring("reward_".length());
                            long rewardId;
                            try {
                                rewardId = Long.parseLong(idPart);
                            } catch (NumberFormatException nfe) {
                                try { inst.removeModifier(mod); } catch (Throwable ignored) {}
                                continue;
                            }
                            var rewardObj = file.getReward(rewardId);
                            boolean shouldKeep = false;
                            if (rewardObj instanceof AttributeReward arExisting) {
                                shouldKeep = teamData.isRewardClaimed(player.getUUID(), arExisting);
                            }

                            if (!shouldKeep) {
                                try { inst.removeModifier(mod); } catch (Throwable ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
