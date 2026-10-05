package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor al cliente, cada pocos ticks si cambia: su linterna y su
 * camara (el servidor manda: si no lleva el objeto, se apagan) y las
 * entidades de los jugadores cercanos con la linterna encendida, para
 * dibujar sus focos en el apagon.
 */
public record Herramientas(boolean linterna, boolean camara, int[] linternasCerca) implements CustomPacketPayload {
	public static final Type<Herramientas> TYPE = new Type<>(BackroomsEvento.id("herramientas"));
	public static final StreamCodec<FriendlyByteBuf, Herramientas> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeBoolean(p.linterna);
			buf.writeBoolean(p.camara);
			buf.writeVarIntArray(p.linternasCerca);
		},
		buf -> new Herramientas(buf.readBoolean(), buf.readBoolean(), buf.readVarIntArray()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
