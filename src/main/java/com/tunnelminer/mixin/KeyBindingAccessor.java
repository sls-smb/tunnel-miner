package com.tunnelminer.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code pressed} field on {@link KeyBinding} so that
 * TunnelMiner can simulate a continuously-held key without relying on LWJGL
 * input events.
 */
@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {

    @Accessor("pressed")
    void setPressed(boolean pressed);

    @Accessor("pressed")
    boolean getPressed();
}
