package net.backrooms.evento.ambiente;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.red.EstadoAmbiente;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Apagones y alarmas de cada fase del Nivel 0.
 *
 * Apagon: se va la luz en toda la fase de golpe (sin ciclo fijo, a ratos al
 * azar). Oscuridad total: sin linterna no se ve nada, solo se oye; el zumbido
 * de los tubos se corta. Es cuando salen los Smilers.
 * Alarma: los tubos se ponen en rojo y suena la sirena; la Bacteria esta mas
 * furiosa. Es lo que hay que grabar en la mision de las alarmas.
 *
 * Nunca coinciden. La frecuencia y la duracion salen de la Fase. El staff los
 * fuerza con /backrooms apagon|alarma|luz.
 */
public final class Ambiente {
	private static final Ambiente INSTANCIA = new Ambiente();
	/** Minimo entre el final de un suceso y el principio del otro. */
	private static final int RESPIRO = 20 * 60;

	public static Ambiente get() {
		return INSTANCIA;
	}

	private static final class Estado {
		boolean apagon;
		boolean alarma;
		long fin;
		long proximoApagon = -1;
		long proximaAlarma = -1;
	}

	private final Map<ResourceKey<Level>, Estado> estados = new HashMap<>();
	private final Random azar = new Random();
	private MinecraftServer servidor;
	private long ticks;
	private boolean automatico = true;

	private Ambiente() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.enviar(h.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((j, de, a) -> INSTANCIA.enviar(j));
		ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> INSTANCIA.enviar(nuevo));
	}

	/* ------------------------------------------------------ consultas */

	private Estado estado(ResourceKey<Level> dim) {
		return this.estados.computeIfAbsent(dim, k -> new Estado());
	}

	public boolean apagon(Level nivel) {
		Estado e = this.estados.get(nivel.dimension());
		return e != null && e.apagon;
	}

	public boolean alarma(Level nivel) {
		Estado e = this.estados.get(nivel.dimension());
		return e != null && e.alarma;
	}

	public boolean automatico() {
		return this.automatico;
	}

	public void automatico(boolean si) {
		this.automatico = si;
	}

	/* --------------------------------------------------------- tiempo */

	private void tick() {
		if (this.servidor == null) {
			return;
		}
		this.ticks++;
		for (ServerLevel nivel : this.servidor.getAllLevels()) {
			Fase f = Fase.de(nivel);
			if (f == null) {
				continue;
			}
			Estado e = this.estado(nivel.dimension());
			if (e.apagon || e.alarma) {
				if (this.ticks >= e.fin) {
					this.terminar(nivel);
				}
				continue;
			}
			if (!this.automatico || nivel.players().isEmpty()) {
				continue;
			}
			if (e.proximoApagon < 0) {
				this.programar(e, f);
			}
			if (this.ticks >= e.proximoApagon) {
				this.apagon(nivel, segundos(f.apagonDura()));
			} else if (this.ticks >= e.proximaAlarma) {
				this.alarma(nivel, segundos(f.alarmaDura()));
			}
		}
	}

	private int segundos(int[] rango) {
		return rango[0] + this.azar.nextInt(rango[1] - rango[0] + 1);
	}

	/** Cuando toca el siguiente apagon y la siguiente alarma, sin que se pisen. */
	private void programar(Estado e, Fase f) {
		long a = this.ticks + minutos(f.apagonCada());
		long b = this.ticks + minutos(f.alarmaCada());
		if (Math.abs(a - b) < RESPIRO * 2) {
			b = a + RESPIRO * 2 + this.azar.nextInt(RESPIRO);
		}
		e.proximoApagon = a;
		e.proximaAlarma = b;
	}

	private long minutos(int[] rango) {
		return (long) ((rango[0] + this.azar.nextDouble() * (rango[1] - rango[0])) * 60 * 20);
	}

	/* ------------------------------------------------------- sucesos */

	public void apagon(ServerLevel nivel, int segundos) {
		Estado e = this.estado(nivel.dimension());
		e.apagon = true;
		e.alarma = false;
		e.fin = this.ticks + segundos * 20L;
		this.enviarATodos(nivel);
		BackroomsEvento.LOG.info("Apagon en {} ({} s)", nivel.dimension().identifier(), segundos);
	}

	public void alarma(ServerLevel nivel, int segundos) {
		Estado e = this.estado(nivel.dimension());
		e.alarma = true;
		e.apagon = false;
		e.fin = this.ticks + segundos * 20L;
		this.enviarATodos(nivel);
		// y la megafonia: "brecha de contencion, busquen refugio"
		for (ServerPlayer j : nivel.players()) {
			net.backrooms.evento.Sonidos.aJugador(j, net.backrooms.evento.Sonidos.MEGAFONIA_ALERTA, 0.9F);
		}
		BackroomsEvento.LOG.info("Alarma en {} ({} s)", nivel.dimension().identifier(), segundos);
	}

	/** Vuelve la luz normal y programa los siguientes. */
	public void terminar(ServerLevel nivel) {
		Estado e = this.estado(nivel.dimension());
		boolean habia = e.apagon || e.alarma;
		e.apagon = false;
		e.alarma = false;
		Fase f = Fase.de(nivel);
		if (f != null) {
			this.programar(e, f);
			e.proximoApagon = Math.max(e.proximoApagon, this.ticks + RESPIRO);
			e.proximaAlarma = Math.max(e.proximaAlarma, this.ticks + RESPIRO);
		}
		if (habia) {
			this.enviarATodos(nivel);
		}
	}

	/** Segundos que faltan para el final del suceso en curso, o hasta el proximo apagon. */
	public String resumen(ServerLevel nivel) {
		Estado e = this.estado(nivel.dimension());
		if (e.apagon || e.alarma) {
			return (e.apagon ? "APAGON" : "ALARMA") + " (quedan " + (e.fin - this.ticks) / 20 + " s)";
		}
		if (e.proximoApagon < 0) {
			return "luz normal";
		}
		return "luz normal; apagon en " + Math.max(0, e.proximoApagon - this.ticks) / 20 + " s, alarma en "
			+ Math.max(0, e.proximaAlarma - this.ticks) / 20 + " s" + (this.automatico ? "" : " (automatico apagado)");
	}

	/* ---------------------------------------------------------- envio */

	private void enviarATodos(ServerLevel nivel) {
		for (ServerPlayer j : nivel.players()) {
			this.enviar(j);
		}
	}

	public void enviar(ServerPlayer j) {
		Estado e = this.estados.get(j.level().dimension());
		EstadoAmbiente p = e == null || Fase.de(j.level()) == null ? EstadoAmbiente.NORMAL : new EstadoAmbiente(e.apagon, e.alarma);
		if (ServerPlayNetworking.canSend(j, EstadoAmbiente.TYPE)) {
			ServerPlayNetworking.send(j, p);
		}
	}
}
