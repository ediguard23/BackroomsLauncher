package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Como esta la luz en la dimension del jugador: apagon (oscuridad total, solo
 * se ve con linterna) o alarma (los tubos en rojo). Nunca las dos a la vez.
 * Se manda al cambiar y al entrar en la dimension.
 */
public record EstadoAmbiente(boolean apagon, boolean alarma) implements CustomPacketPayload {
	public static final Type<EstadoAmbiente> TYPE = new Type<>(BackroomsEvento.id("ambiente"));
	public static final StreamCodec<FriendlyByteBuf, EstadoAmbiente> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeByte((p.apagon ? 1 : 0) | (p.alarma ? 2 : 0)),
		buf -> {
			int b = buf.readByte();
			return new EstadoAmbiente((b & 1) != 0, (b & 2) != 0);
		});

	public static final EstadoAmbiente NORMAL = new EstadoAmbiente(false, false);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
