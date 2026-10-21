package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * La Estacion Electrica para el HUD (JuegoEstacion): combustible del generador, paneles
 * reparados, fusibles puestos, la etapa (0..3) y si hay corriente. etapa < 0: fuera del nivel.
 */
public record EstadoEstacion(float combustible, int paneles, int totalPaneles, int fusibles, int etapa, boolean corriente) implements CustomPacketPayload {
	public static final Type<EstadoEstacion> TYPE = new Type<>(BackroomsEvento.id("estacion"));
	public static final StreamCodec<FriendlyByteBuf, EstadoEstacion> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeFloat(p.combustible);
			buf.writeVarInt(p.paneles);
			buf.writeVarInt(p.totalPaneles);
			buf.writeVarInt(p.fusibles);
			buf.writeInt(p.etapa);
			buf.writeBoolean(p.corriente);
		},
		buf -> new EstadoEstacion(buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readBoolean()));

	public static final EstadoEstacion FUERA = new EstadoEstacion(0, 0, 0, 0, -1, false);

	public boolean fuera() {
		return this.etapa < 0;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
