package com.skinshift;

import com.skinshift.client.ClientPacketHandler;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Сервер -> клиенты: "у игрока такой-то скин (и такие частицы)".
 *
 * @param player           чей скин меняется
 * @param pngBytes         новые PNG-байты (пусто = оригинальный скин)
 * @param instant          true = без анимации (игрок только что попал в зону видимости)
 * @param particleStyle    0 = вихрь, 1 = спираль, 2 = взрыв
 * @param particleColorIdx индекс цвета палитры (0..7)
 */
public record SkinBroadcast(UUID player, byte[] pngBytes, boolean instant, byte particleStyle, byte particleColorIdx) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(player);
        buf.writeByteArray(pngBytes);
        buf.writeBoolean(instant);
        buf.writeByte(particleStyle);
        buf.writeByte(particleColorIdx);
    }

    public static SkinBroadcast decode(FriendlyByteBuf buf) {
        return new SkinBroadcast(buf.readUUID(), buf.readByteArray(), buf.readBoolean(), buf.readByte(), buf.readByte());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPacketHandler.handle(this);
        }
    }
}
