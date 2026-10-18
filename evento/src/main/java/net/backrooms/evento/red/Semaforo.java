package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * El semaforo de la Fiesta (JuegoSemaforo) a quien esta en su pasillo: 0 parado (nadie juega),
 * 1 luz verde, 2 luz roja. El cliente pone la musica de la verde y la corta en la roja.
 */
public record Semaforo(int luz) implements CustomPacketPayload {
	public static final Type<Semaforo> TYPE = new Type<>(BackroomsEvento.id("semaforo"));
	public static final StreamCodec<FriendlyByteBuf, Semaforo> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeVarInt(p.luz),
		buf -> new Semaforo(buf.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
