package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor a quien entra en el ascensor del final de la Estacion (Nivel 3): su cliente pone la
 * cinematica del final (SalidaEstacionCliente) y despues, en el vestibulo, la llegada. Los sitios salen
 * de PlanoEstacion y PlanoVestibulo.
 */
public record CinematicaSalidaEstacion() implements CustomPacketPayload {
	public static final Type<CinematicaSalidaEstacion> TYPE = new Type<>(BackroomsEvento.id("cinematica_salida_estacion"));
	public static final StreamCodec<FriendlyByteBuf, CinematicaSalidaEstacion> CODEC = StreamCodec.unit(new CinematicaSalidaEstacion());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
