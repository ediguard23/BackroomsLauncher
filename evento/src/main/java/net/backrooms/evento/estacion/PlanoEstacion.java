package net.backrooms.evento.estacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El trazado de la Estacion Electrica (Nivel 3): un edificio cerrado, hecho a mano salvo los
 * pasillos de los sectores (laberintos con semilla fija), que GeneradorEstacion copia. Todo cuelga
 * de y = SUELO (se pisa en SUELO + 1):
 *
 *   ACCESO (x -7..7, z 76..92)     donde se llega bajando por el tobogan de la Fiesta: taquillas,
 *                                  el tablon de instrucciones (danadas) y el pasillo principal al norte
 *   NAVE DEL GENERADOR (x -24..24, z -6..42, techo alto): el generador en medio (su boca de
 *                                  combustible al sur), transformadores, cuadros y los cuatro pasos a
 *                                  los sectores (oeste y este)
 *   SALA DE CONTROL (x -14..14, z -24..-8): los cuatro huecos de fusibles (pared oeste), los cuatro
 *                                  interruptores (pared este), la pantalla con la secuencia y, al norte,
 *                                  la persiana de la pasarela
 *   LA PASARELA (x -10..10, z -96..-26): un pozo hondo (fondo en y = POZO) con maquinas, y la
 *                                  pasarela de rejilla a la altura del suelo hasta la plataforma y la
 *                                  puerta del final (z -99)
 *   SECTORES A (rojo, noroeste... al oeste-sur), B (amarillo), C (azul) y D (verde): laberintos de
 *                                  pasillos de 3 de ancho con sus salas: dos de paneles averiados, dos
 *                                  de maquinas y la de fusibles (tras una persiana que abren sus paneles)
 */
public final class PlanoEstacion {
	public static final int SUELO = 40;
	/** Techo de los pasillos y salas normales (se esta de pie de SUELO + 1 a TECHO - 1). */
	public static final int TECHO = SUELO + 5;
	/** Techo de la nave del generador. */
	public static final int TECHO_NAVE = SUELO + 12;
	/** Fondo del pozo de la pasarela y su techo. */
	public static final int POZO = 22;
	public static final int TECHO_POZO = 56;
	public static final int CELDA = 4;
	public static final String[] SECTORES = {"A", "B", "C", "D"};
	public static final String[] COLORES = {"rojo", "amarillo", "azul", "verde"};

	public interface Patron {
		BlockState en(int x, int y, int z);
	}

