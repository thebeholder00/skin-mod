package com.skinshift.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Один скин из папки skinshift/skins, либо специальный пункт "оригинальный скин". */
public final class SkinEntry {
    public final String id;
    public final String displayName;
    public final NativeImage image;   // null у пункта "оригинальный"
    public final byte[] pngBytes;     // null у пункта "оригинальный"

    public DynamicTexture thumbTex;
    public ResourceLocation thumbLoc;

    public static final SkinEntry ORIGINAL = new SkinEntry("__original__", "", null, null);

    public SkinEntry(String id, String displayName, NativeImage image, byte[] pngBytes) {
        this.id = id;
        this.displayName = displayName;
        this.image = image;
        this.pngBytes = pngBytes;
    }

    public boolean isOriginal() {
        return image == null;
    }
}
