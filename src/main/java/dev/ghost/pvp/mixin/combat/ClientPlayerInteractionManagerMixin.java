package dev.ghost.pvp.mixin.combat;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.combat.sword.Reach;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Shadow private int blockHitDelay;

    // FastPlace: zero the placement delay every tick when module is enabled
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (GhostClient.get() == null) return;
        var fp = GhostClient.get().getModuleManager().get("FastPlace");
        if (fp.isPresent() && fp.get().isEnabled()) {
            blockHitDelay = 0;
        }
    }

    // Reach: override the entity interaction range
    // In MC 26.x the method name is getInteractDistance; fallback: attackRange field
    @Inject(method = "getPickRange", at = @At("RETURN"), cancellable = true)
    private float modifyPickRange(float original) {
        if (GhostClient.get() == null) return original;
        var reachMod = GhostClient.get().getModuleManager().get("Reach");
        if (reachMod.isPresent() && reachMod.get().isEnabled()) {
            return ((Reach) reachMod.get()).reach.getValue().floatValue();
        }
        return original;
    }
}

