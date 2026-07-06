package net.pixeldreamstudios.morequesttypes.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class ItemNbtPreviewStorage {
    private ItemNbtPreviewStorage() {}

    public static Optional<String> encode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        try {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return Optional.empty();
            }
            CompoundTag tag = (CompoundTag) stack.save(player.registryAccess());
            return Optional.of(tag.toString());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static ItemStack decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return ItemStack.EMPTY;
            }
            CompoundTag tag = TagParser.parseTag(encoded);
            return ItemStack.parse(player.registryAccess(), tag).orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

}
