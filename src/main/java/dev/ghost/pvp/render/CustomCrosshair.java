package dev.ghost.pvp.render;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;

/**
 * CustomCrosshair — renders a custom crosshair style.
 * Applied via InGameHudMixin which suppresses vanilla crosshair when enabled.
 * Actual draw call is in HudRenderer.
 */
public final class CustomCrosshair extends Module {

    public enum CrosshairStyle { CROSS, DOT, CIRCLE, DYNAMIC }

    public final Setting<CrosshairStyle> style   = addEnum("Style", "Crosshair shape", CrosshairStyle.CROSS);
    public final Setting<Integer>        size     = addInt("Size", "Crosshair size in px", 5, 1, 20);
    public final Setting<Integer>        gap      = addInt("Gap", "Center gap", 2, 0, 10);
    public final Setting<Integer>        colorR   = addInt("Red",   "Red channel",   255, 0, 255);
    public final Setting<Integer>        colorG   = addInt("Green", "Green channel", 255, 0, 255);
    public final Setting<Integer>        colorB   = addInt("Blue",  "Blue channel",  255, 0, 255);
    public final Setting<Integer>        alpha    = addInt("Alpha", "Opacity",        255, 0, 255);

    public CustomCrosshair() { super("CustomCrosshair", "Replaces vanilla crosshair with custom style", Category.RENDER); }
}

