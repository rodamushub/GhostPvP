package dev.ghost.pvp.render;

import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;

/**
 * NoExplosionParticles — cancels explosion particle spawning.
 * Applied via ParticleManagerMixin which checks this module's enabled state.
 */
public final class NoExplosionParticles extends Module {
    public NoExplosionParticles() {
        super("NoExplosionParticles", "Removes explosion particle spam for clarity", Category.RENDER);
    }
}

