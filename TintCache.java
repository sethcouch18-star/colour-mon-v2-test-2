package com.example.colormons;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.*;

public class TintCache {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    public static ResourceLocation getOrCreate(UUID id, String species, ResourceLocation original, MonColors mc) {
        String key = id + "|" + original + "|" + Arrays.toString(mc.colors) + "|" + mc.finish + "|"
                + mc.keepDark + "|" + mc.overrides.size();
        ResourceLocation hit = CACHE.get(key);
        if (hit != null) return hit;

        NativeImage img = read(original);
        if (img == null) return original;

        NativeImage mask = read(ResourceLocation.fromNamespaceAndPath("colormons", "masks/" + species + ".png"));
        int[] group = TextureAnalyzer.analyse(img, mask);
        if (mask != null) mask.close();

        for (int[] ov : mc.overrides) group[ov[0]] = ov[1];

        int w = img.getWidth();
        // per-group average brightness, used to keep the original shading inside a group
        float[] gLum = new float[TextureAnalyzer.GROUP_COUNT];
        int[] gN = new int[TextureAnalyzer.GROUP_COUNT];
        for (int i = 0; i < group.length; i++) {
            int g = group[i];
            if (g < 0) continue;
            gLum[g] += TextureAnalyzer.lum(img, i % w, i / w); gN[g]++;
        }
        for (int g = 0; g < 6; g++) gLum[g] = gN[g] > 0 ? gLum[g] / gN[g] : 1f;

        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < w; x++) {
            int g = group[y * w + x];
            if (g < 0) continue;
            int p = img.getPixelRGBA(x, y);
            int a = (p >>> 24) & 0xFF;
            float l = TextureAnalyzer.lum(img, x, y);
            if (mc.keepDark && l < 0.12f) continue;

            if (mc.colors[g] >= 0) {
                float shade = l / Math.max(gLum[g], 0.05f);
                int t = mc.colors[g];
                int r = clamp(((t >> 16) & 0xFF) * shade);
                int gr = clamp(((t >> 8) & 0xFF) * shade);
                int b = clamp((t & 0xFF) * shade);
                img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (gr << 8) | r);
            } else if (mc.keepDark && l < 0.12f) {
                continue;
            } else {
                applyFinish(img, x, y, p, a, l, mc.finish, shadeOnly(gLum, g, l));
            }
        }

        if (mc.finish == 1 || mc.finish == 2) {
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < w; x++) {
                int g = group[y * w + x];
                int p = img.getPixelRGBA(x, y);
                int a = (p >>> 24) & 0xFF;
                if (a == 0) continue;
                if (mc.keepDark && TextureAnalyzer.lum(img, x, y) < 0.12f) continue;
                if (mc.colors[g] < 0 || g < 0) continue;   // already handled below for tinted pixels
                applyFinish(img, x, y, p, a, TextureAnalyzer.lum(img, x, y), mc.finish, 1f);
            }
        }

        ResourceLocation out = Minecraft.getInstance().getTextureManager().register(
                "colormons_" + id.toString().replace("-", ""), new DynamicTexture(img));
        CACHE.put(key, out);
        return out;
    }

    private static float shadeOnly(float[] gLum, int g, float l) {
        return l / Math.max(gLum[g], 0.05f);
    }

    /** Glossy = punchy contrast plus a soft top highlight. Matte = wash the shading out. */
    private static void applyFinish(NativeImage img, int x, int y, int p, int a, float l, int finish, float shade) {
        if (finish == 2) {
            float flat = 0.62f + 0.38f * shade;
            img.setPixelRGBA(x, y, scale(p, a, flat));
        } else if (finish == 1) {
            float contrast = 1.25f * (shade - 0.5f) + 0.5f;
            float highlight = Math.max(0f, 1f - Math.abs(0.25f - l) * 3.2f) * 0.22f;
            img.setPixelRGBA(x, y, scale(p, a, Math.min(1.6f, contrast + highlight)));
        }
    }

    private static int scale(int p, int a, float f) {
        int r = clamp((p & 0xFF) * f);
        int g = clamp(((p >> 8) & 0xFF) * f);
        int b = clamp(((p >> 16) & 0xFF) * f);
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static NativeImage read(ResourceLocation loc) {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) { return NativeImage.read(in); }
        catch (Exception e) { return null; }
    }

    /** Group map for the paint tools, cached so the UI can show what the analyser found. */
    private static final Map<String, int[]> GROUPS = new HashMap<>();

    public static int[] groupsFor(String species, ResourceLocation original) {
        String key = species + "|" + original;
        int[] g = GROUPS.get(key);
        if (g != null) return g;
        NativeImage img = read(original);
        if (img == null) return null;
        NativeImage mask = read(ResourceLocation.fromNamespaceAndPath("colormons", "masks/" + species + ".png"));
        g = TextureAnalyzer.analyse(img, mask);
        if (mask != null) mask.close();
        GROUPS.put(key, g);
        return g;
    }

    public static void invalidate(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        CACHE.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(id.toString())) { mc.getTextureManager().release(e.getValue()); return true; }
            return false;
        });
    }

    private static int clamp(float v) { return Math.max(0, Math.min(255, (int) v)); }
}
