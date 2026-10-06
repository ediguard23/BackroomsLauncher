package net.backrooms.evento.escondite;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.backrooms.evento.red.EstadoArrastre;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Arrastrarse por el suelo (como en Escape the Backrooms): el jugador se tumba
 * (la postura de nadar de Minecraft fuera del agua, 0,6 de alto) y cabe por los
 * huecos de las paredes, donde la Bacteria no entra. Se activa con su tecla o
 * solo, al agacharse delante de un hueco (ArrastreCliente).
 *
 * La postura la decide cada lado por su cuenta (PlayerMixin): el cliente para
 * moverse sin chocar con el techo del hueco y el servidor para dar por buenos
 * esos movimientos. Por eso el estado va en los dos y el servidor se lo cuenta
 * a todos (si no, a los demas se les veria de pie dentro de la pared).
 */
public final class Arrastre {
	private static final Set<UUID> SERVIDOR = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> CLIENTE = ConcurrentHashMap.newKeySet();

	private Arrastre() {
	}

	public static void registrar() {
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> SERVIDOR.remove(h.player.getUUID()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (entidad instanceof ServerPlayer j) {
				poner(j, false);
			}
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((j, antes, despues) -> poner(j, false));
	}

	/** Si este jugador va arrastrandose (en el lado donde vive). */
	public static boolean arrastrandose(Player p) {
		return (p.level().isClientSide() ? CLIENTE : SERVIDOR).contains(p.getUUID());
	}

	/** El cliente se lo pide al servidor (su tecla o un hueco delante). */
	public static void pedir(ServerPlayer j, boolean si) {
		poner(j, si && !j.isSpectator() && !j.isPassenger());
	}

	private static void poner(ServerPlayer j, boolean si) {
		boolean cambia = si ? SERVIDOR.add(j.getUUID()) : SERVIDOR.remove(j.getUUID());
		if (!cambia) {
			return;
		}
		EstadoArrastre aviso = new EstadoArrastre(j.getUUID(), si);
		for (ServerPlayer otro : j.level().players()) {
			if (otro != j && ServerPlayNetworking.canSend(otro, EstadoArrastre.TYPE)) {
				ServerPlayNetworking.send(otro, aviso);
			}
		}
		if (!si && ServerPlayNetworking.canSend(j, EstadoArrastre.TYPE)) {
			// al morir o cambiar de mundo el cliente tambien tiene que levantarse
			ServerPlayNetworking.send(j, aviso);
		}
	}

	/* ------------------------------------------------------------ cliente */

	public static void cliente(UUID id, boolean si) {
		if (si) {
			CLIENTE.add(id);
		} else {
			CLIENTE.remove(id);
		}
	}

	public static void olvidarCliente() {
		CLIENTE.clear();
	}
}
