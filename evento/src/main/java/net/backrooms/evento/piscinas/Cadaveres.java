package net.backrooms.evento.piscinas;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.backrooms.evento.mision.Entidades;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pone los cuerpos de las Piscinas (PlanoPiscinas#cadaverEnCelda) cuando alguien pasa cerca y
 * los quita cuando ya no queda nadie, como la comida: nunca hay cientos cargados. Son
 * siempre los mismos y en el mismo sitio (salen del plano).
 *
 * Tambien en el Nivel 0 (cadaverNivel0): en ~1 de cada 8 cuadros de C0 x C0 bloques, tirado
 * boca arriba o boca abajo, o sentado contra una pared; a algunos les falta un brazo o una
 * pierna. Alli nunca colgados (el techo esta a 4 bloques).
 */
public final class Cadaveres {
	private static final int RADIO = 56;
	private static final int QUITAR = 80;
	private static MinecraftServer servidor;
	private static long ticks;
	private static final Map<Long, CadaverEntidad> vivos = new HashMap<>();
	/** Los del Nivel 0, por fase: cuadro -> cuerpo puesto. */
	private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Map<Long, CadaverEntidad>> vivos0 = new HashMap<>();
	/** Cuadros del Nivel 0 donde puede haber un cuerpo (lado en bloques) y en cuantos lo hay. */
	private static final int C0 = 24;
	private static final double DENSIDAD0 = 0.12;
	private static final Map<Long, PlanoPiscinas.Cadaver> calculados0 = new java.util.concurrent.ConcurrentHashMap<>();
	private static final PlanoPiscinas.Cadaver NINGUNO = new PlanoPiscinas.Cadaver(0, 0, 0, -1, 0, 0, 0, 0);

	private Cadaveres() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			servidor = null;
			vivos.clear();
			vivos0.clear();
			steve = null;
			calculados0.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> tick());
	}

	private static void tick() {
		if (servidor == null || ++ticks % 20 != 0) {
			return;
		}
		for (ServerLevel n : servidor.getAllLevels()) {
			if (net.backrooms.evento.fase.Fase.de(n) != null && n.getChunkSource().getGenerator() instanceof net.backrooms.evento.mundo.GeneradorNivel0 gen) {
				nivel0(n, gen.plano(n.getChunkSource().randomState()));
			}
		}
		estacion();
		ServerLevel nivel = servidor.getLevel(Piscinas.DIMENSION);
		PlanoPiscinas p = nivel == null ? null : Valvulas.plano(nivel);
		if (p == null) {
			return;
		}
		// tambien para los espectadores (el staff mirando)
		for (ServerPlayer j : nivel.players()) {
			int cx0 = Math.floorDiv(j.getBlockX() - RADIO, PlanoPiscinas.C);
			int cx1 = Math.floorDiv(j.getBlockX() + RADIO, PlanoPiscinas.C);
			int cz0 = Math.floorDiv(j.getBlockZ() - RADIO, PlanoPiscinas.C);
			int cz1 = Math.floorDiv(j.getBlockZ() + RADIO, PlanoPiscinas.C);
			for (int cx = cx0; cx <= cx1; cx++) {
				for (int cz = cz0; cz <= cz1; cz++) {
					PlanoPiscinas.Cadaver c = p.cadaverEnCelda(cx, cz);
					if (c == null) {
						continue;
					}
					long k = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
					CadaverEntidad vivo = vivos.get(k);
					if (vivo != null && !vivo.isRemoved() && nivel.getEntity(vivo.getId()) == vivo) {
						continue;
					}
					if (!nivel.isPositionEntityTicking(BlockPos.containing(c.x(), PlanoPiscinas.PISO, c.z()))) {
						continue;
					}
					if (vivo != null) {
						vivo.discard();
					}
					CadaverEntidad nuevo = new CadaverEntidad(Entidades.CADAVER, nivel);
					nuevo.preparar(c);
					nivel.addFreshEntity(nuevo);
					vivos.put(k, nuevo);
				}
			}
		}
		quitarLejos(nivel, vivos, PlanoPiscinas.PISO);
	}

	/** Steve (el del trailer), muerto en la pasarela de la Estacion. */
	private static CadaverEntidad steve;

	/** Con alguien cerca de la pasarela, Steve esta tirado en ella; sin nadie, se quita. */
	private static void estacion() {
		ServerLevel nivel = net.backrooms.evento.estacion.Estacion.nivel();
		double[] s = net.backrooms.evento.estacion.PlanoEstacion.STEVE;
		if (nivel == null || s == null) {
			return;
		}
		if (steve != null && (steve.isRemoved() || nivel.getEntity(steve.getId()) != steve)) {
			steve = null;
		}
		boolean cerca = nivel.players().stream().anyMatch(j -> j.distanceToSqr(s[0], s[1], s[2]) < (double) RADIO * RADIO);
		if (cerca && steve == null && nivel.isPositionEntityTicking(BlockPos.containing(s[0], s[1], s[2]))) {
			CadaverEntidad c = new CadaverEntidad(Entidades.CADAVER, nivel);
			c.preparar(new PlanoPiscinas.Cadaver(s[0], s[1], s[2], PlanoPiscinas.PIEL_STEVE, PlanoPiscinas.BOCA_ARRIBA, (float) s[3], 0, 0.0F));
			nivel.addFreshEntity(c);
			steve = c;
		} else if (steve != null && nivel.players().stream().noneMatch(j -> j.distanceToSqr(s[0], s[1], s[2]) < (double) QUITAR * QUITAR)) {
			steve.discard();
			steve = null;
		}
	}

	/** Quita los que ya no tienen a nadie a menos de QUITAR. */
	private static void quitarLejos(ServerLevel nivel, Map<Long, CadaverEntidad> mapa, int y) {
		Iterator<CadaverEntidad> it = mapa.values().iterator();
		while (it.hasNext()) {
			CadaverEntidad c = it.next();
			if (c.isRemoved()) {
				it.remove();
			} else if (nivel.players().stream().noneMatch(j -> j.distanceToSqr(c.getX(), y, c.getZ()) < (double) QUITAR * QUITAR)) {
				c.discard();
				it.remove();
			}
		}
	}
	/* --------------------------------------------------------------- Nivel 0 */

	private static void nivel0(ServerLevel nivel, net.backrooms.evento.mundo.Plano plano) {
		Map<Long, CadaverEntidad> mapa = vivos0.computeIfAbsent(nivel.dimension(), k -> new HashMap<>());
		int y = net.backrooms.evento.mundo.GeneradorNivel0.SUELO + 1;
		for (ServerPlayer j : nivel.players()) {
			int cx0 = Math.floorDiv(j.getBlockX() - RADIO, C0);
			int cx1 = Math.floorDiv(j.getBlockX() + RADIO, C0);
			int cz0 = Math.floorDiv(j.getBlockZ() - RADIO, C0);
			int cz1 = Math.floorDiv(j.getBlockZ() + RADIO, C0);
			for (int cx = cx0; cx <= cx1; cx++) {
				for (int cz = cz0; cz <= cz1; cz++) {
					long k = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
					int qx = cx;
					int qz = cz;
					PlanoPiscinas.Cadaver c = calculados0.computeIfAbsent(k ^ ((long) nivel.dimension().hashCode() << 17), kk -> {
						PlanoPiscinas.Cadaver r = cadaverNivel0(plano, qx, qz, y);
						return r == null ? NINGUNO : r;
					});
					if (c.piel() < 0) {
						continue;
					}
					CadaverEntidad vivo = mapa.get(k);
					if (vivo != null && !vivo.isRemoved() && nivel.getEntity(vivo.getId()) == vivo) {
						continue;
					}
					if (!nivel.isPositionEntityTicking(BlockPos.containing(c.x(), y, c.z()))) {
						continue;
					}
					if (vivo != null) {
						vivo.discard();
					}
					CadaverEntidad nuevo = new CadaverEntidad(Entidades.CADAVER, nivel);
					nuevo.preparar(c);
					nivel.addFreshEntity(nuevo);
					mapa.put(k, nuevo);
				}
			}
		}
		if (calculados0.size() > 20000) {
			calculados0.clear();
		}
		quitarLejos(nivel, mapa, y);
	}

	private static long mezcla(long a, long b, long canal) {
		long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ canal * 0x165667B19E3779F9L ^ 0x5EED0CAD;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return h;
	}

	private static double azar0(int cx, int cz, int canal) {
		return (mezcla(cx, cz, canal) >>> 11) * 0x1.0p-53;
	}

	/** Libre para un cuerpo en (x, z) del Nivel 0: ni pared, ni decoracion, ni ascensor. */
	private static boolean libre0(net.backrooms.evento.mundo.Plano p, double x, double z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		return !p.pared(bx, bz) && p.decoracion(bx, bz).tipo() == net.backrooms.evento.mundo.Plano.D_NADA
			&& p.ascensorEn(bx, bz) == net.backrooms.evento.mundo.Plano.ASC_NO;
	}

	/** El cuerpo de ese cuadro del Nivel 0, o null (tirado o sentado; nunca colgado). */
	private static PlanoPiscinas.Cadaver cadaverNivel0(net.backrooms.evento.mundo.Plano p, int cx, int cz, int y) {
		if (azar0(cx, cz, 1) >= DENSIDAD0) {
			return null;
		}
		int piel = (int) Math.floorMod(mezcla(cx, cz, 2), (long) PlanoPiscinas.PIELES);
		int mutilado = azar0(cx, cz, 3) < 0.4 ? (int) Math.floorMod(mezcla(cx, cz, 4), 3L) + 1 : 0;
		int[] dx = {0, 1, 0, -1};
		int[] dz = {-1, 0, 1, 0};
		for (int t = 0; t < 30; t++) {
			int x = cx * C0 + 2 + (int) (azar0(cx, cz, 10 + t) * (C0 - 4));
			int z = cz * C0 + 2 + (int) (azar0(cx, cz, 50 + t) * (C0 - 4));
			if (!libre0(p, x + 0.5, z + 0.5)) {
				continue;
			}
			if (azar0(cx, cz, 5) < 0.35) {
				// sentado contra la pared que tenga al lado, mirando hacia fuera
				for (int m = 0; m < 4; m++) {
					if (p.pared(x + dx[m], z + dz[m]) && libre0(p, x + 0.5 - dx[m], z + 0.5 - dz[m])) {
						int mira = (m + 2) % 4;
						float g = new float[] {180.0F, 270.0F, 0.0F, 90.0F}[mira];
						return new PlanoPiscinas.Cadaver(x + 0.5 + dx[m] * 0.25, y, z + 0.5 + dz[m] * 0.25, piel, PlanoPiscinas.SENTADO, g, mutilado, 0);
					}
				}
				continue;
			}
			float giro = (float) (azar0(cx, cz, 6) * 360.0);
			double r = Math.toRadians(giro);
			double fx = -Math.sin(r);
			double fz = Math.cos(r);
			boolean cabe = true;
			for (double a = -1.2; a <= 1.21 && cabe; a += 0.6) {
				for (double b = -0.45; b <= 0.46 && cabe; b += 0.9) {
					cabe = libre0(p, x + 0.5 + fx * a - fz * b, z + 0.5 + fz * a + fx * b);
				}
			}
			if (cabe) {
				int postura = azar0(cx, cz, 7) < 0.5 ? PlanoPiscinas.BOCA_ARRIBA : PlanoPiscinas.BOCA_ABAJO;
				return new PlanoPiscinas.Cadaver(x + 0.5, y, z + 0.5, piel, postura, giro, mutilado, 0);
			}
		}
		return null;
	}
}
