package com.wynnchayuan.translate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Languages {

    public static final String DEFAULT = "zh_tw";

    private static final String ASSETS = "/assets/wynnchayuan/translations/";

    private Languages() {}

    public static Path dir(Path configDir, String lang) {
        return configDir.resolve("translations").resolve(normalise(lang));
    }

    public static String resource(String lang) {
        return ASSETS + normalise(lang) + "/";
    }

    public static String path(String lang) {
        return normalise(lang) + "/";
    }

    private static final java.util.Map<String, String> NATIVE =
            java.util.Map.ofEntries(
                    java.util.Map.entry("zh_tw", "繁體中文"),
                    java.util.Map.entry("zh_cn", "简体中文"),
                    java.util.Map.entry("ja_jp", "日本語"),
                    java.util.Map.entry("ko_kr", "한국어"),
                    java.util.Map.entry("ru_ru", "Русский"),
                    java.util.Map.entry("de_de", "Deutsch"),
                    java.util.Map.entry("fr_fr", "Français"),
                    java.util.Map.entry("es_es", "Español"),
                    java.util.Map.entry("pt_br", "Português"),
                    java.util.Map.entry("tr_tr", "Türkçe"),
                    java.util.Map.entry("en_us", "English"));

    public static java.util.List<String> known() {
        return NATIVE.keySet().stream().sorted().toList();
    }

    public static String nativeName(String lang) {
        String clean = normalise(lang);
        return NATIVE.getOrDefault(clean, clean);
    }

    public static List<String> bundled() {
        try (InputStream in = Languages.class.getResourceAsStream(
                ASSETS + "_languages.json")) {
            if (in == null) {
                return List.of(DEFAULT);
            }
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            List<String> out = new ArrayList<>();
            for (com.google.gson.JsonElement el
                    : root.getAsJsonObject().getAsJsonArray("languages")) {
                out.add(el.getAsString());
            }
            return out.isEmpty() ? List.of(DEFAULT) : List.copyOf(out);
        } catch (Exception e) {
            return List.of(DEFAULT);
        }
    }

    public static int migrateFlat(Path configDir, String lang) {
        Path root = configDir.resolve("translations");
        Path target = dir(configDir, lang);
        try {
            if (!Files.isDirectory(root) || Files.exists(target)) {
                return 0;
            }
            List<Path> loose = new ArrayList<>();
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(Files::isRegularFile)
                     .filter(p -> p.getFileName().toString().endsWith(".json"))
                     .filter(p -> !inLanguageDir(root, p))
                     .forEach(loose::add);
            }
            if (loose.isEmpty()) {
                return 0;
            }
            Files.createDirectories(target);
            int moved = 0;
            for (Path from : loose) {
                Path to = target.resolve(root.relativize(from));
                Files.createDirectories(to.getParent());
                Files.move(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                moved++;
            }
            try (Stream<Path> rest = Files.walk(root)) {
                rest.sorted(java.util.Comparator.reverseOrder())
                    .filter(p -> !p.equals(root) && Files.isDirectory(p))
                    .filter(p -> !inLanguageDir(root, p) && !isLanguageDir(root, p))
                    .forEach(p -> {
                        try (Stream<Path> inside = Files.list(p)) {
                            if (inside.findAny().isEmpty()) {
                                Files.delete(p);
                            }
                        } catch (Exception ignored) {
                        }
                    });
            }
            System.out.println("[WynnChaYuan] 舊版的譯文已搬進 " + lang + "/（"
                    + moved + " 個檔案）");
            return moved;
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 搬移舊版譯文失敗：" + e.getMessage());
            return 0;
        }
    }

    private static final java.util.regex.Pattern LANGUAGE_DIR =
            java.util.regex.Pattern.compile("[a-z]{2,3}_[a-z0-9]{2,4}");

    private static boolean isLanguageDir(Path root, Path p) {
        Path rel = root.relativize(p);
        return rel.getNameCount() == 1 && LANGUAGE_DIR.matcher(rel.toString()).matches();
    }

    private static boolean inLanguageDir(Path root, Path p) {
        Path rel = root.relativize(p);
        return rel.getNameCount() > 1 && LANGUAGE_DIR.matcher(rel.getName(0).toString()).matches();
    }

    public static String pick(String configured, String gameLanguage) {
        if (configured != null && !configured.isBlank()) {
            return normalise(configured);
        }
        String game = normalise(gameLanguage);
        return bundled().contains(game) ? game : DEFAULT;
    }

    public static boolean known(String gameLanguage) {
        if (gameLanguage == null || gameLanguage.isBlank()) {
            return false;
        }
        return bundled().contains(normalise(gameLanguage));
    }

    public static final String OFF = "off";

    public static String underlay(String chosen, String lang) {
        if (chosen == null || chosen.isBlank() || OFF.equals(chosen.trim())) {
            return null;
        }
        String clean = chosen.trim();
        return clean.equals(lang) ? null : clean;
    }

    private static String normalise(String lang) {
        if (lang == null || lang.isBlank()) {
            return DEFAULT;
        }
        String clean = lang.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        return clean.matches("[a-z0-9_]+") ? clean : DEFAULT;
    }
}
