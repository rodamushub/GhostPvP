package dev.ghost.pvp.mixin.render;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.render.CustomCrosshair;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (GhostClient.get() == null) return;
        var mod = GhostClient.get().getModuleManager().get("CustomCrosshair");
        if (mod.isPresent() && mod.get().isEnabled()) ci.cancel();
    }
}

