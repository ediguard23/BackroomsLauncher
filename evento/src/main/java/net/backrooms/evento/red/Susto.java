package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Un susto que el servidor le da a un jugador: el flashbang de mirar a un
 * Smiler (pantalla en blanco y pitido) o el Smiler que te alcanza (su cara
 * encima de la pantalla).
 */
public record Susto(int tipo) implements CustomPacketPayload {
	public static final int FLASH = 0;
	public static final int CARA = 1;

	public static final Type<Susto> TYPE = new Type<>(BackroomsEvento.id("susto"));
	public static final StreamCodec<FriendlyByteBuf, Susto> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeByte(p.tipo),
		buf -> new Susto(buf.readByte()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
