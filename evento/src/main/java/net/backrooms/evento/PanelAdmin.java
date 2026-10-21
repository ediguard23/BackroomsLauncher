package net.backrooms.evento;

import java.util.ArrayList;
import java.util.List;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.expedicion.Expedicion;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.red.EstadoPanel;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * El panel del staff (/backrooms panel): lo que se ve en directo de cada nivel (quien esta, que
 * entidades hay, como esta la luz, las tarjetas, la inundacion...). Lo dibuja el cliente
 * (cliente/PantallaPanel), que ademas sabe que entidades, misiones y sucesos tiene cada nivel y
 * manda los comandos de sus botones. Cada vez que se pulsa algo, el cliente lo vuelve a pedir.
 */
public final class PanelAdmin {
	private PanelAdmin() {
	}

	/** Le manda el estado (y el cliente abre el panel, o lo refresca si ya esta abierto). */
	public static boolean abrir(ServerPlayer j) {
		if (!ServerPlayNetworking.canSend(j, EstadoPanel.TYPE)) {
			return false;
		}
		ServerPlayNetworking.send(j, estado(j.level().getServer()));
		return true;
	}

	private static EstadoPanel estado(MinecraftServer s) {
		ServerLevel nivel0 = s.overworld();
		ServerLevel piscinas = s.getLevel(net.backrooms.evento.piscinas.Piscinas.DIMENSION);
		ServerLevel fiesta = s.getLevel(net.backrooms.evento.fiesta.Fiesta.DIMENSION);

		List<String> general = new ArrayList<>();
		int conectados = s.getPlayerList().getPlayerCount();
		int expedicion = 0;
		for (ServerPlayer j : s.getPlayerList().getPlayers()) {
			if (Misiones.get().enExpedicion(j)) {
				expedicion++;
			}
		}
		general.add("Conectados: " + conectados + "   ·   en la expedición: " + expedicion);
		general.add("En la recepción esperando el /start: " + Expedicion.get().enRecepcion().size()
			+ (Expedicion.get().enMarcha() ? "   ·   bajando ahora: " + Expedicion.get().pendientes() : ""));
		general.add("Escapados: " + Fases.get().escapados().size());
		general.add("Sucesos automáticos (apagones y alarmas): " + (Ambiente.get().automatico() ? "SÍ" : "NO"));

		List<String> cero = new ArrayList<>();
		cero.add("Jugadores aquí: " + nivel0.players().size());
		cero.add("Luz: " + Ambiente.get().resumen(nivel0));
		cero.add("Bacterias: " + contar(nivel0, net.backrooms.evento.entidad.Bacteria.class)
			+ "   ·   Smilers: " + contar(nivel0, net.backrooms.evento.entidad.Smiler.class)
			+ "   ·   acechadores: " + (net.backrooms.evento.entidad.Acechadores.get().activos() ? "activos" : "parados"));

		List<String> p37 = new ArrayList<>();
		if (piscinas == null) {
			p37.add("Este servidor no tiene las Piscinas.");
		} else {
			p37.add("Jugadores aquí: " + piscinas.players().size() + "   ·   Serpientes: " + contar(piscinas, net.backrooms.evento.entidad.Serpiente.class));
			p37.add("Luz: " + Ambiente.get().resumen(piscinas) + "   ·   " + net.backrooms.evento.piscinas.Inundacion.get().resumen());
			p37.addAll(net.backrooms.evento.piscinas.Tarjetas.get().resumen());
		}

		List<String> fun = new ArrayList<>();
		if (fiesta == null) {
			fun.add("Este servidor no tiene la Fiesta.");
		} else {
			fun.add("Jugadores aquí: " + fiesta.players().size() + "   ·   Anfitriones: " + contar(fiesta, net.backrooms.evento.fiesta.Anfitrion.class));
			fun.add("Luz: " + Ambiente.get().resumen(fiesta));
		}
		ServerLevel estacion = s.getLevel(net.backrooms.evento.estacion.Estacion.DIMENSION);
		List<String> n3 = new ArrayList<>();
		if (estacion == null) {
			n3.add("Este servidor no tiene la Estación.");
		} else {
			n3.add("Jugadores aquí: " + estacion.players().size() + "   ·   Amasijos: " + contar(estacion, net.backrooms.evento.estacion.Amasijo.class));
			n3.add("Luz: " + Ambiente.get().resumen(estacion));
			n3.addAll(net.backrooms.evento.estacion.JuegoEstacion.get().resumen());
		}
		return new EstadoPanel(general, cero, p37, fun, n3);
	}

	private static int contar(Level nivel, Class<? extends Entity> clase) {
		if (!(nivel instanceof ServerLevel s)) {
			return 0;
		}
		return s.getEntities(EntityTypeTest.forClass(clase), e -> e.isAlive()).size();
	}
}
