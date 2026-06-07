package net.pixeldreamstudios.morequesttypes.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class TaskLocationHelper {
    private TaskLocationHelper() {}

    public static boolean matches(
            ServerLevel level,
            BlockPos pos,
            Either<ResourceKey<Structure>, TagKey<Structure>> structure,
            String dimension,
            String biome
    ) {
        if (structure != null && !isInsideStructureOrTag(level, pos, structure)) return false;
        if (!isInsideDimension(level, dimension)) return false;
        return isInsideBiome(level, pos, biome);
    }

    private static boolean isInsideStructureOrTag(ServerLevel level, BlockPos pos,
                                                  Either<ResourceKey<Structure>, TagKey<Structure>> structure) {
        StructureManager mgr = level.structureManager();
        return structure.map(
                key -> {
                    var holder = mgr.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
                    if (holder.isEmpty()) return false;
                    var value = holder.get().value();
                    return mgr.getStructureWithPieceAt(pos, value).isValid();
                },
                tag -> mgr.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .getTag(tag)
                        .map(hs -> {
                            for (var h : hs) {
                                var value = h.value();
                                if (mgr.getStructureWithPieceAt(pos, value).isValid()) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .orElse(false)
        );
    }

    private static boolean isInsideDimension(ServerLevel level, String dimension) {
        if (dimension == null || dimension.isEmpty()) return true;
        return dimension.equals(level.dimension().location().toString());
    }

    private static boolean isInsideBiome(ServerLevel level, BlockPos pos, String biome) {
        if (biome == null || biome.isEmpty()) return true;
        Holder<Biome> h = level.getBiome(pos);
        if (biome.startsWith("#")) {
            String s = biome.substring(1);
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) return false;
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, rl);
            return h.is(tag);
        }
        return h.unwrapKey().map(k -> k.location().toString().equals(biome)).orElse(false);
    }

    public static Either<ResourceKey<Structure>, TagKey<Structure>> parseStructure(String resLoc, ResourceLocation fallback) {
        if (resLoc == null || resLoc.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(resLoc.startsWith("#") ? resLoc.substring(1) : resLoc);
        if (rl == null) rl = fallback;
        return resLoc.startsWith("#")
                ? Either.right(TagKey.create(Registries.STRUCTURE, rl))
                : Either.left(ResourceKey.create(Registries.STRUCTURE, rl));
    }

    public static String formatStructure(Either<ResourceKey<Structure>, TagKey<Structure>> structure) {
        if (structure == null) return "";
        return structure.map(k -> k.location().toString(), t -> "#" + t.location());
    }
}
