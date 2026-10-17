package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * La inundacion de las Piscinas (Inundacion): a que altura esta el agua y a que velocidad
 * sube o baja (bloques por segundo), para que el cliente la dibuje subiendo sin saltos entre
 * paquete y paquete. nivel < 0: no hay inundacion.
 */
public record EstadoInundacion(float nivel, float velocidad) implements CustomPacketPayload {
	public static final Type<EstadoInundacion> TYPE = new Type<>(BackroomsEvento.id("inundacion"));
	public static final StreamCodec<FriendlyByteBuf, EstadoInundacion> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeFloat(p.nivel);
			buf.writeFloat(p.velocidad);
		},
		buf -> new EstadoInundacion(buf.readFloat(), buf.readFloat()));

	public static final EstadoInundacion NADA = new EstadoInundacion(-1.0F, 0.0F);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
