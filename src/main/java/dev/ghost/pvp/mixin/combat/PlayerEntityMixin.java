package dev.ghost.pvp.mixin.combat;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.combat.sword.Reach;
import dev.ghost.pvp.event.events.AttackEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        if (GhostClient.get() == null) return;
        var event = GhostClient.get().getEventBus().post(new AttackEvent(target));
        if (event.isCancelled()) ci.cancel();
    }
}

