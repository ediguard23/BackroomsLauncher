package net.backrooms.evento.red;

import java.util.ArrayList;
import java.util.List;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Estado de las misiones de un jugador, del servidor a su cliente (cada
 * segundo): las tres misiones en orden, cual va, los casetes y la senal del
 * casete pendiente mas cercano (distancia y rumbo en grados, como el yaw de
 * Minecraft) para la pantalla de pausa.
 *
 * @param misiones  nombres de TipoMision; vacio si aun no hay expedicion
 * @param actual    indice de la mision en curso (== misiones.size() si las hizo todas)
 * @param distancia metros hasta el casete mas cercano (solo si esta a menos de
 *                  Misiones.ALCANCE_SENAL), -2 si busca casetes pero no hay senal,
 *                  -1 si no aplica
 */
public record SyncMisiones(List<String> misiones, int actual, int casetes, int necesarios, int distancia, float rumbo)
	implements CustomPacketPayload {

	public static final Type<SyncMisiones> TYPE = new Type<>(BackroomsEvento.id("misiones"));
	public static final StreamCodec<FriendlyByteBuf, SyncMisiones> CODEC = CustomPacketPayload.codec(SyncMisiones::escribir, SyncMisiones::leer);

	public static final SyncMisiones VACIO = new SyncMisiones(List.of(), 0, 0, 0, -1, 0);

	private void escribir(FriendlyByteBuf buf) {
		buf.writeVarInt(this.misiones.size());
		for (String m : this.misiones) {
			buf.writeUtf(m);
		}
		buf.writeVarInt(this.actual);
		buf.writeVarInt(this.casetes);
		buf.writeVarInt(this.necesarios);
		buf.writeInt(this.distancia);
		buf.writeFloat(this.rumbo);
	}

	private static SyncMisiones leer(FriendlyByteBuf buf) {
		int n = buf.readVarInt();
		List<String> m = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			m.add(buf.readUtf());
		}
		return new SyncMisiones(m, buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readFloat());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
