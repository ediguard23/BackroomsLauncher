package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor a quien se mete en el ascensor del salon final de la Fiesta: su cliente pone la
 * cinematica de la salida (SalidaFiestaCliente). Los sitios salen de PlanoFiesta.
 */
public record CinematicaSalidaFiesta() implements CustomPacketPayload {
	public static final Type<CinematicaSalidaFiesta> TYPE = new Type<>(BackroomsEvento.id("cinematica_salida_fiesta"));
	public static final StreamCodec<FriendlyByteBuf, CinematicaSalidaFiesta> CODEC = StreamCodec.unit(new CinematicaSalidaFiesta());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
