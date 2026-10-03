package com.skinshift.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.UUID;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Клиентское состояние эффекта для одного игрока. */
public final class PlayerFx {
    final UUID id;

    /** Настоящий (оригинальный) скин игрока, считанный один раз с видеокарты за сессию. Может быть null. */
    NativeImage originalSkin;
    boolean triedCapture;

    /** Что сейчас полностью отображено (результат последней завершённой анимации). */
    NativeImage currentImg;
    /** Куда сейчас идёт анимация (null, если анимации нет). */
    NativeImage targetImg;
    boolean targetIsCustom;

    boolean animating;
    /** true, если сейчас показан не оригинальный, а загруженный скин. */
    boolean customActive;
    int startAge;
    int lastStep = -1;

    byte particleStyle;
    byte particleColorIdx;

    DynamicTexture tex;
    ResourceLocation loc;

    PlayerFx(UUID id) {
        this.id = id;
    }

    /** Нужно ли рисовать игрока нашим рендерером с динамической текстурой. */
    public boolean useCustom() {
        return (customActive || animating) && tex != null;
    }

    public boolean isAnimating() {
        return animating;
    }

    public int getStartAge() {
        return startAge;
    }

    public ResourceLocation getTexture() {
        return loc;
    }
}
