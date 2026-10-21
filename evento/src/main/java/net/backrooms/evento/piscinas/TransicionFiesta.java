package net.backrooms.evento.piscinas;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.red.CinematicaTobogan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * El paso de las Piscinas a la Fiesta. Quien entra en el pasillo de la boveda (con sus misiones
 * hechas) se queda quieto e invulnerable y su cliente pone la cinematica (ToboganCliente): la
 * alarma de contencion, la megafonia, mira atras y ve subir el agua, corre y se tira por el
 * tobogan. A los DURACION ticks, ya a oscuras, el servidor le lleva a la Fiesta
 * (Fiesta#llegar; si el servidor no la tiene, escapa).
 */
public final class TransicionFiesta {
	private static final TransicionFiesta INSTANCIA = new TransicionFiesta();
	/** Cuando le lleva a la Fiesta (la cinematica sigue a oscuras un poco mas en el cliente). */
	public static final int DURACION = 220;
	private static final Identifier QUIETO = BackroomsEvento.id("tobogan_quieto");

	private final Map<UUID, Integer> en = new HashMap<>();
	private final Map<UUID, Boolean> invulnerable = new HashMap<>();
	private MinecraftServer servidor;

	private TransicionFiesta() {
	}

	public static TransicionFiesta get() {
		return INSTANCIA;
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			INSTANCIA.servidor = null;
			INSTANCIA.en.clear();
			INSTANCIA.invulnerable.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		// quien se va a mitad: se le quita lo de la cinematica antes de que se guarde (si no, se quedaria
		// invulnerable para siempre: Minecraft lo guarda con el jugador)
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			if (INSTANCIA.enCurso(h.player)) {
				INSTANCIA.terminar(h.player, false);
			}
		});
	}

	public boolean enCurso(ServerPlayer j) {
		return this.en.containsKey(j.getUUID());
	}

	/** Empieza la cinematica para ese jugador (ya en el pasillo de la boveda). */
	public void empezar(ServerPlayer j, PlanoPiscinas.Salida s) {
		if (this.enCurso(j)) {
			return;
		}
		this.en.put(j.getUUID(), 0);
		this.invulnerable.put(j.getUUID(), j.isInvulnerable());
		j.setInvulnerable(true);
		quieto(j, true);
		if (ServerPlayNetworking.canSend(j, CinematicaTobogan.TYPE)) {
			ServerPlayNetworking.send(j, new CinematicaTobogan(s.x(), s.z(), s.lado()));
		}
		BackroomsEvento.LOG.info("{} entra en el pasillo de la boveda (cinematica del tobogan)", j.getGameProfile().name());
	}

	private void tick() {
		if (this.servidor == null || this.en.isEmpty()) {
			return;
		}
		for (UUID id : new java.util.ArrayList<>(this.en.keySet())) {
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(id);
			if (j == null) {
				this.en.remove(id);
				this.invulnerable.remove(id);
				continue;
			}
			int t = this.en.merge(id, 1, Integer::sum);
			if (t >= DURACION || !Piscinas.es(j.level()) || !j.isAlive()) {
				this.terminar(j, t >= DURACION && Piscinas.es(j.level()) && j.isAlive());
			}
		}
	}

	private void terminar(ServerPlayer j, boolean viajar) {
		UUID id = j.getUUID();
		this.en.remove(id);
		Boolean antes = this.invulnerable.remove(id);
		j.setInvulnerable(antes != null && antes);
		quieto(j, false);
		if (viajar) {
			BackroomsEvento.LOG.info("{} baja por el tobogan de las Piscinas", j.getGameProfile().name());
			if (!net.backrooms.evento.fiesta.Fiesta.llegar(j, true)) {
				Fases.get().escapar(j);
			}
		}
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
