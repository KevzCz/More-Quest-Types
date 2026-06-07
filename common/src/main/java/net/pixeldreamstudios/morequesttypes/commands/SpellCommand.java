package net.pixeldreamstudios.morequesttypes.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;
import net.pixeldreamstudios.morequesttypes.util.SpellDisplayHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class SpellCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SLOTS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    new String[]{"mainhand", "offhand", "head", "chest", "legs", "feet"},
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CONTENT_TYPES = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    new String[]{"magic", "archery", "melee"},
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SPELLS = (context, builder) -> {
        if (!SpellEngineCompat.isLoaded()) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggestResource(
                SpellEngineCompat.getAllSpells(context.getSource().getLevel()).stream()
                        .filter(Objects::nonNull),
                builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mqt")
                .then(Commands.literal("spell_equipment")
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .then(Commands.argument("spell", ResourceLocationArgument.id())
                                                        .suggests(SUGGEST_SPELLS)
                                                        .executes(ctx -> addItemSpell(
                                                                ctx,
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                StringArgumentType.getString(ctx, "slot"),
                                                                ResourceLocationArgument.getId(ctx, "spell"),
                                                                "MAGIC"
                                                        ))
                                                        .then(Commands.argument("content_type", StringArgumentType.word())
                                                                .suggests(SUGGEST_CONTENT_TYPES)
                                                                .executes(ctx -> addItemSpell(
                                                                        ctx,
                                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                                        StringArgumentType.getString(ctx, "slot"),
                                                                        ResourceLocationArgument.getId(ctx, "spell"),
                                                                        StringArgumentType.getString(ctx, "content_type")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .then(Commands.argument("spell", ResourceLocationArgument.id())
                                                        .suggests(SUGGEST_SPELLS)
                                                        .executes(ctx -> removeItemSpell(
                                                                ctx,
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                StringArgumentType.getString(ctx, "slot"),
                                                                ResourceLocationArgument.getId(ctx, "spell")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .then(Commands.argument("spells", StringArgumentType.greedyString())
                                                        .executes(ctx -> setItemSpells(
                                                                ctx,
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                StringArgumentType.getString(ctx, "slot"),
                                                                StringArgumentType.getString(ctx, "spells"),
                                                                "MAGIC"
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .suggests(SUGGEST_SLOTS)
                                                .executes(ctx -> clearItemSpells(
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
                                                .executes(ctx -> listItemSpells(
                                                        ctx,
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "slot")
                                                ))
                                        )
                                )
                        )
                )
                .then(Commands.literal("player_spell")
                        .then(Commands.literal("grant")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("key", StringArgumentType.string())
                                                .then(Commands.argument("spell", ResourceLocationArgument.id())
                                                        .suggests(SUGGEST_SPELLS)
                                                        .executes(ctx -> grantPlayerSpell(
                                                                ctx,
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                StringArgumentType.getString(ctx, "key"),
                                                                ResourceLocationArgument.getId(ctx, "spell")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("key", StringArgumentType.string())
                                                .executes(ctx -> revokePlayerSpell(
                                                        ctx,
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "key")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("list")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> listPlayerSpells(
                                                ctx,
                                                EntityArgument.getPlayers(ctx, "targets")
                                        ))
                                )
                        )
                )
        );
    }

    private static int requireSpellEngine(CommandSourceStack source) {
        if (!SpellEngineCompat.isLoaded()) {
            source.sendFailure(Component.literal("Spell Engine is not loaded"));
            return 0;
        }
        return 1;
    }

    private static int addItemSpell(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr,
            ResourceLocation spellId,
            String contentType
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;

        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        int successCount = 0;
        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) continue;
            SpellEngineCompat.addItemSpell(stack, spellId, contentType);
            successCount++;
        }

        if (successCount > 0) {
            int count = successCount;
            String spell = spellId.toString();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Added spell " + spell + " to " + slotStr + " for " + count + " player(s)"
            ), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("No valid items found in " + slotStr + " slot"));
        }

        return successCount;
    }

    private static int removeItemSpell(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr,
            ResourceLocation spellId
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;

        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        int successCount = 0;
        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) continue;
            SpellEngineCompat.removeItemSpell(stack, spellId);
            successCount++;
        }

        if (successCount > 0) {
            int count = successCount;
            String spell = spellId.toString();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Removed spell " + spell + " from " + slotStr + " for " + count + " player(s)"
            ), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("No valid items found in " + slotStr + " slot"));
        }

        return successCount;
    }

    private static int setItemSpells(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr,
            String spellsArg,
            String contentType
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;

        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        List<ResourceLocation> spells = parseSpellList(spellsArg);
        if (spells.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No valid spells provided. Use comma-separated spell IDs."));
            return 0;
        }

        int successCount = 0;
        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) continue;
            SpellEngineCompat.setItemSpells(stack, spells, contentType);
            successCount++;
        }

        if (successCount > 0) {
            int count = successCount;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Set " + spells.size() + " spell(s) on " + slotStr + " for " + count + " player(s)"
            ), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("No valid items found in " + slotStr + " slot"));
        }

        return successCount;
    }

    private static int clearItemSpells(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;

        EquipmentSlotGroup slotGroup = parseSlot(slotStr);
        if (slotGroup == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid slot: " + slotStr));
            return 0;
        }

        int successCount = 0;
        for (ServerPlayer player : targets) {
            ItemStack stack = getStackForSlot(player, slotGroup);
            if (stack.isEmpty()) continue;
            SpellEngineCompat.setItemSpells(stack, List.of(), "MAGIC");
            successCount++;
        }

        if (successCount > 0) {
            int count = successCount;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Cleared spells from " + slotStr + " for " + count + " player(s)"
            ), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("No valid items found in " + slotStr + " slot"));
        }

        return successCount;
    }

    private static int listItemSpells(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String slotStr
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;

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
                    "Spells on " + player.getName().getString() + "'s " + stack.getHoverName().getString() + ":"
            ), false);

            List<ResourceLocation> spells = SpellEngineCompat.getItemSpells(stack);
            if (spells.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("  (none)"), false);
            } else {
                for (ResourceLocation spell : spells) {
                    ctx.getSource().sendSuccess(() -> Component.literal("  - " + spell), false);
                }
            }
        }

        return targets.size();
    }

    private static int grantPlayerSpell(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String key,
            ResourceLocation spellId
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;
        if (key.isBlank()) {
            ctx.getSource().sendFailure(Component.literal("Key cannot be empty"));
            return 0;
        }

        for (ServerPlayer player : targets) {
            SpellEngineCompat.installSpells(player, key, List.of(spellId));
        }

        int count = targets.size();
        String spell = spellId.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Granted spell " + spell + " to " + count + " player(s) under key '" + key + "'"
        ), true);
        return count;
    }

    private static int revokePlayerSpell(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets,
            String key
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;
        if (key.isBlank()) {
            ctx.getSource().sendFailure(Component.literal("Key cannot be empty"));
            return 0;
        }

        for (ServerPlayer player : targets) {
            SpellEngineCompat.uninstallSpells(player, key);
        }

        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Revoked spell container '" + key + "' from " + count + " player(s)"
        ), true);
        return count;
    }

    private static int listPlayerSpells(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> targets
    ) {
        if (requireSpellEngine(ctx.getSource()) == 0) return 0;

        for (ServerPlayer player : targets) {
            List<SpellEngineCompat.InstalledSpellContainer> containers =
                    SpellEngineCompat.getPlayerInstalledSpellContainers(player);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Installed spell containers for " + player.getName().getString() + ":"
            ), false);

            if (containers == null || containers.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("  (none)"), false);
            } else {
                for (SpellEngineCompat.InstalledSpellContainer container : containers) {
                    String header = "  - " + container.key() + " (" + container.contentType() + ")";
                    ctx.getSource().sendSuccess(() -> Component.literal(header), false);
                    if (container.spells().isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("      (no spells)"), false);
                    } else {
                        for (ResourceLocation spell : container.spells()) {
                            ctx.getSource().sendSuccess(() -> Component.literal("      • ")
                                    .append(SpellDisplayHelper.spellName(spell))
                                    .append(Component.literal(" [" + spell + "]")), false);
                        }
                    }
                }
            }
        }

        return targets.size();
    }

    private static List<ResourceLocation> parseSpellList(String spellsArg) {
        List<ResourceLocation> spells = new ArrayList<>();
        if (spellsArg == null || spellsArg.isBlank()) {
            return spells;
        }
        for (String part : spellsArg.split(",")) {
            ResourceLocation rl = ResourceLocation.tryParse(part.trim());
            if (rl != null) {
                spells.add(rl);
            }
        }
        return spells;
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

    private SpellCommand() {
    }
}
