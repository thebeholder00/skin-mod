package com.skinshift;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Клиент -> сервер: "я выбрал этот скин в колесе".
 *
 * @param pngBytes        PNG-байты выбранного скина (64x64). Пустой массив = вернуться к оригиналу.
 * @param particleStyle   0 = вихрь, 1 = спираль, 2 = взрыв
 * @param particleColorIdx индекс цвета из палитры (0..7)
 */
public record SkinSelectRequest(byte[] pngBytes, byte particleStyle, byte particleColorIdx) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(pngBytes);
        buf.writeByte(particleStyle);
        buf.writeByte(particleColorIdx);
    }

    public static SkinSelectRequest decode(FriendlyByteBuf buf) {
        return new SkinSelectRequest(buf.readByteArray(), buf.readByte(), buf.readByte());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ServerPlayer sender = ctx.getSender();
        if (sender != null) {
            ServerLogic.select(sender, pngBytes, particleStyle, particleColorIdx);
        }
    }
}
