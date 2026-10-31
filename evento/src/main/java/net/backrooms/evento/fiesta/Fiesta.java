package net.backrooms.evento.fiesta;

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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/**
 * La Fiesta (Nivel Fun, las Feeding Rooms): el nivel de despues de las Piscinas. Quien sube
 * en la cabina de la salida de emergencia de las Piscinas (Tarjetas#cruzar) llega aqui: a un cuartito
 * con una puerta, en un laberinto de fiesta infantil (globos, tartas, juegos, premios).
 *
 * Lo que es de verdad se va sabiendo poco a poco (carteles, dibujos, la cocina, la megafonia):
 * el Proyecto Umbral no construyo este nivel, lo encontro. Las Backrooms aprendieron que a los
 * humanos se les atrae con una fiesta, y las entidades que fueron personas (el Anfitrion)
 * saben como hacerlo. Aqui se "alimenta" al nivel: los tickets que se ganan son lo que cada
 * sujeto le ha dado, y la puerta del final, por la que se sale... o se sirve.
 *
 * Se dan vueltas (PlanoFiesta: laberinto, castillo, sala central) juntando tickets (JuegoFiesta)
 * hasta el objetivo de ConfigFiesta, que abre la puerta del salon final y su tobogan. En los
 * apagones sale el Anfitrion (AnfitrionFiesta). Quien muere aqui cuenta como en el resto de la
 * expedicion; si la muerte no elimina, empieza otra vuelta en una habitacion de inicio.
 */
public final class Fiesta {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, BackroomsEvento.id("fiesta"));
	public static final String NOMBRE = "NIVEL FUN";
	public static final String SUBTITULO = "FEEDING ROOMS";

	private static MinecraftServer servidor;

	private Fiesta() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			servidor = s;
			ConfigFiesta.cargar();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> servidor = null);
	}

	public static boolean es(Level nivel) {
		return nivel.dimension().equals(DIMENSION);
	}

	public static ServerLevel nivel() {
		return servidor == null ? null : servidor.getLevel(DIMENSION);
	}

	/**
	 * Lo deja en una habitacion de inicio al azar, mirando a su puerta, y empieza otra vuelta.
	 * `nueva`: llega de verdad (sus misiones de aqui, el titulo y la bienvenida); si no, reaparece.
	 * False si no hay dimension.
	 */
	public static boolean llegar(ServerPlayer j, boolean nueva) {
		ServerLevel nivel = nivel();
		if (nivel == null) {
			BackroomsEvento.LOG.error("No existe la dimension de la Fiesta ({})", DIMENSION.identifier());
			return false;
		}
		aUnInicio(j);
		j.removeEffect(MobEffects.SLOWNESS);
		j.removeEffect(MobEffects.RESISTANCE);
		Equipo.vestir(j);
		if (nueva) {
			net.backrooms.evento.mision.Misiones.get().asignarFiesta(j);
			JuegoFiesta.get().llega(j);
			JuegoFiesta.get().nuevaVuelta(j);
			net.backrooms.evento.red.TituloNivel.enviar(j, NOMBRE, SUBTITULO);
			Sonidos.aJugador(j, Sonidos.FIESTA_BIENVENIDA, 0.9F);
		}
		if (!nueva) {
			JuegoFiesta.get().nuevaVuelta(j);
		}
		JuegoFiesta.get().enviar(j);
		BackroomsEvento.LOG.info("{} {} a la Fiesta", j.getGameProfile().name(), nueva ? "llega" : "vuelve");
		return true;
	}

	/** A una habitacion de inicio al azar (en el centro del cuartito, mirando a la puerta). */
	public static void aUnInicio(ServerPlayer j) {
		ServerLevel nivel = nivel();
		if (nivel == null || PlanoFiesta.INICIOS.isEmpty()) {
			return;
		}
		double[] p = PlanoFiesta.INICIOS.get(j.getRandom().nextInt(PlanoFiesta.INICIOS.size()));
		j.teleportTo(nivel, p[0], PlanoFiesta.SUELO + 1, p[1], Set.of(), (float) p[2], 0.0F, true);
		j.resetFallDistance();
	}
}
