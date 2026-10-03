package com.skinshift.client;

import com.skinshift.SkinBroadcast;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handle(SkinBroadcast pkt) {
        ClientFx.onPacket(pkt.player(), pkt.pngBytes(), pkt.instant(), pkt.particleStyle(), pkt.particleColorIdx());
    }
}
