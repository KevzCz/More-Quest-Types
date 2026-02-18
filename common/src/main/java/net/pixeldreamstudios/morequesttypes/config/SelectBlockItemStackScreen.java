package net.pixeldreamstudios.morequesttypes.config;

import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.FTBLibraryClientConfig;
import dev.ftb.mods.ftblibrary.config.ui.resource.ResourceSearchMode;
import dev.ftb.mods.ftblibrary.config.ui.resource.ResourceSelectorScreen;
import dev.ftb.mods.ftblibrary.config.ui.resource.SearchModeIndex;
import dev.ftb.mods.ftblibrary.config.ui.resource.SelectableResource;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.util.ModUtils;
import dev.ftb.mods.ftblibrary.util.SearchTerms;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.*;

@Environment(EnvType.CLIENT)
public class SelectBlockItemStackScreen extends ResourceSelectorScreen<ItemStack> {

    private static final ResourceSearchMode<ItemStack> ALL_BLOCKS = new ResourceSearchMode.SearchMode<>(
            Component.translatable("morequesttypes.select_block.list_mode.all"),
            ItemIcon.getItemIcon(Items.BRICKS)
    ) {
        private List<SelectableResource<ItemStack>> allBlocksCache = null;

        @Override
        public Collection<? extends SelectableResource<ItemStack>> getAllResources() {
            if (allBlocksCache == null) {
                allBlocksCache = BuiltInRegistries.ITEM.stream()
                        .filter(item -> item instanceof BlockItem)
                        .map(ItemStack::new)
                        .map(SelectableResource::item)
                        .toList();
            }
            return allBlocksCache;
        }
    };

    private static final ResourceSearchMode<ItemStack> INVENTORY_BLOCKS = new ResourceSearchMode.SearchMode<>(
            Component.translatable("morequesttypes.select_block.list_mode.inv"),
            ItemIcon.getItemIcon(Items.CHEST)
    ) {
        @Override
        public Collection<? extends SelectableResource<ItemStack>> getAllResources() {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return List.of();
            }

            Set<ItemStack> uniqueBlocks = new LinkedHashSet<>();
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                    uniqueBlocks.add(stack.copy());
                }
            }

            return uniqueBlocks.stream()
                    .map(SelectableResource::item)
                    .toList();
        }
    };

    public static final SearchModeIndex<ResourceSearchMode<ItemStack>> BLOCK_MODES = Util.make(new SearchModeIndex<>(), index -> {
        index.appendMode(ALL_BLOCKS);
        index.appendMode(INVENTORY_BLOCKS);
    });

    private final BlockStackConfig config;

    public SelectBlockItemStackScreen(BlockStackConfig config, ConfigCallback callback) {
        super(config, callback);
        this.config = config;
    }

    @Override
    protected SearchModeIndex<ResourceSearchMode<ItemStack>> getSearchModeIndex() {
        return BLOCK_MODES;
    }

    @Override
    protected ResourceButton makeResourceButton(Panel panel, SelectableResource<ItemStack> resource) {
        return new BlockItemButton(panel, Objects.requireNonNullElse(resource, SelectableResource.item(ItemStack.EMPTY)));
    }

    private class BlockItemButton extends ResourceButton {
        private BlockItemButton(Panel panel, SelectableResource<ItemStack> resource) {
            super(panel, resource);
        }

        @Override
        public boolean shouldAdd(SearchTerms searchTerms) {
            ItemStack stack = getResource();
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                return false;
            }

            return searchTerms.match(
                    RegistrarManager.getId(stack.getItem(), Registries.ITEM),
                    stack.getHoverName().getString(),
                    id -> stack.is(TagKey.create(Registries.ITEM, id))
            );
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            if (!getResource().isEmpty()) {
                TooltipFlag flag = Minecraft.getInstance().options.advancedItemTooltips
                        ? TooltipFlag.ADVANCED
                        : TooltipFlag.NORMAL;

                getResource().getTooltipLines(
                        net.minecraft.world.item.Item.TooltipContext.of(Minecraft.getInstance().level),
                        Minecraft.getInstance().player,
                        flag
                ).forEach(list::add);

                if (FTBLibraryClientConfig.ITEM_MODNAME.get()) {
                    ModUtils.getModName(getResource().getItem()).ifPresent(name ->
                            list.add(Component.literal(name).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC))
                    );
                }
            }
        }
    }
}