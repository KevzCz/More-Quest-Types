package net.pixeldreamstudios.morequesttypes.mixin.client.accessor;

import dev.ftb. mods.ftbquests. client.gui.quests.TaskButton;
import dev.ftb.mods.ftbquests.quest.task.Task;
import org.spongepowered.asm. mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TaskButton.class, remap = false)
public interface TaskButtonAccessor {

    @Accessor("task")
    Task mqt$getTask();
}