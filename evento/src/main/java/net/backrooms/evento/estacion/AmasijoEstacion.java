package net.backrooms.evento.estacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.entidad.Acechadores;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.mision.Misiones;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Cuantos Amasijos hay en la Estacion y donde salen. Hasta que se arranca el generador, ninguno (la
 * estacion esta dormida). Luego uno; uno mas con la mitad de los paneles reparados y otro con la
 * pasarela encendida; y uno mas por cada POR_JUGADORES jugadores. Salen lejos de todos (en los
 * pasillos de los sectores) y nunca de golpe: de uno en uno.
 *
 * Cada sistema que vuelve (ruido) hace un estruendo en algun sector lejano: no siempre es el.
 */
public final class AmasijoEstacion {
	private static final AmasijoEstacion INSTANCIA = new AmasijoEstacion();
	private static final int POR_JUGADORES = 8;
	private static List<int[]> ronda;
	/** La etiqueta de los que pone el staff (invocar amasijo): no cuentan ni se quitan solos. */
	public static final String INVOCADO = "invocado";

	private final Random azar = new Random();
	private long ticks;
	private boolean despierto;

	private AmasijoEstacion() {
	}

	public static AmasijoEstacion get() {
		return INSTANCIA;
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(INSTANCIA::tick);
	}

	/** Los sitios de la ronda que estan en los sectores (no en la nave ni el acceso). */
	public static List<int[]> ronda() {
		if (ronda == null) {
			List<int[]> l = new ArrayList<>();
			for (int[] r : PlanoEstacion.RONDA) {
				if (PlanoEstacion.sector(r[0], r[1]) >= 0) {
					l.add(r);
				}
			}
			ronda = l.isEmpty() ? PlanoEstacion.RONDA : l;
		}
		return ronda;
	}

	/** El generador ha arrancado: la estacion se despierta. */
	public void despertar() {
		this.despierto = true;
	}

	/** Un estruendo metalico en un sector lejano (al reparar, al fallar...). */
	public void ruido(ServerLevel nivel) {
		List<int[]> r = ronda();
		int[] p = r.get(this.azar.nextInt(r.size()));
		nivel.playSound(null, p[0] + 0.5, PlanoEstacion.SUELO + 2, p[1] + 0.5, Sonidos.ESTACION_ESTRUENDO, SoundSource.AMBIENT, 6.0F, 0.8F + this.azar.nextFloat() * 0.3F);
	}

	private void tick(MinecraftServer s) {
		this.ticks++;
		if (this.ticks % 40 != 0) {
			return;
		}
		ServerLevel nivel = Estacion.nivel();
		if (nivel == null) {
			return;
		}
		List<ServerPlayer> jugando = new ArrayList<>();
		for (ServerPlayer j : nivel.players()) {
			if (!j.isSpectator() && !j.isCreative() && Misiones.get().enExpedicion(j)) {
				jugando.add(j);
			}
		}
		JuegoEstacion g = JuegoEstacion.get();
		boolean activo = Acechadores.get().activos() && (this.despierto || g.etapa() >= 1);
		int quiero = 0;
		if (activo && !jugando.isEmpty()) {
			quiero = 1 + (g.reparados() >= PlanoEstacion.PANELES.size() / 2 ? 1 : 0) + (g.pasarela() ? 1 : 0) + (jugando.size() - 1) / POR_JUGADORES;
		}
		List<Amasijo> vivos = new ArrayList<>();
		for (var e : nivel.getAllEntities()) {
			if (e instanceof Amasijo a && !a.isRemoved() && !a.getTags().contains(INVOCADO)) {
				vivos.add(a);
			}
		}
		if (vivos.size() > quiero) {
			vivos.sort(Comparator.comparingDouble(a -> -cercania(a.position(), jugando)));
			vivos.get(0).discard();
			return;
		}
		if (vivos.size() < quiero) {
			List<BlockPos> otros = new ArrayList<>();
			for (Amasijo a : vivos) {
				otros.add(a.blockPosition());
			}
			BlockPos p = this.sitioLejos(nivel, jugando, 30, 90, otros);
			if (p == null) {
				return;
			}
			Amasijo a = Entidades.AMASIJO.create(nivel, EntitySpawnReason.EVENT);
			if (a == null) {
				return;
			}
			a.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, this.azar.nextFloat() * 360.0F, 0.0F);
			nivel.addFreshEntity(a);
			nivel.playSound(null, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, Sonidos.AMASIJO_GRITO, SoundSource.HOSTILE, 5.0F, 0.7F);
			BackroomsEvento.LOG.info("Sale un Amasijo en la Estacion en {} {} ({} de {})", p.getX(), p.getZ(), vivos.size() + 1, quiero);
		}
	}

	private static double cercania(Vec3 p, List<ServerPlayer> jugando) {
		double d = Double.MAX_VALUE;
		for (ServerPlayer j : jugando) {
			d = Math.min(d, j.position().distanceTo(p));
		}
		return d;
	}

	private @Nullable BlockPos sitioLejos(ServerLevel nivel, List<ServerPlayer> jugando, double min, double max, List<BlockPos> otros) {
		List<int[]> buenos = new ArrayList<>();
		for (int[] r : ronda()) {
			boolean junto = false;
			for (BlockPos o : otros) {
				junto |= Math.hypot(o.getX() - r[0], o.getZ() - r[1]) < 20;
			}
			if (junto) {
				continue;
			}
			double cerca = Double.MAX_VALUE;
			for (ServerPlayer j : jugando) {
				cerca = Math.min(cerca, Math.hypot(j.getX() - r[0], j.getZ() - r[1]));
			}
			if (cerca >= min && cerca <= max && nivel.isPositionEntityTicking(BlockPos.containing(r[0], PlanoEstacion.SUELO + 1, r[1]))) {
				buenos.add(r);
			}
		}
		if (buenos.isEmpty()) {
			return null;
		}
		int[] r = buenos.get(this.azar.nextInt(buenos.size()));
		return BlockPos.containing(r[0], PlanoEstacion.SUELO + 1, r[1]);
	}

	public void quitarTodos(ServerLevel nivel) {
		List<Amasijo> todos = new ArrayList<>();
		for (var e : nivel.getAllEntities()) {
			if (e instanceof Amasijo a) {
				todos.add(a);
			}
		}
		todos.forEach(Amasijo::discard);
	}
}
