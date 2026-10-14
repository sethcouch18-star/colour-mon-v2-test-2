package com.example.colormons;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = "colormons", dist = Dist.CLIENT)
public class ColormonsClient {
    public static final KeyMapping OPEN = new KeyMapping(
            "key.colormons.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.colormons");

    public ColormonsClient(IEventBus modBus) {
        ColorStore.load();
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(OPEN));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN.consumeClick()) {
                if (mc.crosshairPickEntity instanceof PokemonEntity mon) {
                    mc.setScreen(new ColorWheelScreen(mon));
                }
            }
        });
    }
}
