package net.pixeldreamstudios.morequesttypes.mixin.accessor;

import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.data.ClientSkillScreenData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
@Pseudo
@Mixin(ClientSkillScreenData.class)
public interface ClientSkillScreenDataAccessor {
    @Invoker("getCategory")
    Optional<ClientCategoryData> mqt$getCategory(ResourceLocation categoryId);
}