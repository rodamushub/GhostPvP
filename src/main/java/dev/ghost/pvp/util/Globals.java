package dev.ghost.pvp.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

/** Thin static cache — updated each tick by the client entrypoint. */
public final class Globals {

    public static Minecraft    mc;

    private Globals() {}

    public static LocalPlayer player()  { return mc == null ? null : mc.player; }
    public static Level        world()  { return mc == null ? null : mc.level; }
    public static boolean      inGame() { return mc != null && mc.player != null && mc.level != null; }
}

