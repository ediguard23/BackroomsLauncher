package net.backrooms.evento.piscinas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.objetos.Equipo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/**
 * Las Piscinas (Nivel 37), el nivel de debajo del Nivel 0: su dimension
 * (GeneradorPiscinas) y la llegada. Se baja por un ascensor de salida del Nivel 0
 * con las misiones hechas (Fases); la bajada es la cinematica del guion 2.
 *
 * Aqui viven las Serpientes (Habitantes), hay comida y aguas como en el Nivel 0 (Comida),
 * la cordura baja igual (Supervivencia) y suenan cosas sueltas de vez en cuando
 * (AmbientePiscinas, en el cliente). No es una Fase: ni apagones ni Bacteria. Quien muere
 * aqui cuenta como en el Nivel 0 (Vestibulo y Eliminacion lo miran con Piscinas#es).
 */
public final class Piscinas {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, BackroomsEvento.id("piscinas"));
	public static final String NOMBRE = "NIVEL 37";
	public static final String SUBTITULO = "LAS PISCINAS";
	/** Medio lado del cuadrado donde se reparte a quien llega, y distancia minima entre llegadas. */
	private static final int RADIO = 900;
	private static final int SEPARACION = 72;

	private static MinecraftServer servidor;
	private static final List<BlockPos> ocupadas = new ArrayList<>();
	private static final Random azar = new Random();

	private Piscinas() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			servidor = null;
			ocupadas.clear();
		});
	}

	public static boolean es(Level nivel) {
		return nivel.dimension().equals(DIMENSION);
	}

	/** Un nivel donde se juega la expedicion: una fase del Nivel 0, las Piscinas o la Fiesta. */
	public static boolean delEvento(Level nivel) {
		return Fase.de(nivel) != null || es(nivel) || net.backrooms.evento.fiesta.Fiesta.es(nivel);
	}

	/**
	 * Lo deja en un sitio libre de las Piscinas, lejos de los que ya llegaron, y reparte sus
	 * aguas de almendras alrededor (como al llegar al Nivel 0). Devuelve false si no hay dimension.
	 */
	public static boolean llegar(ServerPlayer j) {
		return llegar(j, true);
	}

	/** Como llegar(j); `aguas` false al reaparecer (sus aguas ya se repartieron al llegar). */
	public static boolean llegar(ServerPlayer j, boolean aguas) {
		ServerLevel nivel = servidor == null ? null : servidor.getLevel(DIMENSION);
		if (nivel == null) {
			BackroomsEvento.LOG.error("No existe la dimension de las Piscinas ({})", DIMENSION.identifier());
			j.displayClientMessage(Component.literal("Las Piscinas no estan disponibles en este servidor.").withStyle(ChatFormatting.RED), false);
			return false;
		}
		BlockPos p = punto(nivel);
		j.teleportTo(nivel, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, Set.of(), azar.nextFloat() * 360.0F - 180.0F, 0.0F, true);
		j.resetFallDistance();
		j.removeEffect(MobEffects.SLOWNESS);
		j.removeEffect(MobEffects.RESISTANCE);
		Equipo.vestir(j);
		if (aguas) {
			// llega de verdad (no reaparece): sus misiones de aqui y sus aguas de almendras
			net.backrooms.evento.mision.Misiones.get().asignarPiscinas(j, p);
			net.backrooms.evento.supervivencia.Comida.get().repartirAguas(nivel, p);
		}
		BackroomsEvento.LOG.info("{} llega a las Piscinas en {} {}", j.getGameProfile().name(), p.getX(), p.getZ());
		return true;
	}

	/**
	 * Lleva al jugador a la sala de ese tipo (PlanoPiscinas.TIPOS) mas cercana, de pie en
	 * un sitio seco cerca de su centro y mirando hacia el. Para el staff y las pruebas.
	 */
	public static boolean irA(ServerPlayer j, int tipo) {
		ServerLevel nivel = servidor == null ? null : servidor.getLevel(DIMENSION);
		if (nivel == null || !(nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas gen)) {
			return false;
		}
		PlanoPiscinas p = gen.plano(nivel.getChunkSource().randomState());
		boolean aqui = es(j.level());
		int cx0 = aqui ? Math.floorDiv(j.getBlockX(), PlanoPiscinas.C) : 0;
		int cz0 = aqui ? Math.floorDiv(j.getBlockZ(), PlanoPiscinas.C) : 0;
		byte[] col = new byte[PlanoPiscinas.ALTO];
		for (int r = 1; r <= 30; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					PlanoPiscinas.Sala s = p.sala(cx0 + dx, cz0 + dz);
					if (s.tipo() != tipo) {
						continue;
					}
					double mx = (s.x0() + s.x1()) / 2.0;
					double mz = (s.z0() + s.z1()) / 2.0;
					// de la pared del oeste hacia el centro: el primer sitio donde estar de pie (aunque moje)
					for (int k = 2; k < s.ancho() / 2; k++) {
						int x = s.x0() + k;
						int z = (int) mz;
						p.columna(x, z, col);
						double y = p.dePie(col);
						if (y > 0) {
							float yaw = (float) Math.toDegrees(Math.atan2(-(mx - x), mz - z));
							j.teleportTo(nivel, x + 0.5, y, z + 0.5, Set.of(), yaw, 0.0F, true);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Lleva al jugador delante del detalle mas cercano (marca, mancha, objeto...); con
	 * `soloCorre`, delante de un CORRE. Para el staff y las pruebas.
	 */
	public static boolean irADetalle(ServerPlayer j, boolean soloCorre) {
		ServerLevel nivel = servidor == null ? null : servidor.getLevel(DIMENSION);
		if (nivel == null || !(nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas gen)) {
			return false;
		}
		PlanoPiscinas p = gen.plano(nivel.getChunkSource().randomState());
		boolean aqui = es(j.level());
		int cx0 = aqui ? Math.floorDiv(j.getBlockX(), PlanoPiscinas.C) : 0;
		int cz0 = aqui ? Math.floorDiv(j.getBlockZ(), PlanoPiscinas.C) : 0;
		for (int r = 0; r <= 40; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					for (PlanoPiscinas.Detalle d : p.detallesCelda(cx0 + dx, cz0 + dz)) {
						if (soloCorre && (d.tipo() != PlanoPiscinas.D_MARCA || d.variante() != PlanoPiscinas.M_CORRE + 1)) {
							continue;
						}
						// tres bloques delante de lo que se mira, mirandolo
						int[] dxz = {0, 1, 0, -1, -1, 0, 1, 0};
						int lado = d.tipo() == PlanoPiscinas.D_AZULEJO ? (d.lado() + 2) % 4 : d.lado();
						int fx = dxz[lado];
						int fz = dxz[lado + 4];
						double x = d.x() + 0.5 + fx * 3.0;
						double z = d.z() + 0.5 + fz * 3.0;
						float yaw = (float) Math.toDegrees(Math.atan2(fx, -fz));
						j.teleportTo(nivel, x, PlanoPiscinas.PISO, z, Set.of(), yaw, 10.0F, true);
						return true;
					}
				}
			}
		}
		return false;
	}

	/* ------------------------------------------- staff: ir a lo de las misiones */

	private static final int[] PASO_X = {0, 1, 0, -1};
	private static final int[] PASO_Z = {-1, 0, 1, 0};

	/** Lo deja a `d` bloques delante de (x, y, z) segun `mira`, mirandolo. */
	private static boolean delante(ServerPlayer j, int x, int y, int z, int mira, double d) {
		ServerLevel nivel = servidor == null ? null : servidor.getLevel(DIMENSION);
		if (nivel == null) {
			return false;
		}
		double px = x + 0.5 + PASO_X[mira] * d;
		double pz = z + 0.5 + PASO_Z[mira] * d;
		float yaw = (float) Math.toDegrees(Math.atan2(-(x + 0.5 - px), z + 0.5 - pz));
		j.teleportTo(nivel, px, PlanoPiscinas.PISO, pz, Set.of(), yaw, 10.0F, true);
		return true;
	}

	private static PlanoPiscinas planoDe() {
		ServerLevel nivel = servidor == null ? null : servidor.getLevel(DIMENSION);
		return nivel == null ? null : Valvulas.plano(nivel);
	}

	/** Delante de su valvula pendiente mas cercana; si no tiene, de la mas cercana. */
	public static boolean irAValvula(ServerPlayer j) {
		PlanoPiscinas p = planoDe();
		if (p == null) {
			return false;
		}
		net.backrooms.evento.mision.Misiones.Estado e = net.backrooms.evento.mision.Misiones.get().estado(j);
		PlanoPiscinas.Valvula mejor = null;
		double dMejor = Double.MAX_VALUE;
		for (PlanoPiscinas.Valvula v : p.valvulas()) {
			boolean suya = e != null && e.valvulas.contains(v.id()) && !e.valvulasHechas.contains(v.id());
			double d = j.distanceToSqr(v.x(), v.y(), v.z()) - (suya ? 1.0E12 : 0);
			if (d < dMejor) {
				dMejor = d;
				mejor = v;
			}
		}
		return mejor != null && delante(j, mejor.x(), mejor.y(), mejor.z(), mejor.lado(), 2.0);
	}

	public static boolean irASalida(ServerPlayer j) {
		PlanoPiscinas p = planoDe();
		if (p == null) {
			return false;
		}
		PlanoPiscinas.Salida s = p.salida();
		int[] c = s.columna(2);
		return delante(j, c[0], PlanoPiscinas.PISO, c[1], s.mira(), 2.2);
	}

	/** Delante del cuerpo mas cercano (PlanoPiscinas#cadaverEnCelda); con `colgado`, de uno que cuelga. */
	public static boolean irACadaver(ServerPlayer j, boolean colgado) {
		PlanoPiscinas p = planoDe();
		ServerLevel nivel = servidor == null ? null : servidor.getLevel(DIMENSION);
		if (p == null || nivel == null) {
			return false;
		}
		int cx0 = es(j.level()) ? Math.floorDiv(j.getBlockX(), PlanoPiscinas.C) : 0;
		int cz0 = es(j.level()) ? Math.floorDiv(j.getBlockZ(), PlanoPiscinas.C) : 0;
		for (int r = 0; r <= 40; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					PlanoPiscinas.Cadaver c = p.cadaverEnCelda(cx0 + dx, cz0 + dz);
					if (c != null && (!colgado || c.colgado())) {
						BlockPos libre = nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas gen
							? gen.puntoLibre(nivel.getChunkSource().randomState(), (int) Math.floor(c.x()) + 3, (int) Math.floor(c.z()))
							: BlockPos.containing(c.x() + 3, PlanoPiscinas.PISO, c.z());
						double ox = c.x() - (libre.getX() + 0.5);
						double oz = c.z() - (libre.getZ() + 0.5);
						float yaw = (float) Math.toDegrees(Math.atan2(-ox, oz));
						j.teleportTo(nivel, libre.getX() + 0.5, libre.getY(), libre.getZ() + 0.5, Set.of(), yaw, c.colgado() ? -15.0F : 25.0F, true);
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Al escondite de esa tarjeta (la roja esta bajo el agua: encima de ella). */
	public static boolean irATarjeta(ServerPlayer j, int color) {
		PlanoPiscinas p = planoDe();
		ServerLevel nivel = servidor == null ? null : servidor.getLevel(DIMENSION);
		if (p == null || nivel == null) {
			return false;
		}
		int[] t = p.tarjeta(color);
		j.teleportTo(nivel, t[0] + 0.5, t[1] + (color == PlanoPiscinas.T_ROJA ? 1.0 : 0.0), t[2] + 2.5, Set.of(), 180.0F, 30.0F, true);
		return true;
	}

	/** Punto al azar, seco y con luz, separado de las llegadas anteriores. */
	public static BlockPos punto(ServerLevel nivel) {
		int x = 0;
		int z = 0;
		for (int intento = 0; intento < 60; intento++) {
			x = azar.nextInt(RADIO * 2 + 1) - RADIO;
			z = azar.nextInt(RADIO * 2 + 1) - RADIO;
			boolean lejos = true;
			for (BlockPos o : ocupadas) {
				long dx = o.getX() - x;
				long dz = o.getZ() - z;
				if (dx * dx + dz * dz < (long) SEPARACION * SEPARACION) {
					lejos = false;
					break;
				}
			}
			if (lejos) {
				break;
			}
		}
		BlockPos p = new BlockPos(x, PlanoPiscinas.PISO, z);
		if (nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas gen) {
			p = gen.puntoLibre(nivel.getChunkSource().randomState(), x, z);
		}
		ocupadas.add(p);
		return p;
	}
}
