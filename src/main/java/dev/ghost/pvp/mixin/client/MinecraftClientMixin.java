package dev.ghost.pvp.mixin.client;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.event.events.KeyPressEvent;
import dev.ghost.pvp.util.Globals;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(boolean renderLevel, CallbackInfo ci) {
        Globals.mc = (Minecraft)(Object)this;
    }
}

