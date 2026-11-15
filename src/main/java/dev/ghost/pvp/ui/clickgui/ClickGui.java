package dev.ghost.pvp.ui.clickgui;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.config.ConfigManager;
import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.module.ModuleManager;
import dev.ghost.pvp.ui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * ClickGUI — full-featured draggable panel UI.
 *
 * Layout:
 *   Top bar: title + search bar + theme selector + profile selector
 *   Per-category panels: draggable, scrollable module list
 *   Module row: name + toggle | click to expand settings
 *   Expanded settings: sliders (double/int), boolean toggles, enum dropdowns, keybind capture
 *
 * Animations: panel open/close alpha fade (10 ticks), module expand height lerp.
 */
public final class ClickGui extends Screen {

    // ── Layout constants ───────────────────────────────────────────────────────
    private static final int PANEL_W        = 160;
    private static final int HEADER_H       = 20;
    private static final int MODULE_ROW_H   = 18;
    private static final int SETTING_ROW_H  = 16;
    private static final int PANEL_GAP      = 8;
    private static final int TOP_BAR_H      = 28;
    private static final int SEARCH_W       = 200;

    // ── State ──────────────────────────────────────────────────────────────────
    private final ModuleManager  mm;
    private final ConfigManager  cfg;

    private Theme   theme       = Theme.DARK();
    private String  searchQuery = "";
    private boolean searchFocus = false;

    // Per-panel drag/scroll state
    private final Map<Category, PanelState> panels = new LinkedHashMap<>();

    // Expanded module settings
    private Module expandedModule = null;

    // Keybind capture
    private Module bindTarget  = null;

    // Open/close animation (alpha 0→1)
    private float openAnim     = 0f;

    // Profile dropdown
    private boolean profileDropOpen = false;

    // Theme dropdown
    private boolean themeDropOpen   = false;

    // Available themes
    private static final Theme[] THEMES  = { Theme.DARK(), Theme.ROSE(), Theme.OCEAN() };
    private static final String[] THEME_NAMES = { "Dark", "Rose", "Ocean" };
    private int activeTheme = 0;

