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
 * Cuantos Amasijos hay en la Estacion y donde salen: uno por cada piso con gente (y uno mas por cada
 * POR_JUGADORES jugadores en ese piso), desde que el generador tiene gasolina o hay alguna central
 * activa; con la pasarela encendida, uno mas en el piso 3. Salen lejos de todos, en los pasillos de su
 * laberinto, y nunca de golpe: de uno en uno. No suben escaleras: el que se sale de su piso, se va.
 *
 * Cada sistema que vuelve (ruido) hace un estruendo en algun sitio lejano: no siempre es el.
 */
public final class AmasijoEstacion {
	private static final AmasijoEstacion INSTANCIA = new AmasijoEstacion();
	private static final int POR_JUGADORES = 10;
	/** La etiqueta de los que pone el staff (invocar amasijo): no cuentan ni se quitan solos. */
	public static final String INVOCADO = "invocado";
	private static final List<List<int[]>> RONDAS = new ArrayList<>();

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

	/** Los sitios de la ronda de un piso: {x, z, piso}. */
	public static List<int[]> ronda(int piso) {
		if (RONDAS.isEmpty()) {
			for (int p = 0; p < PlanoEstacion.PISOS; p++) {
				List<int[]> l = new ArrayList<>();
				for (int[] r : PlanoEstacion.RONDA) {
					if (r[2] == p) {
						l.add(r);
					}
				}
				RONDAS.add(l);
			}
		}
		return RONDAS.get(Math.max(0, Math.min(PlanoEstacion.PISOS - 1, piso)));
	}

	/** El generador ha arrancado: la estacion se despierta. */
	public void despertar() {
		this.despierto = true;
	}

	/** Un estruendo metalico en un sitio lejano del laberinto de algun piso. */
	public void ruido(ServerLevel nivel) {
		List<int[]> r = ronda(this.azar.nextInt(PlanoEstacion.PISOS));
		int[] p = r.get(this.azar.nextInt(r.size()));
		nivel.playSound(null, p[0] + 0.5, PlanoEstacion.SUELOS[p[2]] + 2, p[1] + 0.5, Sonidos.ESTACION_ESTRUENDO, SoundSource.AMBIENT, 6.0F, 0.8F + this.azar.nextFloat() * 0.3F);
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
		JuegoEstacion g = JuegoEstacion.get();
		boolean activo = Acechadores.get().activos() && (this.despierto || g.arrancado() || g.centrales() > 0);
		List<List<ServerPlayer>> jugando = new ArrayList<>();
		List<List<Amasijo>> vivos = new ArrayList<>();
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			jugando.add(new ArrayList<>());
			vivos.add(new ArrayList<>());
		}
		for (ServerPlayer j : nivel.players()) {
			if (!j.isSpectator() && !j.isCreative() && Misiones.get().enExpedicion(j)) {
				jugando.get(PlanoEstacion.piso(j.getY())).add(j);
			}
		}
		for (var e : nivel.getAllEntities()) {
			if (e instanceof Amasijo a && !a.isRemoved() && !a.getTags().contains(INVOCADO)) {
				vivos.get(a.piso()).add(a);
			}
		}
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			List<ServerPlayer> js = jugando.get(p);
			List<Amasijo> vs = vivos.get(p);
			int quiero = 0;
			if (activo && !js.isEmpty()) {
				quiero = 1 + (js.size() - 1) / POR_JUGADORES + (p == PlanoEstacion.PISOS - 1 && g.pasarela() ? 1 : 0);
			}
			if (vs.size() > quiero) {
				vs.sort(Comparator.comparingDouble(a -> -cercania(a.position(), js)));
				vs.get(0).discard();
				continue;
			}
			if (vs.size() < quiero) {
				this.poner(nivel, p, js, vs, quiero);
			}
		}
	}

	private void poner(ServerLevel nivel, int piso, List<ServerPlayer> js, List<Amasijo> vs, int quiero) {
		List<BlockPos> otros = new ArrayList<>();
		for (Amasijo a : vs) {
			otros.add(a.blockPosition());
		}
		BlockPos p = this.sitioLejos(nivel, piso, js, 25, 80, otros);
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
		BackroomsEvento.LOG.info("Sale un Amasijo en el piso {} de la Estacion en {} {} ({} de {})", piso + 1, p.getX(), p.getZ(), vs.size() + 1, quiero);
	}

	private static double cercania(Vec3 p, List<ServerPlayer> jugando) {
		double d = Double.MAX_VALUE;
		for (ServerPlayer j : jugando) {
			d = Math.min(d, j.position().distanceTo(p));
		}
		return d;
	}

	private @Nullable BlockPos sitioLejos(ServerLevel nivel, int piso, List<ServerPlayer> jugando, double min, double max, List<BlockPos> otros) {
		List<int[]> buenos = new ArrayList<>();
		int y = PlanoEstacion.SUELOS[piso] + 1;
		for (int[] r : ronda(piso)) {
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
			if (cerca >= min && cerca <= max && nivel.isPositionEntityTicking(BlockPos.containing(r[0], y, r[1]))) {
				buenos.add(r);
			}
		}
		if (buenos.isEmpty()) {
			return null;
		}
		int[] r = buenos.get(this.azar.nextInt(buenos.size()));
		return BlockPos.containing(r[0], y, r[1]);
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
