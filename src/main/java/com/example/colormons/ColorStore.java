package com.example.colormons;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ColorStore {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("colormons.json");
    private static final Gson GSON = new Gson();
    private static Map<String, int[]> colors = new HashMap<>();

    public static int[] get(UUID id) { return colors.get(id.toString()); }

    public static void set(UUID id, int[] c) {
        colors.put(id.toString(), c.clone());
        TintCache.invalidate(id);
        save();
    }

    public static void clear(UUID id) {
        colors.remove(id.toString());
        TintCache.invalidate(id);
        save();
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                colors = GSON.fromJson(Files.readString(FILE), new TypeToken<Map<String, int[]>>(){}.getType());
            }
        } catch (Exception ignored) {}
        if (colors == null) colors = new HashMap<>();
    }

    private static void save() {
        try { Files.writeString(FILE, GSON.toJson(colors)); } catch (Exception ignored) {}
    }
}
