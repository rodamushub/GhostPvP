package dev.ghost.pvp.config;

import com.google.gson.*;
import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.module.ModuleManager;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ConfigManager — saves and loads module states + settings as JSON.
 *
 * File layout:
 *   .minecraft/ghostpvp/profiles/<profileName>.json
 *
 * JSON structure per profile:
 * {
 *   "modules": {
 *     "AutoCrystal": {
 *       "enabled": true,
 *       "keybind": -1,
 *       "settings": { "Place Range": 4.5, "Explode Range": 5.0, ... }
 *     }, ...
 *   },
 *   "friends": ["playerA", "playerB"]
 * }
 */
public final class ConfigManager {

    private static final String DEFAULT_PROFILE = "default";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Path profilesDir;
    private String currentProfile = DEFAULT_PROFILE;

    public ConfigManager() {
        try {
            profilesDir = Paths.get("ghostpvp", "profiles");
            Files.createDirectories(profilesDir);
        } catch (IOException e) {
            GhostClient.LOG.error("Failed to create config directory", e);
        }
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    public void save(ModuleManager mm) { save(mm, currentProfile); }

    public void save(ModuleManager mm, String profileName) {
        JsonObject root      = new JsonObject();
        JsonObject modules   = new JsonObject();

        for (Module m : mm.getModules()) {
            JsonObject mObj     = new JsonObject();
            mObj.addProperty("enabled", m.isEnabled());
            mObj.addProperty("keybind", m.getKeybind());

            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) {
                Object v = s.getValue();
                if      (v instanceof Boolean b)  settings.addProperty(s.getName(), b);
                else if (v instanceof Integer i)  settings.addProperty(s.getName(), i);
                else if (v instanceof Double  d)  settings.addProperty(s.getName(), d);
                else if (v instanceof Float   f)  settings.addProperty(s.getName(), f);
                else if (v instanceof Enum<?> en) settings.addProperty(s.getName(), en.name());
                else if (v instanceof String  str)settings.addProperty(s.getName(), str);
            }
            mObj.add("settings", settings);
            modules.add(m.getName(), mObj);
        }

        root.add("modules", modules);

        // Friends
        JsonArray friends = new JsonArray();
        GhostClient.get().getFriendManager().getAll().forEach(friends::add);
        root.add("friends", friends);

        try (Writer w = Files.newBufferedWriter(profilePath(profileName))) {
            gson.toJson(root, w);
        } catch (IOException e) {
            GhostClient.LOG.error("Failed to save config '{}'", profileName, e);
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    public void loadAll(ModuleManager mm) { load(mm, currentProfile); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void load(ModuleManager mm, String profileName) {
        Path path = profilePath(profileName);
        if (!Files.exists(path)) return;

        try (Reader r = Files.newBufferedReader(path)) {
            JsonObject root    = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject modules = root.getAsJsonObject("modules");

            if (modules != null) {
                for (Map.Entry<String, JsonElement> entry : modules.entrySet()) {
                    mm.get(entry.getKey()).ifPresent(m -> {
                        JsonObject mObj = entry.getValue().getAsJsonObject();
                        if (mObj.has("enabled")) {
                            boolean en = mObj.get("enabled").getAsBoolean();
                            m.setEnabled(en);
                        }
                        if (mObj.has("keybind")) {
                            mm.bindKey(m, mObj.get("keybind").getAsInt());
                        }
                        if (mObj.has("settings")) {
                            JsonObject settings = mObj.getAsJsonObject("settings");
                            for (Setting<?> s : m.getSettings()) {
                                if (!settings.has(s.getName())) continue;
                                JsonElement val = settings.get(s.getName());
                                try {
                                    Object cur = s.getValue();
                                    if      (cur instanceof Boolean)  ((Setting<Boolean>) s).setValue(val.getAsBoolean());
                                    else if (cur instanceof Integer)  ((Setting<Integer>) s).setValue(val.getAsInt());
                                    else if (cur instanceof Double)   ((Setting<Double>)  s).setValue(val.getAsDouble());
                                    else if (cur instanceof Float)    ((Setting<Float>)   s).setValue(val.getAsFloat());
                                    else if (cur instanceof String)   ((Setting<String>)  s).setValue(val.getAsString());
                                    else if (cur instanceof Enum) {
                                        Class<? extends Enum> ec = ((Enum<?>) cur).getDeclaringClass();
                                        ((Setting) s).setValue(Enum.valueOf(ec, val.getAsString()));
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    });
                }
            }

            // Friends
            if (root.has("friends")) {
                GhostClient.get().getFriendManager().clear();
                root.getAsJsonArray("friends").forEach(el ->
                    GhostClient.get().getFriendManager().add(el.getAsString()));
            }

        } catch (IOException e) {
            GhostClient.LOG.error("Failed to load config '{}'", profileName, e);
        }

        currentProfile = profileName;
    }

    // ── Profiles ───────────────────────────────────────────────────────────────

    public List<String> listProfiles() {
        try (var stream = Files.list(profilesDir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public String getCurrentProfile() { return currentProfile; }
    public void   setCurrentProfile(String name) { this.currentProfile = name; }

    private Path profilePath(String name) {
        return profilesDir.resolve(name + ".json");
    }
}

