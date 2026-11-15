package dev.ghost.pvp.module;

public enum Category {
    CRYSTAL  ("Crystal PvP",   0xFF5555),
    SPEAR    ("Spear & Attr",  0xFF8800),
    MACE     ("Mace PvP",      0xAA00FF),
    COMBAT   ("Combat",        0xFFAA00),
    MOVEMENT ("Movement",      0x55FFFF),
    RENDER   ("Render",        0x00FF55),
    UTILITY  ("Utility",       0xFFFF55),
    CLIENT   ("Client",        0xAAAAAA);

    public final String displayName;
    public final int    color; // ARGB without alpha — renderer adds alpha

    Category(String displayName, int color) {
        this.displayName = displayName;
        this.color       = color;
    }
}

