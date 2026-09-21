package com.whokillme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class WhoKillMeData {

    public static class Entry {
        public String name = "";
        public int iKilled = 0;
        public int killedMe = 0;
    }

    public Map<String, Entry> stats = new LinkedHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WhoKillMeData instance = new WhoKillMeData();

    public static WhoKillMeData get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("whokillme.json");
    }

    public static void load() {
        Path p = file();
        if (!Files.exists(p)) return;
        try {
            String json = Files.readString(p, StandardCharsets.UTF_8);
            WhoKillMeData loaded = GSON.fromJson(json, WhoKillMeData.class);
            if (loaded != null) {
                if (loaded.stats == null) loaded.stats = new LinkedHashMap<>();
                instance = loaded;
            }
        } catch (Exception e) {
            System.err.println("[WhoKillMe] Failed to load data: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[WhoKillMe] Failed to save data: " + e);
        }
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public Entry find(String name) {
        if (name == null) return null;
        return stats.get(key(name));
    }

    public Entry entry(String name) {
        Entry e = stats.get(key(name));
        if (e == null) {
            e = new Entry();
            e.name = name;
            stats.put(key(name), e);
        }
        return e;
    }

    public void reset(String name) {
        Entry e = find(name);
        if (e != null) {
            e.iKilled = 0;
            e.killedMe = 0;
        }
        save();
    }
}
