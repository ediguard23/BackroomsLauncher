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
 *
 * El silencio (`/backrooms silencio`, boton del panel): mientras dura, los jugadores no pueden hablar por el
 * chat de voz (VozCinematica corta su microfono); solo hablan el staff (OP) y quien tenga el megafono encendido.
 * Asi el staff da indicaciones y todos le oyen. Los jugadores lo ven en la barra de accion.
 */
public final class MegafonoStaff {
	private static final Set<UUID> ACTIVOS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** Quien esta conectado (lo lee el hilo del chat de voz: por eso un conjunto concurrente). */
	private static final Set<UUID> CONECTADOS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static long ticks;
	private static volatile boolean silencio;
	/** Quien puede hablar durante el silencio (el staff): se rehace cada segundo en el hilo del servidor. */
	private static volatile Set<UUID> staff = Set.of();

	private MegafonoStaff() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			ACTIVOS.clear();
			CONECTADOS.clear();
			silencio = false;
		});
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> CONECTADOS.add(h.player.getUUID()));
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			CONECTADOS.remove(h.player.getUUID());
			synchronized (MegafonoStaff.class) {
				ACTIVOS.remove(h.player.getUUID());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> {
			if (++ticks % 20 != 0) {
				return;
			}
			if (silencio) {
				actualizarStaff(s);
				for (ServerPlayer j : s.getPlayerList().getPlayers()) {
					if (!staff.contains(j.getUUID()) && !ACTIVOS.contains(j.getUUID())) {
						j.displayClientMessage(Component.literal("SILENCIO: escucha al staff").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
					}
				}
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

	/** Enciende o apaga el silencio para todos; true si queda encendido. */
	public static boolean alternarSilencio(net.minecraft.server.MinecraftServer s) {
		silencio = !silencio;
		actualizarStaff(s);
		s.getPlayerList().broadcastSystemMessage(silencio
			? Component.literal("SILENCIO: el staff va a hablar. El chat de voz queda cortado para los jugadores.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
			: Component.literal("Fin del silencio: ya podéis volver a hablar.").withStyle(ChatFormatting.GREEN), false);
		return silencio;
	}

	private static void actualizarStaff(net.minecraft.server.MinecraftServer s) {
		Set<UUID> nuevo = new java.util.HashSet<>();
		for (ServerPlayer j : s.getPlayerList().getPlayers()) {
			if (net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(j.createCommandSourceStack())) {
				nuevo.add(j.getUUID());
			}
		}
		staff = Set.copyOf(nuevo);
	}

	public static boolean silencio() {
		return silencio;
	}

	/** Si ese jugador puede hablar ahora (lo pregunta el hilo del chat de voz). */
	public static boolean puedeHablar(UUID u) {
		return !silencio || staff.contains(u) || ACTIVOS.contains(u);
	}

	/** Los jugadores conectados. */
	public static Set<UUID> jugadores() {
		return CONECTADOS;
	}
}
