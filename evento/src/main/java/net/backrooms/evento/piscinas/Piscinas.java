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
 * De momento es solo el sitio: sin misiones, sin entidad que cace y sin sonidos
 * propios (eso viene despues). No es una Fase: ni apagones, ni Bacteria, ni cordura.
 * Quien muere aqui cuenta como en el Nivel 0 (Vestibulo y Eliminacion lo miran con
 * Piscinas#es).
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

	/** Un nivel donde se juega la expedicion: una fase del Nivel 0 o las Piscinas. */
	public static boolean delEvento(Level nivel) {
		return Fase.de(nivel) != null || es(nivel);
	}

	/** Lo deja en un sitio libre de las Piscinas, lejos de los que ya llegaron. Devuelve false si no hay dimension. */
	public static boolean llegar(ServerPlayer j) {
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
