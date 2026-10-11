package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor a un jugador: que oiga ya uno de los sonidos sueltos de las Piscinas
 * (AmbientePiscinas: gota, agua, orilla, chapoteo, tuberia, grave, grunido, voz, susurro).
 * Solo para el staff (/backrooms piscinas oir): solos ya suenan cuando les toca.
 */
public record SonidoPiscinas(String que) implements CustomPacketPayload {
	public static final Type<SonidoPiscinas> TYPE = new Type<>(BackroomsEvento.id("sonido_piscinas"));
	public static final StreamCodec<FriendlyByteBuf, SonidoPiscinas> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeUtf(p.que, 32),
		buf -> new SonidoPiscinas(buf.readUtf(32)));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
