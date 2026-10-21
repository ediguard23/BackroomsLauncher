package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * La Estacion Electrica para el HUD (JuegoEstacion): gasolina del generador, el piso donde esta el
 * jugador (0..2), lo puesto en la central de ese piso y lo que pide, cuantas centrales estan activas
 * (0..3), si hay corriente y si la cadena del final esta cortada. piso < 0: fuera del nivel.
 */
public record EstadoEstacion(float combustible, int piso, int puestos, int necesarios, int centrales, boolean corriente, boolean cadena) implements CustomPacketPayload {
	public static final Type<EstadoEstacion> TYPE = new Type<>(BackroomsEvento.id("estacion"));
	public static final StreamCodec<FriendlyByteBuf, EstadoEstacion> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeFloat(p.combustible);
			buf.writeInt(p.piso);
			buf.writeVarInt(p.puestos);
			buf.writeVarInt(p.necesarios);
			buf.writeVarInt(p.centrales);
			buf.writeBoolean(p.corriente);
			buf.writeBoolean(p.cadena);
		},
		buf -> new EstadoEstacion(buf.readFloat(), buf.readInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean()));

	public static final EstadoEstacion FUERA = new EstadoEstacion(0, -1, 0, 0, 0, false, false);

	public boolean fuera() {
		return this.piso < 0;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
