package net.backrooms.evento.fiesta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.backrooms.evento.mundo.Plano;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El trazado de la Fiesta (Nivel Fun): un edificio cerrado, hecho a mano, que GeneradorFiesta
 * pone al generar cada chunk a partir de una lista de cajas de bloques (las de despues pisan a
 * las de antes). Suelo en y = SUELO (se pisa en SUELO + 1), lejos del fondo del mundo para que
 * la niebla no se vuelva negra. Como en Backrooms: Escape Together, una vuelta es:
 *
 *   1. LAS HABITACIONES DE INICIO: se empieza en un cuartito con una puerta (uno de doce, al azar).
 *   2. EL LABERINTO (x -156..-4, z -76..76, 152 x 152): pasillos blancos con columnas de colores, suelo a
 *      cuadros y banderines. Por los pasillos, mesas de fiesta con monedas (se cogen con clic derecho)
 *      o comida; las monedas abren las puertas de las habitaciones (60 salas seguras con dos
 *      puertas, para cruzarlas, y el localizador de salida; la memoria, los regalos, la cocina, la siesta, la sala secreta y la de
 *      las mesas). Los tickets no estan tirados: se ganan. Algun suelo esta defectuoso: cede y
 *      lleva a un parkour, y la trampilla del final sube directa al castillo. La salida es una
 *      puerta roja en la pared este (zumba cuando estas cerca).
 *   3. EL CASTILLO (x 8..56, z -24..24): el parque de bolas, dos pisos de pasillos acolchados
 *      con redes, escaleras de arcoiris y semaforos; se cruza moviendose solo en verde
 *      (JuegoSemaforo). Arriba, al este, el tobogan baja a la sala central con el bote de la vuelta.
 *   4. LA SALA CENTRAL (x 66..94, z -12..12): el marcador de tickets, el bote, los cuadrados de
 *      noclip (por ahi se vuelve a empezar, en otra habitacion de inicio) y la puerta del final,
 *      que se abre con los tickets del objetivo.
 *   5. EL SALON FINAL (x 58..102, z -64..-22): la fiesta de verdad; al fondo, el tobogan por el
 *      que se sale del nivel.
 *
 * Debajo del laberinto (suelo en SUB) estan las tres zonas de parkour.
 */
public final class PlanoFiesta {
	public static final int SUELO = 40;
	/** Techo del laberinto y sus habitaciones (se puede estar de pie de SUELO + 1 a TECHO - 1). */
	public static final int TECHO = SUELO + 6;
	/** El suelo de las zonas de parkour (debajo del laberinto): se pisa en SUB + 1. */
	public static final int SUB = SUELO - 8;

	/* el laberinto: celdas de CELDA (3 de paso y 1 de pared) */
	public static final int LAB_X = -156;
	public static final int LAB_Z = -76;
	public static final int LAB_W = 38;
	public static final int LAB_H = 38;
	public static final int CELDA = 4;
	/** La celda de la salida (en el borde este) y su puerta roja, en la pared de fuera. */
	public static final int SALIDA_CZ = 19;
	public static final int SALIDA_X = LAB_X + LAB_W * CELDA;
	public static final int SALIDA_Z = LAB_Z + SALIDA_CZ * CELDA + 2;

	/* el castillo de bolas: CAS_N x CAS_N celdas en dos pisos */
	public static final int CAS_X = 8;
	public static final int CAS_Z = -24;
	public static final int CAS_N = 12;
	public static final int CAS_LADO = CAS_N * CELDA;
	/** El suelo del piso de arriba (se pisa en CAS_P1 + 1) y el tejado de lona. */
	public static final int CAS_P1 = SUELO + 4;
	public static final int CAS_TEJADO = SUELO + 8;
	public static final int TECHO_CASTILLO = SUELO + 14;
	/** La fila de celdas de la entrada (oeste, abajo) y de la salida al tobogan (este, arriba). */
	public static final int CAS_FILA = 6;
	public static final double CASTILLO_LLEGADA_X = CAS_X + 2.5;
	public static final double CASTILLO_LLEGADA_Z = CAS_Z + CAS_FILA * CELDA + 2.5;
	/** La boca del tobogan, fuera del castillo por el este, a la altura del piso de arriba. */
	public static final int TOB_X0 = CAS_X + CAS_LADO + 2;
	public static final int TOB_X1 = TOB_X0 + 2;
	public static final int TOB_Z0 = CAS_Z + CAS_FILA * CELDA + 1;
	public static final int TOB_Z1 = TOB_Z0 + 2;

	/* la sala central */
	public static final int CEN_X0 = 66;
	public static final int CEN_X1 = 94;
	public static final int CEN_Z0 = -12;
	public static final int CEN_Z1 = 12;
	public static final int TECHO_CENTRAL = SUELO + 7;
	/** Donde sale el tobogan del castillo (mirando al este). */
	public static final double CENTRAL_LLEGADA_X = CEN_X0 + 1.5;
	public static final double CENTRAL_LLEGADA_Z = 0.5;
	/** La puerta del final, en la pared norte de la sala central. */
	public static final int PUERTA_FINAL_X = 80;
	public static final int PUERTA_FINAL_Z = CEN_Z0 - 1;

	/* el salon final y su tobogan (la salida del nivel) */
	public static final int FIN_X0 = 58;
	public static final int FIN_X1 = 102;
	public static final int FIN_Z0 = -64;
	public static final int FIN_Z1 = -22;
	public static final int TECHO_FINAL = SUELO + 12;
	public static final int FINAL_TOB_X0 = 79;
	public static final int FINAL_TOB_X1 = 81;
	public static final int FINAL_TOB_Z0 = FIN_Z0 - 4;
	public static final int FINAL_TOB_Z1 = FIN_Z0 - 2;

	@FunctionalInterface
	public interface Patron {
		BlockState en(int x, int y, int z);
	}

