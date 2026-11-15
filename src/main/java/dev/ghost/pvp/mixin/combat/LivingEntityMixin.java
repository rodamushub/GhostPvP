package dev.ghost.pvp.mixin.combat;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.combat.utility.NameProtect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void onGetName(CallbackInfoReturnable<Component> cir) {
        if (GhostClient.get() == null) return;
        if (!((LivingEntity)(Object)this).equals(dev.ghost.pvp.util.Globals.player())) return;

        var mod = GhostClient.get().getModuleManager().get("NameProtect");
        if (mod.isPresent() && mod.get().isEnabled()) {
            var np = (NameProtect) mod.get();
            cir.setReturnValue(Component.literal(np.fakeName.getValue()));
        }
    }
}

