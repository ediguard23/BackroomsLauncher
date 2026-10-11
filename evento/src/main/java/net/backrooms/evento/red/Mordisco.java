package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor a la victima: una Serpiente (su id de entidad) se le esta echando encima.
 * El cliente le clava la vista en su craneo, bloquea los controles y pone la pantalla del
 * ataque (MordiscoCliente) hasta que muere.
 */
public record Mordisco(int serpiente) implements CustomPacketPayload {
	public static final Type<Mordisco> TYPE = new Type<>(BackroomsEvento.id("mordisco"));
	public static final StreamCodec<FriendlyByteBuf, Mordisco> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeVarInt(p.serpiente),
		buf -> new Mordisco(buf.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
