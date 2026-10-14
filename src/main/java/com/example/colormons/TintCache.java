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

    public static ResourceLocation getOrCreate(UUID id, String species, ResourceLocation original, int[] c) {
        String key = id + "|" + original + "|" + Arrays.toString(c);
        ResourceLocation hit = CACHE.get(key);
        if (hit != null) return hit;

        NativeImage img = read(original);
        if (img == null) return original;

        int[] group = new int[img.getWidth() * img.getHeight()];
        float[] groupLum = new float[3];

        NativeImage mask = read(ResourceLocation.fromNamespaceAndPath("colormons", "masks/" + species + ".png"));
        if (mask != null && mask.getWidth() == img.getWidth() && mask.getHeight() == img.getHeight()) {
            groupsFromMask(img, mask, group, groupLum);
        } else {
            groupsFromClusters(img, group, groupLum);
        }
        if (mask != null) mask.close();

        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
            int g = group[y * img.getWidth() + x];
            if (g < 0) continue;
            int p = img.getPixelRGBA(x, y);
            int a = (p >>> 24) & 0xFF;
            float shade = lum(p) / Math.max(groupLum[g], 0.05f);
            int t = c[g];
            int r = clamp(((t >> 16) & 0xFF) * shade);
            int gr = clamp(((t >> 8) & 0xFF) * shade);
            int b = clamp((t & 0xFF) * shade);
            img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (gr << 8) | r);
        }

        ResourceLocation out = Minecraft.getInstance().getTextureManager().register(
                "colormons_" + id.toString().replace("-", ""), new DynamicTexture(img));
        CACHE.put(key, out);
        return out;
    }

    private static void groupsFromMask(NativeImage img, NativeImage mask, int[] group, float[] groupLum) {
        float[] sum = new float[3]; int[] n = new int[3];
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
            int m = mask.getPixelRGBA(x, y);
            int r = m & 0xFF, g = (m >> 8) & 0xFF, b = (m >> 16) & 0xFF, a = (m >>> 24) & 0xFF;
            int idx = y * img.getWidth() + x, gi = -1;
            if (a > 0 && Math.max(r, Math.max(g, b)) > 128) gi = (r >= g && r >= b) ? 0 : (g >= b ? 1 : 2);
            group[idx] = gi;
            if (gi >= 0) { sum[gi] += lum(img.getPixelRGBA(x, y)); n[gi]++; }
        }
        for (int i = 0; i < 3; i++) groupLum[i] = n[i] > 0 ? sum[i] / n[i] : 1f;
    }

    private static void groupsFromClusters(NativeImage img, int[] group, float[] groupLum) {
        int w = img.getWidth(), h = img.getHeight();
        List<int[]> px = new ArrayList<>();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = img.getPixelRGBA(x, y);
            group[y * w + x] = -1;
            if (((p >>> 24) & 0xFF) > 0) px.add(new int[]{x, y, p & 0xFF, (p >> 8) & 0xFF, (p >> 16) & 0xFF});
        }
        if (px.isEmpty()) return;

        px.sort(Comparator.comparingDouble(q -> q[2] * 0.299 + q[3] * 0.587 + q[4] * 0.114));
        float[][] cen = new float[3][3];
        for (int k = 0; k < 3; k++) {
            int[] s = px.get(Math.min(px.size() - 1, px.size() * (2 * k + 1) / 6));
            cen[k] = new float[]{s[2], s[3], s[4]};
        }
        int[] assign = new int[px.size()];
        for (int it = 0; it < 10; it++) {
            float[][] sum = new float[3][3]; int[] n = new int[3];
            for (int i = 0; i < px.size(); i++) {
                int[] q = px.get(i); int best = 0; float bd = Float.MAX_VALUE;
                for (int k = 0; k < 3; k++) {
                    float dr = q[2] - cen[k][0], dg = q[3] - cen[k][1], db = q[4] - cen[k][2];
                    float d = dr * dr + dg * dg + db * db;
                    if (d < bd) { bd = d; best = k; }
                }
                assign[i] = best; n[best]++;
                sum[best][0] += q[2]; sum[best][1] += q[3]; sum[best][2] += q[4];
            }
            for (int k = 0; k < 3; k++) if (n[k] > 0)
                cen[k] = new float[]{sum[k][0] / n[k], sum[k][1] / n[k], sum[k][2] / n[k]};
        }

        int[] count = new int[3];
        for (int a : assign) count[a]++;
        Integer[] order = {0, 1, 2};
        Arrays.sort(order, (a, b) -> count[b] - count[a]);
        int[] rank = new int[3];
        for (int r = 0; r < 3; r++) rank[order[r]] = r;

        for (int i = 0; i < px.size(); i++) {
            int[] q = px.get(i);
            group[q[1] * w + q[0]] = rank[assign[i]];
        }
        for (int k = 0; k < 3; k++)
            groupLum[rank[k]] = (cen[k][0] * 0.299f + cen[k][1] * 0.587f + cen[k][2] * 0.114f) / 255f;
    }

    private static NativeImage read(ResourceLocation loc) {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) { return NativeImage.read(in); }
        catch (Exception e) { return null; }
    }

    public static void invalidate(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        CACHE.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(id.toString())) { mc.getTextureManager().release(e.getValue()); return true; }
            return false;
        });
    }

    private static float lum(int abgr) {
        return ((abgr & 0xFF) * 0.299f + ((abgr >> 8) & 0xFF) * 0.587f + ((abgr >> 16) & 0xFF) * 0.114f) / 255f;
    }
    private static int clamp(float v) { return Math.max(0, Math.min(255, (int) v)); }
}
