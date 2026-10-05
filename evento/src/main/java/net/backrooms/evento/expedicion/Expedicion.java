package net.backrooms.evento.expedicion;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.objetos.Equipo;
import net.backrooms.evento.red.IniciarCinematica;
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
 * El /start: manda a todos al Nivel 0 poco a poco.
 *
 * Cada medio segundo salen LOTE jugadores: reciben la cinematica del
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
	private final Random azar = new Random();
	private long ticks;

	private Expedicion() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.alVolver(h.player));
	}

	/** Pone en la cola a estos jugadores. Devuelve cuantos. */
	public int empezar(List<ServerPlayer> jugadores) {
		int n = 0;
		for (ServerPlayer j : jugadores) {
			UUID id = j.getUUID();
			if (this.cola.contains(id) || this.viajes.containsKey(id)) {
				continue;
			}
			this.destinos.put(id, this.zonaNueva());
			this.cola.add(id);
			n++;
		}
		return n;
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
		// despierta tumbado mirando a los tubos del techo
		j.teleportTo(nivel, destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, Set.of(), this.azar.nextFloat() * 360.0F - 180.0F, -70.0F, true);
		j.resetFallDistance();
		Equipo.vestir(j);
		j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 25, 1, false, false, false));
		Misiones.get().asignar(j, destino, Fase.TODAS[0]);
		BackroomsEvento.LOG.info("{} ha caido en el Nivel 0 en {} {}", j.getGameProfile().name(), destino.getX(), destino.getZ());
	}

	/** Zona al azar de la fase 1, separada de las ya repartidas. */
	private BlockPos zonaNueva() {
		return Fases.get().zonaNueva(this.servidor.overworld(), Fase.TODAS[0]);
	}
}
