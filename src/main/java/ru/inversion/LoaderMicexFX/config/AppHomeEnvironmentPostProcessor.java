package ru.inversion.LoaderMicexFX.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AppHomeEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final List<String> PATH_PROPERTIES = List.of(
            "app.micex.native.dir",
            "app.data.file.path",
            "app.data.file.raw",
            "app.data.file.template");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        AppHome.init();
        Map<String, Object> resolved = new LinkedHashMap<>();
        resolved.put(AppHome.PROP_HOME, System.getProperty(AppHome.PROP_HOME));

        Properties merged = AppHome.loadMergedApplicationProperties();
        for (String key : PATH_PROPERTIES) {
            String v = merged.getProperty(key);
            if (v == null || v.isBlank()) {
                v = environment.getProperty(key);
            }
            if (v != null && !v.isBlank()) {
                resolved.put(key, AppHome.resolve(stripAppHomePlaceholder(v)).toString());
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource("appHomeResolvedPaths", resolved));
    }

    private static String stripAppHomePlaceholder(String value) {
        String home = System.getProperty(AppHome.PROP_HOME, "");
        return value.replace("${app.home}", home).trim();
    }
}
