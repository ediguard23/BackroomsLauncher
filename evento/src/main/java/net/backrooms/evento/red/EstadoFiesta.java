package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Lo de cada uno en la Fiesta (JuegoFiesta): monedas, tickets y los que pide el Gran Premio,
 * las fichas que ya cogio (el cliente no las dibuja) y en que juego esta (0 ninguno, 1 el
 * semaforo). fuera = true: no esta en la Fiesta (el HUD se esconde).
 */
public record EstadoFiesta(boolean fuera, int monedas, int tickets, int objetivo, int[] cogidas, int juego) implements CustomPacketPayload {
	public static final Type<EstadoFiesta> TYPE = new Type<>(BackroomsEvento.id("fiesta"));
	public static final StreamCodec<FriendlyByteBuf, EstadoFiesta> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeBoolean(p.fuera);
			buf.writeVarInt(p.monedas);
			buf.writeVarInt(p.tickets);
			buf.writeVarInt(p.objetivo);
			buf.writeVarIntArray(p.cogidas);
			buf.writeVarInt(p.juego);
		},
		buf -> new EstadoFiesta(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarIntArray(), buf.readVarInt()));

	public static final EstadoFiesta FUERA = new EstadoFiesta(true, 0, 0, 0, new int[0], 0);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
