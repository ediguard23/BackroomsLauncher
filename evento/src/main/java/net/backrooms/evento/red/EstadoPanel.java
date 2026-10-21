package net.backrooms.evento.red;

import java.util.List;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * El panel del staff (PanelAdmin): las lineas de estado en directo de lo general y de cada nivel.
 * Al recibirlo, el cliente abre el panel o, si ya esta abierto, lo refresca.
 */
public record EstadoPanel(List<String> general, List<String> nivel0, List<String> piscinas, List<String> fiesta) implements CustomPacketPayload {
	public static final Type<EstadoPanel> TYPE = new Type<>(BackroomsEvento.id("panel"));
	public static final StreamCodec<FriendlyByteBuf, EstadoPanel> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			escribir(buf, p.general);
			escribir(buf, p.nivel0);
			escribir(buf, p.piscinas);
			escribir(buf, p.fiesta);
		},
		buf -> new EstadoPanel(leer(buf), leer(buf), leer(buf), leer(buf)));

	private static void escribir(FriendlyByteBuf buf, List<String> l) {
		buf.writeVarInt(Math.min(l.size(), 64));
		for (int i = 0; i < Math.min(l.size(), 64); i++) {
			buf.writeUtf(l.get(i), 256);
		}
	}

	private static List<String> leer(FriendlyByteBuf buf) {
		int n = Math.min(buf.readVarInt(), 64);
		List<String> l = new java.util.ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			l.add(buf.readUtf(256));
		}
		return l;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
