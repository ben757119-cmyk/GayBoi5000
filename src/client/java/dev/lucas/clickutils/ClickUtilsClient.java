package dev.lucas.clickutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClickUtilsClient implements ClientModInitializer {
    public static final String MOD_ID = "clickutils";
    public static final String BRAND = "GayBoi5000";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clickutils.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                client.gui.setScreen(new ClickUtilsScreen());
            }
        });
    }
}
