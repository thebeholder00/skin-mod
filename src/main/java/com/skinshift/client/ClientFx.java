package com.skinshift.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.skinshift.Anim;
import com.skinshift.ModParticles;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public final class ClientFx {
    /** Зажми, чтобы открыть колесо выбора скина; отпусти на нужном, чтобы применить. */
    public static final KeyMapping KEY = new KeyMapping(
            "key.skinshift.wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.skinshift");
    /** Открывает экран настроек мода (цвет/тип частиц). */
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.skinshift.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.skinshift");

    private static final Map<UUID, PlayerFx> MAP = new HashMap<>();

    /** Порог "проявки" каждого пикселя: сверху вниз + шум = органичное растворение. */
    private static final float[] THRESH = new float[64 * 64];

    static {
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int h = x * 73856093 ^ y * 19349663;
                h ^= h >>> 13;
                h *= 1274126177;
                h ^= h >>> 16;
                float noise = (h & 0xFFFF) / 65536f;
                THRESH[y * 64 + x] = 0.35f * (y / 63f) + 0.65f * noise * 0.999f;
            }
        }
    }

    private ClientFx() {}

    public static PlayerFx get(UUID id) {
        return MAP.get(id);
    }

    // ------------------------------------------------------------------ пакеты

    /** @param pngBytes пустой массив = вернуться к оригинальному скину. */
    public static void onPacket(UUID id, byte[] pngBytes, boolean instant, byte style, byte colorIdx) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        AbstractClientPlayer p = level.getPlayerByUUID(id) instanceof AbstractClientPlayer acp ? acp : null;
        PlayerFx fx = MAP.computeIfAbsent(id, PlayerFx::new);

        if (p != null) captureOriginal(fx, p);
        ensureTexture(fx);
        if (fx.currentImg == null) fx.currentImg = fx.originalSkin;

        boolean requestedCustom = pngBytes.length > 0;
        NativeImage target = requestedCustom ? decode(pngBytes) : fx.originalSkin;
        if (target == null) target = fx.originalSkin;
        if (target == null) return; // нечего показывать — оригинал считать не удалось и файл битый

        boolean effectiveCustom = requestedCustom && target != fx.originalSkin;
        fx.particleStyle = style;
        fx.particleColorIdx = colorIdx;

        if (instant || p == null) {
            releaseIfOwned(fx, fx.currentImg);
            fx.currentImg = target;
            fx.customActive = effectiveCustom;
            fx.animating = false;
            paintFull(fx, fx.currentImg);
            return;
        }

        fx.targetImg = target;
        fx.targetIsCustom = effectiveCustom;
        fx.animating = true;
        fx.startAge = p.tickCount;
        fx.lastStep = -1;
        paintFull(fx, fx.currentImg);
    }

    private static NativeImage decode(byte[] bytes) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            if (img.getWidth() != 64 || img.getHeight() != 64) {
                img.close();
                return null;
            }
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ тик игрока

    public static void tick(AbstractClientPlayer p) {
        PlayerFx fx = MAP.get(p.getUUID());
        if (fx == null || !fx.animating) return;

        int t = p.tickCount - fx.startAge;
        if (t < 0) return;

        if (t >= Anim.TOTAL) {
            fx.animating = false;
            releaseIfOwned(fx, fx.currentImg);
            fx.currentImg = fx.targetImg;
            fx.customActive = fx.targetIsCustom;
            fx.targetImg = null;
            paintFull(fx, fx.currentImg);
            return;
        }

        float prog = Anim.skinProgress(t);
        int step = (int) (prog * 48f);
        if (step != fx.lastStep) {
            fx.lastStep = step;
            paintBlend(fx, prog);
        }
        spawnParticles(p, t, fx.particleStyle, fx.particleColorIdx);
    }

    private static void spawnParticles(AbstractClientPlayer p, int t, byte style, byte colorIdx) {
        int local = t - Anim.RAISE;
        if (local < 0 || local > Anim.SWIRL + 6) return;

        float ramp = Math.min(1f, Math.min(local / 6f, (Anim.SWIRL + 6 - local) / 8f));
        if (ramp <= 0f) return;

        SimpleParticleType type = switch (style) {
            case 1 -> ModParticles.SPIRAL.get();
            case 2 -> ModParticles.EXPLOSION.get();
            default -> ModParticles.VORTEX.get();
        };

        RandomSource r = p.getRandom();
        int n = 2 + (int) (6 * ramp);
        for (int i = 0; i < n; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0;
            double radius = 0.6 + r.nextDouble() * 0.6;
            double y = p.getY() + r.nextDouble() * 0.5;
            double rise = 0.035 + r.nextDouble() * 0.04;
            // индекс цвета палитры упакован в целую часть radius (десятками) — так все данные умещаются
            // в штатные 3 double-поля частицы без сетевой пересылки лишних пакетов.
            double packedRadius = radius + colorIdx * 10.0;
            p.level().addParticle(type, p.getX(), y, p.getZ(), angle, rise, packedRadius);
        }
    }

    // ------------------------------------------------------------------ текстура

    private static void ensureTexture(PlayerFx fx) {
        if (fx.tex != null) return;
        fx.tex = new DynamicTexture(64, 64, true);
        fx.loc = ResourceLocation.fromNamespaceAndPath("skinshift", "dyn/" + fx.id.toString().toLowerCase());
        Minecraft.getInstance().getTextureManager().register(fx.loc, fx.tex);
    }

    private static void paintFull(PlayerFx fx, NativeImage img) {
        paintRaw(fx, img, img, 1f);
    }

    /** Перерисовывает текстуру по прогрессу: часть пикселей ещё из currentImg, часть уже из targetImg. */
    private static void paintBlend(PlayerFx fx, float p) {
        paintRaw(fx, fx.currentImg, fx.targetImg, p);
    }

    private static void paintRaw(PlayerFx fx, NativeImage from, NativeImage to, float p) {
        NativeImage px = fx.tex.getPixels();
        if (px == null) return;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                boolean reveal = p >= 1f || THRESH[y * 64 + x] < p;
                px.setPixelRGBA(x, y, sample(reveal ? to : from, x, y));
            }
        }
        fx.tex.upload();
    }

    private static int sample(NativeImage img, int x, int y) {
        if (img == null || x >= img.getWidth() || y >= img.getHeight()) return 0;
        return img.getPixelRGBA(x, y);
    }

    /** Освобождаем NativeImage, только если это НЕ оригинальный скин (его храним всю сессию). */
    private static void releaseIfOwned(PlayerFx fx, NativeImage img) {
        if (img != null && img != fx.originalSkin) {
            img.close();
        }
    }

    /** Считываем реальный скин игрока из видеопамяти (только 64x64) один раз за сессию. */
    private static void captureOriginal(PlayerFx fx, AbstractClientPlayer p) {
        if (fx.triedCapture) return;
        fx.triedCapture = true;
        try {
            ResourceLocation skinLoc = p.getSkin().texture();
            AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(skinLoc);
            RenderSystem.bindTexture(t.getId());
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (w != 64 || h != 64) return;
            NativeImage img = new NativeImage(64, 64, false);
            img.downloadTexture(0, false);
            fx.originalSkin = img;
        } catch (Throwable ignored) {
            fx.originalSkin = null;
        }
    }

    // ------------------------------------------------------------------ очистка

    public static void clearAll() {
        Minecraft mc = Minecraft.getInstance();
        for (PlayerFx fx : MAP.values()) {
            if (fx.loc != null) mc.getTextureManager().release(fx.loc);
            if (fx.originalSkin != null) fx.originalSkin.close();
            if (fx.targetImg != null && fx.targetImg != fx.originalSkin) fx.targetImg.close();
        }
        MAP.clear();
    }
}
