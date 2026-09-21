package com.whokillme;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class WhoKillMeClient implements ClientModInitializer {

    public static KeyBinding openKey;
    private int ticks = 0;

    @Override
    public void onInitializeClient() {
        WhoKillMeData.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.whokillme.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.whokillme"
        ));

        ClientReceiveMessageEvents.GAME.register(DeathTracker::onMessage);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DeathTracker.clearKnown());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (openKey.wasPressed()) {
                DeathTracker.refreshKnown();
                client.setScreen(new WhoKillMeScreen());
            }
            ticks++;
            if (ticks % 40 == 0) {
                DeathTracker.refreshKnown();
            }
        });

        HudRenderCallback.EVENT.register(WhoKillMeHud::render);
    }
}
