package com.example.colormons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ColorStore {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("colormons.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, MonColors>>(){}.getType();

    private static Map<String, MonColors> data = new HashMap<>();
    /** Four quick-save swatches. colours[slot][6], -1 = unset. */
    public static final int[][] SAVED = new int[4][6];
    /** false = only your own Pokemon get the menu. */
    public static boolean allowOthers = false;

    public static MonColors get(UUID id) {
        MonColors m = data.get(id.toString());
        if (m == null) { m = new MonColors(); data.put(id.toString(), m); }
        return m;
    }

    public static boolean has(UUID id) { return data.containsKey(id.toString()); }

    public static void clear(UUID id) {
        data.remove(id.toString());
        TintCache.invalidate(id);
        save();
    }

    public static void applied(UUID id) {
        TintCache.invalidate(id);
        save();
    }

    public static void save() {
        try {
            Map<String, Object> root = new HashMap<>();
            root.put("pokemon", data);
            root.put("saved", SAVED);
            root.put("allowOthers", allowOthers);
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception ignored) {}
    }

    public static void load() {
        data = new HashMap<>();
        try {
            if (Files.exists(FILE)) {
                Map<String, Object> root = GSON.fromJson(Files.readString(FILE),
                        new TypeToken<Map<String, Object>>(){}.getType());
                if (root != null) {
                    Object mon = root.get("pokemon");
                    if (mon != null) data = GSON.fromJson(GSON.toJson(mon), MAP_TYPE);
                    Object sv = root.get("saved");
                    if (sv != null) {
                        int[][] parsed = GSON.fromJson(GSON.toJson(sv), int[][].class);
                        if (parsed != null) for (int i = 0; i < Math.min(4, parsed.length); i++) SAVED[i] = parsed[i];
                    }
                    Object ao = root.get("allowOthers");
                    if (ao != null) allowOthers = (Boolean) ao;
                }
            }
        } catch (Exception ignored) {}
        if (data == null) data = new HashMap<>();
    }

    public static List<String> describe() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, MonColors> e : data.entrySet()) out.add(e.getKey());
        return out;
    }
}
