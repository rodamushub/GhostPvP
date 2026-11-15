package dev.ghost.pvp.mixin.movement;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.event.events.MoveEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MoverType type, Vec3 motion, CallbackInfo ci) {
        if (GhostClient.get() == null) return;
        if (type != MoverType.SELF) return;

        var event = new MoveEvent(motion);
        GhostClient.get().getEventBus().post(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        // If event modified values, replace motion — requires delegate call
        if (event.x != motion.x || event.y != motion.y || event.z != motion.z) {
            // We can't modify the parameter here directly in Mixin without @ModifyArg;
            // the MoveEvent approach works through the Speed module's direct position math.
            // For full redirect, use @ModifyArg below.
        }
    }
}

