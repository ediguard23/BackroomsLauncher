package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Lo de cada uno en la Fiesta (JuegoFiesta): monedas, tickets y los que pide el Gran Premio,
 * las fichas que ya cogio (el cliente no las dibuja), en que juego esta (0 ninguno, 1 el semaforo
 * del castillo) y su bote: el inicial (0 si ya cobro el de esta vuelta), el minimo, lo que baja por
 * minuto y cuando empezo la vuelta (tiempo del nivel; el cliente lo cuenta, MarcadorRenderer).
 * fuera = true: no esta en la Fiesta (el HUD se esconde).
 */
public record EstadoFiesta(boolean fuera, int monedas, int tickets, int objetivo, int[] cogidas, int juego, int bote, int boteMinimo, int botePorMinuto,
	long vueltaDesde) implements CustomPacketPayload {
	public static final Type<EstadoFiesta> TYPE = new Type<>(BackroomsEvento.id("fiesta"));
	public static final StreamCodec<FriendlyByteBuf, EstadoFiesta> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeBoolean(p.fuera);
			buf.writeVarInt(p.monedas);
			buf.writeVarInt(p.tickets);
			buf.writeVarInt(p.objetivo);
			buf.writeVarIntArray(p.cogidas);
			buf.writeVarInt(p.juego);
			buf.writeVarInt(p.bote);
			buf.writeVarInt(p.boteMinimo);
			buf.writeVarInt(p.botePorMinuto);
			buf.writeVarLong(p.vueltaDesde);
		},
		buf -> new EstadoFiesta(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarIntArray(), buf.readVarInt(),
			buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong()));

	public static final EstadoFiesta FUERA = new EstadoFiesta(true, 0, 0, 0, new int[0], 0, 0, 0, 0, 0L);

	/** El bote de la vuelta despues de `ahora - vueltaDesde` ticks (como JuegoFiesta#bote); 0 si ya lo cobro. */
	public int boteAhora(long ahora) {
		if (this.bote <= 0) {
			return 0;
		}
		return Math.max(this.boteMinimo, this.bote - (int) (Math.max(0L, ahora - this.vueltaDesde) * this.botePorMinuto / 1200L));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
