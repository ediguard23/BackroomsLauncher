package net.backrooms.evento.red;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * El rotulo de un nivel al llegar («NIVEL FUN / FEEDING ROOMS»): el cliente lo dibuja con la letra del
 * evento, como el «NIVEL 3» del final de la Fiesta (cliente/TituloNivelCliente), en vez del titulo de
 * Minecraft.
 */
public record TituloNivel(String nivel, String sub) implements CustomPacketPayload {
	public static final Type<TituloNivel> TYPE = new Type<>(BackroomsEvento.id("titulo_nivel"));
	public static final StreamCodec<FriendlyByteBuf, TituloNivel> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUtf(p.nivel);
			buf.writeUtf(p.sub);
		},
		buf -> new TituloNivel(buf.readUtf(), buf.readUtf()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * Se lo manda al jugador; si su cliente no tiene el mod (no deberia pasar), el titulo de Minecraft.
	 */
	public static void enviar(net.minecraft.server.level.ServerPlayer j, String nivel, String sub) {
		if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(j, TYPE)) {
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(j, new TituloNivel(nivel, sub));
			return;
		}
		j.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(20, 80, 30));
		j.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
			net.minecraft.network.chat.Component.literal(nivel).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
		j.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
			net.minecraft.network.chat.Component.literal(sub).withStyle(net.minecraft.ChatFormatting.YELLOW)));
	}
}
