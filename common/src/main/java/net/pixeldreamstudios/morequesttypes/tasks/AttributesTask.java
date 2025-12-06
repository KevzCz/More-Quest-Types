package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

public final class AttributesTask extends Task {
    private ResourceLocation attributeId = ResourceLocation.withDefaultNamespace("generic.max_health");
    private ComparisonMode comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
    private double firstNumber = 20.0D;
    private double secondNumber = 30.0D;

    public AttributesTask(long id, dev.ftb.mods.ftbquests.quest.Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.ATTRIBUTES;
    }

    @Override
    public long getMaxProgress() {
        if (comparisonMode.isRange()) {
            return 1;
        }
        return 100L;
    }

    @Override
    public boolean hideProgressNumbers() {
        return comparisonMode.isRange();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getButtonText() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return Component.literal("? ");

        long p = TeamData.get(player).getProgress(this);

        if (comparisonMode.isRange()) {
            return Component.literal(p >= 1 ? "✓" : "✗");
        }

        return Component.literal(trim(shownCurrentFromProgress(p)));
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
        if (comparisonMode.isRange()) {
            return "1";
        }
        return trim(firstNumber);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public String formatProgress(TeamData teamData, long progress) {
        if (comparisonMode.isRange()) {
            return progress >= 1 ? "1" : "0";
        }
        return trim(shownCurrentFromProgress(progress));
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    private double shownCurrentFromProgress(long progress) {
        double p = Math.max(0, Math.min(100, progress)) / 100.0;
        return p * firstNumber;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
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

        long current = teamData.getProgress(this);
        long target;

        if (comparisonMode.isRange()) {
            double validatedSecond = validateNumbers(firstNumber, secondNumber);
            boolean inRange = compareDouble(bestValue, comparisonMode, firstNumber, validatedSecond);
            target = inRange ? 1 : 0;
        } else {
            boolean satisfied = compareDouble(bestValue, comparisonMode, firstNumber, secondNumber);
            if (satisfied) {
                target = 100L;
            } else {
                double percent = computePercent(bestValue, firstNumber, comparisonMode);
                target = Math.max(0L, Math.min(100L, Math.round(100.0 * percent)));
            }
        }

        if (target != current) {
            teamData.setProgress(this, target);
        }
    }

    private static boolean compareDouble(double value, ComparisonMode mode, double first, double second) {
        double epsilon = 1.0E-6;
        return switch (mode) {
            case EQUALS -> Math.abs(value - first) < epsilon;
            case GREATER_THAN -> value > first + epsilon;
            case LESS_THAN -> value < first - epsilon;
            case GREATER_OR_EQUAL -> value >= first - epsilon;
            case LESS_OR_EQUAL -> value <= first + epsilon;
            case RANGE -> value > first + epsilon && value < second - epsilon;
            case RANGE_EQUAL -> value >= first - epsilon && value <= second + epsilon;
            case RANGE_EQUAL_FIRST -> value >= first - epsilon && value < second - epsilon;
            case RANGE_EQUAL_SECOND -> value > first + epsilon && value <= second + epsilon;
        };
    }

    private static double computePercent(double current, double target, ComparisonMode mode) {
        return switch (mode) {
            case GREATER_OR_EQUAL, GREATER_THAN -> {
                if (target >= 0.0) {
                    double denom = Math.max(Math.abs(target), 1.0E-9);
                    yield clamp01(current / denom);
                } else {
                    double denom = Math.max(Math.abs(target), 1.0E-9);
                    yield clamp01((current - target) / denom);
                }
            }
            case LESS_OR_EQUAL, LESS_THAN -> {
                yield computePercent(-current, -target, ComparisonMode.GREATER_OR_EQUAL);
            }
            case EQUALS -> {
                double denom = Math.max(Math.abs(target), 1.0E-6);
                double diff = Math.abs(current - target);
                yield clamp01(1.0 - (diff / denom));
            }
            default -> 0.0;
        };
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        return Math.min(v, 1.0);
    }

    private double validateNumbers(double first, double second) {
        if (comparisonMode.isRange()) {
            if (second <= first) {
                return first + 10.0;
            }
        }
        return second;
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
        }, ATTRS).setNameKey("morequesttypes.task.attributes.attribute");

        var COMPARISON_MAP = NameMap.of(ComparisonMode.GREATER_OR_EQUAL, ComparisonMode.values())
                .name(cm -> Component.translatable(cm.getTranslationKey()))
                .create();
        config.addEnum("comparison_mode", comparisonMode, v -> {
            comparisonMode = v;
            if (v.isRange() && secondNumber <= firstNumber) {
                secondNumber = firstNumber + 10.0;
            }
        }, COMPARISON_MAP).setNameKey("morequesttypes.task.comparison_mode");

        config.addDouble("first_number", firstNumber, v -> {
                    firstNumber = v;
                    if (comparisonMode.isRange() && secondNumber <= firstNumber) {
                        secondNumber = firstNumber + 10.0;
                    }
                }, 20.0D, -1_000_000D, 1_000_000D)
                .setNameKey("morequesttypes.task.first_number");

        config.addDouble("second_number", secondNumber, v -> {
                    if (comparisonMode.isRange() && v <= firstNumber) {
                        secondNumber = firstNumber + 10.0;
                    } else {
                        secondNumber = v;
                    }
                }, 30.0D, -1_000_000D, 1_000_000D)
                .setNameKey("morequesttypes.task.second_number");
    }

