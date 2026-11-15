package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;

/**
 * NameProtect — replaces your username with a custom string in all client-side renders.
 * Applied via LivingEntityMixin (getName override).
 * Does NOT change the name sent to the server.
 */
public final class NameProtect extends Module {

    public final Setting<String> fakeName;

    public NameProtect() {
        super("NameProtect", "Hides your username in client-side renders", Category.UTILITY);
        fakeName = new Setting<>("Fake Name", "Name displayed in client UI", "Player");
        settings.add(fakeName);
    }
}

