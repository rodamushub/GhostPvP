package dev.ghost.pvp.event.events;

import dev.ghost.pvp.event.Event;
import com.mojang.blaze3d.vertex.PoseStack;

public final class RenderWorldEvent extends Event {
    public final PoseStack poseStack;
    public final float     partialTick;

    public RenderWorldEvent(PoseStack poseStack, float partialTick) {
        this.poseStack   = poseStack;
        this.partialTick = partialTick;
    }
}

