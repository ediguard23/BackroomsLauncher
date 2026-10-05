package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cordura y estamina del jugador, del servidor a su cliente (cuando cambian):
 * para dibujarlas y para las alucinaciones, que son cosa del cliente.
 *
 * @param cordura  0..100
 * @param estamina 0..100
 * @param agotado  sin aliento: no puede correr hasta recuperarse un poco
 * @param activa   si la supervivencia cuenta (en una fase del Nivel 0)
 */
public record EstadoJugador(float cordura, float estamina, boolean agotado, boolean activa) implements CustomPacketPayload {
	public static final Type<EstadoJugador> TYPE = new Type<>(BackroomsEvento.id("estado"));
	public static final StreamCodec<FriendlyByteBuf, EstadoJugador> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeFloat(p.cordura);
			buf.writeFloat(p.estamina);
			buf.writeBoolean(p.agotado);
			buf.writeBoolean(p.activa);
		},
		buf -> new EstadoJugador(buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
