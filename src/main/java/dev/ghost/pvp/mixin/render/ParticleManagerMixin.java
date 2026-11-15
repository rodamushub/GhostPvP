package dev.ghost.pvp.mixin.render;

import dev.ghost.pvp.GhostClient;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private <T extends ParticleOptions> void onCreateParticle(T params, double x, double y, double z,
                                                               double vx, double vy, double vz, CallbackInfo ci) {
        if (GhostClient.get() == null) return;
        var mod = GhostClient.get().getModuleManager().get("NoExplosionParticles");
        if (mod.isEmpty() || !mod.get().isEnabled()) return;

        var type = params.getType();
        if (type == ParticleTypes.EXPLOSION
         || type == ParticleTypes.EXPLOSION_EMITTER
         || type == ParticleTypes.LARGE_SMOKE
         || type == ParticleTypes.SMOKE) {
            ci.cancel();
        }
    }
}

