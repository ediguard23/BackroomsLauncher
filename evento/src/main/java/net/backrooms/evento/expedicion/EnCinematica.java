package net.backrooms.evento.expedicion;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Quien esta viendo una cinematica (la del /start o la bajada a las Piscinas): mientras
 * dura no habla ni oye a nadie por el chat de voz (VozCinematica) ni puede escribir en el
 * chat, para verla tranquilo. Moverse ya no puede: la pantalla de la cinematica se queda
 * con el teclado.
 *
 * Se apunta por tiempo real porque el chat de voz va en sus propios hilos.
 */
public final class EnCinematica {
	/** UUID -> hasta cuando (System.currentTimeMillis). */
	private static final Map<UUID, Long> HASTA = new ConcurrentHashMap<>();

	private EnCinematica() {
	}

	public static void registrar() {
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((mensaje, jugador, tipo) -> {
			if (!en(jugador.getUUID())) {
				return true;
			}
			jugador.displayClientMessage(Component.literal("No se puede hablar durante la cinemática.").withStyle(ChatFormatting.GRAY), true);
			return false;
		});
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> HASTA.remove(h.player.getUUID()));
	}

	/** Empieza una cinematica de esos segundos para el jugador. */
	public static void poner(ServerPlayer j, double segundos) {
		HASTA.put(j.getUUID(), System.currentTimeMillis() + Math.round(segundos * 1000));
	}

	public static boolean en(UUID id) {
		Long h = HASTA.get(id);
		if (h == null) {
			return false;
		}
		if (System.currentTimeMillis() < h) {
			return true;
		}
		HASTA.remove(id, h);
		return false;
	}
}
