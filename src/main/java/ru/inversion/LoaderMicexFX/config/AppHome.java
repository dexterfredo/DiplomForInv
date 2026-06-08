package ru.inversion.LoaderMicexFX.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AppHome {

    public static final String PROP_HOME = "app.home";
    public static final String PROP_CONFIG = "spring.config.additional-location";

    private static volatile Path home;

    private AppHome() {
    }

    
    public static Path init() {
        if (home != null) {
            return home;
        }
        String explicit = System.getProperty(PROP_HOME, "").trim();
        if (!explicit.isEmpty()) {
            home = Paths.get(explicit).toAbsolutePath().normalize();
        } else {
            home = detectFromCodeSource();
        }
        String withSep = ensureTrailingSeparator(home);
        System.setProperty(PROP_HOME, withSep);
        registerExternalConfig(home);
        return home;
    }

    public static Path get() {
        return home != null ? home : init();
    }

    
    public static Path resolve(String path) {
        if (path == null || path.isBlank()) {
            return get();
        }
        Path p = Paths.get(path.trim());
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return get().resolve(p).normalize();
    }

    public static Path configFile() {
        return get().resolve("config").resolve("application.properties");
    }

    
    public static Properties loadMergedApplicationProperties() {
        Properties merged = new Properties();
        try (InputStream in = AppHome.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                merged.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            System.err.println("Не удалось прочитать application.properties из JAR: " + e.getMessage());
        }
        Path external = configFile();
        if (Files.isRegularFile(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                Properties ext = new Properties();
                ext.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                merged.putAll(ext);
            } catch (IOException e) {
                System.err.println("Не удалось прочитать " + external + ": " + e.getMessage());
            }
        }
        return merged;
    }

    private static Path detectFromCodeSource() {
        try {
            URI uri = AppHome.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path code = Paths.get(uri).toAbsolutePath().normalize();
            if (Files.isRegularFile(code)) {
                return code.getParent();
            }
            Path classes = code;
            if (classes.getFileName() != null
                    && "classes".equalsIgnoreCase(classes.getFileName().toString())
                    && classes.getParent() != null
                    && "target".equalsIgnoreCase(classes.getParent().getFileName().toString())) {
                return classes.getParent().getParent();
            }
            return classes;
        } catch (Exception e) {
            return Paths.get("").toAbsolutePath().normalize();
        }
    }

    private static void registerExternalConfig(Path root) {
        Path cfg = root.resolve("config").resolve("application.properties");
        if (!Files.isRegularFile(cfg)) {
            return;
        }
        String location = "optional:file:" + cfg.toAbsolutePath().normalize();
        String existing = System.getProperty(PROP_CONFIG, "").trim();
        if (existing.isEmpty()) {
            System.setProperty(PROP_CONFIG, location);
        } else if (!existing.contains(location)) {
            System.setProperty(PROP_CONFIG, existing + "," + location);
        }
    }

    private static String ensureTrailingSeparator(Path dir) {
        String s = dir.toAbsolutePath().normalize().toString();
        if (s.endsWith("/") || s.endsWith("\\")) {
            return s;
        }
        return s + Path.of("").getFileSystem().getSeparator();
    }
}
