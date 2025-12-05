package net.pixeldreamstudios.morequesttypes.mixin.accessor;

import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientSkillScreenData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
@Pseudo
@Mixin(SkillsClientMod.class)
public interface SkillsClientModAccessor {
    @Accessor("screenData")
    ClientSkillScreenData mqt$getScreenData();
}