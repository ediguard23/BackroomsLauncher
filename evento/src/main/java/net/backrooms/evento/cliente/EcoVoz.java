package net.backrooms.evento.cliente;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.backrooms.evento.estacion.Estacion;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * El eco de la voz de los demas en el cliente (lo registra VozCinematica, solo si hay Simple Voice
 * Chat): cuanto mas lejos esta quien habla, mas reverberacion de sala grande y un poco mas apagada
 * se oye su voz. De cerca, limpia. En la Estacion (una nave de hormigon) el eco es algo mayor.
 *
 * El audio llega por el hilo de la voz: las posiciones las copia tick() en el hilo del juego y
 * aqui solo se leen. Cada canal (cada jugador) tiene su propia reverberacion, con su cola.
 */
public final class EcoVoz {
	/** Por debajo de esto, sin eco; a LEJOS bloques, el eco entero. */
	private static final double CERCA = 6.0;
	private static final double LEJOS = 32.0;
	private static final float MEZCLA = 0.42F;
	private static final float MEZCLA_ESTACION = 0.55F;

	private static volatile Map<UUID, Vec3> posiciones = Map.of();
	private static volatile Vec3 yo;
	private static volatile boolean estacion;
	private static volatile boolean activo;
	private static final Map<UUID, Sala> SALAS = new ConcurrentHashMap<>();

	private EcoVoz() {
	}

	public static void registrar(EventRegistration registro) {
		registro.registerEvent(ClientReceiveSoundEvent.EntitySound.class, e -> procesar(e, posiciones.get(e.getEntityId())));
		registro.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, e -> {
			Position p = e.getPosition();
			procesar(e, p == null ? null : new Vec3(p.getX(), p.getY(), p.getZ()));
		});
	}

	/** En el hilo del juego: donde esta cada jugador del nivel (y yo). */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			activo = false;
			SALAS.clear();
			return;
		}
		Map<UUID, Vec3> m = new java.util.HashMap<>();
		for (Player p : mc.level.players()) {
			m.put(p.getUUID(), p.getEyePosition());
		}
		posiciones = m;
		yo = mc.player.getEyePosition();
		estacion = Estacion.es(mc.level);
		activo = true;
		// los canales que llevan un rato callados se olvidan
		long ahora = System.currentTimeMillis();
		SALAS.values().removeIf(s -> ahora - s.usada > 15_000L);
	}

	private static void procesar(ClientReceiveSoundEvent e, Vec3 donde) {
		Vec3 aqui = yo;
		if (!activo || donde == null || aqui == null) {
			return;
		}
		double d = donde.distanceTo(aqui);
		float k = (float) Math.max(0.0, Math.min(1.0, (d - CERCA) / (LEJOS - CERCA)));
		Sala s = SALAS.computeIfAbsent(e.getId(), u -> new Sala());
		short[] audio = e.getRawAudio();
		if (audio == null || audio.length == 0) {
			return;
		}
		// aunque este cerca se pasa por la sala, para que la cola no se corte de golpe al acercarse
		e.setRawAudio(s.procesar(audio, k * (estacion ? MEZCLA_ESTACION : MEZCLA), k));
	}

	/**
	 * Una sala para un canal: cuatro peines con su filtro (Schroeder/Freeverb) y dos pasos-todo,
	 * mas un eco suelto que rebota en la pared del fondo. A 48 kHz.
	 */
	private static final class Sala {
		private static final int[] PEINES = {1557, 1617, 1491, 1422};
		private static final int[] TODOS = {556, 441};
		private static final int ECO = 48 * 190;
		private final float[][] peine = new float[PEINES.length][];
		private final int[] ip = new int[PEINES.length];
		private final float[] filtro = new float[PEINES.length];
		private final float[][] todo = new float[TODOS.length][];
		private final int[] it = new int[TODOS.length];
		private final float[] eco = new float[ECO];
		private int ie;
		private float ecoFiltro;
		private float apagada;
		private float mezcla;
		private long usada;

		Sala() {
			for (int i = 0; i < PEINES.length; i++) {
				this.peine[i] = new float[PEINES[i] * 2];
			}
			for (int i = 0; i < TODOS.length; i++) {
				this.todo[i] = new float[TODOS[i] * 2];
			}
		}

		short[] procesar(short[] in, float quiero, float lejos) {
			this.usada = System.currentTimeMillis();
			short[] out = new short[in.length];
			float corte = 1.0F - lejos * 0.55F;
			for (int n = 0; n < in.length; n++) {
				// la mezcla cambia poco a poco (sin saltos al moverse)
				this.mezcla += (quiero - this.mezcla) * 0.0005F;
				float x = in[n] / 32768.0F;
				// lejos, la voz directa llega mas apagada
				this.apagada += (x - this.apagada) * corte;
				float seco = this.apagada;
				float rev = 0.0F;
				for (int i = 0; i < PEINES.length; i++) {
					float[] b = this.peine[i];
					float y = b[this.ip[i]];
					this.filtro[i] = y * 0.6F + this.filtro[i] * 0.4F;
					b[this.ip[i]] = x * 0.25F + this.filtro[i] * 0.82F;
					this.ip[i] = (this.ip[i] + 1) % b.length;
					rev += y;
				}
				for (int i = 0; i < TODOS.length; i++) {
					float[] b = this.todo[i];
					float y = b[this.it[i]];
					b[this.it[i]] = rev + y * 0.5F;
					this.it[i] = (this.it[i] + 1) % b.length;
					rev = y - rev;
				}
				// el eco de la pared del fondo, apagado
				float e = this.eco[this.ie];
				this.ecoFiltro += (e - this.ecoFiltro) * 0.25F;
				this.eco[this.ie] = x + this.ecoFiltro * 0.32F;
				this.ie = (this.ie + 1) % this.eco.length;
				float m = this.mezcla;
				float v = seco * (1.0F - m * 0.35F) + rev * m * 0.9F + this.ecoFiltro * m * 0.45F;
				out[n] = (short) Math.max(-32768, Math.min(32767, Math.round(v * 32768.0F)));
			}
			return out;
		}
	}
}
