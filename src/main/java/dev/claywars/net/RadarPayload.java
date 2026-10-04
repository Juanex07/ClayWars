package dev.claywars.net;

import dev.claywars.ClayWars;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Paquete servidor -> cliente con el informe del radar en texto. */
public record RadarPayload(String text) implements CustomPacketPayload {
    public static final Type<RadarPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ClayWars.MODID, "radar"));
    public static final StreamCodec<ByteBuf, RadarPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RadarPayload::text, RadarPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
