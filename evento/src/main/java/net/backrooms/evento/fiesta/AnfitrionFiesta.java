package net.backrooms.evento.fiesta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.ambiente.Ambiente;
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

/**
 * Los Anfitriones de la Fiesta y sus apagones:
 *  - SIEMPRE hay en el laberinto dos (tres con 10 jugadores o mas en la Fiesta). Salen en la ronda del laberinto, lejos de todos (24 bloques), de uno en
 *    uno y riendose alto para que se les oiga; si sobran (se van jugadores), se quitan los mas lejanos.
 *    Con luz merodean y no cazan (Anfitrion).
 *  - APAGONES: cada ConfigFiesta.apagonCada minutos (con alguien jugando) se va la luz durante
 *    apagonDura segundos (Ambiente#apagon; en la Fiesta no es negro del todo, AmbienteCliente). La
 *    megafonia lo anuncia con demasiada alegria; los Anfitriones van mas rapido y cazan a quien vean.
 *    Al volver la luz, la megafonia hace como si nada y ellos vuelven a merodear.
 * Con `/backrooms acechadores false` no hay ninguno (como las demas entidades).
 */
public final class AnfitrionFiesta {
	private static final AnfitrionFiesta INSTANCIA = new AnfitrionFiesta();
	/** Los sitios de la ronda que estan en el laberinto (por ahi salen y merodean). */
	private static List<int[]> rondaLaberinto;

	public static AnfitrionFiesta get() {
		return INSTANCIA;
	}

	private final Random azar = new Random();
	private long ticks;
	private long proximoApagon = -1;
	private boolean habiaApagon;

