package net.backrooms.evento.fiesta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.evento.red.CinematicaSalidaFiesta;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * La salida de la Fiesta al Nivel 3. Quien se mete en el ascensor del salon final (con el premio
 * cobrado) se queda quieto e invulnerable y su cliente pone la cinematica (SalidaFiestaCliente):
 * el Anfitrion revienta a golpes la puerta morada del final y corre hacia el ascensor, las puertas
 * se cierran a tiempo, grita, golpea, amenaza, corta los cables y el ascensor cae y se estrella.
 * A los DURACION ticks, ya a oscuras, el servidor le lleva a la Estacion (sin el titulo de
 * Minecraft: lo pone el cliente) y sigue quieto DESPIERTA ticks mientras vuelve en si; al
 * acabar, la radio. Si el servidor no tiene la Estacion, escapa.
 */
public final class SalidaFiesta {
	private static final SalidaFiesta INSTANCIA = new SalidaFiesta();
	/** Cuando le lleva a la Estacion (el cliente sigue a oscuras un poco mas). */
	public static final int DURACION = 1172;
	/** Lo que tarda en despertar en la Estacion (tumbado, abre los ojos, se levanta, el titulo). */
	public static final int DESPIERTA = 345;
	private static final Identifier QUIETO = BackroomsEvento.id("salida_fiesta_quieto");

	private final Map<UUID, Integer> en = new HashMap<>();
	/** Los que ya estan en la Estacion despertando: cuantos ticks llevan. */
	private final Map<UUID, Integer> despertando = new HashMap<>();
	private final Map<UUID, Boolean> invulnerable = new HashMap<>();
	private MinecraftServer servidor;

	private SalidaFiesta() {
	}

	public static SalidaFiesta get() {
		return INSTANCIA;
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			INSTANCIA.servidor = null;
			INSTANCIA.en.clear();
			INSTANCIA.despertando.clear();
			INSTANCIA.invulnerable.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		// quien se va a mitad: se le quita lo de la cinematica antes de que se guarde
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			if (INSTANCIA.enCurso(h.player)) {
				INSTANCIA.terminar(h.player, false);
			}
		});
	}

	public boolean enCurso(ServerPlayer j) {
		return this.en.containsKey(j.getUUID()) || this.despertando.containsKey(j.getUUID());
	}

	/** Empieza la cinematica para ese jugador (ya dentro del ascensor). Sin el mod en el cliente, se va ya. */
	public void empezar(ServerPlayer j) {
		if (this.enCurso(j)) {
			return;
		}
		if (!ServerPlayNetworking.canSend(j, CinematicaSalidaFiesta.TYPE)) {
			this.viajar(j);
			return;
		}
		this.en.put(j.getUUID(), 0);
		this.invulnerable.put(j.getUUID(), j.isInvulnerable());
		j.setInvulnerable(true);
		quieto(j, true);
		ServerPlayNetworking.send(j, new CinematicaSalidaFiesta());
		BackroomsEvento.LOG.info("{} se mete en el ascensor de la Fiesta (cinematica de la salida)", j.getGameProfile().name());
	}

	private void tick() {
		if (this.servidor == null || this.en.isEmpty() && this.despertando.isEmpty()) {
			return;
		}
		for (UUID id : new java.util.ArrayList<>(this.despertando.keySet())) {
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(id);
			int t = this.despertando.merge(id, 1, Integer::sum);
			if (j == null || t >= DESPIERTA || !j.isAlive()) {
				this.despertando.remove(id);
				if (j != null) {
					this.soltar(j);
					net.backrooms.evento.Sonidos.aJugador(j, net.backrooms.evento.Sonidos.ESTACION_RADIO, 0.9F);
				}
			}
		}
		for (UUID id : new java.util.ArrayList<>(this.en.keySet())) {
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(id);
			if (j == null) {
				this.en.remove(id);
				this.invulnerable.remove(id);
				continue;
			}
			int t = this.en.merge(id, 1, Integer::sum);
			if (t >= DURACION || !Fiesta.es(j.level()) || !j.isAlive()) {
				this.terminar(j, t >= DURACION && Fiesta.es(j.level()) && j.isAlive());
			}
		}
	}

	private void terminar(ServerPlayer j, boolean viajar) {
		this.en.remove(j.getUUID());
		if (viajar && this.viajar(j)) {
			// sigue quieto e invulnerable mientras despierta
			this.despertando.put(j.getUUID(), 0);
			return;
		}
		this.despertando.remove(j.getUUID());
		this.soltar(j);
	}

	private void soltar(ServerPlayer j) {
		Boolean antes = this.invulnerable.remove(j.getUUID());
		j.setInvulnerable(antes != null && antes);
		quieto(j, false);
	}

	/** Le lleva a la Estacion (true) o, si el servidor no la tiene, escapa (false). */
	private boolean viajar(ServerPlayer j) {
		Misiones.get().completar(j, TipoMision.PREMIO);
		BackroomsEvento.LOG.info("{} sale de la Fiesta en el ascensor", j.getGameProfile().name());
		// al Nivel 3 (la Estacion Electrica), mareado por el golpe; si el servidor no la tiene, salir es escapar
		if (net.backrooms.evento.estacion.Estacion.llegar(j, true, false)) {
			// al levantarse aun va algo torpe
			j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DESPIERTA + 100, 0, false, false));
			return true;
		}
		net.backrooms.evento.fase.Fases.get().escapar(j);
		return false;
	}

	/** Sin andar ni saltar (modificadores que no se guardan: si se desconecta, se van). */
	private static void quieto(ServerPlayer j, boolean si) {
		for (var atributo : java.util.List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
			AttributeInstance a = j.getAttribute(atributo);
			if (a == null) {
				continue;
			}
			a.removeModifier(QUIETO);
			if (si) {
				a.addTransientModifier(new AttributeModifier(QUIETO, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
	}
}
