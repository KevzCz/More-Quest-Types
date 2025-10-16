package net.pixeldreamstudios.morequesttypes.accessor;

import java.util.UUID;

public interface LivingEntityLastDamageAccess {
    UUID  mqt$getLastDamageAttacker();
    float mqt$getLastDamageBaselineAmount();
    float mqt$getLastDamageFinalAmount();
    float mqt$getLastDamagePrevHealth();
    float mqt$getLastDamageNewHealth();
    int   mqt$getLastDamageSeq();
}