	private AnfitrionFiesta() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(INSTANCIA::tick);
	}

	/** Si (x, z) esta en el laberinto (sus pasillos o sus habitaciones). */
	public static boolean enLaberinto(double x, double z) {
		return x >= PlanoFiesta.LAB_X && x < PlanoFiesta.SALIDA_X && z >= PlanoFiesta.LAB_Z && z < PlanoFiesta.LAB_Z + PlanoFiesta.LAB_H * PlanoFiesta.CELDA;
	}

	/** Los sitios de PlanoFiesta.RONDA que estan en los pasillos del laberinto. */
	public static List<int[]> rondaLaberinto() {
		if (rondaLaberinto == null) {
			List<int[]> l = new ArrayList<>();
			for (int[] r : PlanoFiesta.RONDA) {
				PlanoFiesta.Sala s = PlanoFiesta.sala(r[0], r[1]);
				if (enLaberinto(r[0], r[1]) && s != null && s.nombre().equals("laberinto")) {
					l.add(r);
				}
			}
			rondaLaberinto = l.isEmpty() ? PlanoFiesta.RONDA : l;
		}
		return rondaLaberinto;
	}

	private long minutos(float[] r) {
		return (long) ((r[0] + this.azar.nextDouble() * (r[1] - r[0])) * 60 * 20);
	}

	private List<ServerPlayer> jugando(ServerLevel nivel) {
		List<ServerPlayer> l = new ArrayList<>();
		for (ServerPlayer j : nivel.players()) {
			if (!j.isSpectator() && !j.isCreative() && Misiones.get().enExpedicion(j)) {
				l.add(j);
			}
		}
		return l;
	}

	private static List<Anfitrion> vivos(ServerLevel nivel) {
		List<Anfitrion> l = new ArrayList<>();
		for (var e : nivel.getAllEntities()) {
			if (e instanceof Anfitrion a && !a.isRemoved() && a.modo() == Anfitrion.CAZA) {
				l.add(a);
			}
		}
		return l;
	}

	private void tick(MinecraftServer s) {
		this.ticks++;
		if (this.ticks % 10 != 0) {
			return;
		}
		ServerLevel nivel = Fiesta.nivel();
		if (nivel == null) {
			return;
		}
		ConfigFiesta c = ConfigFiesta.get();
		List<ServerPlayer> jugando = this.jugando(nivel);
		boolean apagon = Ambiente.get().apagon(nivel);
		if (this.habiaApagon && !apagon) {
			for (ServerPlayer j : nivel.players()) {
				Sonidos.aJugador(j, Sonidos.FIESTA_LUZ, 0.9F);
			}
		}
		this.habiaApagon = apagon;
		boolean activos = Acechadores.get().activos();
		if (this.ticks % 40 == 0) {
			this.poblacion(nivel, jugando, activos ? jugando.size() : 0);
		}
		if (jugando.isEmpty() || !activos) {
			this.proximoApagon = -1;
			return;
		}
		if (this.proximoApagon < 0) {
			this.proximoApagon = this.ticks + this.minutos(c.apagonCada);
		}
		if (!apagon && this.ticks >= this.proximoApagon) {
			this.apagon(nivel, c.apagonDura[0] + this.azar.nextInt(Math.max(1, c.apagonDura[1] - c.apagonDura[0] + 1)));
		}
	}

	/**
	 * Mantiene los Anfitriones que tiene que haber: uno por cada anfitrionPorJugadores (minimo uno con
	 * alguien jugando). Pone uno cada vez (lejos de todos) o quita el que sobre mas lejos de nadie.
	 */
	private void poblacion(ServerLevel nivel, List<ServerPlayer> jugando, int jugadores) {
		List<Anfitrion> vivos = vivos(nivel);
		int quiero = jugadores <= 0 ? 0 : jugadores >= 10 ? 3 : 2;
		if (vivos.size() > quiero) {
			vivos.sort(Comparator.comparingDouble(a -> -cercania(a.position(), jugando)));
			vivos.get(0).discard();
			return;
		}
		if (vivos.size() < quiero) {
			List<BlockPos> otros = new ArrayList<>();
			for (Anfitrion a : vivos) {
				otros.add(a.blockPosition());
			}
			BlockPos p = this.sitioLejos(nivel, jugando, 24, 80, otros);
			if (p != null) {
				Anfitrion a = this.poner(nivel, p, Anfitrion.CAZA);
				if (a != null) {
					// se rie al salir, alto: que se sepa que hay uno mas (y mas o menos donde)
					nivel.playSound(null, p.getX() + 0.5, p.getY() + 2.0, p.getZ() + 0.5, Sonidos.ANFITRION_RISA, SoundSource.HOSTILE, 4.0F, 0.95F);
					BackroomsEvento.LOG.info("Sale un Anfitrion en la Fiesta en {} {} ({} de {})", p.getX(), p.getZ(), vivos.size() + 1, quiero);
				}
			}
		}
	}

	private static double cercania(Vec3 p, List<ServerPlayer> jugando) {
		double d = Double.MAX_VALUE;
		for (ServerPlayer j : jugando) {
			d = Math.min(d, j.position().distanceTo(p));
		}
		return d;
	}

	/** Se va la luz: la megafonia lo anuncia y los Anfitriones cazan hasta que vuelva. */
	public void apagon(ServerLevel nivel, int segundos) {
		ConfigFiesta c = ConfigFiesta.get();
		this.proximoApagon = this.ticks + this.minutos(c.apagonCada) + segundos * 20L;
		Ambiente.get().apagon(nivel, segundos);
		for (ServerPlayer j : nivel.players()) {
			Sonidos.aJugador(j, Sonidos.FIESTA_APAGON, 0.9F);
		}
	}

	/** Uno de pie, lejos, mirando a `j` (staff: /backrooms fiesta aparicion): se va con la linterna. */
	public void aparicion(ServerLevel nivel, ServerPlayer j) {
		BlockPos mejor = null;
		double dMejor = Double.MAX_VALUE;
		for (int[] r : PlanoFiesta.RONDA) {
			Vec3 v = new Vec3(r[0] + 0.5, PlanoFiesta.SUELO + 2.5, r[1] + 0.5);
			double d = v.distanceTo(j.getEyePosition());
			if (d < 14 || d > 40 || d >= dMejor) {
				continue;
			}
			net.minecraft.world.phys.BlockHitResult golpe = nivel.clip(new net.minecraft.world.level.ClipContext(j.getEyePosition(), v,
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, j));
			if (golpe.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
				mejor = BlockPos.containing(r[0], PlanoFiesta.SUELO + 1, r[1]);
				dMejor = d;
			}
		}
		if (mejor == null) {
			return;
		}
		Anfitrion a = this.poner(nivel, mejor, Anfitrion.APARICION);
		if (a != null) {
			a.mirarA(j);
		}
	}

	private Anfitrion poner(ServerLevel nivel, BlockPos p, int modo) {
		if (!nivel.isPositionEntityTicking(p)) {
			return null;
		}
		Anfitrion a = Entidades.ANFITRION.create(nivel, EntitySpawnReason.EVENT);
		if (a == null) {
			return null;
		}
		a.modo(modo);
		a.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, this.azar.nextFloat() * 360.0F, 0.0F);
		nivel.addFreshEntity(a);
		return a;
	}

	/**
	 * Un sitio de la ronda del laberinto a min..max bloques de alguien, a mas de min de todos y lejos de
	 * los Anfitriones que ya hay.
	 */
	private BlockPos sitioLejos(ServerLevel nivel, List<ServerPlayer> jugando, double min, double max, List<BlockPos> otros) {
		List<int[]> buenos = new ArrayList<>();
		for (int[] r : rondaLaberinto()) {
			boolean junto = false;
			for (BlockPos o : otros) {
				junto |= Math.hypot(o.getX() - r[0], o.getZ() - r[1]) < 16;
			}
			if (junto) {
				continue;
			}
			double cerca = Double.MAX_VALUE;
			for (ServerPlayer j : jugando) {
				cerca = Math.min(cerca, Math.hypot(j.getX() - r[0], j.getZ() - r[1]));
			}
			if (cerca >= min && cerca <= max && nivel.isPositionEntityTicking(BlockPos.containing(r[0], PlanoFiesta.SUELO + 1, r[1]))) {
				buenos.add(r);
			}
		}
		if (buenos.isEmpty()) {
			return null;
		}
		int[] r = buenos.get(this.azar.nextInt(buenos.size()));
		return BlockPos.containing(r[0], PlanoFiesta.SUELO + 1, r[1]);
	}

	public void quitarTodos(ServerLevel nivel) {
		List<Anfitrion> todos = new ArrayList<>();
		for (var e : nivel.getAllEntities()) {
			if (e instanceof Anfitrion a) {
				todos.add(a);
			}
		}
		for (Anfitrion a : todos) {
			a.irse(nivel);
		}
	}
}
