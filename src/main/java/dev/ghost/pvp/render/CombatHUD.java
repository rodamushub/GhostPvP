package dev.ghost.pvp.render;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;

/**
 * CombatHUD — in-game overlay drawn by HudRenderer:
 *   - Totem pop counter
 *   - Armor durability bars
 *   - Target health bar with name
 *   - Crystal damage preview
 *   - Combo/hit counter
 * This class holds settings; rendering is in HudRenderer.renderCombatHUD().
 */
public final class CombatHUD extends Module {

    public final Setting<Boolean> totemCounter   = addBool("Totem Counter",   "Show your totem pop count",    true);
    public final Setting<Boolean> armorDurability= addBool("Armor Durability","Show armor durability bars",   true);
    public final Setting<Boolean> targetHealth   = addBool("Target Health",   "Show target HP bar",           true);
    public final Setting<Boolean> crystalPreview = addBool("Crystal Preview", "Show best crystal damage",     true);
    public final Setting<Boolean> comboCounter   = addBool("Combo Counter",   "Show consecutive hits",        true);
    public final Setting<Integer> posX           = addInt("X Position",       "HUD X offset",    10, 0, 1920);
    public final Setting<Integer> posY           = addInt("Y Position",       "HUD Y offset",    10, 0, 1080);

    public CombatHUD() { super("CombatHUD", "In-game combat information overlay", Category.RENDER); }
}

