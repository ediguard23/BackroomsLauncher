package net.backrooms.evento.expedicion;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.objetos.Equipo;
import net.backrooms.evento.red.IniciarCinematica;
import net.backrooms.evento.supervivencia.Supervivencia;
import net.backrooms.evento.vestibulo.Vestibulo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * El /start: abre los ascensores del vestibulo y quien entra en uno baja al
 * Nivel 0 (subir); /start todos o /start <jugadores> los mandan sin ascensor.
 * Con el /start la luz del Nivel 0 empieza de cero: los apagones y las
 * alarmas se sortean desde que llega el primero (Ambiente#reiniciar).
 *
 * Los que bajan esperan en una cola. Cada medio segundo salen LOTE jugadores: reciben la cinematica del
 * ascensor y, RETRASO_VIAJE ticks despues (con su pantalla en negro tras el
 * golpe), se les teletransporta a su zona y se les dan las misiones alli.
 * Las zonas se reparten por toda la fase 1 (10k x 10k) con una separacion
 * minima, para que cada uno empiece solo (Fases.zonaNueva).
 *
 * Si alguien se desconecta antes de viajar, viaja (con su cinematica) al
 * volver a entrar.
 */
public final class Expedicion {
	/** Ticks entre que empieza la cinematica y el viaje: 41 s, la pantalla ya esta en negro tras el golpe. */
	public static final int RETRASO_VIAJE = 820;
	private static final int LOTE = 3;
	private static final int CADA = 10;

	private static final Expedicion INSTANCIA = new Expedicion();

	public static Expedicion get() {
		return INSTANCIA;
	}

	private MinecraftServer servidor;
	private final ArrayDeque<UUID> cola = new ArrayDeque<>();
	private final Map<UUID, Long> viajes = new HashMap<>();
	private final Map<UUID, BlockPos> destinos = new HashMap<>();
	/** Los que ya bajaron desde que se abrieron los ascensores: no vuelven a bajar si vuelven al vestibulo. */
	private final Set<UUID> embarcados = new HashSet<>();
	private final Random azar = new Random();
	private long ticks;

	private Expedicion() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.alVolver(h.player));
	}

	/** El /start: luz del Nivel 0 de cero y ascensores abiertos. */
	public void abrir() {
		this.embarcados.clear();
		ServerLevel v = Vestibulo.get().nivel();
		Fases.get().previstos(v == null ? 0 : (int) v.players().stream().filter(j -> !j.isCreative() && !j.isSpectator()).count());
		Ambiente.get().reiniciar(this.servidor.overworld());
		Vestibulo.get().ascensores(true);
	}

	/** Cierra los ascensores: el siguiente /start es otra ronda (pueden volver a bajar todos). */
	public void cerrar() {
		Vestibulo.get().ascensores(false);
		this.embarcados.clear();
	}

	/**
	 * Alguien entra en la cabina de un ascensor abierto: al hueco de espera y a la cola de
	 * la cinematica. Quien ya esta jugando una fase o ya bajo en esta ronda se queda.
	 */
	public boolean subir(ServerPlayer j) {
		if (this.embarcados.contains(j.getUUID()) || !this.poner(j)) {
			return false;
		}
		Vestibulo.get().alHueco(j);
		return true;
	}

	/** Pone en la cola a estos jugadores (sin ascensor). Devuelve cuantos. */
	public int empezar(List<ServerPlayer> jugadores) {
		// si ya hay gente jugando (se manda a uno que llego tarde) la luz y el reparto siguen como iban
		if (this.cola.isEmpty() && this.viajes.isEmpty()
			&& this.servidor.overworld().players().stream().noneMatch(Misiones.get()::enExpedicion)) {
			Ambiente.get().reiniciar(this.servidor.overworld());
			Fases.get().previstos(jugadores.size());
		}
		int n = 0;
		for (ServerPlayer j : jugadores) {
			if (this.poner(j)) {
				n++;
			}
		}
		return n;
	}

	private boolean poner(ServerPlayer j) {
		UUID id = j.getUUID();
		if (this.pendiente(j) || Misiones.get().enExpedicion(j)) {
			return false;
		}
		this.embarcados.add(id);
		this.destinos.put(id, this.zonaNueva());
		this.cola.add(id);
		return true;
	}

	/** true si hay alguien bajando o jugando una fase. */
	public boolean enMarcha() {
		return !this.cola.isEmpty() || !this.viajes.isEmpty()
			|| (this.servidor != null && this.servidor.getPlayerList().getPlayers().stream().anyMatch(Misiones.get()::enExpedicion));
	}

	/** true si esta en la cola o esperando el viaje. */
	public boolean pendiente(ServerPlayer j) {
		return this.cola.contains(j.getUUID()) || this.viajes.containsKey(j.getUUID());
	}

	/** Solo la cinematica, sin viaje (para verla). */
	public void verCinematica(ServerPlayer j) {
		ServerPlayNetworking.send(j, IniciarCinematica.INSTANCIA);
	}

	public int pendientes() {
		return this.cola.size() + this.viajes.size();
	}

	private void tick() {
		if (this.servidor == null) {
			return;
		}
		this.ticks++;
		if (this.ticks % CADA == 0) {
			for (int i = 0; i < LOTE && !this.cola.isEmpty(); i++) {
				UUID id = this.cola.poll();
				ServerPlayer j = this.servidor.getPlayerList().getPlayer(id);
				if (j == null) {
					this.viajes.put(id, -1L); // viaja al volver
					continue;
				}
				ServerPlayNetworking.send(j, IniciarCinematica.INSTANCIA);
				this.viajes.put(id, this.ticks + RETRASO_VIAJE);
			}
		}
		if (this.viajes.isEmpty()) {
			return;
		}
		this.viajes.entrySet().removeIf(e -> {
			if (e.getValue() < 0 || e.getValue() > this.ticks) {
				return false;
			}
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(e.getKey());
			if (j == null) {
				e.setValue(-1L);
				return false;
			}
			this.viajar(j);
			return true;
		});
	}

	private void alVolver(ServerPlayer j) {
		Long cuando = this.viajes.get(j.getUUID());
		if (cuando != null && cuando < 0) {
			this.viajes.remove(j.getUUID());
			this.cola.addFirst(j.getUUID());
		}
	}

	private void viajar(ServerPlayer j) {
		ServerLevel nivel = this.servidor.overworld();
		BlockPos destino = this.destinos.remove(j.getUUID());
		if (destino == null) {
			destino = this.zonaNueva();
		}
		// sentado en la butaca del vestibulo: levantarle antes de cambiar de mundo
		if (j.isPassenger()) {
			j.stopRiding();
		}
		// despierta tumbado mirando a los tubos del techo
		j.teleportTo(nivel, destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, Set.of(), this.azar.nextFloat() * 360.0F - 180.0F, -70.0F, true);
		j.resetFallDistance();
		Equipo.vestir(j);
		// cada expedicion empieza con la cabeza fresca: la cordura se guarda con el jugador y,
		// si acabo la anterior a 0 sin morir de verdad, moria nada mas llegar
		Supervivencia.get().cordura(j, 100.0F);
		j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 25, 1, false, false, false));
		Misiones.get().asignar(j, destino, Fase.TODAS[0]);
		net.backrooms.evento.supervivencia.Comida.get().repartirAguas(nivel, destino);
		BackroomsEvento.LOG.info("{} ha caido en el Nivel 0 en {} {}", j.getGameProfile().name(), destino.getX(), destino.getZ());
	}

	/** Zona al azar de la fase 1, separada de las ya repartidas. */
	private BlockPos zonaNueva() {
		return Fases.get().zonaNueva(this.servidor.overworld(), Fase.TODAS[0]);
	}
}
