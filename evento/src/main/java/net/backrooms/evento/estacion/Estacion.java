package net.backrooms.evento.estacion;

import java.util.Set;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.objetos.Equipo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * La Estacion Electrica (Nivel 3): el nivel de despues de la Fiesta. Quien se tira por el tobogan
 * del salon final de la Fiesta cae aqui, al acceso de una instalacion industrial enorme que sigue
 * funcionando sin nadie: generadores, transformadores, cables, sectores de colores con numeros.
 *
 * Para salir hay que subir los tres pisos (JuegoEstacion: gasolina para el generador, fusibles para
 * las rejas, la central de cada piso) y cruzar la pasarela hasta la puerta encadenada. Cada sistema
 * que vuelve despierta algo mas en los pasillos (AmasijoEstacion). Primeros indicios de que alguien estudia las Backrooms: documentos con
 * numeros en vez de nombres, sectores que no estan en los planos, una radio que se corta.
 */
public final class Estacion {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, BackroomsEvento.id("estacion"));
	public static final String NOMBRE = "NIVEL 3";
	public static final String SUBTITULO = "ESTACIÓN ELÉCTRICA";

	private static MinecraftServer servidor;

	private Estacion() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> servidor = null);
	}

	public static boolean es(Level nivel) {
		return nivel.dimension().equals(DIMENSION);
	}

	public static ServerLevel nivel() {
		return servidor == null ? null : servidor.getLevel(DIMENSION);
	}

	/**
	 * Lo deja en el acceso, mirando al pasillo. `nueva`: llega de verdad (sus misiones, el titulo y la
	 * radio); si no, reaparece. False si no hay dimension.
	 */
	public static boolean llegar(ServerPlayer j, boolean nueva) {
		return llegar(j, nueva, true);
	}

	/**
	 * Igual, y `presentar`: el titulo y la radio al llegar. Sin presentar (la cinematica del ascensor de la
	 * Fiesta), el titulo lo pone el cliente al despertar y la radio, SalidaFiesta al acabar.
	 */
	public static boolean llegar(ServerPlayer j, boolean nueva, boolean presentar) {
		ServerLevel nivel = nivel();
		if (nivel == null) {
			BackroomsEvento.LOG.error("No existe la dimension de la Estacion ({})", DIMENSION.identifier());
			return false;
		}
		// llega a un sitio al azar de la zona de abajo del piso 1; al reaparecer, al piso mas alto abierto
		int piso = nueva ? 0 : JuegoEstacion.get().pisoAbierto();
		double[] l = piso == 0 ? PlanoEstacion.SPAWNS.get(j.getRandom().nextInt(PlanoEstacion.SPAWNS.size())) : PlanoEstacion.LLEGADAS[piso];
		j.teleportTo(nivel, l[0], l[1], l[2], Set.of(), (float) l[3], 0.0F, true);
		j.resetFallDistance();
		Equipo.vestir(j);
		// aqui ya no hace falta la camara: solo la linterna
		var inv = j.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).is(net.backrooms.evento.objetos.Objetos.CAMARA)) {
				inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
			}
		}
		// y la linterna, al ultimo hueco de la barra (el 9): lo que hubiera alli pasa a su hueco
		int ultimo = net.minecraft.world.entity.player.Inventory.getSelectionSize() - 1;
		for (int i = 0; i < net.minecraft.world.entity.player.Inventory.getSelectionSize(); i++) {
			if (i != ultimo && inv.getItem(i).is(net.backrooms.evento.objetos.Objetos.LINTERNA)) {
				net.minecraft.world.item.ItemStack otro = inv.getItem(ultimo);
				inv.setItem(ultimo, inv.getItem(i));
				inv.setItem(i, otro);
				break;
			}
		}
		if (nueva) {
			net.backrooms.evento.mision.Misiones.get().asignarEstacion(j);
		}
		if (nueva && presentar) {
			j.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 30));
			j.connection.send(new ClientboundSetTitleTextPacket(Component.literal(NOMBRE).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
			j.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(SUBTITULO).withStyle(ChatFormatting.YELLOW)));
			Sonidos.aJugador(j, Sonidos.ESTACION_RADIO, 0.9F);
		}
		BackroomsEvento.LOG.info("{} {} a la Estacion", j.getGameProfile().name(), nueva ? "llega" : "vuelve");
		return true;
	}

	/** Ha cruzado la pasarela: la mision hecha y, mientras no haya un nivel siguiente, escapa. */
	public static void salir(ServerPlayer j) {
		net.backrooms.evento.mision.Misiones.get().completar(j, net.backrooms.evento.mision.TipoMision.PASARELA);
		BackroomsEvento.LOG.info("{} cruza la pasarela de la Estacion", j.getGameProfile().name());
		net.backrooms.evento.fase.Fases.get().escapar(j);
	}
}