	public record Caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron patron) {
	}

	/** Un punto donde hay algo que coger: id fijo (lo que se guarda), sitio y cuanto vale. */
	public record Punto(int id, double x, double y, double z, int valor) {
	}

	/** Una puerta: la casilla de abajo, hacia donde mira (el lado que paga), lo que cuesta y su tipo (Torno.TIPO). */
	public record Puerta(int id, int x, int y, int z, Direction mira, int coste, String nombre, int tipo) {
	}

	/** Comida encima de una mesa o una encimera (Comida la pone): bloque x, z, altura y tipo (Plano.C_*). */
	public record Plato(int x, double y, int z, int tipo) {
	}

	/** Las salas, para saber donde esta cada uno (sonidos, HUD, /backrooms fiesta ir): nombre y caja x0, z0, x1, z1. */
	public record Sala(String nombre, int x0, int z0, int x1, int z1) {
		public boolean dentro(double x, double z) {
			return x >= this.x0 && x < this.x1 + 1 && z >= this.z0 && z < this.z1 + 1;
		}
	}

	/** Una habitacion del laberinto: su tipo, sus celdas (cx, cz, ancho y alto en celdas) y lo que cuesta entrar. */
	public record Habitacion(String tipo, String nombre, int cx, int cz, int w, int h, int coste) {
		public int x0() {
			return LAB_X + this.cx * CELDA + 1;
		}

		public int z0() {
			return LAB_Z + this.cz * CELDA + 1;
		}

		public int x1() {
			return LAB_X + (this.cx + this.w) * CELDA - 1;
		}

		public int z1() {
			return LAB_Z + (this.cz + this.h) * CELDA - 1;
		}
	}

	/**
	 * Una zona de parkour escondida bajo un parche de suelo defectuoso: la caja de dentro, donde
	 * se cae (debajo del parche), la trampilla del final y adonde sube (el castillo); `premio`
	 * son los tickets de la primera vez.
	 */
	public record Parkour(int id, String nombre, int x0, int z0, int x1, int z1, double llegadaX, double llegadaZ,
		int finX0, int finZ0, int finX1, int finZ1, double salidaX, double salidaZ, float salidaGiro, int premio) {
		public boolean dentro(double x, double y, double z) {
			return x >= this.x0 && x < this.x1 + 1 && z >= this.z0 && z < this.z1 + 1 && y < SUELO && y > SUB - 2;
		}

		public boolean enFin(double x, double z) {
			return x >= this.finX0 && x < this.finX1 + 1 && z >= this.finZ0 && z < this.finZ1 + 1;
		}
	}

	public static final List<Caja> CAJAS = new ArrayList<>();
	public static final List<Punto> MONEDAS = new ArrayList<>();
	public static final List<Punto> TICKETS = new ArrayList<>();
	public static final List<Puerta> PUERTAS = new ArrayList<>();
	public static final List<Plato> COMIDA = new ArrayList<>();
	public static final List<Habitacion> HABITACIONES = new ArrayList<>();
	public static final List<Sala> SALAS = new ArrayList<>();
	public static final List<Parkour> PARKOURS = new ArrayList<>();
	/** Donde aparece quien empieza una vuelta: x, z y giro, en una habitacion de inicio mirando a su puerta. */
	public static final List<double[]> INICIOS = new ArrayList<>();
	/** Altavoces (x, y, z): de ahi sale la musica lejana y los anuncios de la megafonia. */
	public static final List<int[]> ALTAVOCES = new ArrayList<>();
	/** Sitios por donde puede aparecer y pasear el Anfitrion (x, z, a la altura del suelo). */
	public static final List<int[]> RONDA = new ArrayList<>();
	/** Los semaforos del castillo (x, y, z): los enciende todos a la vez JuegoSemaforo. */
	public static final List<int[]> SEMAFOROS = new ArrayList<>();
	/** Los cuadrados de noclip de la sala central (x0, z0, de 2 x 2). */
	public static final List<int[]> NOCLIPS = new ArrayList<>();

	/* la memoria y los regalos (salen de sus habitaciones) */
	/** Las baldosas de la memoria (x0, z0 de cada una, de 2x2): roja, amarilla, azul y verde. */
	public static final int[][] MEMORIA = new int[4][2];
	/** El boton de empezar la memoria (x0, z0, de 2x1) y el de los regalos. */
	public static final int[] MEMORIA_BOTON = new int[2];
	public static final int[] REGALOS_BOTON = new int[2];
	/** La sala de los regalos (interior) y sus regalos que se pueden abrir (el de arriba de cada monton): x, y, z. */
	public static final int[] REGALOS_SALA = new int[4];
	public static final List<int[]> REGALOS_SITIOS = new ArrayList<>();

	/** Limites del edificio (con sus muros), para el generador: salen de las cajas. */
	public static final int X0;
	public static final int X1;
	public static final int Z0;
	public static final int Z1;
	public static final int Y1;

	private static int siguienteId;
	/** Las celdas del laberinto con parche de suelo defectuoso (encima de cada parkour). */
	private static final int[][] PARCHES = {{3, 12}, {10, 4}, {16, 18}, {28, 28}, {30, 6}};
	/** Puertas pendientes (van al final, para que ningun muro las pise): x, z, mira, coste, tipo y nombre. */
	private static final List<Object[]> PENDIENTES = new ArrayList<>();
	/** Por habitacion: la casilla de dentro junto a su puerta (para no taparla con muebles). */
	private static final List<int[]> ENTRADAS = new ArrayList<>();
	/** Las celdas de pasillo que tienen delante una puerta de habitacion (ahi no van mesas). */
	private static final java.util.Set<Long> FRENTES = new java.util.HashSet<>();
	/** Por habitacion: las casillas de dentro junto a todas sus puertas. */
	private static final List<List<int[]>> TODAS = new ArrayList<>();

	private PlanoFiesta() {
	}

	/* ------------------------------------------------------------ cajas */

	private static void caja(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		caja(x0, y0, z0, x1, y1, z1, (x, y, z) -> s);
	}

	private static void caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron p) {
		CAJAS.add(new Caja(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), p));
	}

	private static void aire(int x0, int y0, int z0, int x1, int y1, int z1) {
		caja(x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
	}

	private static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	private static BlockState mira(Block b, Direction d) {
		return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
	}

	private static BlockState variante(Block b, Direction d, int v) {
		BlockState e = mira(b, d);
		if (e.hasProperty(ParedVariante.VARIANTE)) {
			return e.setValue(ParedVariante.VARIANTE, v);
		}
		return e.setValue(SueloVariante.VARIANTE, v);
	}

	private static int hash(int x, int z, int canal) {
		int h = x * 73856093 ^ z * 19349663 ^ canal * 83492791;
		h = (h ^ (h >>> 13)) * 0x5bd1e995;
		return h ^ (h >>> 15);
	}

	private static int azar(int x, int z, int canal, int n) {
		return Math.floorMod(hash(x, z, canal), n);
	}

	/** El color del damero del laberinto en (x, z): 0 blanco, 1 azul, 2 amarillo (en diagonales). */
	public static int damero(int x, int z) {
		if (((x + z) & 1) == 0) {
			return 0;
		}
		return Math.floorMod(x - z, 4) < 2 ? 1 : 2;
	}

	/**
	 * Una sala: suelo, paredes (`zocalo` en las `altoZocalo` de abajo, `papel` encima) y techo con
	 * luces cada `luces` bloques (alguna que falla). Interior de x0..x1, z0..z1; los muros alrededor.
	 */
	private static void sala(String nombre, int x0, int z0, int x1, int z1, int techo, Block suelo, Block zocalo, Block papel, int altoZocalo,
		int luces) {
		SALAS.add(new Sala(nombre, x0, z0, x1, z1));
		caja(x0 - 1, SUELO, z0 - 1, x1 + 1, SUELO, z1 + 1, s(suelo));
		Patron pared = (x, y, z) -> y <= SUELO + altoZocalo ? s(zocalo)
			: (papel == BloquesFiesta.PAPEL && (Math.floorMod(hash(x, z, y / 3), 41) == 0) ? s(BloquesFiesta.PAPEL_SUCIO) : s(papel));
		caja(x0 - 1, SUELO + 1, z0 - 1, x1 + 1, techo - 1, z0 - 1, pared);
		caja(x0 - 1, SUELO + 1, z1 + 1, x1 + 1, techo - 1, z1 + 1, pared);
		caja(x0 - 1, SUELO + 1, z0, x0 - 1, techo - 1, z1, pared);
		caja(x1 + 1, SUELO + 1, z0, x1 + 1, techo - 1, z1, pared);
		aire(x0, SUELO + 1, z0, x1, techo - 1, z1);
		int ox = Math.floorMod(x0 + 1, luces);
		int oz = Math.floorMod(z0 + 1, luces);
		caja(x0 - 1, techo, z0 - 1, x1 + 1, techo, z1 + 1, (x, y, z) -> {
			if (x >= x0 && x <= x1 && z >= z0 && z <= z1 && Math.floorMod(x - ox, luces) == 0 && Math.floorMod(z - oz, luces) == 0) {
				return Math.floorMod(hash(x, z, 7), 9) == 0 ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.LUZ);
			}
			return s(BloquesFiesta.TECHO);
		});
	}

	private static void sala(String nombre, int x0, int z0, int x1, int z1, int techo, Block suelo, Block papel, int luces) {
		sala(nombre, x0, z0, x1, z1, techo, suelo, BloquesFiesta.ZOCALO, papel, 1, luces);
	}

	/** Un hueco de paso en un muro (de 3 de alto). */
	private static void hueco(int x0, int z0, int x1, int z1) {
		aire(x0, SUELO + 1, z0, x1, SUELO + 3, z1);
	}

	/** Una puerta en un muro: la hoja de 1 x 2 en (x, z), mirando al lado que paga. */
	private static void puerta(int x, int z, Direction mira, int coste, int tipo, String nombre) {
		BlockState base = BloquesFiesta.PUERTA.defaultBlockState().setValue(Torno.FACING, mira).setValue(Torno.COSTE, coste).setValue(Torno.TIPO, tipo);
		bloque(x, SUELO + 1, z, base.setValue(Torno.PARTE, 0));
		bloque(x, SUELO + 2, z, base.setValue(Torno.PARTE, 1));
		PUERTAS.add(new Puerta(PUERTAS.size(), x, SUELO + 1, z, mira, coste, nombre, tipo));
	}

	/** La altura del tablero de una mesa de fiesta (13/16 de su bloque): ahi se apoyan monedas y comida. */
	private static final double MESA_Y = SUELO + 1 + 13.0 / 16.0 + 0.01;

	private static void moneda(double x, double y, double z) {
		MONEDAS.add(new Punto(siguienteId++, x, y, z, 1));
	}

	/** Si (x, z) esta a `d` o menos (en manhattan) de alguna de las entradas. */
	private static boolean junto(List<int[]> entradas, int x, int z, int d) {
		for (int[] e : entradas) {
			if (Math.abs(e[0] - x) + Math.abs(e[1] - z) <= d) {
				return true;
			}
		}
		return false;
	}

	/** Un regalo de la sala de los regalos: se apunta como sitio (JuegoRegalos elige cuales son de verdad). */
	private static void regalo(int x, int y, int z, int k) {
		bloque(x, y, z, variante(BloquesFiesta.REGALO, Direction.from2DDataValue(k % 4), Math.floorMod(k * 7 + x, 4)));
		REGALOS_SITIOS.add(new int[] {x, y, z});
	}

	/** Un ramo de tres globos atados a un peso, en el suelo (y la altura que se le pase). */
	private static void racimo(int x, int z, int y, int semilla) {
		bloque(x, y, z, variante(BloquesFiesta.RAMO_GLOBOS, Direction.from2DDataValue(Math.floorMod(semilla, 4)), Math.floorMod(semilla * 7 + 3, 4)));
	}

	private static void bloque(int x, int y, int z, BlockState s) {
		caja(x, y, z, x, y, z, s);
	}

	private static void nota(int x, int z, int variante) {
		bloque(x, SUELO + 1, z, BloquesFiesta.NOTA.defaultBlockState().setValue(NotaFiesta.FACING, Direction.from2DDataValue(variante % 4))
			.setValue(NotaFiesta.VARIANTE, variante));
	}

	/** Una mesa de fiesta (2 bloques, de x0, z0 hacia `largo`) con lo que lleva encima. */
	private static void mesa(int x, int z, Direction largo, int color) {
		bloque(x, SUELO + 1, z, variante(BloquesFiesta.MESA, Direction.NORTH, color));
		bloque(x + largo.getStepX(), SUELO + 1, z + largo.getStepZ(), variante(BloquesFiesta.MESA, Direction.NORTH, color));
	}

	/* ----------------------------------------------------------- el plano */

	static {
		// los pasillos van antes que las salas que unen: sus muros son los de ellas
		habitaciones();
		parkours();
		pasilloCastillo();
		laberinto();
		castillo();
		pasilloFinal();
		central();
		salonFinal();
		conexiones();
		int x0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE;
		int z0 = Integer.MAX_VALUE;
		int z1 = Integer.MIN_VALUE;
		int y1 = Integer.MIN_VALUE;
		for (Caja c : CAJAS) {
			x0 = Math.min(x0, c.x0());
			x1 = Math.max(x1, c.x1());
			z0 = Math.min(z0, c.z0());
			z1 = Math.max(z1, c.z1());
			y1 = Math.max(y1, c.y1());
		}
		X0 = x0;
		X1 = x1;
		Z0 = z0;
		Z1 = z1;
		Y1 = y1;
	}

	/** Cuantas salas seguras hay (cada una con su localizador de salida). */
	public static final int SEGURAS = 60;

	/**
	 * Las habitaciones del laberinto (en celdas). Ninguna toca a otra, ni en diagonal: entre ellas
	 * siempre hay pasillo. Las de inicio y las de los juegos van a mano; las salas seguras se reparten
	 * solas (semilla fija) por el sitio que queda.
	 */
	private static void habitaciones() {
		int[][] inicios = {{0, 5}, {0, 16}, {0, 28}, {8, 0}, {19, 0}, {30, 0}, {8, 37}, {19, 37}, {30, 37}, {12, 13}, {25, 9}, {22, 27},
			{5, 31}, {33, 22}, {16, 6}, {28, 33}};
		for (int i = 0; i < inicios.length; i++) {
			HABITACIONES.add(new Habitacion("inicio", "inicio_" + (i + 1), inicios[i][0], inicios[i][1], 1, 1, 0));
		}
		HABITACIONES.add(new Habitacion("memoria", "memoria", 8, 20, 3, 3, 2));
		HABITACIONES.add(new Habitacion("regalos", "regalos", 24, 15, 4, 3, 2));
		HABITACIONES.add(new Habitacion("cocina", "cocina", 9, 3, 3, 2, 2));
		HABITACIONES.add(new Habitacion("siesta", "siesta", 21, 3, 3, 2, 2));
		HABITACIONES.add(new Habitacion("secreta", "secreta", 33, 32, 2, 2, 3));
		HABITACIONES.add(new Habitacion("mesas", "mesas", 14, 24, 3, 2, 1));
		Random azar = new Random(777);
		int seguras = 0;
		for (int intento = 0; intento < 20000 && seguras < SEGURAS; intento++) {
			int forma = azar.nextInt(3);
			int w = forma == 1 ? 1 : 2;
			int h = forma == 2 ? 1 : 2;
			int cx = azar.nextInt(LAB_W - w + 1);
			int cz = azar.nextInt(LAB_H - h + 1);
			if (libre(cx, cz, w, h)) {
				seguras++;
				// las puertas valen 1, 2 o 3 monedas (las dos de la misma sala, lo mismo)
				HABITACIONES.add(new Habitacion("segura", "segura_" + seguras, cx, cz, w, h, 1 + azar.nextInt(3)));
			}
		}
	}

	/** Si cabe una habitacion en esas celdas: con una celda de pasillo alrededor y lejos de la salida y los parches. */
	private static boolean libre(int cx, int cz, int w, int h) {
		for (Habitacion o : HABITACIONES) {
			if (cx - 1 < o.cx() + o.w() && o.cx() < cx + w + 1 && cz - 1 < o.cz() + o.h() && o.cz() < cz + h + 1) {
				return false;
			}
		}
		for (int[] p : PARCHES) {
			if (p[0] >= cx - 1 && p[0] <= cx + w && p[1] >= cz - 1 && p[1] <= cz + h) {
				return false;
			}
		}
		return !(cx + w >= LAB_W - 2 && Math.abs(cz - SALIDA_CZ) <= 2);
	}

	/* ----------------------------------------------------------- laberintos */

	@FunctionalInterface
	private interface Prohibido {
		boolean paso(int cx, int cz, int dx, int dz);
	}

	/**
	 * Un laberinto por recorrido en profundidad en una rejilla de w x h desde (sx, sz): quita
	 * paredes de `este` (entre x y x+1) y `sur` (entre z y z+1). No entra en las `bloqueadas` ni
	 * cruza los pasos `prohibido`. Devuelve las celdas a las que llega.
	 */
	private static boolean[][] recorrido(int w, int h, boolean[][] bloqueada, boolean[][] este, boolean[][] sur, int sx, int sz, Random azar,
		Prohibido prohibido) {
		boolean[][] visto = new boolean[w][h];
		List<int[]> pila = new ArrayList<>();
		pila.add(new int[] {sx, sz});
		visto[sx][sz] = true;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!pila.isEmpty()) {
			int[] c = pila.get(pila.size() - 1);
			List<int[]> libres = new ArrayList<>();
			for (int[] d : dirs) {
				int nx = c[0] + d[0];
				int nz = c[1] + d[1];
				if (nx >= 0 && nz >= 0 && nx < w && nz < h && !visto[nx][nz] && !bloqueada[nx][nz]
					&& (prohibido == null || !prohibido.paso(c[0], c[1], d[0], d[1]))) {
					libres.add(d);
				}
			}
			if (libres.isEmpty()) {
				pila.remove(pila.size() - 1);
				continue;
			}
			int[] d = libres.get(azar.nextInt(libres.size()));
			int nx = c[0] + d[0];
			int nz = c[1] + d[1];
			abrir(este, sur, c[0], c[1], d[0], d[1]);
			visto[nx][nz] = true;
			pila.add(new int[] {nx, nz});
		}
		return visto;
	}

	private static void abrir(boolean[][] este, boolean[][] sur, int cx, int cz, int dx, int dz) {
		if (dx == 1) {
			este[cx][cz] = false;
		} else if (dx == -1) {
			este[cx - 1][cz] = false;
		} else if (dz == 1) {
			sur[cx][cz] = false;
		} else {
			sur[cx][cz - 1] = false;
		}
	}

	private static boolean abierto(boolean[][] este, boolean[][] sur, int cx, int cz, int dx, int dz) {
		if (dx == 1) {
			return !este[cx][cz];
		} else if (dx == -1) {
			return !este[cx - 1][cz];
		} else if (dz == 1) {
			return !sur[cx][cz];
		}
		return !sur[cx][cz - 1];
	}

	private static boolean[][] lleno(int w, int h) {
		boolean[][] m = new boolean[w][h];
		for (boolean[] fila : m) {
			java.util.Arrays.fill(fila, true);
		}
		return m;
	}

	private static Direction dir(int dx, int dz) {
		return dx == 1 ? Direction.EAST : dx == -1 ? Direction.WEST : dz == 1 ? Direction.SOUTH : Direction.NORTH;
	}

	/**
	 * El laberinto: pasillos de 3 con muros blancos y zocalo granate, columnas de colores en las
	 * esquinas, damero en el suelo y el techo oscuro con sus luces. Sale de un recorrido en
	 * profundidad con semilla fija (siempre el mismo) y algunos atajos para que no sea un arbol;
	 * las habitaciones quedan fuera del recorrido y se abren con una puerta a un pasillo.
	 */
	private static void laberinto() {
		int ancho = LAB_W * CELDA;
		int alto = LAB_H * CELDA;
		SALAS.add(new Sala("laberinto", LAB_X + 1, LAB_Z + 1, LAB_X + ancho - 1, LAB_Z + alto - 1));
		int[][] de = new int[LAB_W][LAB_H];
		boolean[][] reservada = new boolean[LAB_W][LAB_H];
		for (int[] fila : de) {
			java.util.Arrays.fill(fila, -1);
		}
		for (int i = 0; i < HABITACIONES.size(); i++) {
			Habitacion h = HABITACIONES.get(i);
			for (int cx = h.cx(); cx < h.cx() + h.w(); cx++) {
				for (int cz = h.cz(); cz < h.cz() + h.h(); cz++) {
					de[cx][cz] = i;
					reservada[cx][cz] = true;
				}
			}
		}
		boolean[][] este = lleno(LAB_W, LAB_H);
		boolean[][] sur = lleno(LAB_W, LAB_H);
		Random azar = new Random(1337);
		boolean[][] llega = recorrido(LAB_W, LAB_H, reservada, este, sur, LAB_W - 1, SALIDA_CZ, azar, null);
		// atajos: quita algunas paredes entre pasillos para que haya vueltas
		for (int i = 0; i < 70; i++) {
			int cx = azar.nextInt(LAB_W - 1);
			int cz = azar.nextInt(LAB_H - 1);
			if (azar.nextBoolean()) {
				if (!reservada[cx][cz] && !reservada[cx + 1][cz] && llega[cx][cz] && llega[cx + 1][cz]) {
					este[cx][cz] = false;
				}
			} else if (!reservada[cx][cz] && !reservada[cx][cz + 1] && llega[cx][cz] && llega[cx][cz + 1]) {
				sur[cx][cz] = false;
			}
		}
		// dentro de las habitaciones no hay paredes; su puerta, a un pasillo al que llega el recorrido
		for (int i = 0; i < HABITACIONES.size(); i++) {
			Habitacion h = HABITACIONES.get(i);
			for (int cx = h.cx(); cx < h.cx() + h.w(); cx++) {
				for (int cz = h.cz(); cz < h.cz() + h.h(); cz++) {
					if (cx < h.cx() + h.w() - 1) {
						este[cx][cz] = false;
					}
					if (cz < h.cz() + h.h() - 1) {
						sur[cx][cz] = false;
					}
				}
			}
			List<int[]> opciones = new ArrayList<>();
			for (int cx = h.cx(); cx < h.cx() + h.w(); cx++) {
				for (int cz = h.cz(); cz < h.cz() + h.h(); cz++) {
					for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
						int nx = cx + d[0];
						int nz = cz + d[1];
						if (nx >= 0 && nz >= 0 && nx < LAB_W && nz < LAB_H && de[nx][nz] < 0 && llega[nx][nz]) {
							opciones.add(new int[] {cx, cz, d[0], d[1]});
						}
					}
				}
			}
			if (opciones.isEmpty()) {
				ENTRADAS.add(new int[] {h.x0(), h.z0()});
				TODAS.add(new ArrayList<>());
				continue;
			}
			int[] o = opciones.get(new Random(100 + i).nextInt(opciones.size()));
			List<int[]> puertas = new ArrayList<>();
			puertas.add(o);
			if (h.tipo().equals("segura")) {
				// la segunda, en otra pared y lo mas lejos de la primera: se entra por un pasillo y se sale por otro
				int[] lejos = null;
				double dl = -1;
				for (int[] p : opciones) {
					if (p[2] == o[2] && p[3] == o[3]) {
						continue;
					}
					double d = Math.hypot(p[0] + p[2] * 0.5 - o[0] - o[2] * 0.5, p[1] + p[3] * 0.5 - o[1] - o[3] * 0.5);
					if (d > dl) {
						dl = d;
						lejos = p;
					}
				}
				if (lejos != null) {
					puertas.add(lejos);
				}
			}
			List<int[]> entradas = new ArrayList<>();
			for (int[] q : puertas) {
				Direction m = dir(q[2], q[3]);
				// la puerta en el muro, en medio del tramo de la celda
				int px = LAB_X + q[0] * CELDA + 2 + q[2] * 2;
				int pz = LAB_Z + q[1] * CELDA + 2 + q[3] * 2;
				PENDIENTES.add(new Object[] {px, pz, m, h.coste(), 0, h.tipo().equals("inicio") ? "la salida" : nombrePuerta(h)});
				entradas.add(new int[] {px - q[2], pz - q[3]});
				FRENTES.add((long) (q[0] + q[2]) << 32 | (q[1] + q[3]));
			}
			ENTRADAS.add(entradas.get(0));
			TODAS.add(entradas);
			if (h.tipo().equals("inicio")) {
				INICIOS.add(new double[] {LAB_X + h.cx() * CELDA + 2.5, LAB_Z + h.cz() * CELDA + 2.5, dir(o[2], o[3]).toYRot()});
			}
		}
		final boolean[][] fe = este;
		final boolean[][] fs = sur;
		final int[][] fd = de;
		// suelo a cuadros, muros (y columnas en las esquinas) y techo con luces
		caja(LAB_X, SUELO, LAB_Z, LAB_X + ancho, SUELO, LAB_Z + alto, (x, y, z) -> s(BloquesFiesta.DAMEROS[damero(x, z)]));
		caja(LAB_X, SUELO + 1, LAB_Z, LAB_X + ancho, TECHO - 1, LAB_Z + alto, (x, y, z) -> {
			int lx = x - LAB_X;
			int lz = z - LAB_Z;
			boolean px = lx % CELDA == 0;
			boolean pz = lz % CELDA == 0;
			int cx = lx / CELDA;
			int cz = lz / CELDA;
			boolean muro;
			if (lx == 0 || lz == 0 || lx == ancho || lz == alto) {
				muro = true;
			} else if (px && pz) {
				int h = fd[cx - 1][cz - 1];
				muro = h < 0 || fd[cx][cz - 1] != h || fd[cx - 1][cz] != h || fd[cx][cz] != h;
			} else if (px) {
				muro = fe[cx - 1][cz];
			} else if (pz) {
				muro = fs[cx][cz - 1];
			} else {
				muro = false;
			}
			if (!muro) {
				return Blocks.AIR.defaultBlockState();
			}
			if (px && pz) {
				return s(BloquesFiesta.COLUMNAS[azar(lx, lz, 61, BloquesFiesta.COLUMNAS.length)]);
			}
			return y == SUELO + 1 ? s(BloquesFiesta.ZOCALO_LAB) : s(BloquesFiesta.PARED_LAB);
		});
		caja(LAB_X, TECHO, LAB_Z, LAB_X + ancho, TECHO, LAB_Z + alto, (x, y, z) -> {
			int lx = x - LAB_X;
			int lz = z - LAB_Z;
			int cx = lx / CELDA;
			int cz = lz / CELDA;
			if (lx % CELDA == 2 && lz % CELDA == 2 && (cx + cz) % 2 == 0) {
				return azar(cx, cz, 53, 7) == 0 ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.LUZ);
			}
			return s(BloquesFiesta.TECHO_LAB);
		});
		pasillos(este, sur, de, llega);
		for (int i = 0; i < HABITACIONES.size(); i++) {
			amueblar(HABITACIONES.get(i), i);
		}
	}

	private static String nombrePuerta(Habitacion h) {
		return switch (h.tipo()) {
			case "segura" -> "la sala segura";
			case "memoria" -> "la memoria de colores";
			case "regalos" -> "la sala de los regalos";
			case "cocina" -> "la cocina";
			case "siesta" -> "la sala de la siesta";
			case "secreta" -> "la sala secreta";
			default -> "la sala de las mesas";
		};
	}

	/**
	 * Lo de los pasillos: mesas de fiesta con monedas o comida (contra una pared), banderines, altavoces
	 * y la ronda del Anfitrion.
	 */
	private static void pasillos(boolean[][] este, boolean[][] sur, int[][] de, boolean[][] llega) {
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		int mesas = 0;
		for (int cx = 0; cx < LAB_W; cx++) {
			for (int cz = 0; cz < LAB_H; cz++) {
				if (de[cx][cz] >= 0 || !llega[cx][cz]) {
					continue;
				}
				int x0 = LAB_X + cx * CELDA;
				int z0 = LAB_Z + cz * CELDA;
				boolean parche = false;
				for (int[] p : PARCHES) {
					parche |= p[0] == cx && p[1] == cz;
				}
				boolean salida = cx == LAB_W - 1 && cz == SALIDA_CZ;
				if (azar(cx, cz, 93, 4) == 1) {
					Direction eje = azar(cx, cz, 94, 2) == 0 ? Direction.NORTH : Direction.EAST;
					for (int k = 1; k <= 3; k++) {
						bloque(eje == Direction.NORTH ? x0 + k : x0 + 2, TECHO - 1, eje == Direction.NORTH ? z0 + 2 : z0 + k,
							variante(BloquesFiesta.BANDERINES, eje, azar(cx, cz + k, 95, 4)));
					}
				}
				if (azar(cx, cz, 71, 3) == 0) {
					RONDA.add(new int[] {x0 + 2, z0 + 2});
				}
				// la mesa, contra una pared que no sea de paso (si la celda tiene alguna)
				if (!parche && !salida && !FRENTES.contains((long) cx << 32 | cz) && azar(cx, cz, 91, 5) == 0) {
					int[] pared = null;
					if (cx == 0 || este[cx - 1][cz]) {
						pared = new int[] {x0 + 1, z0 + 1, 0, 1};
					} else if (cx == LAB_W - 1 || este[cx][cz]) {
						pared = new int[] {x0 + 3, z0 + 1, 0, 1};
					} else if (cz == 0 || sur[cx][cz - 1]) {
						pared = new int[] {x0 + 1, z0 + 1, 1, 0};
					} else if (cz == LAB_H - 1 || sur[cx][cz]) {
						pared = new int[] {x0 + 1, z0 + 3, 1, 0};
					}
					if (pared != null) {
						mesa(pared[0], pared[1], dir(pared[2], pared[3]), azar(cx, cz, 92, 4));
						// en la primera mitad de la mesa la moneda (o la comida); en la segunda, la tarta o la moneda
						int que = mesas++ % 6;
						boolean conComida = que >= 4;
						if (conComida) {
							COMIDA.add(new Plato(pared[0], MESA_Y, pared[1], que == 4 ? Plano.C_GALLETAS : Plano.C_PIZZA));
							moneda(pared[0] + pared[2] + 0.5, MESA_Y, pared[1] + pared[3] + 0.5);
						} else {
							moneda(pared[0] + 0.5, MESA_Y, pared[1] + 0.5);
						}
						if (!conComida && azar(cx, cz, 96, 3) == 0) {
							bloque(pared[0] + pared[2], SUELO + 2, pared[1] + pared[3],
								variante(BloquesFiesta.TARTA, Direction.from2DDataValue(azar(cx, cz, 98, 4)), azar(cx, cz, 99, 4)));
						}
					}
				}
				if (azar(cx, cz, 101, 19) == 0 && (cx == 0 || este[cx - 1][cz])) {
					bloque(x0 + 1, TECHO - 2, z0 + 2, variante(BloquesFiesta.ALTAVOZ, Direction.EAST, 0));
					ALTAVOCES.add(new int[] {x0 + 1, TECHO - 2, z0 + 2});
				}
			}
		}
	}

	/** Lo de dentro de cada habitacion (despues de los muros, que lo pisarian). */
	private static void amueblar(Habitacion h, int i) {
		int x0 = h.x0();
		int z0 = h.z0();
		int x1 = h.x1();
		int z1 = h.z1();
		int[] e = ENTRADAS.get(i);
		SALAS.add(new Sala(h.nombre(), x0, z0, x1, z1));
		switch (h.tipo()) {
			case "inicio" -> {
				// el cuartito: una luz, algo de confeti y, a veces, un dibujo
				bloque(x0 + 1, TECHO, z0 + 1, s(BloquesFiesta.LUZ));
				if (!(e[0] == x0 + 1 && e[1] == z0 + 1)) {
					bloque(x0 + 1, SUELO + 1, z0 + 1, variante(BloquesFiesta.CONFETI, Direction.from2DDataValue(i % 4), i % 4));
				}
				if (h.nombre().equals("inicio_1")) {
					nota(x0 + (e[0] == x0 ? 2 : 0), z0 + (e[1] == z0 ? 2 : 0), 6);
				}
			}
			case "segura" -> {
				// la sala segura: suelo de goma, el localizador de salida en una pared sin puerta y, en otra, una
				// mesa con una moneda y algo de comer
				caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesFiesta.SUELO_GOMA));
				List<int[]> es = TODAS.get(i);
				int cx = (x0 + x1) / 2;
				int cz = (z0 + z1) / 2;
				// las cuatro paredes: el sitio del centro (junto a la pared) y hacia donde mira lo que se cuelga
				int[][] paredes = {{cx, z0, 0}, {cx, z1, 2}, {x0, cz, 3}, {x1, cz, 1}};
				int loc = -1;
				for (int k = 0; k < 4 && loc < 0; k++) {
					if (!junto(es, paredes[k][0], paredes[k][1], 1)) {
						loc = k;
					}
				}
				if (loc >= 0) {
					bloque(paredes[loc][0], SUELO + 2, paredes[loc][1], mira(BloquesFiesta.LOCALIZADOR, Direction.from2DDataValue(paredes[loc][2])));
				}
				for (int k = 0; k < 4; k++) {
					if (k == loc) {
						continue;
					}
					boolean nz = k < 2;
					int ax = nz ? paredes[k][0] - 1 : paredes[k][0];
					int az = nz ? paredes[k][1] : paredes[k][1] - 1;
					int bx = nz ? ax + 1 : ax;
					int bz = nz ? az : az + 1;
					if (junto(es, ax, az, 1) || junto(es, bx, bz, 1)) {
						continue;
					}
					mesa(ax, az, nz ? Direction.EAST : Direction.SOUTH, i % 4);
					moneda(ax + 0.5, MESA_Y, az + 0.5);
					COMIDA.add(new Plato(bx, MESA_Y, bz, i % 2 == 0 ? Plano.C_PIZZA : Plano.C_GALLETAS));
					break;
				}
				bloque(cx, TECHO, cz, s(BloquesFiesta.LUZ));
				if (x1 - x0 >= 6 && !junto(es, x0, z1, 1)) {
					bloque(x0, SUELO + 1, z1, variante(BloquesFiesta.PELUCHE, Direction.EAST, i % 4));
				}
			}
			case "memoria" -> {
				// las baldosas de colores y el boton, sobre una alfombra oscura para que se distingan
				caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesFiesta.MOQUETA_CONFETI));
				int[][] b = {{x0 + 2, z0 + 2}, {x0 + 6, z0 + 2}, {x0 + 2, z0 + 6}, {x0 + 6, z0 + 6}};
				for (int c = 0; c < 4; c++) {
					MEMORIA[c][0] = b[c][0];
					MEMORIA[c][1] = b[c][1];
					caja(b[c][0], SUELO, b[c][1], b[c][0] + 1, SUELO, b[c][1] + 1, BloquesFiesta.BALDOSA_JUEGO.defaultBlockState().setValue(BaldosaJuego.COLOR, c));
				}
				MEMORIA_BOTON[0] = x0 + 4;
				MEMORIA_BOTON[1] = z0 + 9;
				caja(x0 + 4, SUELO, z0 + 9, x0 + 5, SUELO, z0 + 9, BloquesFiesta.BALDOSA_JUEGO.defaultBlockState().setValue(BaldosaJuego.COLOR, 4));
				bloque(x0 + 5, SUELO + 4, z0, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 3));
				bloque(x0, SUELO + 3, z0 + 5, variante(BloquesFiesta.DIBUJO, Direction.EAST, 3));
				racimo(x0 + 9, z0 + 9, SUELO + 1, 2);
				RONDA.add(new int[] {x0 + 5, z0 + 5});
			}
			case "regalos" -> {
				// casi a oscuras, llena de estanterias y montones de regalos: cada uno juega con los suyos
				// (JuegoRegalos sortea cuales de estos son de verdad para cada jugador)
				caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesFiesta.SUELO_GOMA_SUCIO));
				caja(x0, TECHO, z0, x1, TECHO, z1, (x, y, z) -> ((x - x0) % 7 == 3 && (z - z0) % 5 == 2) ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.TECHO_LAB));
				REGALOS_SALA[0] = x0;
				REGALOS_SALA[1] = z0;
				REGALOS_SALA[2] = x1;
				REGALOS_SALA[3] = z1;
				int bx = (x0 + x1) / 2;
				int bz = (z0 + z1) / 2;
				REGALOS_BOTON[0] = bx;
				REGALOS_BOTON[1] = bz;
				bloque(bx, SUELO, bz, BloquesFiesta.BALDOSA_JUEGO.defaultBlockState().setValue(BaldosaJuego.COLOR, 4));
				// estanterias (encimeras de madera) en dos filas, con regalos encima y entre medias
				int k = 0;
				for (int z : new int[] {z0 + 3, z1 - 3}) {
					for (int x = x0 + 2; x <= x1 - 2; x++) {
						if (Math.abs(x - bx) <= 1) {
							continue;
						}
						bloque(x, SUELO + 1, z, s(Blocks.STRIPPED_SPRUCE_LOG));
						if (azar(x, z, 141, 2) == 0) {
							regalo(x, SUELO + 2, z, k++);
						} else if (azar(x, z, 142, 3) == 0) {
							bloque(x, SUELO + 2, z, variante(BloquesFiesta.PELUCHE, Direction.from2DDataValue(azar(x, z, 143, 4)), azar(x, z, 144, 4)));
						}
					}
				}
				// regalos sueltos en las esquinas y contra las paredes, de dos en dos (nunca uno encima de otro)
				int[][] montones = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {bx, z0}, {bx, z1}, {x0, bz}, {x1, bz}, {x0 + 4, z0 + 1}, {x1 - 4, z1 - 1},
					{x0 + 1, z1 - 1}, {x1 - 1, z0 + 1}, {bx - 3, bz}, {bx + 3, bz}};
				for (int[] m : montones) {
					if (Math.abs(m[0] - e[0]) + Math.abs(m[1] - e[1]) <= 1) {
						continue;
					}
					regalo(m[0], SUELO + 1, m[1], k++);
					int sx = m[0] + (m[0] < bx ? 1 : -1);
					if (azar(m[0], m[1], 145, 2) == 0 && Math.abs(sx - e[0]) + Math.abs(m[1] - e[1]) > 1) {
						regalo(sx, SUELO + 1, m[1], k++);
					}
				}
				bloque(bx, SUELO + 3, z0 - 0, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 2));
				RONDA.add(new int[] {bx, bz + 2});
			}
			case "cocina" -> {
				// baldosa blanca y negra, encimeras de acero, ollas enormes y bandejas con nombres
				caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesFiesta.BALDOSA_COCINA));
				boolean norte = e[1] != z0;
				int ez = norte ? z0 : z1;
				Direction cara = norte ? Direction.SOUTH : Direction.NORTH;
				caja(x0, SUELO + 1, ez, x1, SUELO + 1, ez, s(Blocks.IRON_BLOCK));
				caja(x0 + 3, SUELO + 1, z0 + 3, x1 - 3, SUELO + 1, z0 + 3, s(Blocks.IRON_BLOCK));
				for (int x = x0 + 3; x <= x1 - 3; x += 2) {
					bloque(x, SUELO + 2, z0 + 3, variante(BloquesFiesta.BANDEJA, Direction.SOUTH, Math.floorMod(x, 4)));
				}
				for (int x = x0 + 1; x <= x1 - 1; x += 3) {
					bloque(x, SUELO + 2, ez, variante(BloquesFiesta.OLLA, cara, Math.floorMod(x, 4)));
				}
				COMIDA.add(new Plato(x0, SUELO + 2.0, ez, Plano.C_PIZZA));
				COMIDA.add(new Plato(x1, SUELO + 2.0, ez, Plano.C_GALLETAS));
				moneda(x0 + 5.5, SUELO + 2.02, z0 + 3.5);
				moneda(x1 + 0.5, SUELO + 1.02, (norte ? z1 : z0) + 0.5);
				bloque(x0 + 5, SUELO + 3, (norte ? z0 : z1), variante(BloquesFiesta.CARTEL, cara, 6));
				nota(x0 + 1, norte ? z1 : z0, 0);
				RONDA.add(new int[] {x0 + 5, z0 + 5});
			}
			case "siesta" -> {
				// camitas en fila, luz baja y peluches
				caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesFiesta.SUELO_GOMA));
				caja(x0, TECHO, z0, x1, TECHO, z1, (x, y, z) -> (z == z0 + 3 && (x == x0 + 2 || x == x1 - 2)) ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.TECHO));
				for (int x = x0 + 1; x <= x1 - 1; x += 2) {
					for (int z : new int[] {z0 + 1, z1 - 1}) {
						if (Math.abs(x - e[0]) + Math.abs(z - e[1]) <= 1) {
							continue;
						}
						bloque(x, SUELO + 1, z, variante(BloquesFiesta.CAMITA, Direction.NORTH, Math.floorMod(x + z, 4)));
						int pz = z == z0 + 1 ? z + 1 : z - 1;
						if (azar(x, z, 81, 3) == 0 && Math.abs(x - e[0]) + Math.abs(pz - e[1]) > 1 && pz != z0 + 3) {
							bloque(x, SUELO + 1, pz, variante(BloquesFiesta.PELUCHE, Direction.SOUTH, Math.floorMod(x, 4)));
						}
					}
				}
				moneda(x0 + 5.5, SUELO + 1.02, z0 + 3.5);
				nota(x1 - 1, z0 + 3, 5);
				bloque(x0, SUELO + 3, z0 + 3, variante(BloquesFiesta.DIBUJO, Direction.EAST, 4));
				RONDA.add(new int[] {x0 + 5, z0 + 3});
			}
			case "secreta" -> {
				// confeti, regalos, monedas, comida y una nota
				caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesFiesta.MOQUETA_CONFETI));
				moneda(x0 + 1.5, SUELO + 1.02, z0 + 1.5);
				moneda(x1 - 0.5, SUELO + 1.02, z0 + 1.5);
				moneda(x0 + 1.5, SUELO + 1.02, z1 - 0.5);
				COMIDA.add(new Plato(x1 - 1, SUELO + 1.0, z1 - 1, Plano.C_PIZZA));
				bloque(x0 + 3, SUELO + 1, z0 + 3, variante(BloquesFiesta.REGALO, Direction.SOUTH, 3));
				bloque(x0 + 3, SUELO + 2, z0, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, 6));
				bloque(x0, SUELO + 2, z0 + 3, variante(BloquesFiesta.DIBUJO, Direction.EAST, 7));
				nota(x0 + 4, z0 + 4, 4);
			}
			default -> {
				// la sala de las mesas: dos mesas largas con tartas y sillitas, monedas y tickets
				caja(x0, SUELO, z0, x1, SUELO, z1, s(BloquesFiesta.MOQUETA_CONFETI));
				for (int z : new int[] {z0 + 2, z0 + 4}) {
					for (int x = x0 + 2; x <= x1 - 2; x++) {
						bloque(x, SUELO + 1, z, variante(BloquesFiesta.MESA, Direction.NORTH, Math.floorMod(z, 4)));
						if (x % 3 == 0) {
							bloque(x, SUELO + 2, z, variante(BloquesFiesta.TARTA, Direction.from2DDataValue(Math.floorMod(x, 4)), Math.floorMod(x + z, 4)));
						}
					}
				}
				moneda(x0 + 3.5, MESA_Y, z0 + 2.5);
				moneda(x1 - 2.5, MESA_Y, z0 + 4.5);
				moneda(x0 + 0.5, SUELO + 1.02, z1 - 0.5);
				COMIDA.add(new Plato(x0 + 6, MESA_Y, z0 + 4, Plano.C_GALLETAS));
				racimo(x1 - 1, z1 - 1, SUELO + 1, 1);
				bloque(x0 + 5, SUELO + 3, z0, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 0));
				nota(x0, z0 + 6, 2);
				RONDA.add(new int[] {x0 + 5, z0 + 3});
			}
		}
	}

	/* ------------------------------------------------------------ parkour */

	/**
	 * Las tres zonas de parkour, debajo del laberinto (lo primero: el suelo del laberinto, que va
	 * despues, es su techo). Paredes de papel despegado, una piscina de bolas en el fondo y
	 * plataformas acolchadas hasta la trampilla, que sube directa al castillo.
	 */
	private static void parkours() {
		for (int i = 0; i < PARCHES.length; i++) {
			int px = LAB_X + PARCHES[i][0] * CELDA + 1;
			int pz = LAB_Z + PARCHES[i][1] * CELDA + 1;
			parkour("parkour_" + (i + 1), px - 2, pz - 3, px + 25, pz + 4, px, pz, px + 21, pz, 100, 11 + i);
		}
	}

	/** Una zona de parkour de x0..x1, z0..z1 (interior) con el parche en (px, pz) (2x2), hacia el este, y la trampilla en (fx, fz). */
	private static void parkour(String nombre, int x0, int z0, int x1, int z1, int px, int pz, int fx, int fz, int premio, long semilla) {
		SALAS.add(new Sala(nombre, x0, z0, x1, z1));
		caja(x0 - 1, SUB, z0 - 1, x1 + 1, SUB, z1 + 1, s(Blocks.GRAY_CONCRETE));
		Patron pared = (x, y, z) -> y == SUB + 1 ? s(BloquesFiesta.ZOCALO) : s(BloquesFiesta.PAPEL_SUCIO);
		caja(x0 - 1, SUB + 1, z0 - 1, x1 + 1, SUELO - 1, z0 - 1, pared);
		caja(x0 - 1, SUB + 1, z1 + 1, x1 + 1, SUELO - 1, z1 + 1, pared);
		caja(x0 - 1, SUB + 1, z0, x0 - 1, SUELO - 1, z1, pared);
		caja(x1 + 1, SUB + 1, z0, x1 + 1, SUELO - 1, z1, pared);
		caja(x0, SUB + 1, z0, x1, SUB + 1, z1, s(BloquesFiesta.PISCINA_BOLAS));
		for (int x = x0 + 3; x <= x1 - 3; x += 7) {
			bloque(x, SUELO - 1, (z0 + z1) / 2, s(BloquesFiesta.LUZ_PARPADEO));
		}
		// las plataformas: de la llegada a la trampilla, a saltos de 2-3, subiendo poco a poco
		Random azar = new Random(semilla);
		int x = px + 2;
		int z = pz;
		int y = SUB + 1;
		int fin = fx - 2;
		while (x < fin) {
			bloque(x, y, z, s(BloquesFiesta.ACOLCHADOS[Math.floorMod(x + z, BloquesFiesta.ACOLCHADOS.length)]));
			if (azar.nextInt(4) == 0) {
				bloque(x, y, z + 1, s(BloquesFiesta.ACOLCHADOS[Math.floorMod(x + z + 1, BloquesFiesta.ACOLCHADOS.length)]));
			}
			// como mucho SUB + 3: mas alto, al saltar la cabeza da contra el techo (el suelo de arriba)
			int altura = Math.max(SUB + 1, Math.min(SUB + 3, y + (azar.nextInt(3) - (y >= SUB + 3 ? 1 : 0))));
			int salto = altura > y ? 2 : 2 + azar.nextInt(2);
			x += salto;
			z = Math.max(z0 + 1, Math.min(z1 - 1, z + azar.nextInt(3) - 1));
			y = altura;
		}
		caja(fx - 1, SUB + 1, fz - 1, fx + 2, SUB + 3, fz + 2, s(BloquesFiesta.ACOLCHADO_AMARILLO));
		caja(fx, SUB + 3, fz, fx + 1, SUB + 3, fz + 1, s(BloquesFiesta.TRAMPILLA));
		PARKOURS.add(new Parkour(PARKOURS.size(), nombre, x0, z0, x1, z1, px + 1.0, pz + 1.0, fx, fz, fx + 1, fz + 1,
			CASTILLO_LLEGADA_X, CASTILLO_LLEGADA_Z, -90.0F, premio));
	}

	/* ------------------------------------------------------------ el castillo */

	/** El pasillo de la puerta roja al castillo. */
	private static void pasilloCastillo() {
		sala("pasillo_castillo", SALIDA_X + 1, SALIDA_Z - 1, CAS_X - 2, SALIDA_Z + 1, TECHO, BloquesFiesta.DAMEROS[0], BloquesFiesta.ZOCALO_LAB,
			BloquesFiesta.PARED_LAB, 1, 4);
		caja(SALIDA_X + 1, SUELO, SALIDA_Z - 1, CAS_X - 2, SUELO, SALIDA_Z + 1, (x, y, z) -> s(BloquesFiesta.DAMEROS[damero(x, z)]));
	}

	/**
	 * El castillo de bolas: en una nave oscura, una estructura de dos pisos en celdas de CELDA con
	 * postes amarillos acolchados en las esquinas, vigas amarillas, suelo y tejado de lona azul y
	 * redes negras donde hay pared. Cada piso es su propio laberinto; tres escaleras de arcoiris
	 * suben del de abajo al de arriba (en el hueco de la escalera, arriba no hay suelo). Se entra
	 * abajo por el oeste y se sale arriba por el este, al tobogan. Semaforos en las redes.
	 */
	private static void castillo() {
		int x0 = CAS_X;
		int z0 = CAS_Z;
		int n = CAS_N;
		int lado = CAS_LADO;
		SALAS.add(new Sala("castillo", x0, z0, x0 + lado, z0 + lado));
		// la nave: moqueta, paredes oscuras y un techo alto con alguna luz que falla
		caja(x0 - 1, SUELO, z0 - 1, x0 + lado + 1, SUELO, z0 + lado + 1, s(BloquesFiesta.MOQUETA_CONFETI));
		caja(x0 - 1, SUELO + 1, z0 - 1, x0 + lado + 1, TECHO_CASTILLO - 1, z0 + lado + 1, (x, y, z) -> {
			boolean borde = x == x0 - 1 || x == x0 + lado + 1 || z == z0 - 1 || z == z0 + lado + 1;
			return borde ? s(BloquesFiesta.TECHO_LAB) : Blocks.AIR.defaultBlockState();
		});
		caja(x0 - 1, TECHO_CASTILLO, z0 - 1, x0 + lado + 1, TECHO_CASTILLO, z0 + lado + 1,
			(x, y, z) -> (Math.floorMod(x - x0, 12) == 6 && Math.floorMod(z - z0, 12) == 6) ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.TECHO_LAB));
		// las escaleras: celda de abajo (sube hacia el este) y, arriba, la celda a la que llega
		int[][] escaleras = {{3, 2}, {6, 9}, {8, 4}, {2, 9}};
		boolean[][] hueco = new boolean[n][n];
		for (int[] e : escaleras) {
			hueco[e[0]][e[1]] = true;
		}
		Random azar = new Random(4711);
		boolean[][] este0 = lleno(n, n);
		boolean[][] sur0 = lleno(n, n);
		// abajo no se pasa por el lado de los escalones altos (el este de la escalera)
		recorrido(n, n, new boolean[n][n], este0, sur0, 0, CAS_FILA, azar,
			(cx, cz, dx, dz) -> (dx == 1 && hueco[cx][cz]) || (dx == -1 && cx > 0 && hueco[cx - 1][cz]));
		boolean[][] este1 = lleno(n, n);
		boolean[][] sur1 = lleno(n, n);
		recorrido(n, n, hueco, este1, sur1, n - 1, CAS_FILA, azar, null);
		for (int[] e : escaleras) {
			este1[e[0]][e[1]] = false;
		}
		for (int i = 0; i < 10; i++) {
			int cx = azar.nextInt(n - 1);
			int cz = azar.nextInt(n - 1);
			if (!hueco[cx][cz] && !hueco[cx + 1][cz] && !hueco[cx][cz + 1]) {
				if (azar.nextBoolean()) {
					este1[cx][cz] = false;
				} else {
					sur1[cx][cz] = false;
				}
			}
		}
		final int p1 = CAS_P1;
		final int tejado = CAS_TEJADO;
		caja(x0, SUELO + 1, z0, x0 + lado, tejado, z0 + lado, (x, y, z) -> {
			int lx = x - x0;
			int lz = z - z0;
			boolean px = lx % CELDA == 0;
			boolean pz = lz % CELDA == 0;
			int cx = lx / CELDA;
			int cz = lz / CELDA;
			boolean nivel = y == p1 || y == tejado;
			if (px && pz) {
				return nivel ? s(BloquesFiesta.ACOLCHADO_AMARILLO) : mira(BloquesFiesta.POSTE, Direction.NORTH);
			}
			if (px || pz) {
				if (nivel) {
					return s(BloquesFiesta.ACOLCHADO_AMARILLO);
				}
				boolean arriba = y > p1;
				boolean muro;
				if (px) {
					if (cx == 0) {
						muro = arriba || cz != CAS_FILA;
					} else if (cx == n) {
						muro = !arriba || cz != CAS_FILA;
					} else {
						muro = arriba ? este1[cx - 1][cz] : este0[cx - 1][cz];
					}
				} else if (cz == 0 || cz == n) {
					muro = true;
				} else {
					muro = arriba ? sur1[cx][cz - 1] : sur0[cx][cz - 1];
				}
				if (!muro) {
					return Blocks.AIR.defaultBlockState();
				}
				// el tramo entre dos postes son 3 redes: la primera y la ultima llegan hasta el poste
				int t = px ? lz % CELDA : lx % CELDA;
				return variante(BloquesFiesta.RED, px ? Direction.EAST : Direction.NORTH, t == 1 ? 1 : t == CELDA - 1 ? 2 : 0);
			}
			int ix = lx % CELDA;
			if (y == p1) {
				if (hueco[cx][cz]) {
					return Blocks.AIR.defaultBlockState();
				}
				return azar(cx, cz, 131, 9) == 0 ? s(BloquesFiesta.LUZ) : s(BloquesFiesta.ACOLCHADO_AZUL);
			}
			if (y == tejado) {
				return azar(cx, cz, 132, 5) == 0 ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.ACOLCHADO_AZUL);
			}
			// los escalones de arcoiris: el 1 de alto, el 2 de dos, el 3 de tres
			if (y < p1 && hueco[cx][cz] && y <= SUELO + ix) {
				return s(BloquesFiesta.ACOLCHADOS[(ix + y) % BloquesFiesta.ACOLCHADOS.length]);
			}
			return Blocks.AIR.defaultBlockState();
		});
		// los semaforos: en el medio de algunas redes, a la altura de los ojos, mirando a un pasillo
		Random r = new Random(99);
		for (int k = 0; k < 220 && SEMAFOROS.size() < 22; k++) {
			boolean enX = r.nextBoolean();
			int i = 1 + r.nextInt(n - 1);
			int c = r.nextInt(n);
			boolean arriba = r.nextBoolean();
			boolean muro = enX ? (arriba ? este1[i - 1][c] : este0[i - 1][c]) : (arriba ? sur1[c][i - 1] : sur0[c][i - 1]);
			if (!muro || (enX && arriba && (hueco[i - 1][c] || hueco[i][c])) || (!enX && arriba && (hueco[c][i - 1] || hueco[c][i]))) {
				continue;
			}
			int x = enX ? x0 + i * CELDA : x0 + c * CELDA + 2;
			int z = enX ? z0 + c * CELDA + 2 : z0 + i * CELDA;
			int y = arriba ? p1 + 3 : SUELO + 3;
			boolean ya = false;
			for (int[] s : SEMAFOROS) {
				ya |= Math.abs(s[0] - x) + Math.abs(s[2] - z) < 8 && s[1] == y;
			}
			if (ya) {
				continue;
			}
			Direction cara = enX ? (r.nextBoolean() ? Direction.EAST : Direction.WEST) : (r.nextBoolean() ? Direction.SOUTH : Direction.NORTH);
			bloque(x, y, z, mira(BloquesFiesta.SEMAFORO, cara));
			SEMAFOROS.add(new int[] {x, y, z});
		}
		// la boca del tobogan: fuera, por el este, un tubo rojo a la altura del piso de arriba
		caja(TOB_X0 - 1, p1, TOB_Z0 - 1, TOB_X1 + 1, p1 + 4, TOB_Z1 + 1, s(BloquesFiesta.TOBOGAN));
		aire(TOB_X0 - 1, p1 + 1, TOB_Z0, TOB_X1, p1 + 3, TOB_Z1);
		caja(TOB_X1 + 1, p1 + 1, TOB_Z0, TOB_X1 + 1, p1 + 3, TOB_Z1, s(Blocks.BLACK_CONCRETE));
		ALTAVOCES.add(new int[] {x0 + lado / 2, TECHO_CASTILLO - 1, z0 + lado / 2});
	}

	/* ----------------------------------------------------------- sala central */

	/**
	 * La sala central: pared verde agua con la franja de abajo, moqueta de carreteras y piezas de
	 * goma, el tubo del tobogan que sale de la pared oeste, el marcador de tickets encima de la
	 * puerta del final (norte), el bote junto a la nube y los cuadrados de noclip.
	 */
	private static void central() {
		sala("central", CEN_X0, CEN_Z0, CEN_X1, CEN_Z1, TECHO_CENTRAL, BloquesFiesta.MOQUETA_CIUDAD, BloquesFiesta.PARED_CENTRAL_BAJA,
			BloquesFiesta.PARED_CENTRAL, 1, 5);
		// el tubo del tobogan: un aro rojo en la pared oeste con el agujero oscuro
		caja(CEN_X0 - 1, SUELO + 1, -2, CEN_X0 - 1, SUELO + 4, 2, s(BloquesFiesta.TOBOGAN));
		caja(CEN_X0 - 1, SUELO + 1, -1, CEN_X0 - 1, SUELO + 3, 1, s(Blocks.BLACK_CONCRETE));
		// banderines de lado a lado del techo
		for (int z = CEN_Z0 + 3; z <= CEN_Z1 - 3; z += 6) {
			for (int x = CEN_X0; x <= CEN_X1; x++) {
				bloque(x, TECHO_CENTRAL - 1, z, variante(BloquesFiesta.BANDERINES, Direction.NORTH, Math.floorMod(x / 3 + z, 4)));
			}
		}
		// el marcador de tickets encima de la puerta y el bote a la izquierda, con su nube
		for (int k = 0; k < 3; k++) {
			bloque(PUERTA_FINAL_X - 1 + k, SUELO + 4, CEN_Z0, mira(BloquesFiesta.MARCADOR, Direction.SOUTH).setValue(Marcador.PARTE, k).setValue(Marcador.TIPO, 0));
			bloque(70 + k, SUELO + 3, CEN_Z0, mira(BloquesFiesta.MARCADOR, Direction.SOUTH).setValue(Marcador.PARTE, k).setValue(Marcador.TIPO, 1));
			bloque(70 + k, SUELO + 5, CEN_Z0, variante(BloquesFiesta.NUBE, Direction.SOUTH, k));
		}
		// a los lados de la puerta, el escaparate de premios: mostrador con peluches y regalos encima
		for (int x : new int[] {76, 77, 83, 84}) {
			bloque(x, SUELO + 1, CEN_Z0, s(Blocks.STRIPPED_CHERRY_LOG));
			bloque(x, SUELO + 2, CEN_Z0, variante(x % 2 == 0 ? BloquesFiesta.PELUCHE : BloquesFiesta.REGALO, Direction.SOUTH, Math.floorMod(x, 4)));
		}
		racimo(75, CEN_Z0 + 1, SUELO + 1, 0);
		racimo(85, CEN_Z0 + 1, SUELO + 1, 3);
		// los cuadrados de noclip, al sur: un pozo oscuro sin suelo, con su franja de aviso alrededor y
		// un cartel que dice que por ahi se vuelve a empezar
		int[][] noclip = {{70, 7}, {79, 8}, {88, 7}};
		for (int[] c : noclip) {
			NOCLIPS.add(c);
			caja(c[0] - 1, SUELO, c[1] - 1, c[0] + 2, SUELO, c[1] + 2, s(BloquesFiesta.FRANJA));
			caja(c[0] - 1, SUELO - 5, c[1] - 1, c[0] + 2, SUELO - 1, c[1] + 2, s(Blocks.BLACK_CONCRETE));
			aire(c[0], SUELO - 4, c[1], c[0] + 1, SUELO - 1, c[1] + 1);
			caja(c[0], SUELO, c[1], c[0] + 1, SUELO, c[1] + 1, s(BloquesFiesta.NOCLIP));
			bloque(c[0], SUELO + 1, c[1] - 2, mira(BloquesFiesta.CARTEL_NOCLIP, Direction.NORTH));
		}
		// mesas de fiesta con sus sillitas y tartas
		for (int[] m : new int[][] {{71, -6}, {86, -6}, {78, 0}}) {
			mesa(m[0], m[1], Direction.EAST, Math.floorMod(m[0], 4));
			bloque(m[0] + 1, SUELO + 2, m[1], variante(BloquesFiesta.TARTA, Direction.SOUTH, Math.floorMod(m[0] + m[1], 4)));
			bloque(m[0], SUELO + 1, m[1] - 1, variante(BloquesFiesta.SILLITA, Direction.SOUTH, 0));
			bloque(m[0] + 1, SUELO + 1, m[1] - 1, variante(BloquesFiesta.SILLITA, Direction.SOUTH, 1));
			bloque(m[0], SUELO + 1, m[1] + 1, variante(BloquesFiesta.SILLITA, Direction.NORTH, 2));
			bloque(m[0] + 1, SUELO + 1, m[1] + 1, variante(BloquesFiesta.SILLITA, Direction.NORTH, 3));
		}
		// maquinitas de feria contra la pared este
		for (int z = -9; z <= 3; z += 3) {
			Block col = BloquesFiesta.ACOLCHADOS[Math.floorMod(z, BloquesFiesta.ACOLCHADOS.length)];
			caja(CEN_X1 - 1, SUELO + 1, z, CEN_X1, SUELO + 2, z + 1, s(col));
			caja(CEN_X1, SUELO + 3, z, CEN_X1, SUELO + 3, z + 1, s(BloquesFiesta.LUZ));
		}
		// una piscina de bolas pequena con su valla en la esquina noroeste
		caja(CEN_X0, SUELO, CEN_Z0, CEN_X0 + 3, SUELO, CEN_Z0 + 3, s(BloquesFiesta.PISCINA_BOLAS));
		for (int k = 0; k <= 4; k++) {
			if (k <= 3) {
				bloque(CEN_X0 + k, SUELO + 1, CEN_Z0 + 4, mira(BloquesFiesta.VALLA, Direction.NORTH));
			}
			bloque(CEN_X0 + 4, SUELO + 1, CEN_Z0 + k, mira(BloquesFiesta.VALLA, Direction.EAST));
		}
		// piezas de goma por el suelo, ramos de globos, carteles y dibujos
		int[][] goma = {{74, 3}, {83, 3}, {90, -2}};
		for (int[] g : goma) {
			caja(g[0], SUELO, g[1], g[0] + 2, SUELO, g[1] + 1, s(BloquesFiesta.SUELO_GOMA));
		}
		racimo(CEN_X0 + 1, CEN_Z1 - 1, SUELO + 1, 1);
		racimo(CEN_X1 - 2, CEN_Z1 - 1, SUELO + 1, 2);
		racimo(CEN_X0 + 6, CEN_Z0 + 1, SUELO + 1, 3);
		bloque(CEN_X1, SUELO + 4, 7, variante(BloquesFiesta.CARTEL, Direction.WEST, 1));
		bloque(78, SUELO + 3, CEN_Z1, variante(BloquesFiesta.CARTEL, Direction.NORTH, 3));
		bloque(88, SUELO + 3, CEN_Z1, variante(BloquesFiesta.DIBUJO, Direction.NORTH, 5));
		bloque(70, SUELO + 3, CEN_Z1, variante(BloquesFiesta.DIBUJO, Direction.NORTH, 2));
		nota(74, -3, 1);
		bloque(CEN_X1, SUELO + 5, 8, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {CEN_X1 - 1, SUELO + 5, 8});
	}

	/** El pasillo de la puerta del final al salon. */
	private static void pasilloFinal() {
		sala("pasillo_final", PUERTA_FINAL_X - 1, FIN_Z1 + 2, PUERTA_FINAL_X + 1, PUERTA_FINAL_Z - 1, TECHO, BloquesFiesta.MOQUETA_CONFETI,
			BloquesFiesta.PAPEL, 3);
		nota(PUERTA_FINAL_X + 1, FIN_Z1 + 3, 7);
	}

	/**
	 * El salon final: la fiesta de verdad, enorme. Damero grande rojo, blanco y morado, mesas largas
	 * con tartas, globos, regalos, una piscina de bolas con su valla blanca, el escenario y, al
	 * fondo (norte), el tubo rojo del tobogan con el cartel de CERRAMOS: por ahi se sale.
	 */
	private static void salonFinal() {
		sala("final", FIN_X0, FIN_Z0, FIN_X1, FIN_Z1, TECHO_FINAL, BloquesFiesta.DAMEROS[0], BloquesFiesta.PAPEL_ESTRELLAS, 6);
		caja(FIN_X0, SUELO, FIN_Z0, FIN_X1, SUELO, FIN_Z1, (x, y, z) -> {
			int a = Math.floorDiv(x, 2);
			int b = Math.floorDiv(z, 2);
			if (((a + b) & 1) == 0) {
				return s(BloquesFiesta.DAMEROS[0]);
			}
			return Math.floorMod(a - b, 4) < 2 ? s(BloquesFiesta.DAMERO_ROJO) : s(BloquesFiesta.DAMERO_MORADO);
		});
		// mesas largas con mantel, tartas y sillitas
		for (int mx : new int[] {64, 70, 90, 96}) {
			for (int z = FIN_Z0 + 8; z <= FIN_Z1 - 6; z++) {
				bloque(mx, SUELO + 1, z, variante(BloquesFiesta.MESA, Direction.NORTH, Math.floorMod(mx, 4)));
				if (z % 4 == 0) {
					bloque(mx, SUELO + 2, z, variante(BloquesFiesta.TARTA, Direction.from2DDataValue(Math.floorMod(z, 4)), Math.floorMod(z / 4 + mx, 4)));
				}
				if (z % 2 == 0) {
					bloque(mx - 1, SUELO + 1, z, variante(BloquesFiesta.SILLITA, Direction.EAST, Math.floorMod(z + mx, 4)));
					bloque(mx + 1, SUELO + 1, z, variante(BloquesFiesta.SILLITA, Direction.WEST, Math.floorMod(z + mx + 1, 4)));
				}
			}
		}
		// la piscina de bolas con su valla blanca
		int bx0 = 75;
		int bx1 = 85;
		int bz0 = -44;
		int bz1 = -36;
		caja(bx0, SUELO, bz0, bx1, SUELO, bz1, s(BloquesFiesta.PISCINA_BOLAS));
		for (int x = bx0 - 1; x <= bx1 + 1; x++) {
			if (Math.abs(x - 80) > 1) {
				bloque(x, SUELO + 1, bz0 - 1, mira(BloquesFiesta.VALLA, Direction.NORTH));
				bloque(x, SUELO + 1, bz1 + 1, mira(BloquesFiesta.VALLA, Direction.NORTH));
			}
		}
		for (int z = bz0; z <= bz1; z++) {
			bloque(bx0 - 1, SUELO + 1, z, mira(BloquesFiesta.VALLA, Direction.EAST));
			bloque(bx1 + 1, SUELO + 1, z, mira(BloquesFiesta.VALLA, Direction.EAST));
		}
		// el escenario al oeste: tarima, telon rojo y el carton del Anfitrion
		caja(FIN_X0, SUELO + 1, -50, FIN_X0 + 3, SUELO + 1, -38, s(Blocks.DARK_OAK_PLANKS));
		caja(FIN_X0, SUELO + 2, -50, FIN_X0, TECHO_FINAL - 1, -38, s(Blocks.RED_WOOL));
		bloque(FIN_X0 + 1, SUELO + 4, -44, variante(BloquesFiesta.CARTEL, Direction.EAST, 4));
		// regalos amontonados en las esquinas y globos en el techo
		int[][] regalos = {{FIN_X0, FIN_Z0}, {FIN_X0 + 1, FIN_Z0}, {FIN_X1, FIN_Z0}, {FIN_X1 - 1, FIN_Z0}, {FIN_X1, FIN_Z1}, {FIN_X0, FIN_Z1}};
		for (int i = 0; i < regalos.length; i++) {
			bloque(regalos[i][0], SUELO + 1, regalos[i][1], variante(BloquesFiesta.REGALO, Direction.from2DDataValue(i % 4), i % 4));
		}
		for (int x = FIN_X0 + 3; x <= FIN_X1 - 3; x += 5) {
			for (int z = FIN_Z0 + 3; z <= FIN_Z1 - 3; z += 6) {
				if (azar(x, z, 31, 3) != 0) {
					bloque(x, TECHO_FINAL - 1, z, variante(BloquesFiesta.GLOBO, Direction.from2DDataValue(Math.floorMod(x + z, 4)), azar(x, z, 32, 4)));
				}
			}
		}
		racimo(62, -26, SUELO + 1, 0);
		racimo(99, -60, SUELO + 1, 2);
		for (int x = FIN_X0 + 2; x <= FIN_X1 - 2; x += 4) {
			bloque(x, SUELO + 8, FIN_Z1, variante(BloquesFiesta.GUIRNALDA, Direction.NORTH, Math.floorMod(x, 2)));
		}
		bloque(66, SUELO + 3, FIN_Z1, variante(BloquesFiesta.CARTEL, Direction.NORTH, 0));
		bloque(94, SUELO + 3, FIN_Z1, variante(BloquesFiesta.DIBUJO, Direction.NORTH, 1));
		bloque(FIN_X1, SUELO + 4, -40, variante(BloquesFiesta.CARTEL, Direction.WEST, 5));
		bloque(FIN_X1, SUELO + 3, -52, variante(BloquesFiesta.DIBUJO, Direction.WEST, 3));
		// confeti por todo el suelo y regalos sueltos (lo primero: lo de despues lo pisa)
		for (int i = 0; i < 140; i++) {
			int x = FIN_X0 + 1 + azar(i, 1, 161, FIN_X1 - FIN_X0 - 1);
			int z = FIN_Z0 + 1 + azar(i, 2, 161, FIN_Z1 - FIN_Z0 - 1);
			boolean mesa = x == 63 || x == 64 || x == 65 || x == 69 || x == 70 || x == 71 || x == 89 || x == 90 || x == 91 || x == 95 || x == 96 || x == 97;
			boolean ocupado = mesa || x <= FIN_X0 + 3 && z >= -50 && z <= -38 || x >= 74 && x <= 86 && z >= -45 && z <= -35;
			if (!ocupado) {
				bloque(x, SUELO + 1, z, variante(i % 9 == 0 ? BloquesFiesta.REGALO : BloquesFiesta.CONFETI, Direction.from2DDataValue(i % 4), i % 4));
			}
		}
		// banderines de lado a lado del techo, cada 6 bloques
		for (int z = FIN_Z0 + 2; z <= FIN_Z1 - 2; z += 6) {
			for (int x = FIN_X0; x <= FIN_X1; x++) {
				bloque(x, TECHO_FINAL - 1, z, variante(BloquesFiesta.BANDERINES, Direction.NORTH, Math.floorMod(x / 3 + z, 4)));
			}
		}
		// columnas de colores con su racimo de globos arriba, en dos filas por el centro
		for (int x : new int[] {67, 75, 85, 93}) {
			for (int z : new int[] {-30, -50}) {
				caja(x, SUELO + 1, z, x, TECHO_FINAL - 1, z, s(BloquesFiesta.COLUMNAS[Math.floorMod(x + z, BloquesFiesta.COLUMNAS.length)]));
				bloque(x + 1, TECHO_FINAL - 1, z, variante(BloquesFiesta.GLOBO, Direction.NORTH, Math.floorMod(x + z, 4)));
				bloque(x - 1, TECHO_FINAL - 1, z, variante(BloquesFiesta.GLOBO, Direction.SOUTH, Math.floorMod(x + z + 1, 4)));
				racimo(x, z + 1, SUELO + 1, x + z);
			}
		}
		// el mostrador de premios contra la pared este, con peluches y regalos encima
		caja(FIN_X1 - 3, SUELO + 1, -56, FIN_X1 - 3, SUELO + 1, -44, s(Blocks.STRIPPED_CHERRY_LOG));
		for (int z = -56; z <= -44; z++) {
			if (z % 2 == 0) {
				bloque(FIN_X1 - 3, SUELO + 2, z, variante(BloquesFiesta.PELUCHE, Direction.EAST, Math.floorMod(z, 4)));
			} else if (Math.floorMod(z, 3) == 0) {
				bloque(FIN_X1 - 3, SUELO + 2, z, variante(BloquesFiesta.REGALO, Direction.EAST, Math.floorMod(z, 4)));
			}
		}
		bloque(FIN_X1, SUELO + 6, -50, variante(BloquesFiesta.CARTEL, Direction.WEST, 1));
		// la pista de baile: baldosas de colores encendidas y apagadas, en la esquina noroeste
		for (int x = FIN_X0 + 1; x <= FIN_X0 + 8; x++) {
			for (int z = FIN_Z0 + 1; z <= FIN_Z0 + 6; z++) {
				bloque(x, SUELO, z, BloquesFiesta.BALDOSA_JUEGO.defaultBlockState().setValue(BaldosaJuego.COLOR, azar(x, z, 151, 4))
					.setValue(BaldosaJuego.LUZ, azar(x, z, 152, 3) == 0));
			}
		}
		bloque(FIN_X0, SUELO + 4, FIN_Z0 + 3, variante(BloquesFiesta.ALTAVOZ, Direction.EAST, 0));
		// maquinitas de feria contra la pared oeste (al sur del escenario): cajas acolchadas con su pantalla
		for (int z = -34; z <= -24; z += 3) {
			Block col = BloquesFiesta.ACOLCHADOS[Math.floorMod(z, BloquesFiesta.ACOLCHADOS.length)];
			caja(FIN_X0, SUELO + 1, z, FIN_X0 + 1, SUELO + 2, z + 1, s(col));
			caja(FIN_X0, SUELO + 3, z, FIN_X0, SUELO + 3, z + 1, s(BloquesFiesta.LUZ));
			bloque(FIN_X0 + 2, SUELO + 1, z, variante(BloquesFiesta.SILLITA, Direction.WEST, Math.floorMod(z, 4)));
		}
		// la tarta gigante delante del tobogan: tres pisos con velas encendidas
		caja(76, SUELO + 1, FIN_Z0 + 6, 84, SUELO + 2, FIN_Z0 + 12, s(Blocks.PINK_TERRACOTTA));
		caja(77, SUELO + 3, FIN_Z0 + 7, 83, SUELO + 4, FIN_Z0 + 11, s(Blocks.WHITE_TERRACOTTA));
		caja(78, SUELO + 5, FIN_Z0 + 8, 82, SUELO + 5, FIN_Z0 + 10, s(Blocks.PINK_TERRACOTTA));
		for (int[] v : new int[][] {{78, FIN_Z0 + 8}, {80, FIN_Z0 + 8}, {82, FIN_Z0 + 8}, {78, FIN_Z0 + 10}, {80, FIN_Z0 + 10}, {82, FIN_Z0 + 10}, {80, FIN_Z0 + 9}}) {
			bloque(v[0], SUELO + 6, v[1], Blocks.RED_CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
		}
		// dos ramos de globos a los lados de la boca del tobogan
		racimo(77, FIN_Z0, SUELO + 1, 1);
		racimo(83, FIN_Z0, SUELO + 1, 2);
		// el tobogan del final: un tubo rojo en la pared norte, con el cartel encima
		caja(FINAL_TOB_X0 - 1, SUELO, FINAL_TOB_Z0 - 1, FINAL_TOB_X1 + 1, SUELO + 4, FIN_Z0 - 1, s(BloquesFiesta.TOBOGAN));
		aire(FINAL_TOB_X0, SUELO + 1, FINAL_TOB_Z0, FINAL_TOB_X1, SUELO + 3, FIN_Z0 - 1);
		caja(FINAL_TOB_X0, SUELO + 1, FINAL_TOB_Z0 - 1, FINAL_TOB_X1, SUELO + 3, FINAL_TOB_Z0 - 1, s(Blocks.BLACK_CONCRETE));
		bloque(80, SUELO + 5, FIN_Z0, variante(BloquesFiesta.LETRERO, Direction.SOUTH, 1));
		bloque(FIN_X0, SUELO + 5, -30, variante(BloquesFiesta.ALTAVOZ, Direction.EAST, 0));
		bloque(FIN_X1, SUELO + 5, -58, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {FIN_X0 + 1, SUELO + 5, -30});
		ALTAVOCES.add(new int[] {FIN_X1 - 1, SUELO + 5, -58});
		RONDA.add(new int[] {80, -30});
		RONDA.add(new int[] {67, -56});
		RONDA.add(new int[] {93, -48});
		RONDA.add(new int[] {80, -58});
	}

	/**
	 * Al final, lo que tiene que pisar los muros de las salas: las puertas (de las habitaciones, la
	 * roja de la salida y la del final), los pasos abiertos y los parches de suelo defectuoso.
	 */
	private static void conexiones() {
		for (Object[] p : PENDIENTES) {
			puerta((int) p[0], (int) p[1], (Direction) p[2], (int) p[3], (int) p[4], (String) p[5]);
		}
		puerta(SALIDA_X, SALIDA_Z, Direction.WEST, 0, 1, "la salida del laberinto");
		bloque(SALIDA_X - 1, SUELO + 3, SALIDA_Z, variante(BloquesFiesta.LETRERO, Direction.WEST, 0));
		// la entrada del castillo (abajo, oeste) y su salida al tobogan (arriba, este)
		hueco(CAS_X - 1, TOB_Z0, CAS_X - 1, TOB_Z1);
		aire(CAS_X + CAS_LADO + 1, CAS_P1 + 1, TOB_Z0, CAS_X + CAS_LADO + 1, CAS_P1 + 3, TOB_Z1);
		puerta(PUERTA_FINAL_X, PUERTA_FINAL_Z, Direction.SOUTH, 0, 2, "la salida de la fiesta");
		hueco(PUERTA_FINAL_X - 1, FIN_Z1 + 1, PUERTA_FINAL_X + 1, FIN_Z1 + 1);
		// los parches de suelo defectuoso, del color del damero de su sitio
		for (int[] c : PARCHES) {
			int px = LAB_X + c[0] * CELDA + 1;
			int pz = LAB_Z + c[1] * CELDA + 1;
			caja(px, SUELO, pz, px + 1, SUELO, pz + 1, (x, y, z) -> BloquesFiesta.SUELO_DEFECTUOSO.defaultBlockState()
				.setValue(SueloDefectuoso.VARIANTE, 2 + damero(x, z)));
		}
		nota(LAB_X + PARCHES[0][0] * CELDA + 3, LAB_Z + PARCHES[0][1] * CELDA + 3, 3);
	}

	/** La sala (por nombre) donde esta ese punto, o null. Las de despues (habitaciones) mandan. */
	public static Sala sala(double x, double z) {
		for (int i = SALAS.size() - 1; i >= 0; i--) {
			if (SALAS.get(i).dentro(x, z)) {
				return SALAS.get(i);
			}
		}
		return null;
	}

	public static Puerta puerta(int x, int y, int z) {
		for (Puerta p : PUERTAS) {
			if (p.x() == x && p.y() == y && p.z() == z) {
				return p;
			}
		}
		return null;
	}

	public static boolean enCastillo(double x, double z) {
		return x >= CAS_X && x < CAS_X + CAS_LADO + 1 && z >= CAS_Z && z < CAS_Z + CAS_LADO + 1;
	}

	public static boolean enCentral(double x, double z) {
		return x >= CEN_X0 && x < CEN_X1 + 1 && z >= CEN_Z0 && z < CEN_Z1 + 1;
	}
}
