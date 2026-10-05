package net.backrooms.evento.red;

import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Alguien ha caido: a todos los jugadores se les muestra la animacion de la
 * eliminacion con su nick y su cara, y cuantos exploradores quedan.
 */
public record Eliminado(String nombre, UUID id, int quedan) implements CustomPacketPayload {
	public static final Type<Eliminado> TYPE = new Type<>(BackroomsEvento.id("eliminado"));
	public static final StreamCodec<FriendlyByteBuf, Eliminado> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUtf(p.nombre, 16);
			buf.writeUUID(p.id);
			buf.writeVarInt(p.quedan);
		},
		buf -> new Eliminado(buf.readUtf(16), buf.readUUID(), buf.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