    @Override
    public void writeData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (attributeId != null) nbt.putString("attribute", attributeId.toString());
        nbt.putString("comparison_mode", comparisonMode.name());
        nbt.putDouble("first_number", firstNumber);
        nbt.putDouble("second_number", secondNumber);
    }

    @Override
    public void readData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        attributeId = ResourceLocation.tryParse(nbt.getString("attribute"));

        if (nbt.contains("comparison_mode")) {
            try {
                comparisonMode = ComparisonMode.valueOf(nbt.getString("comparison_mode"));
            } catch (Throwable ignored) {
                comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
            }
        }

        firstNumber = nbt.contains("first_number") ? nbt.getDouble("first_number") : 20.0D;
        secondNumber = nbt.contains("second_number") ? nbt.getDouble("second_number") : 30.0D;

        // Legacy support for old "operator" and "amount" fields
        if (nbt.contains("operator") && ! nbt.contains("comparison_mode")) {
            try {
                String oldOp = nbt.getString("operator");
                comparisonMode = switch (oldOp) {
                    case "EQ" -> ComparisonMode.EQUALS;
                    case "GT" -> ComparisonMode.GREATER_THAN;
                    case "LT" -> ComparisonMode.LESS_THAN;
                    case "GE" -> ComparisonMode.GREATER_OR_EQUAL;
                    case "LE" -> ComparisonMode.LESS_OR_EQUAL;
                    default -> ComparisonMode.GREATER_OR_EQUAL;
                };
            } catch (Throwable ignored) {}
        }

        if (nbt.contains("amount") && ! nbt.contains("first_number")) {
            firstNumber = nbt.getDouble("amount");
        }

        if (comparisonMode.isRange() && secondNumber <= firstNumber) {
            secondNumber = firstNumber + 10.0;
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(attributeId == null ? "" : attributeId.toString());
        buf.writeEnum(comparisonMode);
        buf.writeDouble(firstNumber);
        buf.writeDouble(secondNumber);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        String s = buf.readUtf();
        attributeId = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        comparisonMode = buf.readEnum(ComparisonMode.class);
        firstNumber = buf.readDouble();
        secondNumber = buf.readDouble();

        if (comparisonMode.isRange() && secondNumber <= firstNumber) {
            secondNumber = firstNumber + 10.0;
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        Component attrName;
        if (attributeId == null) {
            attrName = Component.literal("?");
        } else {
            String translationKey = "attribute.name." + attributeId.getNamespace() + "." + attributeId.getPath();
            attrName = Component.translatable(translationKey);
        }

        String compDesc = getComparisonDescription();
        return Component.translatable("morequesttypes.task.attributes.title", attrName, compDesc);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        String compDesc = getComparisonDescription();

        Component attrName;
        if (attributeId == null) {
            attrName = Component.literal("?");
        } else {
            String translationKey = "attribute.name." + attributeId.getNamespace() + "." + attributeId.getPath();
            attrName = Component.translatable(translationKey);
        }

        list.add(Component.translatable("morequesttypes.task.attributes.tooltip", attrName, compDesc));
    }

    private String getComparisonDescription() {
        double validatedSecond = validateNumbers(firstNumber, secondNumber);

        return switch (comparisonMode) {
            case EQUALS -> "= " + trim(firstNumber);
            case GREATER_THAN -> "> " + trim(firstNumber);
            case LESS_THAN -> "< " + trim(firstNumber);
            case GREATER_OR_EQUAL -> "≥ " + trim(firstNumber);
            case LESS_OR_EQUAL -> "≤ " + trim(firstNumber);
            case RANGE -> trim(firstNumber) + " < x < " + trim(validatedSecond);
            case RANGE_EQUAL -> trim(firstNumber) + " ≤ x ≤ " + trim(validatedSecond);
            case RANGE_EQUAL_FIRST -> trim(firstNumber) + " ≤ x < " + trim(validatedSecond);
            case RANGE_EQUAL_SECOND -> trim(firstNumber) + " < x ≤ " + trim(validatedSecond);
        };
    }
}