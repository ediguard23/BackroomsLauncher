package net.backrooms.evento.estacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.red.CinematicaSalidaEstacion;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * El final del evento: quien entra en el ascensor del final de la Estacion (con la cadena cortada) se
 * queda quieto e invulnerable y su cliente pone la cinematica (SalidaEstacionCliente): otro superviviente
 * corre hacia el ascensor y el Amasijo lo atrapa; el panel hace ruido, el Amasijo viene a por el; las
 * puertas se cierran por poco; el silencio, la musica, las manos manchadas, la campanilla y el corte. A
 * los DURACION ticks (ya en negro) el servidor le lleva al ascensor del vestibulo; alli se abren las
 * puertas y a los ESCAPA ticks cuenta como escapado (Fases.escapar, sin moverle: ya esta en el
 * vestibulo); a los LLEGADA ticks se le suelta.
 *
 * Cada jugador tiene la suya (los demas no ven nada: el superviviente y ese Amasijo son de su cliente).
 * Si el cliente no tiene el mod o el servidor no tiene vestibulo, escapa como antes.
 */
public final class SalidaEstacion {
	private static final SalidaEstacion INSTANCIA = new SalidaEstacion();
	/** Cuando le lleva al vestibulo (la imagen ya esta en negro desde la campanilla). */
	public static final int DURACION = 872;
	/** En el vestibulo: cuando cuenta como escapado (las puertas ya abiertas) y cuando se le suelta. */
	public static final int ESCAPA = 80;
	public static final int LLEGADA = 110;
	private static final Identifier QUIETO = BackroomsEvento.id("salida_estacion_quieto");

	private final Map<UUID, Integer> en = new HashMap<>();
	private final Map<UUID, Integer> llegando = new HashMap<>();
	private final Map<UUID, Boolean> invulnerable = new HashMap<>();
	private MinecraftServer servidor;

	private SalidaEstacion() {
	}

	public static SalidaEstacion get() {
		return INSTANCIA;
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			INSTANCIA.servidor = null;
			INSTANCIA.en.clear();
			INSTANCIA.llegando.clear();
			INSTANCIA.invulnerable.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		// quien se va a mitad: si ya estaba llegando, escapa igual; si no, se queda en la Estacion
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			ServerPlayer j = h.player;
			if (INSTANCIA.llegando.containsKey(j.getUUID())) {
				net.backrooms.evento.fase.Fases.get().escapar(j, false);
			}
			if (INSTANCIA.enCurso(j)) {
				INSTANCIA.en.remove(j.getUUID());
				INSTANCIA.llegando.remove(j.getUUID());
				INSTANCIA.soltar(j);
			}
		});
	}

	public boolean enCurso(ServerPlayer j) {
		return this.en.containsKey(j.getUUID()) || this.llegando.containsKey(j.getUUID());
	}

	/** Entra en el ascensor. Sin el mod en el cliente o sin vestibulo, escapa sin mas. */
	public void empezar(ServerPlayer j) {
		if (this.enCurso(j)) {
			return;
		}
		if (!ServerPlayNetworking.canSend(j, CinematicaSalidaEstacion.TYPE) || net.backrooms.evento.vestibulo.Vestibulo.get().nivel() == null) {
			net.backrooms.evento.fase.Fases.get().escapar(j);
			return;
		}
		this.en.put(j.getUUID(), 0);
		this.invulnerable.put(j.getUUID(), j.isInvulnerable());
		j.setInvulnerable(true);
		quieto(j, true);
		ServerPlayNetworking.send(j, new CinematicaSalidaEstacion());
		BackroomsEvento.LOG.info("{} entra en el ascensor del final (cinematica del final)", j.getGameProfile().name());
	}

	private void tick() {
		if (this.servidor == null || this.en.isEmpty() && this.llegando.isEmpty()) {
			return;
		}
		for (UUID id : new ArrayList<>(this.en.keySet())) {
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(id);
			if (j == null) {
				this.en.remove(id);
				this.invulnerable.remove(id);
				continue;
			}
			int t = this.en.merge(id, 1, Integer::sum);
			if (!j.isAlive()) {
				this.en.remove(id);
				this.soltar(j);
			} else if (t >= DURACION) {
				this.en.remove(id);
				if (net.backrooms.evento.vestibulo.Vestibulo.get().llevarAlAscensor(j)) {
					this.llegando.put(id, 0);
				} else {
					this.soltar(j);
					net.backrooms.evento.fase.Fases.get().escapar(j);
				}
			}
		}
		for (UUID id : new ArrayList<>(this.llegando.keySet())) {
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(id);
			if (j == null) {
				this.llegando.remove(id);
				this.invulnerable.remove(id);
				continue;
			}
			int t = this.llegando.merge(id, 1, Integer::sum);
			if (t == ESCAPA) {
				// las puertas ya estan abiertas: ahora si, ha escapado (sin moverle: ya esta en el vestibulo)
				net.backrooms.evento.fase.Fases.get().escapar(j, false);
			}
			if (t >= LLEGADA) {
				this.llegando.remove(id);
				this.soltar(j);
			}
		}
	}

	private void soltar(ServerPlayer j) {
		Boolean antes = this.invulnerable.remove(j.getUUID());
		j.setInvulnerable(antes != null && antes);
		quieto(j, false);
	}

	/** Sin andar ni saltar (modificadores que no se guardan). */
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
