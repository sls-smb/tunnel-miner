package com.tunnelminer.mixin;

import com.tunnelminer.MinerState;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    private double tunnelMiner_lastCursorX = Double.MIN_VALUE;
    private double tunnelMiner_lastCursorY = Double.MIN_VALUE;

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (!MinerState.getInstance().isRunning()) {
            tunnelMiner_lastCursorX = x;
            tunnelMiner_lastCursorY = y;
            return;
        }

        if (tunnelMiner_lastCursorX == Double.MIN_VALUE) {
            tunnelMiner_lastCursorX = x;
            tunnelMiner_lastCursorY = y;
            return;
        }

        double dx = x - tunnelMiner_lastCursorX;
        double dy = y - tunnelMiner_lastCursorY;
        double distSq = dx * dx + dy * dy;

        if (distSq > 4.0) { // > 2 pixels movement
            MinerState.mouseMoved = true;
        }

        tunnelMiner_lastCursorX = x;
        tunnelMiner_lastCursorY = y;
    }
}