	public record Caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron patron) {
	}

	public record Sala(String nombre, int x0, int z0, int x1, int z1, int sector) {
		public boolean dentro(double x, double z) {
			return x >= this.x0 && x < this.x1 + 1 && z >= this.z0 && z < this.z1 + 1;
		}
	}

	/** Donde se coge algo: x, y, z del bloque y tipo (Interactivos.TIPO). */
	public record Punto(int id, int x, int y, int z, int tipo) {
	}

	/** Un panel averiado: su bloque, lo que necesita (0 cable, 1 herramientas, 2 cinta) y su sector. */
	public record Panel(int id, int x, int y, int z, int tipo, int sector) {
	}

	/**
	 * Una persiana de 3x3: el bloque de abajo a la izquierda (mirando hacia `mira`), hacia donde se
	 * extiende (`lado`) y hacia donde mira. `id`: 0..3 la de fusibles de cada sector, 4 la de la
	 * pasarela, 5 la puerta del final.
	 */
	public record Persiana(int id, int x, int z, Direction mira, Direction lado) {
		public int[] bloque(int parte) {
			int col = parte / 3;
			return new int[] {this.x + this.lado.getStepX() * col, SUELO + 1 + parte % 3, this.z + this.lado.getStepZ() * col};
		}
	}

	public static final List<Caja> CAJAS = new ArrayList<>();
	public static final List<Sala> SALAS = new ArrayList<>();
	public static final List<Punto> PUNTOS = new ArrayList<>();
	public static final List<Panel> PANELES = new ArrayList<>();
	public static final List<Persiana> PERSIANAS = new ArrayList<>();
	/** Los focos de la pasarela, en el orden en que se encienden (de la sala de control a la puerta). */
	public static final List<int[]> FOCOS = new ArrayList<>();
	/** Los huecos de fusibles y los interruptores de la sala de control: {x, y, z} por color. */
	public static final int[][] FUSIBLERA = new int[4][];
	public static final int[][] PALANCAS = new int[4][];
	/** La boca del generador y su centro (de ahi sale su zumbido). */
	public static final int[] BOCA = {0, SUELO + 2, 20};
	public static final double[] GENERADOR = {0.5, SUELO + 2.5, 17.5};
	/** Donde se escribe la secuencia (encima de la persiana de la pasarela), mirando al sur. */
	public static final double[] PANTALLA = {0.5, SUELO + 4.6, -23.92};
	/** Por donde ronda la entidad: el centro de las celdas de los sectores y algunos de la nave. */
	public static final List<int[]> RONDA = new ArrayList<>();
	/** Donde se llega (x, z, giro): en el acceso, mirando al norte. */
	public static final double[] LLEGADA = {0.5, 89.5, 180.0F};
	/** La comida (x, z, tipo de Plano.C_*): en el suelo del acceso y de las salas de maquinas. */
	public static final List<int[]> COMIDA = new ArrayList<>();
	/** La plataforma del final: quien llega con la pasarela encendida sale del nivel. */
	public static final int[] FINAL = {-3, -98, 3, -96};

	/** Limites del edificio, para el generador: salen de las cajas. */
	public static final int X0;
	public static final int X1;
	public static final int Z0;
	public static final int Z1;

	private PlanoEstacion() {
	}

	/* ------------------------------------------------------------ utilidades */

	private static void caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron p) {
		CAJAS.add(new Caja(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), p));
	}

	private static void caja(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		caja(x0, y0, z0, x1, y1, z1, (x, y, z) -> s);
	}

	private static void aire(int x0, int y0, int z0, int x1, int y1, int z1) {
		caja(x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
	}

	private static void bloque(int x, int y, int z, BlockState s) {
		caja(x, y, z, x, y, z, s);
	}

	private static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	private static BlockState mira(Block b, Direction d) {
		return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
	}

	private static BlockState var(Block b, Direction d, int v) {
		return mira(b, d).setValue(Variante.VARIANTE, v);
	}

	private static int hash(int x, int z, int canal) {
		int h = x * 73856093 ^ z * 19349663 ^ canal * 83492791;
		h = (h ^ (h >>> 13)) * 0x5bd1e995;
		return h ^ (h >>> 15);
	}

	private static int azar(int x, int z, int canal, int n) {
		return Math.floorMod(hash(x, z, canal), n);
	}

	/** La pared tipica: dos de verde gris pintado abajo, la franja del sector y hormigon arriba. */
	private static Patron pared(int color) {
		return (x, y, z) -> {
			if (y <= SUELO + 2) {
				return s(BloquesEstacion.PINTADA);
			}
			if (y == SUELO + 3 && color >= 0) {
				return BloquesEstacion.FRANJA.defaultBlockState().setValue(Interactivos.COLOR, color);
			}
			return s(BloquesEstacion.HORMIGON);
		};
	}

	/** El suelo de hormigon con alguna mancha de aceite. */
	private static final Patron SUELO_H = (x, y, z) -> azar(x, z, 5, 11) == 0 ? s(BloquesEstacion.SUELO_MANCHADO) : s(BloquesEstacion.SUELO);

	/**
	 * Una sala: suelo, muros (`color`: la franja de su sector, -1 sin franja) y techo a `techo` con
	 * lamparas cada `luces` (alguna fundida). Interior x0..x1, z0..z1; los muros alrededor.
	 */
	private static void sala(String nombre, int sector, int x0, int z0, int x1, int z1, int techo, Patron suelo, int color, int luces) {
		SALAS.add(new Sala(nombre, x0, z0, x1, z1, sector));
		caja(x0 - 1, SUELO, z0 - 1, x1 + 1, SUELO, z1 + 1, suelo);
		Patron p = pared(color);
		caja(x0 - 1, SUELO + 1, z0 - 1, x1 + 1, techo - 1, z0 - 1, p);
		caja(x0 - 1, SUELO + 1, z1 + 1, x1 + 1, techo - 1, z1 + 1, p);
		caja(x0 - 1, SUELO + 1, z0, x0 - 1, techo - 1, z1, p);
		caja(x1 + 1, SUELO + 1, z0, x1 + 1, techo - 1, z1, p);
		aire(x0, SUELO + 1, z0, x1, techo - 1, z1);
		techo(x0, z0, x1, z1, techo, luces);
	}

	private static void techo(int x0, int z0, int x1, int z1, int y, int luces) {
		int ox = Math.floorMod(x0 + luces / 2, luces);
		int oz = Math.floorMod(z0 + luces / 2, luces);
		caja(x0 - 1, y, z0 - 1, x1 + 1, y, z1 + 1, (x, yy, z) -> {
			if (x >= x0 && x <= x1 && z >= z0 && z <= z1 && Math.floorMod(x - ox, luces) == 0 && Math.floorMod(z - oz, luces) == 0) {
				return azar(x, z, 8, 7) == 0 ? s(BloquesEstacion.LAMPARA_FUNDIDA) : s(BloquesEstacion.LAMPARA);
			}
			return Math.floorMod(x, 6) == 0 ? s(BloquesEstacion.TECHO_TUBOS) : s(BloquesEstacion.TECHO);
		});
	}

	/** Una persiana de 3 de ancho en un muro: `x, z` su bloque de abajo a la izquierda (mirando hacia `mira`). */
	private static void persiana(int id, int x, int z, Direction mira, Direction lado) {
		Persiana p = new Persiana(id, x, z, mira, lado);
		PERSIANAS.add(p);
		for (int k = 0; k < 9; k++) {
			int[] b = p.bloque(k);
			bloque(b[0], b[1], b[2], mira(BloquesEstacion.PERSIANA, mira).setValue(Interactivos.PARTE, k));
		}
	}

	private static void punto(int x, int z, int tipo, Direction mira) {
		PUNTOS.add(new Punto(PUNTOS.size(), x, SUELO + 1, z, tipo));
		bloque(x, SUELO + 1, z, mira(BloquesEstacion.PUNTO, mira).setValue(Interactivos.TIPO, tipo));
	}

	private static void rotulo(int x, int y, int z, Direction mira, int sector, int num) {
		bloque(x, y, z, mira(BloquesEstacion.ROTULO, mira).setValue(Interactivos.COLOR, sector).setValue(Interactivos.Rotulo.NUM, num));
	}

	/* ------------------------------------------------------------- el plano */

	static {
		acceso();
		nave();
		// la pasarela antes que la sala de control: su muro sur es el de la sala (y la persiana va encima)
		pasarela();
		control();
		sector(0, -98, 8, 17, 22, false, 5);
		sector(1, 30, 8, 17, 22, true, 5);
		sector(2, -98, -98, 17, 25, false, 23);
		sector(3, 30, -98, 17, 25, true, 23);
		int x0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE;
		int z0 = Integer.MAX_VALUE;
		int z1 = Integer.MIN_VALUE;
		for (Caja c : CAJAS) {
			x0 = Math.min(x0, c.x0());
			x1 = Math.max(x1, c.x1());
			z0 = Math.min(z0, c.z0());
			z1 = Math.max(z1, c.z1());
		}
		X0 = x0;
		X1 = x1;
		Z0 = z0;
		Z1 = z1;
	}

	/** El acceso: donde se llega. Taquillas, banco, el tablon y el pasillo principal hacia el norte. */
	private static void acceso() {
		sala("acceso", -1, -7, 76, 7, 92, TECHO + 1, SUELO_H, -1, 4);
		// el pasillo principal hasta la nave
		sala("pasillo", -1, -2, 43, 2, 75, TECHO + 1, (x, y, z) -> Math.abs(x) == 2 ? s(BloquesEstacion.CHAPA) : s(BloquesEstacion.SUELO), -1, 4);
		aire(-2, SUELO + 1, 75, 2, SUELO + 3, 75);
		for (int z = 46; z <= 72; z += 3) {
			bloque(-2, SUELO + 2, z, var(BloquesEstacion.TUBERIAS, Direction.EAST, (z / 3) % 2));
			bloque(2, SUELO + 2, z + 1, var(BloquesEstacion.TUBERIAS, Direction.WEST, 2 + (z / 3) % 2));
		}
		bloque(-2, SUELO + 2, 58, var(BloquesEstacion.SENAL, Direction.EAST, 0));
		bloque(2, SUELO + 2, 52, var(BloquesEstacion.SENAL, Direction.WEST, 2));
		// taquillas y banco al oeste, el carro de herramientas y bidones al este
		for (int z = 78; z <= 90; z += 2) {
			bloque(-7, SUELO + 1, z, var(BloquesEstacion.ARMARIO, Direction.EAST, 3));
		}
		bloque(-5, SUELO + 1, 84, var(BloquesEstacion.ESTANTERIA, Direction.EAST, 2));
		bloque(7, SUELO + 1, 80, var(BloquesEstacion.BIDONES, Direction.WEST, 0));
		bloque(7, SUELO + 1, 86, var(BloquesEstacion.ESTANTERIA, Direction.WEST, 0));
		bloque(6, SUELO + 1, 90, var(BloquesEstacion.MAQUINA, Direction.WEST, 3));
		// algo de comer que se dejo la ultima cuadrilla
		COMIDA.add(new int[] {-5, 80, net.backrooms.evento.mundo.Plano.C_GALLETAS});
		COMIDA.add(new int[] {5, 84, net.backrooms.evento.mundo.Plano.C_AGUA});
		// el tablon de instrucciones (sur, mirando al norte) y los carteles
		bloque(-1, SUELO + 2, 92, var(BloquesEstacion.SENAL, Direction.NORTH, 4));
		bloque(0, SUELO + 2, 92, var(BloquesEstacion.SENAL, Direction.NORTH, 5));
		bloque(1, SUELO + 2, 92, var(BloquesEstacion.SENAL, Direction.NORTH, 6));
		bloque(-3, SUELO + 3, 76, var(BloquesEstacion.SENAL, Direction.SOUTH, 1));
		bloque(3, SUELO + 3, 76, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
		// el agujero por donde se ha caido (arriba, en el techo del acceso)
		aire(0, TECHO + 1, 88, 1, TECHO + 1, 89);
		caja(-1, TECHO + 2, 87, 2, TECHO + 2, 90, s(BloquesEstacion.CHAPA));
		RONDA.add(new int[] {0, 60});
	}

	/** La nave del generador, con el generador en medio y los pasos a los sectores. */
	private static void nave() {
		sala("nave", -1, -24, -6, 24, 42, TECHO_NAVE, (x, y, z) -> Math.abs(x) <= 6 && z >= 10 && z <= 25 ? s(BloquesEstacion.CHAPA) : SUELO_H.en(x, y, z), -1, 6);
		// del pasillo a la nave
		aire(-2, SUELO + 1, 43, 2, SUELO + 3, 43);
		// el generador: carcasa de 9 x 7 x 4 con radiadores en los extremos, tapa y dos escapes hasta el techo
		caja(-4, SUELO + 1, 14, 4, SUELO + 4, 20, (x, y, z) -> {
			if (y == SUELO + 4) {
				return s(BloquesEstacion.GENERADOR_TAPA);
			}
			if (Math.abs(x) == 4) {
				return mira(BloquesEstacion.GENERADOR_RADIADOR, x < 0 ? Direction.WEST : Direction.EAST);
			}
			return s(BloquesEstacion.GENERADOR);
		});
		caja(-3, SUELO + 2, 15, 3, SUELO + 3, 19, s(BloquesEstacion.MACIZO));
		bloque(BOCA[0], BOCA[1], BOCA[2], mira(BloquesEstacion.BOCA, Direction.SOUTH));
		for (int x : new int[] {-2, 2}) {
			caja(x, SUELO + 5, 16, x, TECHO_NAVE - 1, 16, s(BloquesEstacion.ESCAPE));
		}
		// bidones vacios junto al generador y la senal de combustible
		bloque(-6, SUELO + 1, 21, var(BloquesEstacion.BIDONES, Direction.SOUTH, 1));
		bloque(6, SUELO + 1, 21, var(BloquesEstacion.BIDONES, Direction.SOUTH, 2));
		bloque(-1, SUELO + 1, 22, var(BloquesEstacion.SENAL, Direction.SOUTH, 3));
		// a lo largo de las paredes: transformadores, armarios y cuadros
		for (int z = -4; z <= 40; z += 4) {
			if (Math.abs(z - 30) <= 3 || Math.abs(z + 4) <= 1) {
				continue;
			}
			bloque(-24, SUELO + 1, z, var(BloquesEstacion.TRANSFORMADOR, Direction.EAST, Math.floorMod(z, 3)));
			bloque(24, SUELO + 1, z, var(BloquesEstacion.ARMARIO, Direction.WEST, Math.floorMod(z, 4)));
			bloque(-24, SUELO + 3, z + 2, var(BloquesEstacion.CUADRO, Direction.EAST, 4 + Math.floorMod(z, 4)));
			bloque(24, SUELO + 3, z + 2, var(BloquesEstacion.CUADRO, Direction.WEST, 4 + Math.floorMod(z + 1, 4)));
			bloque(-24, SUELO + 6, z, var(BloquesEstacion.TUBERIAS, Direction.EAST, 1));
			bloque(24, SUELO + 6, z, var(BloquesEstacion.TUBERIAS, Direction.WEST, 1));
		}
		// lamparas en las paredes, a media altura: el techo de la nave queda muy alto y no llega abajo
		for (int z = -2; z <= 40; z += 7) {
			if (Math.abs(z - 30) <= 2 || Math.abs(z + 4) <= 2) {
				continue;
			}
			bloque(-25, SUELO + 5, z, s(BloquesEstacion.LAMPARA));
			bloque(25, SUELO + 5, z, s(BloquesEstacion.LAMPARA));
		}
		for (int x : new int[] {-18, -10, 10, 18}) {
			bloque(x, SUELO + 5, -7, s(BloquesEstacion.LAMPARA));
			bloque(x, SUELO + 5, 43, s(BloquesEstacion.LAMPARA));
		}
		// maquinas viejas (turbinas) en fila al norte
		for (int x = -18; x <= 18; x += 6) {
			if (Math.abs(x) <= 2) {
				continue;
			}
			bloque(x, SUELO + 1, 0, var(BloquesEstacion.MAQUINA, Direction.SOUTH, Math.floorMod(x, 3)));
			bloque(x + 1, SUELO + 1, 0, var(BloquesEstacion.MAQUINA, Direction.SOUTH, 3));
		}
		for (int x : new int[] {-20, -12, 12, 20}) {
			bloque(x, TECHO_NAVE - 1, 10, var(BloquesEstacion.CABLES, Direction.NORTH, 0));
			bloque(x, TECHO_NAVE - 2, 10, var(BloquesEstacion.CABLES, Direction.NORTH, 1));
		}
		bloque(-23, SUELO + 4, 41, var(BloquesEstacion.LUZ_ROJA, Direction.NORTH, 0));
		bloque(23, SUELO + 4, 41, var(BloquesEstacion.LUZ_ROJA, Direction.NORTH, 0));
		bloque(-3, SUELO + 3, -6, var(BloquesEstacion.SENAL, Direction.SOUTH, 7));
		RONDA.add(new int[] {-14, 30});
		RONDA.add(new int[] {14, 30});
		RONDA.add(new int[] {-14, 4});
		RONDA.add(new int[] {14, 4});
	}

	/** La sala de control: fusibles, interruptores, pantalla y la persiana de la pasarela. */
	private static void control() {
		sala("control", -1, -14, -24, 14, -8, TECHO + 2, (x, y, z) -> s(BloquesEstacion.CHAPA), -1, 4);
		// de la nave a la sala
		aire(-1, SUELO + 1, -7, 1, SUELO + 3, -7);
		// los huecos de fusibles (pared oeste) y los interruptores (pared este), uno por color
		for (int c = 0; c < 4; c++) {
			int z = -20 + c * 2;
			FUSIBLERA[c] = new int[] {-14, SUELO + 2, z};
			PALANCAS[c] = new int[] {14, SUELO + 2, z};
			bloque(-14, SUELO + 2, z, mira(BloquesEstacion.FUSIBLE, Direction.EAST).setValue(Interactivos.COLOR, c));
			bloque(14, SUELO + 2, z, mira(BloquesEstacion.INTERRUPTOR, Direction.WEST).setValue(Interactivos.COLOR, c));
			bloque(-14, SUELO + 3, z, var(BloquesEstacion.SENAL, Direction.EAST, 0));
		}
		// cuadros con relojes por todas partes (como una sala de control vieja)
		for (int z = -23; z <= -9; z++) {
			if (Math.floorMod(z, 2) == 0 && z >= -20 && z <= -14) {
				continue;
			}
			bloque(-14, SUELO + 2, z, var(BloquesEstacion.CUADRO, Direction.EAST, 4 + Math.floorMod(z, 4)));
			bloque(14, SUELO + 2, z, var(BloquesEstacion.CUADRO, Direction.WEST, 4 + Math.floorMod(z + 2, 4)));
		}
		for (int x = -13; x <= 13; x++) {
			if (Math.abs(x) > 2) {
				bloque(x, SUELO + 2, -24, var(BloquesEstacion.CUADRO, Direction.SOUTH, 4 + Math.floorMod(x, 4)));
				bloque(x, SUELO + 3, -24, var(BloquesEstacion.CUADRO, Direction.SOUTH, Math.floorMod(x, 4)));
			}
		}
		// las mesas de control en el medio
		for (int x = -6; x <= 6; x += 3) {
			bloque(x, SUELO + 1, -16, var(BloquesEstacion.CONSOLA, Direction.NORTH, Math.floorMod(x, 4)));
		}
		// la persiana de la pasarela, en la pared norte
		persiana(4, 1, -25, Direction.SOUTH, Direction.WEST);
		bloque(-2, SUELO + 4, -24, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
		bloque(2, SUELO + 4, -24, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
	}

	/** El pozo de la pasarela: maquinas en el fondo y la pasarela hasta la plataforma y la puerta del final. */
	private static void pasarela() {
		SALAS.add(new Sala("pasarela", -10, -98, 10, -26, -1));
		int x0 = -10;
		int x1 = 10;
		int z0 = -98;
		int z1 = -26;
		Patron muro = (x, y, z) -> y < SUELO - 4 ? s(BloquesEstacion.HORMIGON) : pared(-1).en(x, y - (y > SUELO + 3 ? 0 : 0), z);
		caja(x0 - 1, POZO, z0 - 1, x1 + 1, POZO, z1 + 1, (x, y, z) -> s(BloquesEstacion.SUELO_MANCHADO));
		caja(x0 - 1, POZO + 1, z0 - 1, x1 + 1, TECHO_POZO - 1, z0 - 1, muro);
		caja(x0 - 1, POZO + 1, z1 + 1, x1 + 1, TECHO_POZO - 1, z1 + 1, muro);
		caja(x0 - 1, POZO + 1, z0, x0 - 1, TECHO_POZO - 1, z1, muro);
		caja(x1 + 1, POZO + 1, z0, x1 + 1, TECHO_POZO - 1, z1, muro);
		aire(x0, POZO + 1, z0, x1, TECHO_POZO - 1, z1);
		techo(x0, z0, x1, z1, TECHO_POZO, 10);
		// la pasarela de rejilla con su barandilla y los focos apagados a los lados
		caja(-1, SUELO, z0 + 3, 1, SUELO, z1, s(BloquesEstacion.REJILLA));
		for (int z = z1; z >= z0 + 3; z--) {
			bloque(-2, SUELO + 1, z, mira(BloquesEstacion.BARANDILLA, Direction.WEST));
			bloque(2, SUELO + 1, z, mira(BloquesEstacion.BARANDILLA, Direction.EAST));
			bloque(-2, SUELO, z, s(BloquesEstacion.CHAPA));
			bloque(2, SUELO, z, s(BloquesEstacion.CHAPA));
			if (Math.floorMod(z1 - z, 5) == 2) {
				bloque(-2, SUELO, z, BloquesEstacion.FOCO.defaultBlockState());
				bloque(2, SUELO, z, BloquesEstacion.FOCO.defaultBlockState());
				FOCOS.add(new int[] {-2, SUELO, z});
				FOCOS.add(new int[] {2, SUELO, z});
			}
		}
		// los soportes de la pasarela, del fondo hasta ella
		for (int z = z1 - 4; z >= z0 + 4; z -= 12) {
			caja(-2, POZO + 1, z, -2, SUELO - 1, z, s(BloquesEstacion.CHAPA));
			caja(2, POZO + 1, z, 2, SUELO - 1, z, s(BloquesEstacion.CHAPA));
		}
		// la plataforma del final y la puerta (cerrada hasta que haya corriente; detras, oscuro)
		caja(-4, SUELO, z0, 4, SUELO, z0 + 2, s(BloquesEstacion.CHAPA));
		for (int x = -4; x <= 4; x++) {
			if (Math.abs(x) >= 2) {
				bloque(x, SUELO + 1, z0 + 2, mira(BloquesEstacion.BARANDILLA, Direction.SOUTH));
			}
		}
		persiana(5, 1, z0 - 1, Direction.SOUTH, Direction.WEST);
		caja(-2, SUELO, z0 - 5, 2, SUELO + 4, z0 - 2, s(Blocks.BLACK_CONCRETE));
		aire(-1, SUELO + 1, z0 - 4, 1, SUELO + 3, z0 - 2);
		bloque(0, SUELO + 4, z0, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
		bloque(-3, SUELO + 3, z0, var(BloquesEstacion.SENAL, Direction.SOUTH, 2));
		// el fondo: maquinas, transformadores, bidones, cables colgando y luces rojas
		for (int z = z0 + 4; z <= z1 - 2; z += 7) {
			for (int x : new int[] {-8, -5, 5, 8}) {
				int k = azar(x, z, 31, 4);
				Block b = k == 0 ? BloquesEstacion.TRANSFORMADOR : k == 1 ? BloquesEstacion.BIDONES : BloquesEstacion.MAQUINA;
				bloque(x, POZO + 1, z, var(b, x < 0 ? Direction.EAST : Direction.WEST, azar(x, z, 32, 4)));
			}
			bloque(x0, POZO + 4, z, var(BloquesEstacion.LUZ_ROJA, Direction.EAST, 0));
			bloque(x1, POZO + 4, z + 3, var(BloquesEstacion.LUZ_ROJA, Direction.WEST, 0));
			bloque(x0, SUELO + 2, z + 1, var(BloquesEstacion.TUBERIAS, Direction.EAST, 2));
			bloque(x1, SUELO + 2, z + 1, var(BloquesEstacion.TUBERIAS, Direction.WEST, 3));
			int cx = azar(z, 7, 33, 2) == 0 ? -6 : 6;
			for (int y = TECHO_POZO - 1; y >= TECHO_POZO - 6; y--) {
				bloque(cx, y, z + 2, var(BloquesEstacion.CABLES, Direction.NORTH, y % 2));
			}
		}
	}

	/* ------------------------------------------------------------ los sectores */

	/**
	 * Un sector: laberinto de w x h celdas desde (sx, sz) (lineas de muro cada CELDA), con su paso a
	 * la nave en la celda (borde del lado de la nave, ecz). `este`: el sector esta al este (la nave al
	 * oeste). Las salas van en sitios fijos (en espejo segun el lado).
	 */
	private static void sector(int id, int sx, int sz, int w, int h, boolean este, int ecz) {
		String n = SECTORES[id];
		SALAS.add(new Sala("sector_" + n.toLowerCase(), sx + 1, sz + 1, sx + w * CELDA - 1, sz + h * CELDA - 1, id));
		int ecx = este ? 0 : w - 1;
		// las salas, en celdas (cx, cz, ancho, alto), con la x en espejo en los del este
		boolean norte = sz < 0;
		int[][] salas = norte
			? new int[][] {{9, 18, 2, 2}, {3, 6, 2, 2}, {10, 9, 3, 3}, {4, 14, 3, 3}, {0, 0, 3, 2}}
			: new int[][] {{9, 1, 2, 2}, {3, 14, 2, 2}, {11, 12, 3, 3}, {5, 6, 3, 3}, {0, 19, 3, 2}};
		String[] tipos = {"paneles_1", "paneles_2", "maquinas_1", "maquinas_2", "fusibles"};
		for (int[] r : salas) {
			if (este) {
				r[0] = w - r[0] - r[2];
			}
		}
		boolean[][] enSala = new boolean[w][h];
		for (int[] r : salas) {
			for (int i = 0; i < r[2]; i++) {
				for (int j = 0; j < r[3]; j++) {
					enSala[r[0] + i][r[1] + j] = true;
				}
			}
		}
		// el laberinto (backtracking con semilla fija) con algunos pasos de mas para que haya vueltas
		boolean[][] este2 = new boolean[w][h];   // paso de (cx, cz) a (cx + 1, cz)
		boolean[][] sur = new boolean[w][h];     // paso de (cx, cz) a (cx, cz + 1)
		Random azar = new Random(4310L + id * 977L);
		boolean[][] visto = new boolean[w][h];
		int[] pila = new int[w * h * 2];
		int tope = 0;
		pila[tope++] = ecx;
		pila[tope++] = ecz;
		visto[ecx][ecz] = true;
		int[][] DIR = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (tope > 0) {
			int cx = pila[tope - 2];
			int cz = pila[tope - 1];
			int[] orden = {0, 1, 2, 3};
			for (int i = 3; i > 0; i--) {
				int k = azar.nextInt(i + 1);
				int t = orden[i];
				orden[i] = orden[k];
				orden[k] = t;
			}
			boolean avanzo = false;
			for (int o : orden) {
				int nx = cx + DIR[o][0];
				int nz = cz + DIR[o][1];
				if (nx < 0 || nz < 0 || nx >= w || nz >= h || visto[nx][nz]) {
					continue;
				}
				abrir(este2, sur, cx, cz, nx, nz);
				visto[nx][nz] = true;
				pila[tope++] = nx;
				pila[tope++] = nz;
				avanzo = true;
				break;
			}
			if (!avanzo) {
				tope -= 2;
			}
		}
		for (int cx = 0; cx < w; cx++) {
			for (int cz = 0; cz < h; cz++) {
				if (cx + 1 < w && !este2[cx][cz] && azar.nextInt(100) < 12) {
					este2[cx][cz] = true;
				}
				if (cz + 1 < h && !sur[cx][cz] && azar.nextInt(100) < 12) {
					sur[cx][cz] = true;
				}
			}
		}
		// dentro de las salas, todo abierto
		for (int[] r : salas) {
			for (int i = 0; i < r[2]; i++) {
				for (int j = 0; j < r[3]; j++) {
					if (i + 1 < r[2]) {
						este2[r[0] + i][r[1] + j] = true;
					}
					if (j + 1 < r[3]) {
						sur[r[0] + i][r[1] + j] = true;
					}
				}
			}
		}
		// la de fusibles: cerrada salvo una puerta (la persiana) al pasillo
		int[] f = salas[4];
		for (int i = 0; i < f[2]; i++) {
			for (int j = 0; j < f[3]; j++) {
				int cx = f[0] + i;
				int cz = f[1] + j;
				if (i == f[2] - 1 && cx + 1 < w) {
					este2[cx][cz] = false;
				}
				if (i == 0 && cx > 0) {
					este2[cx - 1][cz] = false;
				}
				if (j == f[3] - 1 && cz + 1 < h) {
					sur[cx][cz] = false;
				}
				if (j == 0 && cz > 0) {
					sur[cx][cz - 1] = false;
				}
			}
		}
		// la puerta de la sala de fusibles: hacia dentro del sector (por arriba o por abajo, la del medio)
		int pcx = f[0] + 1;
		boolean puertaSur = f[1] == 0;
		int pcz = puertaSur ? f[1] + f[3] - 1 : f[1];
		if (puertaSur) {
			sur[pcx][pcz] = true;
		} else {
			sur[pcx][pcz - 1] = true;
		}

		int color = id;
		int xw = sx + w * CELDA;
		int zw = sz + h * CELDA;
		// todo macizo y luego se excava
		caja(sx, SUELO, sz, xw, SUELO, zw, SUELO_H);
		caja(sx, SUELO + 1, sz, xw, TECHO - 1, zw, pared(color));
		for (int cx = 0; cx < w; cx++) {
			for (int cz = 0; cz < h; cz++) {
				int x0 = sx + 1 + cx * CELDA;
				int z0 = sz + 1 + cz * CELDA;
				aire(x0, SUELO + 1, z0, x0 + 2, TECHO - 1, z0 + 2);
				if (este2[cx][cz]) {
					aire(x0 + 3, SUELO + 1, z0, x0 + 3, TECHO - 1, z0 + 2);
				}
				if (sur[cx][cz]) {
					aire(x0, SUELO + 1, z0 + 3, x0 + 2, TECHO - 1, z0 + 3);
				}
				// las esquinas entre cuatro celdas abiertas (dentro de las salas)
				if (este2[cx][cz] && sur[cx][cz] && cx + 1 < w && cz + 1 < h && sur[cx + 1][cz] && este2[cx][cz + 1]) {
					aire(x0 + 3, SUELO + 1, z0 + 3, x0 + 3, TECHO - 1, z0 + 3);
				}
			}
		}
		// el techo: lamparas en una de cada dos celdas (alguna fundida), zonas enteras sin luz
		caja(sx, TECHO, sz, xw, TECHO, zw, (x, y, z) -> {
			int cx = Math.floorDiv(x - sx - 1, CELDA);
			int cz = Math.floorDiv(z - sz - 1, CELDA);
			int u = Math.floorMod(x - sx - 1, CELDA);
			int v = Math.floorMod(z - sz - 1, CELDA);
			boolean oscura = azar(cx / 4 + id * 7, cz / 4, 41, 4) == 0;
			if (u == 1 && v == 1 && ((cx + cz) & 1) == 0 && !oscura) {
				return azar(cx, cz, 42 + id, 5) == 0 ? s(BloquesEstacion.LAMPARA_FUNDIDA) : s(BloquesEstacion.LAMPARA);
			}
			return u == 1 ? s(BloquesEstacion.TECHO_TUBOS) : s(BloquesEstacion.TECHO);
		});
		// el paso a la nave
		int px = este ? sx : xw;
		int ez0 = sz + 1 + ecz * CELDA;
		int naveX = este ? 25 : -25;
		caja(Math.min(px, naveX), SUELO, ez0 - 1, Math.max(px, naveX), SUELO, ez0 + 3, SUELO_H);
		caja(Math.min(px, naveX), SUELO + 1, ez0 - 1, Math.max(px, naveX), TECHO - 1, ez0 - 1, pared(color));
		caja(Math.min(px, naveX), SUELO + 1, ez0 + 3, Math.max(px, naveX), TECHO - 1, ez0 + 3, pared(color));
		caja(Math.min(px, naveX), TECHO, ez0 - 1, Math.max(px, naveX), TECHO, ez0 + 3, s(BloquesEstacion.TECHO));
		aire(Math.min(px, naveX), SUELO + 1, ez0, Math.max(px, naveX), SUELO + 3, ez0 + 2);
		Direction haciaNave = este ? Direction.WEST : Direction.EAST;
		bloque(este ? naveX - 1 : naveX + 1, SUELO + 4, ez0 + 1, mira(BloquesEstacion.ROTULO, haciaNave).setValue(Interactivos.COLOR, id).setValue(Interactivos.Rotulo.NUM, 0));
		// las salas: lo que llevan
		Direction[] lados = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int k = 0; k < salas.length; k++) {
			int[] r = salas[k];
			int x0 = sx + 1 + r[0] * CELDA;
			int z0 = sz + 1 + r[1] * CELDA;
			int x1 = x0 + r[2] * CELDA - 2;
			int z1 = z0 + r[3] * CELDA - 2;
			SALAS.add(new Sala(n.toLowerCase() + "_" + tipos[k], x0, z0, x1, z1, id));
			// el suelo de chapa y el rotulo de la sala en una pared
			caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesEstacion.CHAPA));
			int num = k < 2 ? k + 1 : k < 4 ? k + 1 : 9;
			if (k < 2) {
				// el panel averiado en la pared norte (mirando al sur), en medio; lo que necesita depende del sector y la sala
				int tipo = (id + k * 2) % 3;
				int ppx = (x0 + x1) / 2;
				PANELES.add(new Panel(PANELES.size(), ppx, SUELO + 2, z0, tipo, id));
				bloque(ppx, SUELO + 2, z0, mira(BloquesEstacion.PANEL, Direction.SOUTH).setValue(Interactivos.TIPO, tipo));
				bloque(ppx - 1, SUELO + 2, z0, var(BloquesEstacion.CUADRO, Direction.SOUTH, 4 + k));
				bloque(ppx + 1, SUELO + 2, z0, var(BloquesEstacion.CUADRO, Direction.SOUTH, 6 + k));
				bloque(x0, SUELO + 1, z1, var(BloquesEstacion.ARMARIO, Direction.NORTH, k));
				bloque(x1, SUELO + 1, z1, var(BloquesEstacion.ESTANTERIA, Direction.NORTH, 3));
				rotulo(ppx, SUELO + 4, z0, Direction.SOUTH, id, num);
			} else if (k < 4) {
				// maquinas: un transformador grande en medio, armarios y bidones (y un bidon que se coge)
				int cx = (x0 + x1) / 2;
				int cz = (z0 + z1) / 2;
				caja(cx - 1, SUELO + 1, cz - 1, cx + 1, SUELO + 1, cz + 1, var(BloquesEstacion.TRANSFORMADOR, Direction.SOUTH, k));
				caja(cx - 1, SUELO + 2, cz - 1, cx + 1, SUELO + 2, cz + 1, var(BloquesEstacion.MAQUINA, Direction.SOUTH, 2));
				bloque(x0, SUELO + 1, z0, var(BloquesEstacion.BIDONES, Direction.SOUTH, 0));
				bloque(x1, SUELO + 1, z0, var(BloquesEstacion.ARMARIO, Direction.SOUTH, 2));
				punto(x0, z1, 3, Direction.NORTH);
				bloque(x1, SUELO + 1, z1, var(BloquesEstacion.CABLES, Direction.NORTH, 0));
				rotulo(cx, SUELO + 4, z0, Direction.SOUTH, id, num);
				COMIDA.add(new int[] {cx, z1, k == 2 ? net.backrooms.evento.mundo.Plano.C_PIZZA : net.backrooms.evento.mundo.Plano.C_AGUA});
			} else {
				// fusibles: el fusible de su color en un pedestal, cuadros y luz roja
				// en la pared del fondo (la contraria a la puerta): cuadros, y delante el fusible en su pedestal
				int cx = (x0 + x1) / 2;
				int fondo = puertaSur ? z0 : z1;
				Direction m = puertaSur ? Direction.SOUTH : Direction.NORTH;
				for (int x = x0; x <= x1; x++) {
					bloque(x, SUELO + 2, fondo, var(BloquesEstacion.CUADRO, m, 4 + Math.floorMod(x, 4)));
				}
				punto(cx, fondo + (puertaSur ? 1 : -1), 4 + id, m);
				bloque(x0, SUELO + 3, (z0 + z1) / 2, var(BloquesEstacion.LUZ_ROJA, Direction.EAST, 0));
				// la persiana: en la pared de la puerta, mirando afuera
				int pz = puertaSur ? z1 + 1 : z0 - 1;
				int pxl = sx + 1 + pcx * CELDA;
				persiana(id, puertaSur ? pxl + 2 : pxl, pz, puertaSur ? Direction.SOUTH : Direction.NORTH, puertaSur ? Direction.WEST : Direction.EAST);
				rotulo(pxl + 1, SUELO + 4, puertaSur ? pz + 1 : pz - 1, puertaSur ? Direction.SOUTH : Direction.NORTH, id, 9);
			}
		}
		// por los pasillos: senales, tuberias, cuadros, cables, materiales y bidones en los callejones sin salida
		int materiales = 0;
		for (int cx = 0; cx < w; cx++) {
			for (int cz = 0; cz < h; cz++) {
				if (enSala[cx][cz]) {
					continue;
				}
				int x0 = sx + 1 + cx * CELDA;
				int z0 = sz + 1 + cz * CELDA;
				RONDA.add(new int[] {x0 + 1, z0 + 1});
				boolean[] abierto = {cz > 0 && sur[cx][cz - 1], cx + 1 < w && este2[cx][cz], cz + 1 < h && sur[cx][cz], cx > 0 && este2[cx - 1][cz]};
				if (cx == ecx && cz == ecz) {
					abierto[este ? 3 : 1] = true;
				}
				int salidas = 0;
				for (boolean b : abierto) {
					salidas += b ? 1 : 0;
				}
				// el lado que es pared, para pegar cosas
				int ladoPared = -1;
				for (int i = 0; i < 4; i++) {
					int l = (azar(cx, cz, 50 + id, 4) + i) % 4;
					if (!abierto[l]) {
						ladoPared = l;
						break;
					}
				}
				if (ladoPared < 0) {
					continue;
				}
				// el bloque junto a esa pared, en medio, mirando hacia dentro
				int bx = x0 + 1 + (ladoPared == 1 ? 1 : ladoPared == 3 ? -1 : 0);
				int bz = z0 + 1 + (ladoPared == 2 ? 1 : ladoPared == 0 ? -1 : 0);
				Direction dentro = lados[(ladoPared + 2) % 4];
				if (salidas == 1 && materiales < 9) {
					// callejon sin salida: un punto de recogida (material o bidon)
					// por sector: dos de cada material y tres bidones
					int[] reparto = {0, 3, 1, 2, 3, 0, 1, 2, 3};
					punto(bx, bz, reparto[(materiales + id) % 9], dentro);
					materiales++;
					continue;
				}
				int r = azar(cx, cz, 60 + id, 20);
				if (r < 4) {
					bloque(bx, SUELO + 2, bz, var(BloquesEstacion.TUBERIAS, dentro, r));
				} else if (r < 7) {
					bloque(bx, SUELO + 2, bz, var(BloquesEstacion.CUADRO, dentro, r));
				} else if (r < 9) {
					bloque(bx, SUELO + 2, bz, var(BloquesEstacion.SENAL, dentro, r == 7 ? 0 : 7));
				} else if (r < 11) {
					rotulo(bx, SUELO + 3, bz, dentro, id, 1 + Math.floorMod(cx / 4 + cz / 4 * 3, 8));
				} else if (r < 12) {
					bloque(bx, SUELO + 1, bz, var(BloquesEstacion.BIDONES, dentro, 2));
				} else if (r < 13) {
					bloque(x0 + 1, TECHO - 1, z0 + 1, var(BloquesEstacion.CABLES, Direction.NORTH, 0));
				} else if (r < 14) {
					bloque(bx, SUELO + 3, bz, var(BloquesEstacion.LUZ_ROJA, dentro, 0));
				}
			}
		}
	}

	private static void abrir(boolean[][] este, boolean[][] sur, int ax, int az, int bx, int bz) {
		if (bx == ax + 1) {
			este[ax][az] = true;
		} else if (bx == ax - 1) {
			este[bx][bz] = true;
		} else if (bz == az + 1) {
			sur[ax][az] = true;
		} else {
			sur[bx][bz] = true;
		}
	}

	/** La sala (por nombre) donde esta ese punto, o null. Las de despues (mas pequenas) mandan. */
	public static Sala sala(double x, double z) {
		Sala r = null;
		for (Sala s : SALAS) {
			if (s.dentro(x, z)) {
				r = s;
			}
		}
		return r;
	}

	/** Si (x, z) esta en el sector `id` (en su laberinto o sus salas). */
	public static int sector(double x, double z) {
		Sala s = sala(x, z);
		return s == null ? -1 : s.sector();
	}
}
