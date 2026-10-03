package com.skinshift.client;

import com.skinshift.Net;
import com.skinshift.SkinSelectRequest;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Радиальное колесо выбора скина.
 * Открывается, пока зажата клавиша V (см. ClientInit#onClientTick), закрывается
 * и применяет выбор при её отпускании (confirmAndClose), либо отменяется по Esc.
 */
public class SkinWheelScreen extends Screen {
    private List<SkinEntry> entries;
    private int selected = 0;
    private boolean resolved = false;

    public SkinWheelScreen() {
        super(Component.translatable("gui.skinshift.wheel.title"));
    }

    @Override
    protected void init() {
        this.entries = SkinFileManager.list();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Вызывается из ClientInit, когда физически отпущена клавиша V. Применяет выбор и закрывает экран. */
    public void confirmAndClose() {
        if (resolved) return;
        resolved = true;
        if (entries != null && !entries.isEmpty()) {
            SkinEntry e = entries.get(selected);
            byte[] bytes = e.isOriginal() ? new byte[0] : e.pngBytes;
            Net.CHANNEL.send(new SkinSelectRequest(bytes, (byte) ClientConfig.particleStyle.ordinal(),
                            (byte) ClientConfig.colorIndex),
                    PacketDistributor.SERVER.noArg());
        }
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        // Esc — просто закрыть без применения выбора
        resolved = true;
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (entries != null && !entries.isEmpty()) {
            selected = Math.floorMod(selected - (int) Math.signum(sy), entries.size());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (entries != null && !entries.isEmpty()) {
            if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
                selected = Math.floorMod(selected + 1, entries.size());
                return true;
            }
            if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
                selected = Math.floorMod(selected - 1, entries.size());
                return true;
            }
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        pickByMouse(mx, my);
        return true;
    }

    @Override
    public boolean mouseMoved(double mx, double my) {
        pickByMouse(mx, my);
        return false;
    }

    private void pickByMouse(double mx, double my) {
        if (entries == null || entries.isEmpty()) return;
        int cx = width / 2, cy = height / 2;
        int radius = Math.min(width, height) / 3;
        double best = Double.MAX_VALUE;
        int bestIdx = selected;
        for (int i = 0; i < entries.size(); i++) {
            double a = -Math.PI / 2 + i * (2 * Math.PI / entries.size());
            double ix = cx + Math.cos(a) * radius;
            double iy = cy + Math.sin(a) * radius;
            double d = (ix - mx) * (ix - mx) + (iy - my) * (iy - my);
            if (d < best) { best = d; bestIdx = i; }
        }
        // выбираем мышью, только если курсор реально рядом с кольцом (не в центре)
        double cd = (mx - cx) * (mx - cx) + (my - cy) * (my - cy);
        if (cd > (radius * 0.4) * (radius * 0.4)) {
            selected = bestIdx;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g, mx, my, partialTick);

        if (entries == null || entries.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("gui.skinshift.wheel.empty"),
                    width / 2, height / 2, 0xFFFFFF);
            return;
        }

        int cx = width / 2, cy = height / 2;
        int radius = Math.min(width, height) / 3;
        int icon = 32;

        for (int i = 0; i < entries.size(); i++) {
            SkinEntry e = entries.get(i);
            double a = -Math.PI / 2 + i * (2 * Math.PI / entries.size());
            int ix = (int) (cx + Math.cos(a) * radius);
            int iy = (int) (cy + Math.sin(a) * radius);
            boolean sel = i == selected;
            int s = sel ? icon + 10 : icon;

            g.fill(ix - s / 2 - 2, iy - s / 2 - 2, ix + s / 2 + 2, iy + s / 2 + 2,
                    sel ? 0xCC3DBFFF : 0x99000000);

            if (e.isOriginal()) {
                g.drawCenteredString(this.font, Component.translatable("gui.skinshift.wheel.original_short"),
                        ix, iy - 4, 0xFFFFFF);
            } else {
                g.blit(e.thumbLoc, ix - s / 2, iy - s / 2, s, s, 8, 8, 8, 8, 64, 64);
            }
        }

        SkinEntry cur = entries.get(selected);
        Component name = cur.isOriginal()
                ? Component.translatable("gui.skinshift.wheel.original")
                : Component.literal(cur.displayName);
        g.drawCenteredString(this.font, name, cx, cy - 10, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("gui.skinshift.wheel.hint").withStyle(ChatFormatting.GRAY),
                cx, cy + 6, 0xAAAAAA);
    }
}
