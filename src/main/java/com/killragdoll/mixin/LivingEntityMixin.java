package com.killragdoll.mixin;

import com.killragdoll.CorpseManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // Entity event 3 = death
    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void ragdoll$onEvent(byte id, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (id == 3 && self.level() instanceof ClientLevel cl) {
            CorpseManager.onMobDeath(self, cl);
        }
    }

    // Let the crosshair pass through corpses (they don't exist on the server)
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void ragdoll$notPickable(CallbackInfoReturnable<Boolean> cir) {
        if (CorpseManager.isCorpse((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
