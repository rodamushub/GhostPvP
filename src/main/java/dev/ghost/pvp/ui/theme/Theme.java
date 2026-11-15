package dev.ghost.pvp.ui.theme;

/** Holds all color/style tokens used by the ClickGUI renderer. */
public final class Theme {

    public int background;      // main panel bg
    public int backgroundDark;  // slightly darker bg for sub-areas
    public int accent;          // primary accent (enabled indicators, sliders)
    public int accentDim;       // dimmed accent for borders
    public int textPrimary;     // main text color
    public int textSecondary;   // secondary / label text
    public int toggleOn;        // enabled toggle color
    public int toggleOff;       // disabled toggle color
    public int searchBarBg;     // search field background
    public int scrollbar;       // scrollbar track color
    public float cornerRadius;  // panel/button rounding
    public float panelAlpha;    // 0-1 overall panel transparency
    public int  blurRadius;     // background blur passes (0 = no blur)

    public static Theme DARK() {
        Theme t = new Theme();
        t.background     = rgba(18, 18, 24, 240);
        t.backgroundDark = rgba(12, 12, 16, 245);
        t.accent         = rgba(100, 80, 240, 255);
        t.accentDim      = rgba(60,  50, 160, 180);
        t.textPrimary    = rgba(230, 230, 240, 255);
        t.textSecondary  = rgba(140, 140, 160, 220);
        t.toggleOn       = rgba(80, 220, 120, 255);
        t.toggleOff      = rgba(80, 80, 100, 200);
        t.searchBarBg    = rgba(28, 28, 36, 255);
        t.scrollbar      = rgba(70, 70, 90, 180);
        t.cornerRadius   = 6f;
        t.panelAlpha     = 0.95f;
        t.blurRadius     = 3;
        return t;
    }

    public static Theme ROSE() {
        Theme t = new Theme();
        t.background     = rgba(24, 14, 20, 240);
        t.backgroundDark = rgba(16, 8,  14, 245);
        t.accent         = rgba(240, 80, 120, 255);
        t.accentDim      = rgba(180, 50, 90, 180);
        t.textPrimary    = rgba(240, 220, 230, 255);
        t.textSecondary  = rgba(170, 130, 150, 220);
        t.toggleOn       = rgba(240, 100, 140, 255);
        t.toggleOff      = rgba(80, 50, 60, 200);
        t.searchBarBg    = rgba(32, 18, 26, 255);
        t.scrollbar      = rgba(140, 70, 100, 180);
        t.cornerRadius   = 8f;
        t.panelAlpha     = 0.95f;
        t.blurRadius     = 3;
        return t;
    }

    public static Theme OCEAN() {
        Theme t = new Theme();
        t.background     = rgba(10, 18, 30, 240);
        t.backgroundDark = rgba(6,  12, 20, 245);
        t.accent         = rgba(40, 160, 240, 255);
        t.accentDim      = rgba(20, 100, 180, 180);
        t.textPrimary    = rgba(200, 230, 255, 255);
        t.textSecondary  = rgba(120, 160, 200, 220);
        t.toggleOn       = rgba(40, 200, 220, 255);
        t.toggleOff      = rgba(30, 60,  90, 200);
        t.searchBarBg    = rgba(14, 24,  40, 255);
        t.scrollbar      = rgba(40, 100, 160, 180);
        t.cornerRadius   = 6f;
        t.panelAlpha     = 0.95f;
        t.blurRadius     = 3;
        return t;
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Extract channels
    public static int alpha(int color) { return (color >> 24) & 0xFF; }
    public static int red(int color)   { return (color >> 16) & 0xFF; }
    public static int green(int color) { return (color >>  8) & 0xFF; }
    public static int blue(int color)  { return  color        & 0xFF; }

    /** Multiply alpha channel by factor 0-1. */
    public static int withAlpha(int color, float factor) {
        int a = (int)(alpha(color) * factor);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** Linear interpolate between two ARGB colors. */
    public static int lerp(int a, int b, float t) {
        float r  = red(a)   + (red(b)   - red(a))   * t;
        float g  = green(a) + (green(b) - green(a)) * t;
        float bl = blue(a)  + (blue(b)  - blue(a))  * t;
        float al = alpha(a) + (alpha(b) - alpha(a)) * t;
        return rgba((int)r, (int)g, (int)bl, (int)al);
    }
}

