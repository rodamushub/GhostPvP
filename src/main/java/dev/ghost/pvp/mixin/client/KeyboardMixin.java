package dev.ghost.pvp.mixin.client;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.event.events.KeyPressEvent;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (GhostClient.get() != null) {
            GhostClient.get().getEventBus().post(new KeyPressEvent(key, action));
        }
    }
}

