package com.tunnelminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class TunnelMinerMod implements ClientModInitializer {

    public static final String MOD_ID = "tunnel-miner";
    public static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tunnel_miner.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories.tunnel_miner"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open GUI when keybind is pressed
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new TunnelMinerScreen());
                }
            }

            // Delegate mining logic
            MinerTickHandler.tick(client);
        });
    }
}
