package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor a la victima: una Bacteria (su id de entidad) te acaba de agarrar.
 * El cliente le clava la vista en su cara, bloquea los controles y pone la
 * pantalla del agarre (AgarreCliente).
 */
public record Agarrado(int bacteria) implements CustomPacketPayload {
	public static final Type<Agarrado> TYPE = new Type<>(BackroomsEvento.id("agarrado"));
	public static final StreamCodec<FriendlyByteBuf, Agarrado> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeVarInt(p.bacteria),
		buf -> new Agarrado(buf.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
