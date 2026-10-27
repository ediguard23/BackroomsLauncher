package net.backrooms.evento.red;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Del servidor a todos: el numero de participante de cada jugador (va en la espalda del traje). */
public record NumerosParticipantes(Map<UUID, Integer> numeros) implements CustomPacketPayload {
	public static final Type<NumerosParticipantes> TYPE = new Type<>(BackroomsEvento.id("numeros_participantes"));
	public static final StreamCodec<FriendlyByteBuf, NumerosParticipantes> CODEC = StreamCodec.composite(
		ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.VAR_INT), NumerosParticipantes::numeros,
		NumerosParticipantes::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
