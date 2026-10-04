package dev.claywars.client;

import dev.claywars.net.RadarPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Solo se ejecuta en el cliente: abre la pantalla del radar. */
public class ClientNet {
    public static void handleRadar(RadarPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new RadarScreen(payload.text())));
    }
}
