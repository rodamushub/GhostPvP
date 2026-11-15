package dev.ghost.pvp.render;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Fullbright — applies permanent Night Vision effect client-side. */
public final class Fullbright extends Module {

    public enum FBMode { NIGHT_VISION, GAMMA }
    private final Setting<FBMode> mode = addEnum("Mode", "Fullbright method", FBMode.GAMMA);

    private float savedGamma = 1.0f;

    public Fullbright() { super("Fullbright", "Full visibility in darkness", Category.RENDER); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    @Override protected void onEnable() {
        savedGamma = (float)(double) Globals.mc.options.gamma().get();
    }

    @Override protected void onDisable() {
        if (mode.getValue() == FBMode.GAMMA) {
            Globals.mc.options.gamma().set((double) savedGamma);
        }
        if (Globals.inGame()) {
            Globals.player().removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        switch (mode.getValue()) {
            case GAMMA -> Globals.mc.options.gamma().set(16.0);
            case NIGHT_VISION -> {
                var player = Globals.player();
                var effect = player.getEffect(MobEffects.NIGHT_VISION);
                if (effect == null || effect.getDuration() < 400) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 800, 0, false, false));
                }
            }
        }
    }
}

