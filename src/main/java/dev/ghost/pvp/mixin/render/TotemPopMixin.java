package dev.ghost.pvp.mixin.render;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.ui.hud.HudRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class TotemPopMixin {

    // Event ID 35 = totem of undying activation
    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onEntityEvent(byte eventId, CallbackInfo ci) {
        if (eventId == 35 && GhostClient.get() != null) {
            GhostClient.get().getHudRenderer().incrementTotemPops();
        }
    }
}

