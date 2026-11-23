package com.killragdoll.mixin;

import com.killragdoll.CorpseRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements CorpseRenderState {
    @Unique private boolean ragdoll$active;
    @Unique private float ragdoll$roll, ragdoll$pitch, ragdoll$width, ragdoll$height;

    @Override
    public void ragdoll$set(boolean active, float roll, float pitch, float width, float height) {
        this.ragdoll$active = active;
        this.ragdoll$roll = roll;
        this.ragdoll$pitch = pitch;
        this.ragdoll$width = width;
        this.ragdoll$height = height;
    }

    @Override public boolean ragdoll$active() { return ragdoll$active; }
    @Override public float ragdoll$roll() { return ragdoll$roll; }
    @Override public float ragdoll$pitch() { return ragdoll$pitch; }
    @Override public float ragdoll$width() { return ragdoll$width; }
    @Override public float ragdoll$height() { return ragdoll$height; }
}
