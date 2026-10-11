package net.backrooms.evento.fase;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Las fases del Nivel 0. Desde la 0.10.0 solo hay una (las 2, 3 y 4 se quitaron para
 * hacer sitio a mas niveles): todos empiezan en ella y quien completa sus misiones
 * sigue el radar hasta un ascensor de salida, que baja al nivel siguiente (las
 * Piscinas, ver piscinas/Piscinas). El sistema sigue aceptando varias fases: cada una
 * es una dimension con su propio Nivel 0 (otro trazado, GeneradorNivel0 con otra
 * variante) y sus numeros de dificultad.
 *
 * @param radio          medio lado del cuadrado donde se reparte a la gente al llegar, con 200
 *                       jugadores (con menos se encoge: Fases#radio)
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
 * @param caza           bloques por segundo de la Bacteria cazando (un jugador anda a 4,3 y corre a 5,6)
 * @param cazaAlarma     lo mismo con la alarma encendida
 * @param cordura        multiplica lo que baja la cordura
 * @param smiler         probabilidad de que a cada jugador le salga un Smiler en un apagon
 */
public record Fase(int numero, String nombre, String dificultad, ResourceKey<Level> dimension,
	int radio, int separacion, int casetes, int alcanceSenal, double repartoCasetes, int grabar,
	int[] apagonCada, int[] apagonDura, int[] alarmaCada, int[] alarmaDura,
	double bacterias, double caza, double cazaAlarma, double cordura, double smiler) {

	public static final Fase[] TODAS = {
		new Fase(1, "NIVEL 0", "MEDIA", Level.OVERWORLD,
			4800, 280, 5, 90, 1.0, 2,
			new int[] {6, 10}, new int[] {45, 75}, new int[] {7, 12}, new int[] {35, 55},
			1.0, 5.5, 6.2, 1.0, 0.45)
	};

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
