package net.backrooms.evento.fase;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Las fases de la expedicion, de la mas facil a la final. Cada una es una
 * dimension con su propio Nivel 0 (otro trazado) y va depurando jugadores:
 * todos empiezan en la 1 (dificultad media); quien completa sus misiones
 * sigue el radar hasta un ascensor de salida y baja a la siguiente, que es
 * mas pequena (se espera menos gente) y mas dura. Quien sale de la ultima,
 * escapa.
 *
 * Mas dura = la senal de los casetes llega menos lejos y estan mas
 * repartidos, hay mas misiones de grabar, los apagones y las alarmas son mas
 * frecuentes, hay mas Bacterias y mas rapidas y la cordura baja antes.
 *
 * @param radio          medio lado del cuadrado donde se reparte a la gente al llegar
 * @param separacion     distancia minima entre los puntos de llegada
 * @param casetes        casetes a recoger
 * @param alcanceSenal   bloques a los que el detector capta un casete
 * @param repartoCasetes multiplica la distancia de los anillos de casetes
 * @param grabar         misiones de grabar (ademas de los casetes)
 * @param apagonCada     minutos entre apagones {min, max}
 * @param apagonDura     segundos que dura un apagon {min, max}
 * @param alarmaCada     minutos entre alarmas {min, max}
 * @param alarmaDura     segundos que dura una alarma {min, max}
 * @param bacterias      Bacterias por cada 10 jugadores en la fase (minimo 1)
 * @param velocidad      multiplica la velocidad de la Bacteria
 * @param cordura        multiplica lo que baja la cordura
 * @param smiler         probabilidad de que a cada jugador le salga un Smiler en un apagon
 */
public record Fase(int numero, String nombre, String dificultad, ResourceKey<Level> dimension,
	int radio, int separacion, int casetes, int alcanceSenal, double repartoCasetes, int grabar,
	int[] apagonCada, int[] apagonDura, int[] alarmaCada, int[] alarmaDura,
	double bacterias, double velocidad, double cordura, double smiler) {

	public static final Fase[] TODAS = {
		new Fase(1, "NIVEL 0", "MEDIA", Level.OVERWORLD,
			4800, 280, 10, 70, 1.0, 2,
			new int[] {6, 10}, new int[] {45, 75}, new int[] {7, 12}, new int[] {35, 55},
			1.0, 1.0, 1.0, 0.45),
		new Fase(2, "NIVEL 0 · SECTOR B", "MEDIA-ALTA", clave("fase_2"),
			2600, 200, 10, 58, 1.15, 2,
			new int[] {5, 8}, new int[] {55, 85}, new int[] {6, 10}, new int[] {40, 60},
			1.5, 1.1, 1.25, 0.6),
		new Fase(3, "NIVEL 0 · SECTOR C", "ALTA", clave("fase_3"),
			1400, 140, 10, 46, 1.3, 3,
			new int[] {4, 7}, new int[] {60, 95}, new int[] {5, 9}, new int[] {45, 65},
			2.2, 1.2, 1.5, 0.75),
		new Fase(4, "NIVEL 0 · ZONA ROJA", "EXTREMA", clave("fase_4"),
			700, 90, 10, 36, 1.45, 3,
			new int[] {3, 5}, new int[] {70, 110}, new int[] {4, 7}, new int[] {50, 70},
			3.0, 1.3, 1.8, 0.9)
	};

	private static ResourceKey<Level> clave(String id) {
		return ResourceKey.create(Registries.DIMENSION, BackroomsEvento.id(id));
	}

	/** La fase que se juega en esa dimension, o null si no es del Nivel 0 (el vestibulo, por ejemplo). */
	public static @Nullable Fase de(ResourceKey<Level> dimension) {
		for (Fase f : TODAS) {
			if (f.dimension.equals(dimension)) {
				return f;
			}
		}
		return null;
	}

	public static @Nullable Fase de(Level nivel) {
		return de(nivel.dimension());
	}

	public static Fase numero(int n) {
		return TODAS[Math.max(1, Math.min(TODAS.length, n)) - 1];
	}

	public boolean ultima() {
		return this.numero == TODAS.length;
	}

	public @Nullable Fase siguiente() {
		return this.ultima() ? null : TODAS[this.numero];
	}
}
