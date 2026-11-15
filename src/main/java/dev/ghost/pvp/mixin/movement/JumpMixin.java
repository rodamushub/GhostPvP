package dev.ghost.pvp.mixin.movement;

import dev.ghost.pvp.GhostClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class JumpMixin {

    /**
     * Zero the jump delay each tick.
     * In MC 26.x the field may be named jumpDelay or noJumpTicks; both are
     * set to 10 on landing. We inject at HEAD of aiStep and reset via reflection
     * to cover either name without depending on a specific mapping.
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        if (GhostClient.get() == null) return;
        var mod = GhostClient.get().getModuleManager().get("NoJumpDelay");
        if (mod.isEmpty() || !mod.get().isEnabled()) return;

        try {
            // Try common field names; actual name is per-build — per-build derivation required
            Class<?> cls = this.getClass().getSuperclass().getSuperclass(); // LivingEntity or Player
            for (String name : new String[]{"jumpDelay", "noJumpTicks", "jumpRidingTicks"}) {
                try {
                    var field = cls.getDeclaredField(name);
                    field.setAccessible(true);
                    field.setInt(this, 0);
                    return;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception ignored) {}
    }
}

