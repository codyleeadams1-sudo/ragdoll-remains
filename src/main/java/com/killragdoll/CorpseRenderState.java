package com.killragdoll;

/** Duck interface added to LivingEntityRenderState. */
public interface CorpseRenderState {
    void ragdoll$set(boolean active, float roll, float pitch, float width, float height);
    boolean ragdoll$active();
    float ragdoll$roll();
    float ragdoll$pitch();
    float ragdoll$width();
    float ragdoll$height();
}
