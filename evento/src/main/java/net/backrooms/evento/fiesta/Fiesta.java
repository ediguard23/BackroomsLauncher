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
 * en la cabina de la salida de emergencia de las Piscinas (Tarjetas#cruzar) llega aqui, en un
 * ascensor que se abre a una fiesta infantil: globos, tartas, juegos, premios.
 *
 * Lo que es de verdad se va sabiendo poco a poco (carteles, dibujos, la cocina, la megafonia):
 * el Proyecto Umbral no construyo este nivel, lo encontro. Las Backrooms aprendieron que a los
 * humanos se les atrae con una fiesta, y las entidades que fueron personas (el Anfitrion)
 * saben como hacerlo. Aqui se "alimenta" al nivel: los tickets que se ganan son lo que cada
 * sujeto le ha dado, y el Gran Premio, la puerta por la que se sale... o se sirve.
 *
 * Se juntan tickets (JuegoFiesta: monedas para abrir puertas, tickets por las salas y los
 * juegos, como el semaforo) hasta el objetivo de ConfigFiesta y se canjea el Gran Premio. En
 * los apagones sale el Anfitrion (AnfitrionFiesta). Quien muere aqui cuenta como en el resto
 * de la expedicion; si la muerte no elimina, vuelve a la llegada.
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
	 * Lo deja en el ascensor de la llegada, mirando a la fiesta. `nueva`: llega de verdad
	 * (sus misiones de aqui, el titulo y la bienvenida); si no, reaparece. False si no hay
	 * dimension.
	 */
	public static boolean llegar(ServerPlayer j, boolean nueva) {
		ServerLevel nivel = nivel();
		if (nivel == null) {
			BackroomsEvento.LOG.error("No existe la dimension de la Fiesta ({})", DIMENSION.identifier());
			return false;
		}
		// unos pasos de lado a lado para que no aparezcan todos uno encima de otro
		double dx = (j.getRandom().nextDouble() - 0.5) * 3.0;
		j.teleportTo(nivel, PlanoFiesta.LLEGADA_X + dx, PlanoFiesta.LLEGADA_Y, PlanoFiesta.LLEGADA_Z, Set.of(), 180.0F, 0.0F, true);
		j.resetFallDistance();
		j.removeEffect(MobEffects.SLOWNESS);
		j.removeEffect(MobEffects.RESISTANCE);
		Equipo.vestir(j);
		if (nueva) {
			net.backrooms.evento.mision.Misiones.get().asignarFiesta(j);
			JuegoFiesta.get().llega(j);
			j.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 30));
			j.connection.send(new ClientboundSetTitleTextPacket(Component.literal(NOMBRE).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
			j.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(SUBTITULO).withStyle(ChatFormatting.YELLOW)));
			Sonidos.aJugador(j, Sonidos.FIESTA_BIENVENIDA, 0.9F);
		}
		JuegoFiesta.get().enviar(j);
		BackroomsEvento.LOG.info("{} {} a la Fiesta", j.getGameProfile().name(), nueva ? "llega" : "vuelve");
		return true;
	}
}
