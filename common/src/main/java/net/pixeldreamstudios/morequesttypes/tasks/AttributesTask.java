package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

public final class AttributesTask extends Task {
    public enum Operator { EQ, LE, GE, GT, LT }

    private ResourceLocation attributeId = ResourceLocation.withDefaultNamespace("generic.max_health");
    private Operator op = Operator.GE;
    private double amount = 20.0D;

    public AttributesTask(long id, dev.ftb.mods.ftbquests.quest.Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.ATTRIBUTES;
    }

    @Override
    public long getMaxProgress() {
        return 100L;
    }

    @Override
    public boolean hideProgressNumbers() {
        return false;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getButtonText() {
        return Component.literal(trim(amount));
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.3f", v);
        int i = s.length() - 1;
        while (i > 0 && s.charAt(i) == '0') i--;
        if (s.charAt(i) == '.') i--;
        return s.substring(0, i + 1);
    }
    @Override
    public String formatMaxProgress() {
        return trim(amount);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public String formatProgress(TeamData teamData, long progress) {
        return trim(shownCurrentFromProgress(progress));
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }
    private static double fromPercent(double p, double target, Operator op) {
        return switch (op) {
            case GE, GT -> (target >= 0.0)
                    ? p * target
                    : target + p * Math.abs(target);
            case LE, LT -> -fromPercent(p, -target, Operator.GE);
            case EQ     -> target * p;
        };
    }

    private double shownCurrentFromProgress(long progress) {
        double p = Math.max(0, Math.min(100, progress)) / 100.0;
        return fromPercent(p, amount, op);
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, net.minecraft.world.item.ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        Holder<Attribute> holder = resolveAttributeHolder(player.registryAccess(), attributeId);
        if (holder == null) return;

        Collection<ServerPlayer> online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;

        double bestValue = Double.NEGATIVE_INFINITY;
        for (ServerPlayer sp : online) {
            double v = read(sp, holder);
            if (v > bestValue) bestValue = v;
        }

        long targetProgress = Math.max(0L, Math.min(100L, Math.round(100.0 * computePercent(bestValue, amount, op))));
        long cur = teamData.getProgress(this);
        if (targetProgress != cur) teamData.setProgress(this, targetProgress);

        boolean satisfied = compare(bestValue, amount, op);
        if (satisfied && targetProgress < 100L) teamData.setProgress(this, 100L);
    }

    private static boolean compare(double a, double b, Operator op) {
        return switch (op) {
            case EQ -> Math.abs(a - b) < 1.0E-6;
            case LE -> a <= b + 1.0E-6;
            case GE -> a >= b - 1.0E-6;
            case GT -> a >  b + 1.0E-6;
            case LT -> a <  b - 1.0E-6;
        };
    }

    private static double computePercent(double current, double target, Operator op) {
        return switch (op) {
            case GE, GT -> percentGE(current, target);
            case LE, LT -> percentGE(-current, -target);
            case EQ -> {
                double denom = Math.max(Math.abs(target), 1.0E-6);
                double diff = Math.abs(current - target);
                double p = 1.0 - (diff / denom);
                yield clamp01(p);
            }
        };
    }

    private static double percentGE(double current, double target) {
        if (target >= 0.0) {
            double denom = Math.max(Math.abs(target), 1.0E-9);
            return clamp01(current / denom);
        } else {
            double denom = Math.max(Math.abs(target), 1.0E-9);
            double p = (current - target) / denom;
            return clamp01(p);
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static double read(LivingEntity e, Holder<Attribute> holder) {
        AttributeInstance inst = e.getAttribute(holder);
        return (inst == null) ? 0.0D : inst.getValue();
    }

    private static Holder<Attribute> resolveAttributeHolder(RegistryAccess access, ResourceLocation rl) {
        if (rl == null) return null;
        var reg = access.registryOrThrow(Registries.ATTRIBUTE);
        var key = ResourceKey.create(Registries.ATTRIBUTE, rl);
        return reg.getHolder(key).orElse(null);
    }

    @Environment(EnvType.CLIENT)
    private double lastSeenBest() {
        try {
            var clientPlayer = FTBQuestsClient.getClientPlayer();
            var level = Minecraft.getInstance().level;
            if (clientPlayer == null || level == null) return 0.0D;
            Holder<Attribute> holder = resolveAttributeHolder(level.registryAccess(), attributeId);
            if (holder == null) return 0.0D;
            return read(clientPlayer, holder);
        } catch (Throwable t) {
            return 0.0D;
        }
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
                .name(s -> Component.literal(s))
                .create();

        config.addEnum("attribute", attributeId.toString(), s -> {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) attributeId = rl;
        }, ATTRS).setNameKey("ftbquests.task.attributes.attribute");

        var OPS = NameMap.of(Operator.GE, Operator.values())
                .name(o -> Component.literal(switch (o) {
                    case EQ -> "="; case LE -> "<="; case GE -> ">="; case GT -> ">"; case LT -> "<";
                })).create();
        config.addEnum("operator", op, v -> op = v, OPS)
                .setNameKey("ftbquests.task.attributes.operator");

        config.addDouble("amount", amount, v -> amount = v, 20.0D, -1_000_000D, 1_000_000D)
                .setNameKey("ftbquests.task.attributes.amount");
    }

    @Override
    public void writeData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (attributeId != null) nbt.putString("attribute", attributeId.toString());
        nbt.putString("operator", op.name());
        nbt.putDouble("amount", amount);
    }

    @Override
    public void readData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        attributeId = ResourceLocation.tryParse(nbt.getString("attribute"));
        try { op = Operator.valueOf(nbt.getString("operator")); } catch (Throwable ignored) { op = Operator.GE; }
        amount = nbt.contains("amount") ? nbt.getDouble("amount") : 0.0D;
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(attributeId == null ? "" : attributeId.toString());
        buf.writeEnum(op);
        buf.writeDouble(amount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        String s = buf.readUtf();
        attributeId = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        op = buf.readEnum(Operator.class);
        amount = buf.readDouble();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        String sym = switch (op) {
            case EQ -> "="; case LE -> "<="; case GE -> ">="; case GT -> ">"; case LT -> "<";
        };
        String id = attributeId == null ? "?" : attributeId.toString();
        return Component.translatable("ftbquests.morequesttypes.task.attributes.title", id, sym, trim(amount));
    }
}
