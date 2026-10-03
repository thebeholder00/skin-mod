package com.skinshift;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

public final class Net {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(SkinShiftMod.MODID, "main"))
            .networkProtocolVersion(2)
            .simpleChannel();

    private Net() {}

    public static void init() {
        // клиент -> сервер: "я выбрал вот такой скин в колесе"
        CHANNEL.messageBuilder(SkinSelectRequest.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkinSelectRequest::encode)
                .decoder(SkinSelectRequest::decode)
                .consumerMainThread(SkinSelectRequest::handle)
                .add();

        // сервер -> клиенты: "у игрока X теперь вот такой скин"
        CHANNEL.messageBuilder(SkinBroadcast.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkinBroadcast::encode)
                .decoder(SkinBroadcast::decode)
                .consumerMainThread(SkinBroadcast::handle)
                .add();
    }
}
