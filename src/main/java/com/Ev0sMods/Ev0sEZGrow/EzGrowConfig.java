package com.Ev0sMods.Ev0sEZGrow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EzGrowConfig {

    /** 0.0 = never grow, 1.0 = always grow (default). */
    public static volatile double  growthChance      = 1.0;
    /** When true, one fertilizer item is consumed per successfully advanced stage. */
    public static volatile boolean consumeFertilizer = false;
    /** Radius (blocks) around the player to scan for crops/saplings. */
    public static volatile int     radius            = 5;

    private static final String DEFAULT_JSON =
            "{\n" +
            "  \"growthChance\": 1.0,\n" +
            "  \"consumeFertilizer\": false,\n" +
            "  \"radius\": 5\n" +
            "}\n";

    private static final Pattern CHANCE_PATTERN       = Pattern.compile("\"growthChance\"\\s*:\\s*([0-9]*\\.?[0-9]+)");
    private static final Pattern FERTILIZER_PATTERN   = Pattern.compile("\"consumeFertilizer\"\\s*:\\s*(true|false)");
    private static final Pattern RADIUS_PATTERN       = Pattern.compile("\"radius\"\\s*:\\s*([0-9]+)");

    public static void load() {
        Path path = findOrCreate();
        if (path == null) {
            System.out.println("[Ev0sEZGrow] Could not locate config; using defaults");
            return;
        }
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            Matcher m = CHANCE_PATTERN.matcher(content);
            if (m.find()) {
                double value = Double.parseDouble(m.group(1));
                growthChance = Math.max(0.0, Math.min(1.0, value));
            }

            m = FERTILIZER_PATTERN.matcher(content);
            if (m.find()) {
                consumeFertilizer = Boolean.parseBoolean(m.group(1));
            }

            m = RADIUS_PATTERN.matcher(content);
            if (m.find()) {
                int value = Integer.parseInt(m.group(1));
                radius = Math.max(1, Math.min(64, value));
            }

            System.out.println("[Ev0sEZGrow] Config loaded from " + path
                    + " -> growthChance=" + growthChance
                    + ", consumeFertilizer=" + consumeFertilizer
                    + ", radius=" + radius);
        } catch (Exception e) {
            System.out.println("[Ev0sEZGrow] Failed to read config (" + e.getMessage() + "); using defaults");
        }
    }

    private static Path findOrCreate() {
        String[] candidates = {
            "Ev0sEZGrow.json",
            "mods/Ev0sEZGrow.json",
            "Mods/Ev0sEZGrow.json",
        };
        for (String rel : candidates) {
            Path p = Paths.get(rel);
            if (Files.exists(p)) return p;
        }
        Path def = Paths.get("Ev0sEZGrow.json");
        try {
            Files.write(def, DEFAULT_JSON.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW);
            System.out.println("[Ev0sEZGrow] Created default config at " + def.toAbsolutePath());
            return def;
        } catch (IOException e) {
            return null;
        }
    }

    private EzGrowConfig() {}
}
