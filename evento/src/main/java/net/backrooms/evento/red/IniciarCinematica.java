package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor al cliente: empieza la cinematica del ascensor. El servidor
 * teletransporta al jugador Expedicion.RETRASO_VIAJE ticks despues, cuando su
 * pantalla ya esta en negro.
 */
public record IniciarCinematica() implements CustomPacketPayload {
	public static final IniciarCinematica INSTANCIA = new IniciarCinematica();
	public static final Type<IniciarCinematica> TYPE = new Type<>(BackroomsEvento.id("cinematica"));
	public static final StreamCodec<FriendlyByteBuf, IniciarCinematica> CODEC = StreamCodec.unit(INSTANCIA);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
