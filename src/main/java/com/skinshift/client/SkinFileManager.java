package com.skinshift.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.skinshift.SkinShiftMod;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Сканирует папку &lt;корень игры&gt;/skinshift/skins и даёт список доступных скинов
 * для колеса выбора. Папка создаётся в корне игры (рядом с saves/resourcepacks),
 * как и просил пользователь — не внутри мода.
 */
public final class SkinFileManager {
    public static final Path SKINS_DIR = FMLPaths.GAMEDIR.get().resolve("skinshift").resolve("skins");

    private static final List<SkinEntry> ENTRIES = new ArrayList<>();
    private static int thumbCounter = 0;

    private SkinFileManager() {}

    /** Создаёт папку (и подсказку в ней), если её ещё нет. Вызывается один раз при старте клиента. */
    public static void ensureFolder() {
        try {
            Files.createDirectories(SKINS_DIR);
            Path readme = SKINS_DIR.resolve("положи сюда свои скины.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme,
                        "Положи сюда PNG-файлы скинов 64x64 (обычный формат скина Minecraft).\n" +
                        "Каждый файл станет отдельным пунктом в колесе выбора (зажми клавишу V).\n" +
                        "Имя файла (без .png) будет использовано как название скина.\n");
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Список скинов, первым пунктом всегда идёт "оригинальный скин".
     * Пересканирует папку на диске при каждом вызове — вызывается при каждом открытии колеса.
     */
    public static List<SkinEntry> list() {
        releasePrevious();
        ENTRIES.clear();
        ENTRIES.add(SkinEntry.ORIGINAL);

        if (!Files.isDirectory(SKINS_DIR)) {
            return ENTRIES;
        }
        try (Stream<Path> files = Files.list(SKINS_DIR)) {
            files.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                 .sorted()
                 .forEach(SkinFileManager::tryLoad);
        } catch (IOException ignored) {
        }
        return ENTRIES;
    }

    private static void tryLoad(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            if (img.getWidth() != 64 || img.getHeight() != 64) {
                img.close();
                return; // пропускаем файлы неправильного размера
            }
            String name = path.getFileName().toString();
            String id = name.substring(0, name.length() - 4);
            SkinEntry entry = new SkinEntry(id, id, img, bytes);
            registerThumb(entry);
            ENTRIES.add(entry);
        } catch (Exception ignored) {
            // битый/нечитаемый PNG — пропускаем, не роняем игру
        }
    }

    private static void registerThumb(SkinEntry entry) {
        entry.thumbTex = new DynamicTexture(entry.image.getWidth(), entry.image.getHeight(), false);
        NativeImage px = entry.thumbTex.getPixels();
        for (int y = 0; y < entry.image.getHeight(); y++) {
            for (int x = 0; x < entry.image.getWidth(); x++) {
                px.setPixelRGBA(x, y, entry.image.getPixelRGBA(x, y));
            }
        }
        entry.thumbTex.upload();
        String safe = entry.id.toLowerCase().replaceAll("[^a-z0-9]", "_");
        entry.thumbLoc = ResourceLocation.fromNamespaceAndPath(
                SkinShiftMod.MODID, "skinthumb/" + (thumbCounter++) + "_" + safe);
        Minecraft.getInstance().getTextureManager().register(entry.thumbLoc, entry.thumbTex);
    }

    private static void releasePrevious() {
        for (SkinEntry e : ENTRIES) {
            if (e.thumbLoc != null) {
                Minecraft.getInstance().getTextureManager().release(e.thumbLoc);
            }
            if (e.image != null) {
                e.image.close();
            }
        }
    }
}
