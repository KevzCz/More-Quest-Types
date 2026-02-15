package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.api.IQuestSummonedEntity;
import net.pixeldreamstudios.morequesttypes.config.RewardTableDropConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDropMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void mqt$addCustomDrops(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof IQuestSummonedEntity questEntity)) {
            return;
        }

        if (!questEntity.isQuestSummoned()) {
            return;
        }

        ServerPlayer owner = null;
        if (questEntity.getQuestOwnerUuid() != null) {
            owner = level.getServer().getPlayerList().getPlayer(questEntity.getQuestOwnerUuid());
        }

        ServerPlayer killer = null;
        if (damageSource.getEntity() instanceof ServerPlayer) {
            killer = (ServerPlayer) damageSource.getEntity();
        }

        boolean rewardAnyone = entity.getTags().contains("mqt_reward_anyone");

        ServerPlayer rewardTarget = rewardAnyone ? killer : owner;

        String customDrops = questEntity.getQuestCustomDrops();
        String equipmentDropRates = questEntity.getQuestEquipmentDropRates();
        String rewardTables = questEntity.getQuestRewardTables();

        if (customDrops != null && !customDrops.isEmpty()) {
            String[] dropEntries = customDrops.split(";");
            for (String entry : dropEntries) {
                entry = entry.trim();
                if (entry.isEmpty()) continue;

                try {
                    String[] parts = entry.split("\\|");
                    String itemData = parts[0];
                    double dropRate = parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0;

                    if (level.random.nextDouble() > dropRate) {
                        continue;
                    }

                    CompoundTag fullTag = TagParser.parseTag(itemData);
                    if (fullTag.contains("id")) {
                        ItemStack stack = ItemStack.parseOptional(level.registryAccess(), fullTag);
                        if (!stack.isEmpty()) {
                            ItemEntity itemEntity = new ItemEntity(
                                    level,
                                    entity.getX(),
                                    entity.getY(),
                                    entity.getZ(),
                                    stack
                            );
                            level.addFreshEntity(itemEntity);
                        }
                    }
                } catch (Exception e) {
                }
            }
        }

        if (equipmentDropRates != null && !equipmentDropRates.isEmpty()) {
            String[] rates = equipmentDropRates.split(",");
            double[] dropChances = new double[6];
            for (int i = 0; i < Math.min(rates.length, 6); i++) {
                try {
                    dropChances[i] = Double.parseDouble(rates[i].trim());
                } catch (Exception e) {
                    dropChances[i] = 1.0;
                }
            }

            ItemStack[] equipment = new ItemStack[]{
                    entity.getItemBySlot(EquipmentSlot.HEAD),
                    entity.getItemBySlot(EquipmentSlot.CHEST),
                    entity.getItemBySlot(EquipmentSlot.LEGS),
                    entity.getItemBySlot(EquipmentSlot.FEET),
                    entity.getItemBySlot(EquipmentSlot.MAINHAND),
                    entity.getItemBySlot(EquipmentSlot.OFFHAND)
            };

            for (int i = 0; i < equipment.length; i++) {
                if (!equipment[i].isEmpty() && level.random.nextDouble() <= dropChances[i]) {
                    ItemEntity itemEntity = new ItemEntity(
                            level,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            equipment[i].copy()
                    );
                    level.addFreshEntity(itemEntity);
                }
            }
        }

        if (rewardTables != null && !rewardTables.isEmpty() && rewardTarget != null) {
            ServerQuestFile questFile = ServerQuestFile.INSTANCE;
            if (questFile == null) return;

            String[] tableEntries = rewardTables.split(";");
            for (String entry : tableEntries) {
                entry = entry.trim();
                if (entry.isEmpty()) continue;

                try {
                    RewardTableDropConfig.RewardTableDrop tableDrop = RewardTableDropConfig.RewardTableDrop.deserialize(entry);

                    if (level.random.nextDouble() > tableDrop.dropRate) {
                        continue;
                    }

                    RewardTable table = questFile.getRewardTable(questFile.getID(tableDrop.rewardTableId));
                    if (table == null) continue;

                    if (tableDrop.mode == RewardTableDropConfig.TriggerMode.ALL) {
                        for (WeightedReward wr : table.getWeightedRewards()) {
                            Reward reward = wr.getReward();

                            if (reward instanceof ItemReward itemReward) {
                                ItemStack stack = itemReward.getItem().copy();
                                stack.setCount(itemReward.getCount());

                                ItemEntity itemEntity = new ItemEntity(
                                        level,
                                        entity.getX(),
                                        entity.getY(),
                                        entity.getZ(),
                                        stack
                                );
                                level.addFreshEntity(itemEntity);
                            } else {
                                reward.claim(rewardTarget, false);
                            }
                        }
                    } else {
                        Collection<WeightedReward> rewards = table.generateWeightedRandomRewards(level.random, 1, true);
                        for (WeightedReward wr : rewards) {
                            Reward reward = wr.getReward();

                            if (reward instanceof ItemReward itemReward) {
                                ItemStack stack = itemReward.getItem().copy();
                                stack.setCount(itemReward.getCount());

                                ItemEntity itemEntity = new ItemEntity(
                                        level,
                                        entity.getX(),
                                        entity.getY(),
                                        entity.getZ(),
                                        stack
                                );
                                level.addFreshEntity(itemEntity);
                            } else {
                                reward.claim(rewardTarget, false);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}