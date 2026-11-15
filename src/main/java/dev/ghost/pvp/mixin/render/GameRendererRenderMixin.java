package dev.ghost.pvp.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes projection/modelview matrices for RenderUtil.worldToScreen().
 * Uses @Shadow since we can't use @Accessor on a non-interface mixin for private fields.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererRenderMixin {

    @Shadow private Matrix4f projectionMatrix;

    public Matrix4f ghost_getProjectionMatrix() { return projectionMatrix; }
}

