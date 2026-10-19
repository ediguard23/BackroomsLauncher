package net.backrooms.evento.fiesta;

import java.util.ArrayList;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * Cuando sale el Anfitrion (la entidad de la Fiesta) y cuando no:
 *  - APAGONES: cada ConfigFiesta.apagonCada minutos (con alguien jugando) se va la luz durante
 *    apagonDura segundos (Ambiente#apagon: la oscuridad de siempre, solo las linternas). La
 *    megafonia lo anuncia con demasiada alegria y aparecen Anfitriones en el laberinto (uno por cada
 *    3 jugadores que esten dentro, como minimo uno), lejos de todos y riendose para que se les oiga;
 *    luego cazan. Se espantan con la linterna encendida (Anfitrion#alumbrado). Al volver la luz se van.
 *  - APARICIONES: con luz, cada aparicionCada minutos, a alguien le toca verlo de pie al fondo
 *    de un pasillo, mirandole; en cuanto le mira (o a los pocos segundos) desaparece.
 * Con `/backrooms acechadores false` no sale (como las demas entidades).
 */
public final class AnfitrionFiesta {
	private static final AnfitrionFiesta INSTANCIA = new AnfitrionFiesta();

	public static AnfitrionFiesta get() {
		return INSTANCIA;
	}

	private final Random azar = new Random();
	private long ticks;
	private long proximoApagon = -1;
	private long proximaAparicion = -1;
	private boolean habiaApagon;

	private AnfitrionFiesta() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(INSTANCIA::tick);
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
			// vuelve la luz: se van todos y la megafonia hace como si nada
			this.quitarTodos(nivel);
			for (ServerPlayer j : nivel.players()) {
				Sonidos.aJugador(j, Sonidos.FIESTA_LUZ, 0.9F);
			}
		}
		this.habiaApagon = apagon;
		if (jugando.isEmpty() || !Acechadores.get().activos()) {
			this.proximoApagon = -1;
			this.proximaAparicion = -1;
			return;
		}
		if (this.proximoApagon < 0) {
			this.proximoApagon = this.ticks + this.minutos(c.apagonCada);
		}
		if (this.proximaAparicion < 0) {
			this.proximaAparicion = this.ticks + this.minutos(c.aparicionCada);
		}
		if (!apagon && this.ticks >= this.proximoApagon) {
			this.apagon(nivel, c.apagonDura[0] + this.azar.nextInt(Math.max(1, c.apagonDura[1] - c.apagonDura[0] + 1)));
		} else if (!apagon && this.ticks >= this.proximaAparicion) {
			this.proximaAparicion = this.ticks + this.minutos(c.aparicionCada);
			this.aparicion(nivel, jugando.get(this.azar.nextInt(jugando.size())));
		}
	}

	/** Se va la luz y salen a cazar. */
	public void apagon(ServerLevel nivel, int segundos) {
		ConfigFiesta c = ConfigFiesta.get();
		this.proximoApagon = this.ticks + this.minutos(c.apagonCada) + segundos * 20L;
		Ambiente.get().apagon(nivel, segundos);
		for (ServerPlayer j : nivel.players()) {
			Sonidos.aJugador(j, Sonidos.FIESTA_APAGON, 0.9F);
		}
		List<ServerPlayer> jugando = this.jugando(nivel);
		int enLaberinto = 0;
		for (ServerPlayer j : jugando) {
			if (enLaberinto(j.getX(), j.getZ())) {
				enLaberinto++;
			}
		}
		int n = Math.max(1, (int) Math.ceil(enLaberinto / 3.0));
		List<BlockPos> puestos = new ArrayList<>();
		for (int i = 0; i < n && !jugando.isEmpty(); i++) {
			BlockPos p = this.sitioLejos(nivel, jugando, 24, 70, puestos);
			if (p != null) {
				Anfitrion a = this.poner(nivel, p, Anfitrion.CAZA);
				if (a != null) {
					puestos.add(p);
					// se rie al salir, alto: que se sepa que ya esta ahi (y mas o menos donde)
					nivel.playSound(null, p.getX() + 0.5, p.getY() + 2.0, p.getZ() + 0.5, Sonidos.ANFITRION_RISA, net.minecraft.sounds.SoundSource.HOSTILE, 4.0F, 0.95F);
					BackroomsEvento.LOG.info("Apagon en la Fiesta: un Anfitrion sale en {} {}", p.getX(), p.getZ());
				}
			}
		}
	}

	/** Uno de pie, lejos, mirando a `j` (con luz): se va en cuanto le mira. */
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

	/** Si (x, z) esta en el laberinto (sus pasillos o sus habitaciones). */
	private static boolean enLaberinto(double x, double z) {
		return x >= PlanoFiesta.LAB_X && x < PlanoFiesta.SALIDA_X && z >= PlanoFiesta.LAB_Z && z < PlanoFiesta.LAB_Z + PlanoFiesta.LAB_H * PlanoFiesta.CELDA;
	}

	/**
	 * Un sitio de la ronda del laberinto a min..max bloques de alguien, a mas de min de todos y lejos de
	 * los Anfitriones ya puestos.
	 */
	private BlockPos sitioLejos(ServerLevel nivel, List<ServerPlayer> jugando, double min, double max, List<BlockPos> puestos) {
		List<int[]> buenos = new ArrayList<>();
		for (int[] r : PlanoFiesta.RONDA) {
			if (!enLaberinto(r[0], r[1])) {
				continue;
			}
			boolean junto = false;
			for (BlockPos o : puestos) {
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
