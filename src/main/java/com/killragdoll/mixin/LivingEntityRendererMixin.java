package com.killragdoll.mixin;

import com.killragdoll.CorpseManager;
import com.killragdoll.CorpseRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL"))
    private void ragdoll$extract(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        CorpseRenderState cs = (CorpseRenderState) state;
        CorpseManager.Corpse c = CorpseManager.get(entity);
        if (c == null) {
            cs.ragdoll$set(false, 0, 0, 0, 0);
            return;
        }
        cs.ragdoll$set(true,
            CorpseManager.lerp(partialTick, c.prevRoll, c.roll),
            CorpseManager.lerp(partialTick, c.prevPitch, c.pitch),
            entity.getBbWidth(),
            entity.getBbHeight());
    }

    @Inject(
        method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
        at = @At("TAIL"))
    private void ragdoll$rotate(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        CorpseRenderState cs = (CorpseRenderState) state;
        if (!cs.ragdoll$active()) return;

        float h = cs.ragdoll$height();
        float w = cs.ragdoll$width();
        float roll = cs.ragdoll$roll();
        float drop = Math.max(0f, h / 2f - w / 2f) * (float) Math.abs(Math.sin(Math.toRadians(roll)));

        poseStack.translate(0.0, -drop, 0.0);
        poseStack.translate(0.0, h / 2.0, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(cs.ragdoll$pitch()));
        poseStack.translate(0.0, -h / 2.0, 0.0);
    }
}
