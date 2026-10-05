package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * El ascensor de salida arranca con el jugador dentro: la pantalla se
 * funde a negro con las puertas, el descenso y la campanilla, y al llegar
 * sale el rotulo de la fase nueva (o el de haber escapado).
 *
 * @param fase   fase a la que baja (0 si escapa)
 * @param nombre nombre de esa fase
 * @param dificultad su dificultad
 */
public record ViajeAscensor(int fase, String nombre, String dificultad) implements CustomPacketPayload {
	public static final Type<ViajeAscensor> TYPE = new Type<>(BackroomsEvento.id("ascensor"));
	public static final StreamCodec<FriendlyByteBuf, ViajeAscensor> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(p.fase);
			buf.writeUtf(p.nombre);
			buf.writeUtf(p.dificultad);
		},
		buf -> new ViajeAscensor(buf.readVarInt(), buf.readUtf(), buf.readUtf()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
