package com.killragdoll.mixin;

import com.killragdoll.CorpseManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ragdoll$tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        CorpseManager.tick((ClientLevel) (Object) this);
    }
}
