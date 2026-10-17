package net.backrooms.evento.expedicion;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * El megafono del staff: con `/backrooms megafono` lo que dice un miembro del staff por el chat
 * de voz lo oyen todos los jugadores del servidor, esten donde esten (VozCinematica lo manda
 * como sonido "estatico", sin distancia). Se apaga con el mismo comando o al salir. Mientras
 * esta encendido, el staff lo ve en la barra de accion.
 */
public final class MegafonoStaff {
	private static final Set<UUID> ACTIVOS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** Quien esta conectado (lo lee el hilo del chat de voz: por eso un conjunto concurrente). */
	private static final Set<UUID> CONECTADOS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static long ticks;

	private MegafonoStaff() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			ACTIVOS.clear();
			CONECTADOS.clear();
		});
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> CONECTADOS.add(h.player.getUUID()));
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			CONECTADOS.remove(h.player.getUUID());
			synchronized (MegafonoStaff.class) {
				ACTIVOS.remove(h.player.getUUID());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> {
			if (++ticks % 20 != 0 || ACTIVOS.isEmpty()) {
				return;
			}
			for (UUID u : List.copyOf(ACTIVOS)) {
				ServerPlayer j = s.getPlayerList().getPlayer(u);
				if (j != null) {
					j.displayClientMessage(Component.literal("MEGÁFONO: todos te oyen").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
				}
			}
		});
	}

	/** Lo enciende o lo apaga para ese miembro del staff; true si queda encendido. */
	public static synchronized boolean alternar(ServerPlayer j) {
		if (ACTIVOS.remove(j.getUUID())) {
			j.displayClientMessage(Component.literal("Megáfono apagado: vuelves a hablar solo con los de cerca.").withStyle(ChatFormatting.GRAY), false);
			return false;
		}
		ACTIVOS.add(j.getUUID());
		j.displayClientMessage(Component.literal("Megáfono encendido: todos los jugadores oyen tu voz. Repite el comando para apagarlo.").withStyle(ChatFormatting.GOLD), false);
		return true;
	}

	public static synchronized boolean activo(UUID u) {
		return ACTIVOS.contains(u);
	}

	/** Los jugadores conectados. */
	public static Set<UUID> jugadores() {
		return CONECTADOS;
	}
}
