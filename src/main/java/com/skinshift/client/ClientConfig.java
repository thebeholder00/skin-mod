package com.skinshift.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraftforge.fml.loading.FMLPaths;

/** Настройки мода на этом клиенте: стиль и цвет частиц при смене скина. */
public final class ClientConfig {
    public enum ParticleStyle { VORTEX, SPIRAL, EXPLOSION }

    /** r,g,b (0..1) для каждого пункта палитры. Порядок соответствует кнопкам в экране настроек. */
    public static final float[][] COLORS = {
            {1.00f, 0.86f, 0.08f}, // жёлтый (по умолчанию)
            {0.25f, 0.85f, 1.00f}, // голубой
            {0.72f, 0.33f, 1.00f}, // фиолетовый
            {1.00f, 0.45f, 0.75f}, // розовый
            {0.35f, 1.00f, 0.45f}, // зелёный
            {1.00f, 0.25f, 0.25f}, // красный
            {1.00f, 1.00f, 1.00f}, // белый
            {1.00f, 0.55f, 0.10f}, // оранжевый
    };

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("skinshift-client.properties");

    public static ParticleStyle particleStyle = ParticleStyle.VORTEX;
    public static int colorIndex = 0;

    private ClientConfig() {}

    public static void load() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            particleStyle = ParticleStyle.valueOf(p.getProperty("particleStyle", "VORTEX"));
            colorIndex = Math.max(0, Math.min(COLORS.length - 1,
                    Integer.parseInt(p.getProperty("colorIndex", "0"))));
        } catch (Exception ignored) {
            // файла ещё нет или он битый — остаёмся на значениях по умолчанию
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("particleStyle", particleStyle.name());
        p.setProperty("colorIndex", String.valueOf(colorIndex));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "SkinShift client settings");
            }
        } catch (IOException ignored) {
        }
    }
}
