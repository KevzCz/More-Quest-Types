package net.pixeldreamstudios.morequesttypes.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.rewards.manager.EquipmentBonusManager;

import java.util.Collection;

public final class EquipmentAttributeCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SLOTS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    new String[]{"mainhand", "offhand", "head", "chest", "legs", "feet"},
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_OPERATIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    new String[]{"add_value", "add_multiplied_base", "add_multiplied_total"},
                    builder
            );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mqt")
                .then(Commands.literal("equipment_attribute")
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .then(Commands.argument("attribute", ResourceLocationArgument.id())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                                BuiltInRegistries.ATTRIBUTE.keySet(), builder))
                                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> addAttribute(
                                                                        ctx,
                                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                                        StringArgumentType.getString(ctx, "slot"),
                                                                        ResourceLocationArgument.getId(ctx, "attribute"),
                                                                        DoubleArgumentType.getDouble(ctx, "amount"),
                                                                        AttributeModifier.Operation.ADD_VALUE
                                                                ))
                                                                .then(Commands.argument("operation", StringArgumentType.word())
                                                                        .suggests(SUGGEST_OPERATIONS)
                                                                        .executes(ctx -> addAttribute(
                                                                                ctx,
                                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                                StringArgumentType.getString(ctx, "slot"),
                                                                                ResourceLocationArgument.getId(ctx, "attribute"),
                                                                                DoubleArgumentType.getDouble(ctx, "amount"),
                                                                                parseOperation(StringArgumentType.getString(ctx, "operation"))
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .then(Commands.argument("attribute", ResourceLocationArgument.id())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                                BuiltInRegistries.ATTRIBUTE.keySet(), builder))
                                                        .executes(ctx -> removeAttribute(
                                                                ctx,
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                StringArgumentType.getString(ctx, "slot"),
                                                                ResourceLocationArgument.getId(ctx, "attribute")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .executes(ctx -> clearAttributes(
                                                        ctx,
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "slot")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("list")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .executes(ctx -> listAttributes(
                                                        ctx,
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "slot")
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private static int addAttribute(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr,
            ResourceLocation attributeId,
            double amount,
            AttributeModifier.Operation operation
    ) {
        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        if (!BuiltInRegistries.ATTRIBUTE.containsKey(attributeId)) {
            ctx.getSource().sendFailure(Component.literal("Invalid attribute: " + attributeId));
            return 0;
        }

        int successCount = 0;
        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) continue;

            String modifierId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            EquipmentBonusManager.modifyBonus(
                    stack,
                    modifierId,
                    attributeId.toString(),
                    amount,
                    operation,
                    slotGroup.getSerializedName(),
                    EquipmentBonusManager.ModifyMode.ADD,
                    false,
                    false,
                    null,
                    null,
                    0,
                    0,
                    null,
                    null
            );
            successCount++;
        }

        if (successCount > 0) {
            int finalSuccessCount = successCount;
            String attrStr = attributeId.toString();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Added " + attrStr + " (+" + amount + ") to " +
                            slotStr + " slot for " + finalSuccessCount + " player(s)"
            ), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("No valid items found in " + slotStr + " slot"));
        }

        return successCount;
    }

    private static int removeAttribute(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr,
            ResourceLocation attributeId
    ) {
        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        int successCount = 0;
        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) continue;

            String modifierId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            EquipmentBonusManager.modifyBonus(
                    stack,
                    modifierId,
                    attributeId.toString(),
                    0,
                    AttributeModifier.Operation.ADD_VALUE,
                    slotGroup.getSerializedName(),
                    EquipmentBonusManager.ModifyMode.REMOVE,
                    false,
                    false,
                    null,
                    null,
                    0,
                    0,
                    null,
                    null
            );
            successCount++;
        }

        if (successCount > 0) {
            int finalSuccessCount = successCount;
            String attrStr = attributeId.toString();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Removed " + attrStr + " from " + slotStr + " slot for " + finalSuccessCount + " player(s)"
            ), true);
        }

        return successCount;
    }

    private static int clearAttributes(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr
    ) {
        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        int successCount = 0;
        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) continue;

            EquipmentBonusManager.getBonuses(stack).ifPresent(bonuses -> {
                var toRemove = bonuses.bonuses().stream()
                        .filter(entry -> entry.slot().equals(slotGroup.getSerializedName()))
                        .toList();

                for (var entry : toRemove) {
                    EquipmentBonusManager.modifyBonus(
                            stack,
                            entry.modifierId(),
                            entry.attributeId(),
                            0,
                            entry.operation(),
                            entry.slot(),
                            EquipmentBonusManager.ModifyMode.REMOVE,
                            false,
                            false,
                            null,
                            null,
                            0,
                            0,
                            null,
                            null
                    );
                }
            });

            successCount++;
        }

        if (successCount > 0) {
            int finalSuccessCount = successCount;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Cleared all quest attributes from " + slotStr + " slot for " + finalSuccessCount + " player(s)"
            ), true);
        }

        return successCount;
    }

    private static int listAttributes(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr
    ) {
        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal(
                        player.getName().getString() + " has no item in " + slotStr + " slot"
                ));
                continue;
            }

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Quest attributes on " + player.getName().getString() + "'s " +
                            stack.getHoverName().getString() + ":"
            ), false);

            EquipmentBonusManager.getBonuses(stack).ifPresentOrElse(bonuses -> {
                var matching = bonuses.bonuses().stream()
                        .filter(entry -> entry.slot().equals(slotGroup.getSerializedName()))
                        .toList();

                if (matching.isEmpty()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("  No quest attributes"), false);
                } else {
                    for (var entry : matching) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "  - " + entry.attributeId() + ": " +
                                        (entry.amount() >= 0 ? "+" : "") + entry.amount() +
                                        " (" + entry.operation().name().toLowerCase() + ")"
                        ), false);
                    }
                }
            }, () -> {
                ctx.getSource().sendSuccess(() -> Component.literal("  No quest attributes"), false);
            });
        }

        return targets.size();
    }

    private static ItemStack getStackForSlot(ServerPlayer player, EquipmentSlotGroup slotGroup) {
        return switch (slotGroup.getSerializedName()) {
            case "mainhand" -> player.getMainHandItem();
            case "offhand" -> player.getOffhandItem();
            case "head" -> player.getInventory().getArmor(3);
            case "chest" -> player.getInventory().getArmor(2);
            case "legs" -> player.getInventory().getArmor(1);
            case "feet" -> player.getInventory().getArmor(0);
            default -> ItemStack.EMPTY;
        };
    }

    private static EquipmentSlotGroup parseSlot(String slot) {
        return switch (slot.toLowerCase()) {
            case "mainhand", "main_hand" -> EquipmentSlotGroup.MAINHAND;
            case "offhand", "off_hand" -> EquipmentSlotGroup.OFFHAND;
            case "head", "helmet" -> EquipmentSlotGroup.HEAD;
            case "chest", "chestplate" -> EquipmentSlotGroup.CHEST;
            case "legs", "leggings" -> EquipmentSlotGroup.LEGS;
            case "feet", "boots" -> EquipmentSlotGroup.FEET;
            default -> null;
        };
    }

    private static AttributeModifier.Operation parseOperation(String operation) {
        return switch (operation.toLowerCase()) {
            case "add_value", "add" -> AttributeModifier.Operation.ADD_VALUE;
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total", "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private EquipmentAttributeCommand() {}
}