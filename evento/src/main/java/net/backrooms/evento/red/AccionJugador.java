package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del cliente al servidor: el jugador enciende o apaga la linterna (F) o
 * levanta o baja la camara (C). El servidor comprueba que lleve el objeto.
 */
public record AccionJugador(int accion, boolean valor) implements CustomPacketPayload {
	public static final int LINTERNA = 0;
	public static final int CAMARA = 1;

	public static final Type<AccionJugador> TYPE = new Type<>(BackroomsEvento.id("accion"));
	public static final StreamCodec<FriendlyByteBuf, AccionJugador> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeByte(p.accion);
			buf.writeBoolean(p.valor);
		},
		buf -> new AccionJugador(buf.readByte(), buf.readBoolean()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
