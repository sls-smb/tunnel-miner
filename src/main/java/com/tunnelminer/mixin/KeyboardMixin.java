package com.tunnelminer.mixin;

import com.tunnelminer.MinerState;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects physical key presses so that TunnelMiner can stop when the player
 * touches the keyboard (anti-detection / safety measure).
 */
@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void tunnelMiner_onKey(long window, int key, int scancode, int action, int modifiers,
                                   CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) {
            MinerState.keyPressedFlag = true;
        }
    }
}
