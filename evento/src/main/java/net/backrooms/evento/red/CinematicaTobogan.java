package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Empieza la cinematica del tobogan de las Piscinas a la Fiesta (TransicionFiesta): donde esta la
 * salida (PlanoPiscinas.Salida: la columna de delante de la pared y hacia donde va la pared).
 */
public record CinematicaTobogan(int x, int z, int lado) implements CustomPacketPayload {
	public static final Type<CinematicaTobogan> TYPE = new Type<>(BackroomsEvento.id("cinematica_tobogan"));
	public static final StreamCodec<FriendlyByteBuf, CinematicaTobogan> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeInt(p.x);
			buf.writeInt(p.z);
			buf.writeByte(p.lado);
		},
		buf -> new CinematicaTobogan(buf.readInt(), buf.readInt(), buf.readByte()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
