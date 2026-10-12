package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Del servidor a quien acaba de grabar a la Serpiente lo que pedia su mision: el visor de su
 * camara muestra "EVIDENCIA OBTENIDA — SUJETO CONFIRMADO" (CamaraHud).
 */
public record Evidencia() implements CustomPacketPayload {
	public static final Type<Evidencia> TYPE = new Type<>(BackroomsEvento.id("evidencia"));
	public static final StreamCodec<FriendlyByteBuf, Evidencia> CODEC = StreamCodec.unit(new Evidencia());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
