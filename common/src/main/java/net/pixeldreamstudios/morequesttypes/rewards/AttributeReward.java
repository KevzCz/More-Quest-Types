package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;
import java.util.Objects;

public final class AttributeReward extends Reward {
    private ResourceLocation attributeId = ResourceLocation.withDefaultNamespace("generic.max_health");
    private double amount = 2.0D;

    private AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;

    public AttributeReward(long id, dev.ftb.mods.ftbquests.quest.Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.ATTRIBUTE;
    }

    private AttributeModifier buildModifier() {
        ResourceLocation modId = ResourceLocation.fromNamespaceAndPath("morequesttypes", "reward_" + id);
        return new AttributeModifier(modId, amount, operation);
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        try {
            applyToPlayer(player);
        } catch (Throwable ignored) {}
    }

    public void applyToPlayer(ServerPlayer player) {
        if (player == null) return;

        RegistryAccess ra = player.server.registryAccess();
        Holder<Attribute> holder = resolveAttributeHolder(ra, attributeId);
        if (holder == null) return;

        AttributeInstance inst = player.getAttribute(holder);
        if (inst == null) return;

        AttributeModifier mod = buildModifier();
        try {
            var existing = inst.getModifier(mod.id());
            if (existing != null) {
                inst.removeModifier(existing);
            }
        } catch (Throwable ignored) {}

        try {
            inst.addPermanentModifier(mod);
        } catch (Throwable t) {
            try {
                inst.addTransientModifier(mod);
            } catch (Throwable ignored) {}
        }
    }

    public void removeFromPlayer(ServerPlayer player) {
        if (player == null) return;

        RegistryAccess ra = player.server.registryAccess();
        Holder<Attribute> holder = resolveAttributeHolder(ra, attributeId);
        if (holder == null) return;

        AttributeInstance inst = player.getAttribute(holder);
        if (inst == null) return;

        AttributeModifier mod = buildModifier();
        try {
            var existing = inst.getModifier(mod.id());
            if (existing != null) inst.removeModifier(existing);
        } catch (Throwable ignored) {}
    }

    private static Holder<Attribute> resolveAttributeHolder(RegistryAccess access, ResourceLocation rl) {
        if (rl == null) return null;
        var reg = access.registryOrThrow(Registries.ATTRIBUTE);
        var key = ResourceKey.create(Registries.ATTRIBUTE, rl);
        return reg.getHolder(key).orElse(null);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        String valueStr;
        switch (operation) {
            case ADD_MULTIPLIED_BASE -> valueStr = signedPercent(amount) + " base";
            case ADD_MULTIPLIED_TOTAL -> valueStr = signedPercent(amount) + " total";
            case ADD_VALUE -> valueStr = signedValue(amount);
            default -> valueStr = signedValue(amount);
        }

        Component attrName;
        if (attributeId == null) {
            attrName = Component.literal("?");
        } else {
            var attr = BuiltInRegistries.ATTRIBUTE.get(attributeId);
            if (attr != null) {
                String key = attr.getDescriptionId();
                if (!key.isEmpty()) {
                    attrName = Component.translatable(key);
                } else {
                    attrName = Component.literal(attributeId.toString());
                }
            } else {
                String attrKey = "attribute.name." + attributeId.toString().replace(':', '.');
                attrName = Component.translatable(attrKey);
            }
        }

        return Component.translatable("morequesttypes.reward.attribute.title", Component.literal(valueStr), attrName);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list) {
        super.addMouseOverText(list);

        try {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                var teamData = TeamData.get(player);
                boolean isClaimed = teamData.isRewardClaimed(player.getUUID(), this);

                Component status = isClaimed
                        ? Component.translatable("morequesttypes.reward.status.on").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("morequesttypes.reward.status.off").withStyle(ChatFormatting.RED);

                list.add(Component.translatable("morequesttypes.reward.status", status));
                list.add(Component.translatable("morequesttypes.reward.toggle_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        } catch (Throwable ignored) {}
    }

    private static String signedValue(double v) {
        String s = trimDouble(v);
        return (v > 0 ? "+" : "") + s;
    }

    private static String signedPercent(double v) {
        double p = v * 100.0;
        long rounded = Math.round(p);
        String s = (Math.abs(p - rounded) < 0.005) ? Long.toString(rounded) : trimDouble(p);
        return (p > 0 ? "+" : "") + s + "%";
    }

    private static String trimDouble(double v) {
        String s = String.format(Locale.ROOT, "%.3f", v);
        int i = s.length() - 1;
        while (i > 0 && s.charAt(i) == '0') i--;
        if (s.charAt(i) == '.') i--;
        return s.substring(0, i + 1);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var keys = BuiltInRegistries.ATTRIBUTE.keySet().stream()
                .map(Objects::toString)
                .sorted()
                .toList();

        var ATTRS = NameMap.of(attributeId.toString(), keys.toArray(String[]::new))
                .name(Component::literal)
                .create();

        config.addEnum("attribute", attributeId.toString(), s -> {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) attributeId = rl;
        }, ATTRS).setNameKey("morequesttypes.reward.attribute.attribute");

        var OPS = NameMap.of(operation, AttributeModifier.Operation.values())
                .name(o -> Component.literal(o.name()))
                .create();

        config.addEnum("operation", operation, v -> operation = v, OPS)
                .setNameKey("morequesttypes.reward.attribute.operation");

        config.addDouble("amount", amount, v -> amount = v, 2.0D, -1_000_000D, 1_000_000D)
                .setNameKey("morequesttypes.reward.attribute.amount");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (attributeId != null) nbt.putString("attribute", attributeId.toString());
        nbt.putDouble("amount", amount);
        nbt.putString("operation", operation.name());
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        attributeId = ResourceLocation.tryParse(nbt.getString("attribute"));
        amount = nbt.contains("amount") ? nbt.getDouble("amount") : 0.0D;
        try { operation = AttributeModifier.Operation.valueOf(nbt.getString("operation")); } catch (Throwable t) { operation = AttributeModifier.Operation.ADD_VALUE; }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(attributeId == null ? "" : attributeId.toString());
        buf.writeDouble(amount);
        buf.writeUtf(operation.name());
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        String s = buf.readUtf();
        attributeId = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        amount = buf.readDouble();
        try { operation = AttributeModifier.Operation.valueOf(buf.readUtf()); } catch (Throwable t) { operation = AttributeModifier.Operation.ADD_VALUE; }
    }

    @Override
    public boolean getExcludeFromClaimAll() { return false; }

    public ResourceLocation getAttributeId() {
        return attributeId;
    }
}