    public ClickGui(ModuleManager mm, ConfigManager cfg) {
        super(Component.literal("GhostPvP"));
        this.mm  = mm;
        this.cfg = cfg;

        // Initialize panel positions in a grid
        int col = 0, row = 0;
        int startX = 10;
        int startY = TOP_BAR_H + 10;

        for (Category cat : Category.values()) {
            PanelState ps = new PanelState();
            ps.x = startX + col * (PANEL_W + PANEL_GAP);
            ps.y = startY + row * 24; // slight vertical stagger
            ps.open = true;
            panels.put(cat, ps);
            col++;
            if (col >= 5) { col = 0; row++; }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        openAnim = Math.min(1f, openAnim + delta * 0.15f);

        renderBackground(g, mouseX, mouseY, delta);
        renderTopBar(g, mouseX, mouseY);
        renderPanels(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        // Dim world behind GUI
        g.fill(0, 0, width, height, Theme.withAlpha(0x000000, 0.45f * openAnim));
    }

    private void renderTopBar(GuiGraphics g, int mx, int my) {
        int barColor = Theme.withAlpha(theme.backgroundDark, openAnim);
        g.fill(0, 0, width, TOP_BAR_H, barColor);

        // Title
        GuiRenderer.drawString(g, font, "§bGhost§fPvP", 6, (TOP_BAR_H - font.lineHeight) / 2, theme.textPrimary);

        // Search bar
        int sx = width / 2 - SEARCH_W / 2;
        int sy = (TOP_BAR_H - 14) / 2;
        GuiRenderer.roundedRect(g, sx, sy, SEARCH_W, 14, 4f, theme.searchBarBg);
        String searchDisplay = searchQuery.isEmpty() && !searchFocus
            ? "§7Search modules..."
            : searchQuery + (searchFocus ? "§f|" : "");
        GuiRenderer.drawString(g, font, searchDisplay, sx + 4, sy + (14 - font.lineHeight) / 2, theme.textPrimary);

        // Theme button
        int tx = width - 160;
        GuiRenderer.roundedRect(g, tx, 4, 50, TOP_BAR_H - 8, 4f, theme.accentDim);
        GuiRenderer.drawCenteredString(g, font, "Theme: " + THEME_NAMES[activeTheme], tx + 25, (TOP_BAR_H - font.lineHeight) / 2, theme.textPrimary);

        // Profile button
        int px = width - 104;
        GuiRenderer.roundedRect(g, px, 4, 60, TOP_BAR_H - 8, 4f, theme.accentDim);
        GuiRenderer.drawCenteredString(g, font, "§7Profile: §f" + cfg.getCurrentProfile(), px + 30, (TOP_BAR_H - font.lineHeight) / 2, theme.textPrimary);

        // Save button
        int saveX = width - 40;
        GuiRenderer.roundedRect(g, saveX, 4, 36, TOP_BAR_H - 8, 4f, theme.accent);
        GuiRenderer.drawCenteredString(g, font, "Save", saveX + 18, (TOP_BAR_H - font.lineHeight) / 2, theme.textPrimary);
    }

    private void renderPanels(GuiGraphics g, int mx, int my) {
        List<Module> searchResults = searchQuery.isEmpty() ? null : mm.search(searchQuery);

        for (Map.Entry<Category, PanelState> entry : panels.entrySet()) {
            Category   cat = entry.getKey();
            PanelState ps  = entry.getValue();

            List<Module> modules = searchResults != null
                ? searchResults.stream().filter(m -> m.getCategory() == cat).toList()
                : mm.getByCategory(cat);

            if (searchResults != null && modules.isEmpty()) continue;

            renderPanel(g, mx, my, cat, ps, modules);
        }
    }

    private void renderPanel(GuiGraphics g, int mx, int my,
                              Category cat, PanelState ps, List<Module> modules) {
        int panelH = HEADER_H + (ps.open ? computePanelBodyH(modules) : 0);
        panelH = (int) lerp(ps.open ? HEADER_H : panelH, ps.open ? panelH : HEADER_H, ps.anim);
        ps.anim = clamp01(ps.anim + (ps.open ? 0.15f : -0.15f));

        // Panel background
        int bgColor = Theme.withAlpha(theme.background, openAnim);
        GuiRenderer.roundedRect(g, ps.x, ps.y, PANEL_W, panelH, theme.cornerRadius, bgColor);

        // Header
        int[] catCol = RGBfromHex(cat.color);
        int headerColor = Theme.withAlpha(asARGB(catCol[0], catCol[1], catCol[2], 180), openAnim);
        GuiRenderer.roundedRect(g, ps.x, ps.y, PANEL_W, HEADER_H, theme.cornerRadius, headerColor);

        // Category name + arrow
        String arrow = ps.open ? "▼" : "▶";
        GuiRenderer.drawString(g, font, arrow + " " + cat.displayName,
            ps.x + 5, ps.y + (HEADER_H - font.lineHeight) / 2, theme.textPrimary);

        if (!ps.open) return;

        // Module rows with scissor clipping
        GuiRenderer.enableScissor(g, ps.x, ps.y + HEADER_H, PANEL_W, panelH - HEADER_H);

        int rowY = ps.y + HEADER_H - ps.scrollOffset;
        for (Module m : modules) {
            renderModuleRow(g, mx, my, m, ps.x, rowY);
            rowY += MODULE_ROW_H;

            if (expandedModule == m) {
                rowY += renderSettingsRows(g, mx, my, m, ps.x, rowY);
            }
        }

        GuiRenderer.disableScissor(g);

        // Scrollbar
        int totalH    = computePanelBodyH(modules);
        int visibleH  = panelH - HEADER_H;
        if (totalH > visibleH) {
            float ratio    = (float) visibleH / totalH;
            int   sbH      = (int)(visibleH * ratio);
            int   sbY      = ps.y + HEADER_H + (int)((float) ps.scrollOffset / totalH * visibleH);
            g.fill(ps.x + PANEL_W - 3, sbY, ps.x + PANEL_W - 1, sbY + sbH, theme.scrollbar);
        }
    }

    private void renderModuleRow(GuiGraphics g, int mx, int my, Module m, int x, int y) {
        boolean hovered = mx >= x && mx < x + PANEL_W && my >= y && my < y + MODULE_ROW_H;
        int rowBg = hovered ? Theme.withAlpha(theme.accentDim, 0.5f) : 0;
        if (rowBg != 0) g.fill(x, y, x + PANEL_W, y + MODULE_ROW_H, rowBg);

        // Name
        int nameColor = m.isEnabled() ? theme.textPrimary : theme.textSecondary;
        GuiRenderer.drawString(g, font, m.getName(), x + 5, y + (MODULE_ROW_H - font.lineHeight) / 2, nameColor);

        // Toggle
        GuiRenderer.drawToggle(g, x + PANEL_W - 30, y + (MODULE_ROW_H - 12) / 2, m.isEnabled(), theme);

        // Keybind hint
        if (m.getKeybind() >= 0) {
            String kb = keyName(m.getKeybind());
            int kbW   = font.width(kb);
            GuiRenderer.drawString(g, font, "§7" + kb,
                x + PANEL_W - 34 - kbW - 2, y + (MODULE_ROW_H - font.lineHeight) / 2, theme.textSecondary);
        }

        // Binding indicator
        if (bindTarget == m) {
            g.fill(x, y, x + PANEL_W, y + MODULE_ROW_H, Theme.withAlpha(theme.accent, 0.3f));
            GuiRenderer.drawCenteredString(g, font, "Press key...", x + PANEL_W / 2, y + (MODULE_ROW_H - font.lineHeight) / 2, theme.accent);
        }
    }

    private int renderSettingsRows(GuiGraphics g, int mx, int my, Module m, int x, int y) {
        int drawn = 0;
        for (Setting<?> s : m.getSettings()) {
            int rowY = y + drawn;
            g.fill(x, rowY, x + PANEL_W, rowY + SETTING_ROW_H, Theme.withAlpha(theme.backgroundDark, 0.8f));

            Object val = s.getValue();
            if (val instanceof Boolean b) {
                GuiRenderer.drawString(g, font, "  " + s.getName(), x + 4, rowY + (SETTING_ROW_H - font.lineHeight) / 2, theme.textSecondary);
                GuiRenderer.drawToggle(g, x + PANEL_W - 30, rowY + (SETTING_ROW_H - 12) / 2, b, theme);
            } else if (s instanceof Setting.IntSetting is) {
                GuiRenderer.drawSlider(g, font, x + 4, rowY + 1, PANEL_W - 8, SETTING_ROW_H - 2,
                    (double)(int) s.getValue(), is.min, is.max, s.getName(), theme);
            } else if (s instanceof Setting.DoubleSetting ds) {
                GuiRenderer.drawSlider(g, font, x + 4, rowY + 1, PANEL_W - 8, SETTING_ROW_H - 2,
                    (double) s.getValue(), ds.min, ds.max, s.getName(), theme);
            } else if (val instanceof Enum<?> en) {
                String enumStr = s.getName() + ": " + en.name();
                GuiRenderer.drawString(g, font, "  " + enumStr, x + 4, rowY + (SETTING_ROW_H - font.lineHeight) / 2, theme.textSecondary);
            } else if (val instanceof String str) {
                GuiRenderer.drawString(g, font, "  " + s.getName() + ": " + str, x + 4, rowY + (SETTING_ROW_H - font.lineHeight) / 2, theme.textSecondary);
            }

            drawn += SETTING_ROW_H;
        }

        // Keybind row
        {
            int rowY = y + drawn;
            g.fill(x, rowY, x + PANEL_W, rowY + SETTING_ROW_H, Theme.withAlpha(theme.backgroundDark, 0.9f));
            String bindStr = "  Keybind: " + (m.getKeybind() < 0 ? "NONE" : keyName(m.getKeybind()));
            GuiRenderer.drawString(g, font, bindStr, x + 4, rowY + (SETTING_ROW_H - font.lineHeight) / 2, theme.textSecondary);
            drawn += SETTING_ROW_H;
        }

        return drawn;
    }

    // ── Mouse ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx, imy = (int) my;

        // Top bar: search
        int sx = width / 2 - SEARCH_W / 2;
        if (imx >= sx && imx < sx + SEARCH_W && imy >= 0 && imy < TOP_BAR_H) {
            searchFocus = true; return true;
        } else {
            searchFocus = false;
        }

        // Theme button
        int tx = width - 160;
        if (imx >= tx && imx < tx + 50 && imy >= 4 && imy < TOP_BAR_H - 4) {
            activeTheme = (activeTheme + 1) % THEMES.length;
            theme = THEMES[activeTheme];
            return true;
        }

        // Profile button
        int px = width - 104;
        if (imx >= px && imx < px + 60 && imy >= 4 && imy < TOP_BAR_H - 4) {
            List<String> profiles = GhostClient.get().getConfigManager().listProfiles();
            int cur = profiles.indexOf(cfg.getCurrentProfile());
            if (!profiles.isEmpty()) {
                String next = profiles.get((cur + 1) % profiles.size());
                cfg.load(mm, next);
            }
            return true;
        }

        // Save button
        int saveX = width - 40;
        if (imx >= saveX && imx < saveX + 36 && imy >= 4 && imy < TOP_BAR_H - 4) {
            cfg.save(mm);
            GhostClient.get().getNotificationManager().push("Config", "Saved to " + cfg.getCurrentProfile(), 3000);
            return true;
        }

        // Panels
        for (Map.Entry<Category, PanelState> entry : panels.entrySet()) {
            PanelState ps = entry.getValue();
            Category   cat = entry.getKey();
            int panelH = computeFullPanelH(cat);

            // Header click → toggle panel
            if (imx >= ps.x && imx < ps.x + PANEL_W && imy >= ps.y && imy < ps.y + HEADER_H) {
                if (button == 0) { ps.open = !ps.open; return true; }
            }

            if (!ps.open) continue;

            // Module row click
            int rowY = ps.y + HEADER_H - ps.scrollOffset;
            for (Module m : mm.getByCategory(cat)) {
                if (imx >= ps.x && imx < ps.x + PANEL_W && imy >= rowY && imy < rowY + MODULE_ROW_H) {
                    if (bindTarget != null) { /* handled via keyboard */ }
                    else if (button == 0) {
                        m.toggle();
                    } else if (button == 1) {
                        // Right click → expand/collapse settings
                        expandedModule = (expandedModule == m) ? null : m;
                    } else if (button == 2) {
                        // Middle click → start keybind capture
                        bindTarget = m;
                    }
                    return true;
                }
                rowY += MODULE_ROW_H;

                if (expandedModule == m) {
                    // Settings click
                    for (Setting<?> s : m.getSettings()) {
                        int sRowY = rowY;
                        if (imx >= ps.x && imx < ps.x + PANEL_W && imy >= sRowY && imy < sRowY + SETTING_ROW_H) {
                            handleSettingClick(s, imx, ps.x, button);
                            return true;
                        }
                        rowY += SETTING_ROW_H;
                    }
                    // Keybind row
                    if (imx >= ps.x && imx < ps.x + PANEL_W && imy >= rowY && imy < rowY + SETTING_ROW_H) {
                        bindTarget = m;
                        return true;
                    }
                    rowY += SETTING_ROW_H;
                }
            }

            // Panel drag (not on any child)
            if (imx >= ps.x && imx < ps.x + PANEL_W && imy >= ps.y && imy < ps.y + panelH) {
                ps.dragging = true;
                ps.dragOffX = imx - ps.x;
                ps.dragOffY = imy - ps.y;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        panels.values().forEach(ps -> ps.dragging = false);
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        for (PanelState ps : panels.values()) {
            if (ps.dragging) {
                ps.x = (int)(mx - ps.dragOffX);
                ps.y = (int)(my - ps.dragOffY);
                // Clamp to screen
                ps.x = Math.max(0, Math.min(ps.x, width - PANEL_W));
                ps.y = Math.max(TOP_BAR_H, ps.y);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hDelta, double vDelta) {
        for (Map.Entry<Category, PanelState> entry : panels.entrySet()) {
            PanelState ps = entry.getValue();
            int ph = computeFullPanelH(entry.getKey());
            if ((int)mx >= ps.x && (int)mx < ps.x + PANEL_W
             && (int)my >= ps.y && (int)my < ps.y + ph && ps.open) {
                ps.scrollOffset = Math.max(0, ps.scrollOffset - (int)(vDelta * 8));
                return true;
            }
        }
        return false;
    }

    // ── Keyboard ───────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keybind capture
        if (bindTarget != null) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                mm.bindKey(bindTarget, -1);
            } else {
                mm.bindKey(bindTarget, keyCode);
            }
            bindTarget = null;
            return true;
        }

        // Search input
        if (searchFocus) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                searchFocus = false;
                searchQuery = "";
                return true;
            }
        }

        // Close on Escape
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && chr >= 32) {
            searchQuery += chr;
            return true;
        }
        return false;
    }

    // ── Settings interaction ───────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleSettingClick(Setting<?> s, int mx, int panelX, int button) {
        Object val = s.getValue();

        if (val instanceof Boolean b) {
            ((Setting<Boolean>) s).setValue(!b);

        } else if (s instanceof Setting.IntSetting is) {
            int delta = button == 0 ? 1 : -1;
            is.setValue((int) is.getValue() + delta);

        } else if (s instanceof Setting.DoubleSetting ds) {
            double delta = button == 0 ? 0.1 : -0.1;
            ds.setValue((double) ds.getValue() + delta);

        } else if (val instanceof Enum en) {
            Class<? extends Enum> ec = en.getDeclaringClass();
            Enum[] constants = ec.getEnumConstants();
            int idx = (en.ordinal() + 1) % constants.length;
            ((Setting) s).setValue(constants[idx]);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private int computePanelBodyH(List<Module> modules) {
        int h = 0;
        for (Module m : modules) {
            h += MODULE_ROW_H;
            if (expandedModule == m) {
                h += m.getSettings().size() * SETTING_ROW_H + SETTING_ROW_H; // +1 keybind row
            }
        }
        return h;
    }

    private int computeFullPanelH(Category cat) {
        List<Module> modules = mm.getByCategory(cat);
        PanelState ps = panels.get(cat);
        return HEADER_H + (ps != null && ps.open ? computePanelBodyH(modules) : 0);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static int   asARGB(int r, int g, int b, int a) { return (a<<24)|(r<<16)|(g<<8)|b; }

    private static int[] RGBfromHex(int hex) {
        return new int[]{ (hex>>16)&0xFF, (hex>>8)&0xFF, hex&0xFF };
    }

    private String keyName(int key) {
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase() : "KEY" + key;
    }

    // ── PanelState ─────────────────────────────────────────────────────────────

    private static final class PanelState {
        int   x, y;
        boolean open    = true;
        float   anim    = 1f;
        boolean dragging = false;
        int     dragOffX, dragOffY;
        int     scrollOffset = 0;
    }
}

