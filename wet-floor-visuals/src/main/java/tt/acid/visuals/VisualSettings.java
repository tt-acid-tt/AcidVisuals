package tt.acid.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VisualSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Boolean> FLAGS = new LinkedHashMap<>();
    private static final Map<String, Float> SLIDERS = new LinkedHashMap<>();
    private static final int[] COLORS = {0xFF67E7FF, 0xFFAA94FF, 0xFFFFA5D8, 0xFFFFD27C, 0xFFFFFFFF};
    private static int accentIndex = 0;
    private static int skyStyle = 0;

    static {
        FLAGS.put("jump_circle", true);
        FLAGS.put("ambient_particles", true);
        FLAGS.put("custom_hotbar", true);
        FLAGS.put("wet_floor", false);
        FLAGS.put("rain_ripples", true);
        FLAGS.put("sky_overlay", true);
        FLAGS.put("soft_lighting", true);
        FLAGS.put("entity_boxes", false);
        FLAGS.put("player_names", true);
        FLAGS.put("hud_enabled", true);
        FLAGS.put("hud_watermark", true);
        FLAGS.put("hud_fps", true);
        FLAGS.put("hud_coords", true);
        FLAGS.put("hud_speed", true);
        FLAGS.put("hud_direction", true);
        FLAGS.put("hud_armor", true);
        FLAGS.put("hud_target", true);
        FLAGS.put("hud_keystrokes", true);
        SLIDERS.put("jump_size", 1.15f);
        SLIDERS.put("particle_density", 0.65f);
        SLIDERS.put("wet_intensity", 0.78f);
        SLIDERS.put("puddle_radius", 18.0f);
        SLIDERS.put("reflection_wave", 0.72f);
        SLIDERS.put("box_range", 32.0f);
    }

    private VisualSettings() {}
    public static boolean get(String key) { return FLAGS.getOrDefault(key, false); }
    public static void set(String key, boolean value) {
        if (!FLAGS.containsKey(key)) return;
        FLAGS.put(key, value);
        save();
    }
    public static float slider(String key) { return SLIDERS.getOrDefault(key, 0.5f); }
    public static void setSlider(String key, float value) {
        if (!SLIDERS.containsKey(key)) return;
        float min = switch (key) {
            case "jump_size" -> 0.55f; case "particle_density" -> 0.1f; case "wet_intensity" -> 0.15f;
            case "puddle_radius" -> 6.0f; case "reflection_wave" -> 0.0f; case "box_range" -> 8.0f; default -> 0.0f;
        };
        float max = switch (key) {
            case "jump_size" -> 2.8f; case "particle_density", "wet_intensity" -> 1.0f;
            case "puddle_radius" -> 24.0f; case "reflection_wave" -> 1.0f; case "box_range" -> 64.0f; default -> 1.0f;
        };
        SLIDERS.put(key, Math.max(min, Math.min(max, value)));
        save();
    }
    public static int accent() { return COLORS[Math.max(0, Math.min(COLORS.length - 1, accentIndex))]; }
    public static int accentIndex() { return accentIndex; }
    public static int colorCount() { return COLORS.length; }
    public static int colorAt(int index) { return COLORS[Math.max(0, Math.min(COLORS.length - 1, index))]; }
    public static void setAccentIndex(int index) { accentIndex = Math.max(0, Math.min(COLORS.length - 1, index)); save(); }
    public static int skyStyle() { return Math.max(0, Math.min(7, skyStyle)); }
    public static void setSkyStyle(int style) { skyStyle = Math.max(0, Math.min(7, style)); save(); }

    public static void applyPreset(int preset) {
        switch (preset) {
            case 0 -> {
                skyStyle = 0;
                FLAGS.put("wet_floor", false); FLAGS.put("rain_ripples", true); FLAGS.put("jump_circle", true);
                FLAGS.put("ambient_particles", false); FLAGS.put("custom_hotbar", true); FLAGS.put("sky_overlay", false);
                FLAGS.put("soft_lighting", true); FLAGS.put("entity_boxes", false); FLAGS.put("player_names", true);
                SLIDERS.put("jump_size", 1.0f); SLIDERS.put("particle_density", 0.45f);
                SLIDERS.put("wet_intensity", 0.55f); SLIDERS.put("reflection_wave", 0.35f); SLIDERS.put("puddle_radius", 18.0f); accentIndex = 0;
            }
            case 1 -> {
                skyStyle = 2;
                FLAGS.put("hud_enabled", true);
                FLAGS.put("wet_floor", true); FLAGS.put("rain_ripples", true); FLAGS.put("jump_circle", true);
                FLAGS.put("ambient_particles", true); FLAGS.put("custom_hotbar", true); FLAGS.put("sky_overlay", true);
                FLAGS.put("soft_lighting", true); FLAGS.put("entity_boxes", false); FLAGS.put("player_names", true);
                SLIDERS.put("jump_size", 1.35f); SLIDERS.put("particle_density", 0.8f);
                SLIDERS.put("wet_intensity", 0.95f); SLIDERS.put("reflection_wave", 0.85f); SLIDERS.put("puddle_radius", 18.0f); accentIndex = 0;
            }
            case 2 -> {
                skyStyle = 4;
                FLAGS.put("hud_enabled", true);
                FLAGS.put("wet_floor", true); FLAGS.put("rain_ripples", false); FLAGS.put("jump_circle", false);
                FLAGS.put("ambient_particles", true); FLAGS.put("custom_hotbar", true); FLAGS.put("sky_overlay", true);
                FLAGS.put("soft_lighting", true); FLAGS.put("entity_boxes", true); FLAGS.put("player_names", true);
                SLIDERS.put("jump_size", 1.1f); SLIDERS.put("particle_density", 0.9f);
                SLIDERS.put("wet_intensity", 0.86f); SLIDERS.put("reflection_wave", 0.75f); SLIDERS.put("puddle_radius", 18.0f); accentIndex = 1;
            }
        }
        save();
    }

    public static void load() {
        try {
            File file = configFile();
            if (!file.isFile()) return;
            JsonObject root = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
            if (root.has("flags") && root.get("flags").isJsonObject()) {
                JsonObject object = root.getAsJsonObject("flags");
                for (String key : FLAGS.keySet()) if (object.has(key)) FLAGS.put(key, object.get(key).getAsBoolean());
            }
            if (root.has("sliders") && root.get("sliders").isJsonObject()) {
                JsonObject object = root.getAsJsonObject("sliders");
                for (String key : SLIDERS.keySet()) if (object.has(key)) SLIDERS.put(key, object.get(key).getAsFloat());
            }
            if (root.has("accent")) accentIndex = Math.max(0, Math.min(COLORS.length - 1, root.get("accent").getAsInt()));
            if (root.has("skyStyle")) skyStyle = Math.max(0, Math.min(7, root.get("skyStyle").getAsInt()));
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            File file = configFile();
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            JsonObject root = new JsonObject();
            JsonObject flags = new JsonObject();
            FLAGS.forEach((key, value) -> flags.addProperty(key, value));
            JsonObject sliders = new JsonObject();
            SLIDERS.forEach((key, value) -> sliders.addProperty(key, value));
            root.add("flags", flags); root.add("sliders", sliders); root.addProperty("accent", accentIndex); root.addProperty("skyStyle", skyStyle);
            Files.writeString(file.toPath(), GSON.toJson(root));
        } catch (Exception ignored) { }
    }

    private static File configFile() {
        return new File(new File(MinecraftClient.getInstance().runDirectory, "config"), "acidvisuals.json");
    }
}
