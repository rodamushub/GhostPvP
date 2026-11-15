package dev.ghost.pvp.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.event.events.RenderWorldEvent;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevel(float partialTick, long finishNanoTime, PoseStack poseStack, CallbackInfo ci) {
        if (GhostClient.get() != null) {
            GhostClient.get().getEventBus().post(new RenderWorldEvent(poseStack, partialTick));
        }
    }
}

