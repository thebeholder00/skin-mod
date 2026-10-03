package com.skinshift;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

/** Серверная часть: хранит текущий выбранный скин и настройки частиц каждого игрока, рассылает смену наблюдателям. */
public final class ServerLogic {
    private static final int MAX_SKIN_BYTES = 200_000; // защита от мусора/абьюза по сети

    private static final Map<UUID, byte[]> CURRENT_SKIN = new HashMap<>();
    private static final Map<UUID, Byte> CURRENT_STYLE = new HashMap<>();
    private static final Map<UUID, Byte> CURRENT_COLOR = new HashMap<>();
    private static final Map<UUID, Long> BUSY_UNTIL = new HashMap<>();

    private ServerLogic() {}

    public static void select(ServerPlayer player, byte[] pngBytes, byte style, byte colorIdx) {
        if (pngBytes.length > MAX_SKIN_BYTES) return;

        UUID id = player.getUUID();
        long now = player.level().getGameTime();

        Long busy = BUSY_UNTIL.get(id);
        if (busy != null && busy > now) {
            return; // анимация ещё идёт
        }

        if (pngBytes.length == 0) {
            CURRENT_SKIN.remove(id);
        } else {
            CURRENT_SKIN.put(id, pngBytes);
        }
        CURRENT_STYLE.put(id, style);
        CURRENT_COLOR.put(id, colorIdx);
        BUSY_UNTIL.put(id, now + Anim.TOTAL + 2);

        Net.CHANNEL.send(new SkinBroadcast(id, pngBytes, false, style, colorIdx),
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    /** Когда игрок X попадает в зону видимости игрока Y — отправляем Y текущий скин X (без анимации). */
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            byte[] bytes = CURRENT_SKIN.get(target.getUUID());
            if (bytes != null) {
                byte style = CURRENT_STYLE.getOrDefault(target.getUUID(), (byte) 0);
                byte color = CURRENT_COLOR.getOrDefault(target.getUUID(), (byte) 0);
                Net.CHANNEL.send(new SkinBroadcast(target.getUUID(), bytes, true, style, color),
                        PacketDistributor.PLAYER.with(viewer));
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        CURRENT_SKIN.remove(id);
        CURRENT_STYLE.remove(id);
        CURRENT_COLOR.remove(id);
        BUSY_UNTIL.remove(id);
    }
}
