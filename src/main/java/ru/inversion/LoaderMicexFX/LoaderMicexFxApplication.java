package ru.inversion.LoaderMicexFX;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.inversion.LoaderMicexFX.config.AppHome;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@SpringBootApplication
@EnableScheduling
public class LoaderMicexFxApplication {

    public static void main(String[] args) {
        AppHome.init();
        loadMicexNative();
        SpringApplication.run(LoaderMicexFxApplication.class, args);
    }

    private static void loadMicexNative() {
        Properties p = AppHome.loadMergedApplicationProperties();
        String dir = p.getProperty("app.micex.native.dir", "").trim();
        if (dir.isEmpty()) {
            dir = System.getProperty("micex.native.dir", "").trim();
        }
        if (dir.isEmpty()) {
            return;
        }

        Path folder = AppHome.resolve(dir);
        Path dll = folder.resolve("mtejni.dll");
        if (!Files.isRegularFile(dll)) {
            System.err.println("MICEX: в каталоге нет mtejni.dll: " + folder.toAbsolutePath());
            System.err.println("Скопируйте DLL из дистрибутива MOEX в " + folder.toAbsolutePath() + " (см. native/win64/README.txt)");
            return;
        }

        String absolute = dll.toAbsolutePath().normalize().toString();
        try {
            System.load(absolute);
            System.err.println("MICEX: загружена нативная библиотека " + absolute);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("MICEX: не удалось загрузить " + absolute + " — " + e.getMessage());
            System.err.println("Проверьте, что в этой же папке лежат все зависимые dll.");
            throw e;
        }
    }

}
