package net.backrooms.evento.entidad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.mundo.Plano;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pone y quita a las entidades del Nivel 0.
 *
 * Bacterias: en cada fase hay unas cuantas por cada 10 jugadores
 * (Fase.bacterias, minimo 1). Aparecen lejos de todos (45-75 bloques de
 * alguien) y se quitan si se quedan sin nadie a menos de 140.
 *
 * Smilers: al irse la luz, a cada jugador le puede tocar uno (Fase.smiler)
 * unos segundos despues, a 12-20 bloques y casi siempre a su espalda. Se
 * van solos cuando vuelve la luz.
 */
public final class Acechadores {
	private static final Acechadores INSTANCIA = new Acechadores();
	private static final int MAX_BACTERIAS = 40;

	public static Acechadores get() {
		return INSTANCIA;
	}

	private final Random azar = new Random();
	private final Map<ResourceKey<Level>, Boolean> habiaApagon = new HashMap<>();
	/** Smilers pendientes: jugador -> tick en que sale. */
	private final Map<UUID, Long> smilers = new HashMap<>();
	private MinecraftServer servidor;
	private long ticks;
	private boolean activos = true;

	private Acechadores() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
	}

	public boolean activos() {
		return this.activos;
	}

	public void activos(boolean si) {
		this.activos = si;
	}

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
			boolean apagon = Ambiente.get().apagon(nivel);
			boolean antes = this.habiaApagon.getOrDefault(nivel.dimension(), false);
			this.habiaApagon.put(nivel.dimension(), apagon);
			if (apagon && !antes && this.activos) {
				this.sortearSmilers(nivel, f);
			}
			if (this.ticks % 20 == 0) {
				this.bacterias(nivel, f);
			}
		}
		if (!this.smilers.isEmpty()) {
			this.smilers.entrySet().removeIf(e -> {
				if (e.getValue() > this.ticks) {
					return false;
				}
				ServerPlayer j = this.servidor.getPlayerList().getPlayer(e.getKey());
				if (j != null && Ambiente.get().apagon(j.level()) && Misiones.get().enExpedicion(j)) {
					this.smiler(j);
				}
				return true;
			});
		}
	}

	private List<ServerPlayer> jugando(ServerLevel nivel) {
		List<ServerPlayer> l = new ArrayList<>();
		for (ServerPlayer j : nivel.players()) {
			if (!j.isSpectator() && !j.isCreative() && Misiones.get().enExpedicion(j) && !net.backrooms.evento.fase.Fases.get().viajando(j)) {
				l.add(j);
			}
		}
		return l;
	}

	/* ------------------------------------------------------- bacterias */

	private void bacterias(ServerLevel nivel, Fase f) {
		List<Bacteria> vivas = new ArrayList<>();
		for (var e : nivel.getAllEntities()) {
			if (e instanceof Bacteria b && b.isAlive()) {
				vivas.add(b);
			}
		}
		// las que se han quedado sin nadie cerca se van
		for (Bacteria b : vivas) {
			if (nivel.getNearestPlayer(b, 140) == null) {
				b.discard();
			}
		}
		vivas.removeIf(b -> b.isRemoved());
		List<ServerPlayer> jugando = this.jugando(nivel);
		if (!this.activos || jugando.isEmpty()) {
			return;
		}
		int quiero = Math.min(MAX_BACTERIAS, Math.max(1, (int) Math.round(jugando.size() / 10.0 * f.bacterias())));
		if (vivas.size() >= quiero) {
			return;
		}
		ServerPlayer cerca = jugando.get(this.azar.nextInt(jugando.size()));
		BlockPos p = this.puntoLejos(nivel, cerca, 45, 75, 30);
		if (p != null) {
			this.poner(nivel, p);
		}
	}

	public @Nullable Bacteria poner(ServerLevel nivel, BlockPos p) {
		Bacteria b = Entidades.BACTERIA.create(nivel, EntitySpawnReason.EVENT);
		if (b == null) {
			return null;
		}
		b.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, this.azar.nextFloat() * 360.0F, 0.0F);
		nivel.addFreshEntity(b);
		return b;
	}

	/**
	 * Punto de suelo libre a entre `min` y `max` bloques de `j`, y a mas de
	 * `lejosDeTodos` de cualquier otro jugador; null si no encuentra.
	 */
	private @Nullable BlockPos puntoLejos(ServerLevel nivel, ServerPlayer j, int min, int max, int lejosDeTodos) {
		if (!(nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen)) {
			return null;
		}
		Plano plano = gen.plano(nivel.getChunkSource().randomState());
		for (int intento = 0; intento < 12; intento++) {
			double ang = this.azar.nextDouble() * Math.PI * 2;
			double d = min + this.azar.nextDouble() * (max - min);
			int x = (int) Math.round(j.getX() + Math.cos(ang) * d);
			int z = (int) Math.round(j.getZ() + Math.sin(ang) * d);
			BlockPos p = gen.puntoLibre(nivel.getChunkSource().randomState(), x, z);
			if (plano.ascensorEn(p.getX(), p.getZ()) != Plano.ASC_NO) {
				continue;
			}
			if (nivel.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, lejosDeTodos, false) == null) {
				return p;
			}
		}
		return null;
	}

	/* --------------------------------------------------------- smilers */

	private void sortearSmilers(ServerLevel nivel, Fase f) {
		for (ServerPlayer j : this.jugando(nivel)) {
			if (this.azar.nextDouble() < f.smiler()) {
				this.smilers.put(j.getUUID(), this.ticks + 20L * (5 + this.azar.nextInt(20)));
			}
		}
	}

	/** Un Smiler para `j`, a su espalda si se puede. */
	public @Nullable Smiler smiler(ServerPlayer j) {
		ServerLevel nivel = j.level();
		if (!(nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen)) {
			return null;
		}
		Vec3 mira = j.getViewVector(1.0F);
		for (int intento = 0; intento < 10; intento++) {
			// detras: entre 110 y 250 grados de donde mira
			double base = Math.atan2(mira.z, mira.x) + Math.PI;
			double ang = base + (this.azar.nextDouble() - 0.5) * Math.toRadians(140);
			double d = 12 + this.azar.nextDouble() * 8;
			BlockPos p = gen.puntoLibre(nivel.getChunkSource().randomState(),
				(int) Math.round(j.getX() + Math.cos(ang) * d), (int) Math.round(j.getZ() + Math.sin(ang) * d));
			if (!nivel.getEntitiesOfClass(Smiler.class, new AABB(p).inflate(10)).isEmpty()) {
				continue;
			}
			Smiler s = Entidades.SMILER.create(nivel, EntitySpawnReason.EVENT);
			if (s == null) {
				return null;
			}
			s.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0F, 0.0F);
			s.perseguir(j);
			nivel.addFreshEntity(s);
			BackroomsEvento.LOG.debug("Smiler para {} en {}", j.getGameProfile().name(), p);
			return s;
		}
		return null;
	}
}
