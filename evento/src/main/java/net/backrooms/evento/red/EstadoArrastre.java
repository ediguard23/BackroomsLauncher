package net.backrooms.evento.red;

import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Del servidor a los clientes: tal jugador se ha tumbado (o levantado). Ver Arrastre. */
public record EstadoArrastre(UUID jugador, boolean si) implements CustomPacketPayload {
	public static final Type<EstadoArrastre> TYPE = new Type<>(BackroomsEvento.id("arrastre"));
	public static final StreamCodec<FriendlyByteBuf, EstadoArrastre> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUUID(p.jugador);
			buf.writeBoolean(p.si);
		},
		buf -> new EstadoArrastre(buf.readUUID(), buf.readBoolean()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
