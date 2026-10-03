package com.skinshift.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Экран настроек мода: стиль и цвет частиц при смене скина. Открывается отдельной клавишей. */
public class SkinShiftConfigScreen extends Screen {
    private static final String[] STYLE_KEYS = {
            "gui.skinshift.config.style.vortex",
            "gui.skinshift.config.style.spiral",
            "gui.skinshift.config.style.explosion"
    };

    public SkinShiftConfigScreen() {
        super(Component.translatable("gui.skinshift.config.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int styleY = height / 2 - 50;

        ClientConfig.ParticleStyle[] styles = ClientConfig.ParticleStyle.values();
        for (int i = 0; i < styles.length; i++) {
            ClientConfig.ParticleStyle st = styles[i];
            int x = cx - 150 + i * 100;
            boolean active = ClientConfig.particleStyle == st;
            Component label = active
                    ? Component.literal("> ").append(Component.translatable(STYLE_KEYS[i])).append(" <")
                    : Component.translatable(STYLE_KEYS[i]);
            this.addRenderableWidget(Button.builder(label, b -> {
                        ClientConfig.particleStyle = st;
                        this.rebuild();
                    })
                    .bounds(x, styleY, 95, 20).build());
        }

        int colorY = styleY + 40;
        int n = ClientConfig.COLORS.length;
        int bw = 36;
        int startX = cx - (n * bw) / 2;
        for (int i = 0; i < n; i++) {
            int idx = i;
            int x = startX + i * bw;
            Component label = idx == ClientConfig.colorIndex ? Component.literal("[ ]") : Component.literal(" ");
            this.addRenderableWidget(Button.builder(label, b -> {
                        ClientConfig.colorIndex = idx;
                        this.rebuild();
                    })
                    .bounds(x, colorY, bw - 4, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        b -> this.onClose())
                .bounds(cx - 50, colorY + 36, 100, 20).build());
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void onClose() {
        ClientConfig.save();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g, mx, my, partialTick);

        int cx = width / 2;
        int colorY = height / 2 - 10;
        int n = ClientConfig.COLORS.length;
        int bw = 36;
        int startX = cx - (n * bw) / 2;
        for (int i = 0; i < n; i++) {
            float[] c = ClientConfig.COLORS[i];
            int argb = 0xFF000000 | ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
            g.fill(startX + i * bw + 5, colorY + 5, startX + i * bw + bw - 9, colorY + 15, argb);
        }

        super.render(g, mx, my, partialTick);
        g.drawCenteredString(this.font, this.title, width / 2, height / 2 - 80, 0xFFFFFF);
    }
}
