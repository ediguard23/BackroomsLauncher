package net.backrooms.evento.estacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El trazado de la Estacion Electrica (Nivel 3), como la de Backrooms: Escape Together. Todo el nivel
 * sube en diagonal hacia el norte: se aparece en un sitio al azar de la zona de abajo del PISO 1 (SPAWNS).
 * Cada piso es un laberinto de tres zonas, cada una 4 bloques mas alta que la anterior; entre zona y
 * zona, dos REJAS (puertas corredizas: la hoja del medio se desliza a la izquierda cuando se pone el
 * fusible en la caja del marco de la derecha) con su escalera. Arriba del todo, un pasillo largo
 * de lado a lado y, en un costado, la CENTRAL: fusibles, cinta y cable. Activa, abre su puerta y por
 * ahi se pasa, a la misma altura, al piso siguiente. La central del piso 3 abre la PASARELA; al otro
 * lado, la puerta encadenada (la cizalla).
 *
 * Por los laberintos, en los callejones: fusibles, cinta, cable (los materiales,
 * sobre todo en las zonas de abajo: hay que volver a por ellos), comida, BUZONES (se abren con un
 * fusible, que se queda dentro, y tienen algo... o nada) y dos TRASTEROS cerrados con reja.
 *
 * Los laberintos tienen semilla fija (el mismo mapa siempre). GeneradorEstacion copia las cajas;
 * las de despues mandan. Hacia el norte es -z; x crece al este.
 */
public final class PlanoEstacion {
	public static final int PISOS = 3;
	/** La base (zona de abajo) de cada piso: cada zona sube ZONA_SUBE y el piso siguiente empieza donde acaba el anterior. */
	public static final int[] BASES = {40, 48, 56};
	public static final int ZONA_SUBE = 4;
	public static final int ALTO = 5;
	public static final int CELDA = 4;
	/** El laberinto de cada zona: MW x FILAS celdas, de MX0 a MX1 (lineas de muro). */
	public static final int MW = 15;
	public static final int FILAS = 4;
	public static final int MX0 = -30;
	public static final int MX1 = MX0 + MW * CELDA;
	/** Fondo de la zona en z (de muro a muro) y de la franja de la escalera entre zonas. */
	public static final int ZONA_Z = FILAS * CELDA;
	public static final int FRANJA = 5;
	public static final String[] PISO = {"PISO 1", "PISO 2", "PISO 3"};

	/** Tipos de punto de recogida (Interactivos.TIPO). */
	public static final int CABLE = 0;
	public static final int CIZALLA = 1;
	public static final int CINTA = 2;
	public static final int FUSIBLE = 4;

	/** Lo que pide la central de cada piso: {fusibles, cintas, cables}. */
	public static final int[][] NECESITA = {{2, 1, 0}, {2, 1, 1}, {3, 2, 2}};

	public interface Patron {
		BlockState en(int x, int y, int z);
	}

	public record Caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron patron) {
	}

	/** Una sala o zona: su piso y la altura de su suelo (para el staff y la entidad). */
	public record Sala(String nombre, int x0, int z0, int x1, int z1, int piso, int y) {
		public boolean dentro(double x, double yy, double z) {
			return x >= this.x0 && x < this.x1 + 1 && z >= this.z0 && z < this.z1 + 1 && Math.abs(yy - (this.y + 1)) < 4.5;
		}
	}

	/** Donde se coge algo: el bloque, el tipo (CABLE, CIZALLA...) y su piso. */
	public record Punto(int id, int x, int y, int z, int tipo, int piso) {
	}

	/** Un buzon: se abre con un fusible (que se queda) y da lo que tenga. */
	public record Buzon(int id, int x, int y, int z, int piso) {
	}

	/**
	 * Una puerta de 3x3 (persiana o reja): el bloque de abajo a un lado, hacia donde se extiende
	 * (`lado`) y hacia donde mira; y = el suelo. Las rejas llevan su caja de fusible (`caja`).
	 */
	public record Puerta(int id, int x, int y, int z, Direction mira, Direction lado, int piso, int @org.jspecify.annotations.Nullable [] caja, int ancho) {
		/** Cuantos bloques tiene (3 de alto por cada columna). */
		public int partes() {
			return this.ancho * 3;
		}

		/** Si es corrediza (una hoja que se desliza hacia el marco de la izquierda) y no una persiana que sube. */
		public boolean corrediza() {
			return this.ancho == 1;
		}

		public int[] bloque(int parte) {
			int col = parte / 3;
			return new int[] {this.x + this.lado.getStepX() * col, this.y + 1 + parte % 3, this.z + this.lado.getStepZ() * col};
		}

		public boolean es(int bx, int by, int bz) {
			for (int k = 0; k < this.partes(); k++) {
				int[] b = this.bloque(k);
				if (b[0] == bx && b[1] == by && b[2] == bz) {
					return true;
				}
			}
			return false;
		}
	}

	/** La central de un piso: el cuadro donde se pone todo y la pantalla de encima (x, y, z, giro). */
	public record Central(int piso, int x, int y, int z, double[] pantalla) {
	}

	public static final List<Caja> CAJAS = new ArrayList<>();
	public static final List<Sala> SALAS = new ArrayList<>();
	public static final List<Punto> PUNTOS = new ArrayList<>();
	public static final List<Buzon> BUZONES = new ArrayList<>();
	/** Las rejas (puertas corredizas con su caja de fusible): las de las escaleras y las de los trasteros. */
	public static final List<Puerta> REJAS = new ArrayList<>();
	/** La puerta de cada central (al piso siguiente; la del piso 3, a la pasarela). */
	public static final Puerta[] SALIDAS = new Puerta[PISOS];
	public static Puerta puertaFinal;
	public static final Central[] CENTRALES = new Central[PISOS];
	/** Los focos de la pasarela, en el orden en que se encienden. */
	public static final List<int[]> FOCOS = new ArrayList<>();
	/** Por donde ronda la entidad: {x, z, piso, y del suelo}, el centro de las celdas de los laberintos. */
	public static final List<int[]> RONDA = new ArrayList<>();
	/** Donde se aparece en cada piso: {x, y (los pies), z, giro}. En el 0, uno de SPAWNS al azar. */
	public static final double[][] LLEGADAS = new double[PISOS][];
	/** Los sitios donde se aparece al llegar (la zona de abajo del piso 1): {x, y (los pies), z, giro}. */
	public static final List<double[]> SPAWNS = new ArrayList<>();
	/** La comida: {x, z, tipo de Plano.C_*, y (los pies)}. */
	public static final List<int[]> COMIDA = new ArrayList<>();
	/** Detras de la puerta del final: quien llega aqui sale del nivel {x0, z0, x1, z1, y}. */
	public static int[] FINAL;
	/** Donde empieza cada piso (el muro sur de su zona de abajo): para saber en que piso se esta. */
	public static final int[] INICIO = new int[PISOS];

	/** Limites del edificio, para el generador: salen de las cajas. */
	public static final int X0;
	public static final int X1;
	public static final int Z0;
	public static final int Z1;

	private PlanoEstacion() {
	}

	/** El piso de un sitio (por su z: el nivel avanza hacia el norte). */
	public static int piso(double z) {
		return z > INICIO[1] ? 0 : z > INICIO[2] ? 1 : 2;
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

	/** La pared tipica: dos de verde gris pintado abajo, la franja del piso y hormigon arriba. */
	private static Patron pared(int color, int suelo) {
		return (x, y, z) -> {
			if (y <= suelo + 2) {
				return s(BloquesEstacion.PINTADA);
			}
			if (y == suelo + 3 && color >= 0) {
				return BloquesEstacion.FRANJA.defaultBlockState().setValue(Interactivos.COLOR, color);
			}
			return s(BloquesEstacion.HORMIGON);
		};
	}

	/** El suelo de hormigon con alguna mancha de aceite. */
	private static final Patron SUELO_H = (x, y, z) -> azar(x, z, 5, 11) == 0 ? s(BloquesEstacion.SUELO_MANCHADO) : s(BloquesEstacion.SUELO);

	/**
	 * Una sala: suelo en y, muros (`color`: la franja, -1 sin franja) y techo a y + alto con lamparas
	 * cada `luces`. Interior x0..x1, z0..z1; los muros alrededor. Sin nombre no se apunta.
	 */
	private static void sala(String nombre, int piso, int y, int x0, int z0, int x1, int z1, int alto, Patron suelo, int color, int luces) {
		if (nombre != null) {
			SALAS.add(new Sala(nombre, x0, z0, x1, z1, piso, y));
		}
		caja(x0 - 1, y, z0 - 1, x1 + 1, y, z1 + 1, suelo);
		Patron p = pared(color, y);
		caja(x0 - 1, y + 1, z0 - 1, x1 + 1, y + alto - 1, z0 - 1, p);
		caja(x0 - 1, y + 1, z1 + 1, x1 + 1, y + alto - 1, z1 + 1, p);
		caja(x0 - 1, y + 1, z0, x0 - 1, y + alto - 1, z1, p);
		caja(x1 + 1, y + 1, z0, x1 + 1, y + alto - 1, z1, p);
		aire(x0, y + 1, z0, x1, y + alto - 1, z1);
		techo(x0, z0, x1, z1, y + alto, luces);
	}

	private static void techo(int x0, int z0, int x1, int z1, int y, int luces) {
		int ox = Math.floorMod(x0 + luces / 2, luces);
		int oz = Math.floorMod(z0 + luces / 2, luces);
		caja(x0 - 1, y, z0 - 1, x1 + 1, y, z1 + 1, (x, yy, z) -> {
			if (x >= x0 && x <= x1 && z >= z0 && z <= z1 && Math.floorMod(x - ox, luces) == 0 && Math.floorMod(z - oz, luces) == 0) {
				return azar(x, z, 8 + yy, 7) == 0 ? s(BloquesEstacion.LAMPARA_FUNDIDA) : s(BloquesEstacion.LAMPARA);
			}
			return Math.floorMod(x, 6) == 0 ? s(BloquesEstacion.TECHO_TUBOS) : s(BloquesEstacion.TECHO);
		});
	}

	/** Un hueco de paso (de 3 de alto) a traves de lo que haya. */
	private static void hueco(int x0, int y, int z0, int x1, int z1) {
		aire(x0, y + 1, z0, x1, y + 3, z1);
	}

	/** Una persiana (o reja) de 3x3. */
	private static Puerta puerta(Block b, int id, int x, int y, int z, Direction mira, Direction lado, int piso, int[] caja, int ancho) {
		Puerta p = new Puerta(id, x, y, z, mira, lado, piso, caja, ancho);
		for (int k = 0; k < p.partes(); k++) {
			int[] c = p.bloque(k);
			bloque(c[0], c[1], c[2], mira(b, mira).setValue(Interactivos.PARTE, k));
		}
		return p;
	}

	/**
	 * Una reja en un hueco de 3 (desde x, z hacia `lado`): el marco de la izquierda fijo (alli se mete la hoja
	 * al abrirse), la hoja corrediza en medio y el marco de la derecha con la caja del fusible en medio
	 * (se ve y se usa por los dos lados).
	 */
	private static void reja(int x, int y, int z, Direction lado, int piso) {
		Direction mira = lado == Direction.EAST ? Direction.NORTH : Direction.EAST;
		int dx = lado.getStepX();
		int dz = lado.getStepZ();
		caja(x, y + 1, z, x, y + 3, z, s(BloquesEstacion.MARCO_REJA));
		caja(x + 2 * dx, y + 1, z + 2 * dz, x + 2 * dx, y + 3, z + 2 * dz, s(BloquesEstacion.MARCO_REJA));
		int[] caja = {x + 2 * dx, y + 2, z + 2 * dz};
		bloque(caja[0], caja[1], caja[2], s(BloquesEstacion.CAJA_REJA));
		REJAS.add(puerta(BloquesEstacion.REJA, REJAS.size(), x + dx, y, z + dz, mira, lado, piso, caja, 1));
	}

	private static void punto(int x, int y, int z, int tipo, Direction mira, int piso) {
		PUNTOS.add(new Punto(PUNTOS.size(), x, y + 1, z, tipo, piso));
		bloque(x, y + 1, z, mira(BloquesEstacion.PUNTO, mira).setValue(Interactivos.TIPO, tipo));
	}

	private static void buzon(int x, int y, int z, Direction mira, int piso) {
		BUZONES.add(new Buzon(BUZONES.size(), x, y, z, piso));
		bloque(x, y, z, mira(BloquesEstacion.BUZON, mira));
	}

	private static void rotulo(int x, int y, int z, Direction mira, int color, int num) {
		bloque(x, y, z, mira(BloquesEstacion.ROTULO, mira).setValue(Interactivos.COLOR, color).setValue(Interactivos.Rotulo.NUM, num));
	}

	/* ------------------------------------------------------------- el plano */

	static {
		int s = 0;
		for (int p = 0; p < PISOS; p++) {
			INICIO[p] = s;
			int entrada = p == 0 ? MW / 2 : p == 1 ? MW - 1 : 0;
			s = piso(p, s, entrada, p != 1);
		}
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

	/* ------------------------------------------------------------ los pisos */

	/**
	 * El piso p: tres zonas de laberinto (cada una ZONA_SUBE mas alta), las escaleras con sus rejas,
	 * el pasillo final y la central (al este si `este`). `s` es el muro sur de la zona de abajo y
	 * `entrada` la columna por donde se entra. Devuelve el muro sur del piso siguiente.
	 */
	private static int piso(int p, int s, int entrada, boolean este) {
		Random azar = new Random(9173L + p * 2311L);
		int[][][] reparto = {
			{{FUSIBLE, FUSIBLE, CINTA, FUSIBLE}, {FUSIBLE, FUSIBLE, CINTA}, {FUSIBLE, FUSIBLE}},
			{{FUSIBLE, FUSIBLE, CABLE}, {FUSIBLE, CINTA, FUSIBLE}, {FUSIBLE, FUSIBLE}},
			{{FUSIBLE, FUSIBLE, CABLE, CINTA}, {FUSIBLE, FUSIBLE, CABLE, CINTA}, {FUSIBLE, FUSIBLE, FUSIBLE}}
		};
		int[][] enTrasteros = {{FUSIBLE, CABLE}, {CABLE, CINTA}, {CIZALLA, CABLE}};
		int[] columnasSubida = null;
		int sur = s;
		for (int k = 0; k < 3; k++) {
			int y = BASES[p] + k * ZONA_SUBE;
			// las columnas por donde se sube a la zona siguiente (dos, separadas)
			int[] subida = k < 2 ? new int[] {1 + azar.nextInt(MW / 2 - 2), MW / 2 + 1 + azar.nextInt(MW / 2 - 2)} : new int[] {MW / 2};
			int[] bajada = k == 0 ? new int[] {entrada} : columnasSubida;
			zona(p, k, sur, y, bajada, subida, k < 2, azar, reparto[p][k], k == 0 ? -1 : enTrasteros[p][k - 1], !(p == 0 && k == 0));
			int norte = sur - ZONA_Z;
			if (k < 2) {
				escaleras(p, k, norte, y, subida);
				sur = norte - FRANJA;
			} else {
				sur = norte;
			}
			columnasSubida = subida;
		}
		return finalDelPiso(p, sur, BASES[p] + 2 * ZONA_SUBE, este);
	}

	/**
	 * Una zona de laberinto: MW x FILAS celdas con su muro sur en `sur` y suelo en y. Se entra por las
	 * columnas `bajada` (muro sur) y se sale por `subida` (muro norte: rejas si `rejas`, si no hueco).
	 */
	private static void zona(int p, int k, int sur, int y, int[] bajada, int[] subida, boolean rejas, Random azar, int[] cosas, int trastero, boolean entrada) {
		int techo = y + ALTO;
		int norte = sur - ZONA_Z;
		SALAS.add(new Sala("piso_" + (p + 1) + "_zona_" + (k + 1), MX0 + 1, norte + 1, MX1 - 1, sur - 1, p, y));
		// el laberinto (backtracking) con algunos pasos de mas para que haya vueltas; fila 0 = la del sur
		boolean[][] este = new boolean[MW][FILAS];
		boolean[][] nor = new boolean[MW][FILAS];   // paso de (cx, f) a (cx, f + 1), hacia el norte
		boolean[][] visto = new boolean[MW][FILAS];
		int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		int[] pila = new int[MW * FILAS * 2];
		int tope = 0;
		pila[tope++] = bajada[0];
		pila[tope++] = 0;
		visto[bajada[0]][0] = true;
		while (tope > 0) {
			int cx = pila[tope - 2];
			int f = pila[tope - 1];
			int[] orden = {0, 1, 2, 3};
			for (int i = 3; i > 0; i--) {
				int j = azar.nextInt(i + 1);
				int t = orden[i];
				orden[i] = orden[j];
				orden[j] = t;
			}
			boolean avanzo = false;
			for (int o : orden) {
				int nx = cx + dir[o][0];
				int nf = f + dir[o][1];
				if (nx < 0 || nf < 0 || nx >= MW || nf >= FILAS || visto[nx][nf]) {
					continue;
				}
				if (nx != cx) {
					este[Math.min(cx, nx)][f] = true;
				} else {
					nor[cx][Math.min(f, nf)] = true;
				}
				visto[nx][nf] = true;
				pila[tope++] = nx;
				pila[tope++] = nf;
				avanzo = true;
				break;
			}
			if (!avanzo) {
				tope -= 2;
			}
		}
		for (int cx = 0; cx < MW; cx++) {
			for (int f = 0; f < FILAS; f++) {
				if (cx + 1 < MW && !este[cx][f] && azar.nextInt(100) < 12) {
					este[cx][f] = true;
				}
				if (f + 1 < FILAS && !nor[cx][f] && azar.nextInt(100) < 12) {
					nor[cx][f] = true;
				}
			}
		}
		// todo macizo y luego se excava
		caja(MX0, y, norte, MX1, y, sur, SUELO_H);
		caja(MX0, y + 1, norte, MX1, techo - 1, sur, pared(p, y));
		for (int cx = 0; cx < MW; cx++) {
			for (int f = 0; f < FILAS; f++) {
				int x0 = MX0 + 1 + cx * CELDA;
				int z1 = sur - 1 - f * CELDA;          // la fila de mas al sur de la celda
				aire(x0, y + 1, z1 - 2, x0 + 2, techo - 1, z1);
				if (este[cx][f]) {
					aire(x0 + 3, y + 1, z1 - 2, x0 + 3, techo - 1, z1);
				}
				if (nor[cx][f]) {
					aire(x0, y + 1, z1 - 3, x0 + 2, techo - 1, z1 - 3);
				}
				RONDA.add(new int[] {x0 + 1, z1 - 1, p, y});
			}
		}
		caja(MX0, techo, norte, MX1, techo, sur, (x, yy, z) -> {
			int cx = Math.floorDiv(x - MX0 - 1, CELDA);
			int f = Math.floorDiv(sur - 1 - z, CELDA);
			int u = Math.floorMod(x - MX0 - 1, CELDA);
			int v = Math.floorMod(sur - 1 - z, CELDA);
			boolean oscura = azar(cx / 4 + p * 7 + k * 3, f / 2, 41, 4) == 0;
			if (u == 1 && v == 1 && ((cx + f) & 1) == 0 && !oscura) {
				return azar(cx + k * 31, f + p * 17, 42, 6) == 0 ? s(BloquesEstacion.LAMPARA_FUNDIDA) : s(BloquesEstacion.LAMPARA);
			}
			return u == 1 ? s(BloquesEstacion.TECHO_TUBOS) : s(BloquesEstacion.TECHO);
		});
		// las entradas (muro sur; la zona de abajo del piso 1 no tiene: alli se aparece) y, si no hay
		// rejas, las salidas (muro norte)
		for (int c : entrada ? bajada : new int[0]) {
			int x0 = MX0 + 1 + c * CELDA;
			aire(x0, y + 1, sur, x0 + 2, y + 3, sur);
		}
		if (!rejas) {
			for (int c : subida) {
				int x0 = MX0 + 1 + c * CELDA;
				aire(x0, y + 1, norte, x0 + 2, techo - 1, norte);
			}
		}
		// los callejones sin salida (no los de entrada ni salida)
		List<int[]> callejones = new ArrayList<>();
		for (int cx = 0; cx < MW; cx++) {
			for (int f = 0; f < FILAS; f++) {
				boolean[] ab = abiertos(este, nor, cx, f);
				int n = 0;
				for (boolean b : ab) {
					n += b ? 1 : 0;
				}
				boolean paso = false;
				for (int c : bajada) {
					paso |= c == cx && f == 0;
				}
				for (int c : subida) {
					paso |= c == cx && f == FILAS - 1;
				}
				if (n == 1 && !paso) {
					callejones.add(new int[] {cx, f});
				}
			}
		}
		Collections.shuffle(callejones, azar);
		boolean[][] usado = new boolean[MW][FILAS];
		Direction[] lados = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// el trastero: un callejon cuyo paso se cierra con una reja (dentro, una estanteria y algo bueno)
		if (trastero >= 0 && !callejones.isEmpty()) {
			int[] c = callejones.remove(0);
			usado[c[0]][c[1]] = true;
			boolean[] ab = abiertos(este, nor, c[0], c[1]);
			int lado = ab[0] ? 0 : ab[1] ? 1 : ab[2] ? 2 : 3;
			int x0 = MX0 + 1 + c[0] * CELDA;
			int z1 = sur - 1 - c[1] * CELDA;
			switch (lado) {
				case 0 -> {
					caja(x0, techo - 1, z1 - 3, x0 + 2, techo - 1, z1 - 3, pared(p, y));
					reja(x0, y, z1 - 3, Direction.EAST, p);
				}
				case 2 -> {
					caja(x0, techo - 1, z1 + 1, x0 + 2, techo - 1, z1 + 1, pared(p, y));
					reja(x0, y, z1 + 1, Direction.EAST, p);
				}
				case 1 -> {
					caja(x0 + 3, techo - 1, z1 - 2, x0 + 3, techo - 1, z1, pared(p, y));
					reja(x0 + 3, y, z1 - 2, Direction.SOUTH, p);
				}
				default -> {
					caja(x0 - 1, techo - 1, z1 - 2, x0 - 1, techo - 1, z1, pared(p, y));
					reja(x0 - 1, y, z1 - 2, Direction.SOUTH, p);
				}
			}
			caja(x0, y, z1 - 2, x0 + 2, y, z1, s(BloquesEstacion.CHAPA));
			int fondo = (lado + 2) % 4;
			int[] b = pared1(x0, z1, fondo);
			punto(b[0], y, b[1], trastero, lados[lado], p);
			int[] e = pared1(x0, z1, (fondo + 1) % 4);
			bloque(e[0], y + 1, e[1], var(BloquesEstacion.ESTANTERIA, lados[(fondo + 3) % 4], 1));
		}
		// lo que se coge, en los callejones; si no hay bastantes, pegado a una pared cualquiera
		int i = 0;
		for (int cosa : cosas) {
			int[] c = i < callejones.size() ? callejones.get(i) : celdaLibre(azar, usado, bajada, subida);
			i++;
			usado[c[0]][c[1]] = true;
			int l = paredLibre(este, nor, c[0], c[1], azar);
			int x0 = MX0 + 1 + c[0] * CELDA;
			int z1 = sur - 1 - c[1] * CELDA;
			int[] b = pared1(x0, z1, l);
			punto(b[0], y, b[1], cosa, lados[(l + 2) % 4], p);
		}
		// dos buzones por zona, en callejones o pegados a una pared
		for (int n = 0; n < 2; n++) {
			int[] c = i < callejones.size() ? callejones.get(i) : celdaLibre(azar, usado, bajada, subida);
			i++;
			usado[c[0]][c[1]] = true;
			int l = paredLibre(este, nor, c[0], c[1], azar);
			int x0 = MX0 + 1 + c[0] * CELDA;
			int z1 = sur - 1 - c[1] * CELDA;
			int[] b = pared1(x0, z1, l);
			buzon(b[0], y + 2, b[1], lados[(l + 2) % 4], p);
		}
		if (p == 0 && k == 0) {
			// ocho sitios repartidos donde aparecer (celdas libres, mirando hacia un pasillo abierto)
			float[] giros = {180.0F, -90.0F, 0.0F, 90.0F};
			for (int n = 0; n < 8; n++) {
				int[] c = celdaLibre(azar, usado, bajada, subida);
				usado[c[0]][c[1]] = true;
				int x0 = MX0 + 1 + c[0] * CELDA;
				int z1 = sur - 1 - c[1] * CELDA;
				boolean[] ab = abiertos(este, nor, c[0], c[1]);
				int mira = 0;
				while (mira < 3 && !ab[mira]) {
					mira++;
				}
				SPAWNS.add(new double[] {x0 + 1.5, y + 1, z1 - 0.5, giros[mira]});
			}
			LLEGADAS[0] = SPAWNS.get(0);
		}
		// por los pasillos: tuberias de pared a pared, cuadros, senales, rotulos, bidones vacios, cables,
		// luces rojas y algo de comer (siempre en la celda, pegado a una pared de verdad)
		for (int cx = 0; cx < MW; cx++) {
			for (int f = 0; f < FILAS; f++) {
				if (usado[cx][f]) {
					continue;
				}
				boolean[] ab = abiertos(este, nor, cx, f);
				int l = -1;
				for (int t = 0; t < 4; t++) {
					int q = (azar(cx + k * 13, f + p * 7, 50, 4) + t) % 4;
					if (!ab[q] && !esPasoExterior(cx, f, q, bajada, subida)) {
						l = q;
						break;
					}
				}
				if (l < 0) {
					continue;
				}
				int x0 = MX0 + 1 + cx * CELDA;
				int z1 = sur - 1 - f * CELDA;
				int[] b = pared1(x0, z1, l);
				Direction dentro = lados[(l + 2) % 4];
				int r = azar(cx + k * 17, f + p * 5, 60, 20);
				if (r < 3) {
					// una tuberia que va de pared a pared (las tres de ese lado)
					for (int d = -1; d <= 1; d++) {
						int tx = l % 2 == 0 ? b[0] + d : b[0];
						int tz = l % 2 == 0 ? b[1] : b[1] + d;
						bloque(tx, y + 3, tz, var(BloquesEstacion.TUBERIAS, dentro, r == 0 ? 1 : 0));
					}
				} else if (r < 6) {
					bloque(b[0], y + 2, b[1], var(BloquesEstacion.CUADRO, dentro, r));
				} else if (r < 7) {
					bloque(b[0], y + 2, b[1], var(BloquesEstacion.SENAL, dentro, 7));
				} else if (r < 9) {
					rotulo(b[0], y + 3, b[1], dentro, p, 1 + Math.floorMod(cx / 3 + f * 5 + k * 3, 9));
				} else if (r < 10) {
					bloque(b[0], y + 1, b[1], var(BloquesEstacion.BIDONES, dentro, 2));
				} else if (r < 11) {
					bloque(x0 + 1, techo - 1, z1 - 1, var(BloquesEstacion.CABLES, Direction.NORTH, 0));
				} else if (r < 12) {
					bloque(b[0], y + 3, b[1], var(BloquesEstacion.LUZ_ROJA, dentro, 0));
				} else if (r < 13 && azar(cx + 3, f + k * 7 + p * 11, 71, 4) != 0) {
					COMIDA.add(new int[] {x0 + 1, z1 - 1, azar(cx, f + k, 70, 2) == 0 ? net.backrooms.evento.mundo.Plano.C_AGUA : net.backrooms.evento.mundo.Plano.C_GALLETAS, y + 1});
				}
			}
		}
	}

	/** El bloque de dentro de la celda (x0: su oeste, z1: su sur) pegado a la pared del lado l (0 N, 1 E, 2 S, 3 O). */
	private static int[] pared1(int x0, int z1, int l) {
		return switch (l) {
			case 0 -> new int[] {x0 + 1, z1 - 2};
			case 1 -> new int[] {x0 + 2, z1 - 1};
			case 2 -> new int[] {x0 + 1, z1};
			default -> new int[] {x0, z1 - 1};
		};
	}

	/** Si el lado l de la celda es un hueco hacia fuera de la zona (la entrada o la salida). */
	private static boolean esPasoExterior(int cx, int f, int l, int[] bajada, int[] subida) {
		for (int c : bajada) {
			if (c == cx && f == 0 && l == 2) {
				return true;
			}
		}
		for (int c : subida) {
			if (c == cx && f == FILAS - 1 && l == 0) {
				return true;
			}
		}
		return false;
	}

	private static int paredLibre(boolean[][] este, boolean[][] nor, int cx, int f, Random azar) {
		boolean[] ab = abiertos(este, nor, cx, f);
		for (int t = 0; t < 4; t++) {
			int q = (azar.nextInt(4) + t) % 4;
			if (!ab[q]) {
				return q;
			}
		}
		return 0;
	}

	private static int[] celdaLibre(Random azar, boolean[][] usado, int[] bajada, int[] subida) {
		for (int t = 0; t < 300; t++) {
			int cx = azar.nextInt(MW);
			int f = azar.nextInt(FILAS);
			boolean paso = false;
			for (int c : bajada) {
				paso |= c == cx && f == 0;
			}
			for (int c : subida) {
				paso |= c == cx && f == FILAS - 1;
			}
			if (!usado[cx][f] && !paso) {
				return new int[] {cx, f};
			}
		}
		return new int[] {0, 1};
	}

	/** Los pasos de una celda: norte, este, sur, oeste. */
	private static boolean[] abiertos(boolean[][] este, boolean[][] nor, int cx, int f) {
		return new boolean[] {f + 1 < FILAS && nor[cx][f], cx + 1 < MW && este[cx][f], f > 0 && nor[cx][f - 1], cx > 0 && este[cx - 1][f]};
	}

	/**
	 * La franja entre la zona k y la de arriba: en cada columna de subida, la reja corrediza en el muro
	 * norte de la zona (la caja del fusible en su marco de la derecha) y una escalera de ZONA_SUBE
	 * peldanos hasta el muro sur de la zona de arriba.
	 */
	private static void escaleras(int p, int k, int norte, int y, int[] columnas) {
		int arriba = y + ZONA_SUBE;
		caja(MX0, y, norte - FRANJA + 1, MX1, arriba + ALTO, norte - 1, s(BloquesEstacion.HORMIGON));
		caja(MX0, y + ALTO + 1, norte, MX1, arriba + ALTO, norte, s(BloquesEstacion.HORMIGON));
		for (int c : columnas) {
			int x0 = MX0 + 1 + c * CELDA;
			// la reja (con su dintel)
			caja(x0, y + 4, norte, x0 + 2, y + ALTO - 1, norte, pared(p, y));
			reja(x0, y, norte, Direction.EAST, p);
			rotulo(x0 + 1, y + 4, norte + 1, Direction.SOUTH, p, 10 + Math.floorMod(c + k * 3, 6));
			// la escalera: aire hasta el techo de arriba y los peldanos
			aire(x0, y + 1, norte - FRANJA + 1, x0 + 2, arriba + ALTO - 1, norte - 1);
			for (int j = 1; j <= ZONA_SUBE; j++) {
				int z = norte - j;
				if (j > 1) {
					caja(x0, y + 1, z, x0 + 2, y + j - 1, z, s(BloquesEstacion.HORMIGON));
				}
				caja(x0, y + j, z, x0 + 2, y + j, z, Blocks.POLISHED_ANDESITE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
			}
			// una lampara en cada escalera
			bloque(x0 + 3, y + 5, norte - 2, s(BloquesEstacion.LAMPARA));
			// la entrada a la zona de arriba (su muro sur)
			aire(x0, arriba + 1, norte - FRANJA, x0 + 2, arriba + 3, norte - FRANJA);
		}
	}

	/**
	 * El final del piso: el pasillo largo de lado a lado (se sale del laberinto por el medio) y, en un
	 * costado, la sala de la central con su puerta al piso siguiente (o a la pasarela). Devuelve el muro
	 * sur del piso siguiente.
	 */
	private static int finalDelPiso(int p, int norte, int y, boolean este) {
		int zc = norte - 2;
		sala("pasillo_final_" + (p + 1), p, y, MX0 + 1, zc - 1, MX1 - 1, zc + 1, ALTO, SUELO_H, p, 6);
		aire(MX0 + 1 + MW / 2 * CELDA, y + 1, norte, MX0 + 3 + MW / 2 * CELDA, y + ALTO - 1, norte);
		// cosas por el pasillo (dentro, contra el muro de enfrente) y, al otro costado, una puerta tapiada
		for (int x = MX0 + 3; x <= MX1 - 3; x += 5) {
			int r = azar(x, p, 80, 4);
			if (r == 0) {
				bloque(x, y + 2, zc - 1, var(BloquesEstacion.CUADRO, Direction.SOUTH, 4 + Math.floorMod(x, 4)));
			} else if (r == 1) {
				bloque(x, y + 3, zc - 1, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
			}
		}
		int lejos = este ? MX0 + 1 : MX1 - 1;
		bloque(lejos, y + 2, zc, var(BloquesEstacion.SENAL, este ? Direction.EAST : Direction.WEST, 7));
		// la sala de la central
		int cx0 = este ? MX1 + 2 : MX0 - 12;
		int cx1 = cx0 + 10;
		int cz0 = zc - 6;
		int cz1 = zc + 6;
		sala("central_" + (p + 1), p, y, cx0, cz0, cx1, cz1, ALTO + 1, (x, yy, z) -> s(BloquesEstacion.CHAPA), p, 4);
		aire(este ? MX1 : cx1 + 1, y + 1, zc - 1, este ? MX1 + 1 : MX0, y + 3, zc + 1);
		Direction mira = este ? Direction.WEST : Direction.EAST;
		int px = este ? cx1 : cx0;
		bloque(px, y + 2, zc, mira(BloquesEstacion.PANEL, mira).setValue(Interactivos.TIPO, p));
		for (int d = 1; d <= 3; d++) {
			bloque(px, y + 2, zc + d, var(BloquesEstacion.CUADRO, mira, 4 + d));
			bloque(px, y + 2, zc - d, var(BloquesEstacion.CUADRO, mira, d));
			bloque(px, y + 1, zc + d, var(BloquesEstacion.ARMARIO, mira, d));
			bloque(px, y + 1, zc - d, var(BloquesEstacion.ARMARIO, mira, d + 1));
		}
		bloque(px, y + 3, zc - 4, var(BloquesEstacion.SENAL, mira, 0));
		bloque(px, y + 3, zc + 4, var(BloquesEstacion.SENAL, mira, 0));
		double pxx = px + 0.5 - mira.getStepX() * 0.43;
		CENTRALES[p] = new Central(p, px, y + 2, zc, new double[] {pxx, y + 3.9, zc + 0.5, mira.toYRot()});
		rotulo(px, y + 5, zc, mira, p, 9);
		int mx = (cx0 + cx1) / 2;
		for (int d = -2; d <= 2; d += 2) {
			bloque(mx + (este ? 2 : -2), y + 1, zc + d, var(BloquesEstacion.CONSOLA, mira, Math.floorMod(d, 4)));
		}
		COMIDA.add(new int[] {mx, cz1 - 1, net.backrooms.evento.mundo.Plano.C_AGUA, y + 1});
		// la puerta de la central (muro norte): al piso siguiente o a la pasarela
		int dx = mx - 1;
		bloque(mx, y + 4, cz0, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
		if (p == PISOS - 1) {
			pasarela(dx, y, cz0 - 1);
			SALIDAS[p] = puerta(BloquesEstacion.PERSIANA, p, dx, y, cz0 - 1, Direction.NORTH, Direction.EAST, p, null, 3);
			return norte;
		}
		// el paso al piso siguiente: hacia el norte y luego de lado hasta su columna de entrada
		int sig = zc - 13;
		int ex0 = este ? MX1 - 3 : MX0 + 1;
		sala(null, p, y, dx, sig + 1, dx + 2, cz0 - 2, ALTO, SUELO_H, p + 1, 4);
		sala(null, p, y, Math.min(dx, ex0), sig + 1, Math.max(dx + 2, ex0 + 2), sig + 3, ALTO, SUELO_H, p + 1, 4);
		aire(dx, y + 1, sig + 1, dx + 2, y + ALTO - 1, cz0 - 2);
		LLEGADAS[p + 1] = new double[] {dx + 1.5, y + 1, sig + 2.5, este ? 90.0 : 270.0};
		rotulo(ex0 + 1, y + 4, sig + 1, Direction.SOUTH, p + 1, 0);
		SALIDAS[p] = puerta(BloquesEstacion.PERSIANA, p, dx, y, cz0 - 1, Direction.NORTH, Direction.EAST, p, null, 3);
		return sig;
	}

	/** La nave de la pasarela: hacia el norte desde la puerta de la central del piso 3, sobre un pozo. */
	private static void pasarela(int dx, int y, int zPuerta) {
		int x0 = dx - 5;
		int x1 = dx + 7;
		int z1 = zPuerta - 1;
		int z0 = z1 - 58;
		int pozo = y - 24;
		int techoPozo = y + 12;
		SALAS.add(new Sala("pasarela", x0, z0, x1, z1, PISOS - 1, y));
		Patron muro = (x, yy, z) -> yy < y ? s(BloquesEstacion.HORMIGON) : pared(-1, y).en(x, yy, z);
		caja(x0 - 1, pozo, z0 - 1, x1 + 1, pozo, z1 + 1, s(BloquesEstacion.SUELO_MANCHADO));
		caja(x0 - 1, pozo + 1, z0 - 1, x1 + 1, techoPozo - 1, z0 - 1, muro);
		caja(x0 - 1, pozo + 1, z1 + 1, x1 + 1, techoPozo - 1, z1 + 1, muro);
		caja(x0 - 1, pozo + 1, z0, x0 - 1, techoPozo - 1, z1, muro);
		caja(x1 + 1, pozo + 1, z0, x1 + 1, techoPozo - 1, z1, muro);
		aire(x0, pozo + 1, z0, x1, techoPozo - 1, z1);
		techo(x0, z0, x1, z1, techoPozo, 6);
		// la puerta de la central da a la pasarela (el muro sur de la nave es el de la central)
		hueco(dx, y, z1 + 1, dx + 2, z1 + 1);
		// la pasarela de rejilla, su barandilla y los focos a los lados
		int fin = z0 + 6;
		caja(dx, y, fin, dx + 2, y, z1, s(BloquesEstacion.REJILLA));
		for (int z = fin; z <= z1; z++) {
			bloque(dx - 1, y + 1, z, mira(BloquesEstacion.BARANDILLA, Direction.EAST));
			bloque(dx + 3, y + 1, z, mira(BloquesEstacion.BARANDILLA, Direction.WEST));
			bloque(dx - 1, y, z, s(BloquesEstacion.CHAPA));
			bloque(dx + 3, y, z, s(BloquesEstacion.CHAPA));
			if (Math.floorMod(z1 - z, 5) == 2) {
				bloque(dx - 1, y, z, BloquesEstacion.FOCO.defaultBlockState());
				bloque(dx + 3, y, z, BloquesEstacion.FOCO.defaultBlockState());
				FOCOS.add(new int[] {dx - 1, y, z});
				FOCOS.add(new int[] {dx + 3, y, z});
			}
		}
		// se encienden desde la puerta de la central (al sur) hacia la del final
		FOCOS.sort((a, b) -> b[2] - a[2]);
		for (int z = z1 - 5; z >= fin + 2; z -= 12) {
			caja(dx - 1, pozo + 1, z, dx - 1, y - 1, z, s(BloquesEstacion.CHAPA));
			caja(dx + 3, pozo + 1, z, dx + 3, y - 1, z, s(BloquesEstacion.CHAPA));
		}
		// la plataforma del final y la puerta encadenada (al norte); detras, oscuro
		caja(x0, y, z0, x1, y, fin - 1, s(BloquesEstacion.CHAPA));
		for (int x = x0; x <= x1; x++) {
			if (x < dx - 1 || x > dx + 3) {
				bloque(x, y + 1, fin - 1, mira(BloquesEstacion.BARANDILLA, Direction.SOUTH));
			}
		}
		hueco(dx, y, z0 - 1, dx + 2, z0 - 1);
		puertaFinal = puerta(BloquesEstacion.REJA, 0, dx, y, z0 - 1, Direction.NORTH, Direction.EAST, PISOS - 1, null, 3);
		caja(dx - 1, y, z0 - 6, dx + 3, y + 4, z0 - 2, s(Blocks.BLACK_CONCRETE));
		aire(dx, y + 1, z0 - 5, dx + 2, y + 3, z0 - 2);
		FINAL = new int[] {dx, z0 - 5, dx + 2, z0 - 2, y};
		bloque(dx + 1, y + 4, z0, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
		bloque(dx - 2, y + 3, z0, var(BloquesEstacion.SENAL, Direction.SOUTH, 2));
		// el fondo: maquinas, transformadores, bidones, cables colgando y luces rojas
		for (int z = z0 + 3; z <= z1 - 2; z += 7) {
			for (int x : new int[] {x0 + 1, x0 + 3, x1 - 3, x1 - 1}) {
				int k = azar(x, z, 31, 4);
				Block b = k == 0 ? BloquesEstacion.TRANSFORMADOR : k == 1 ? BloquesEstacion.BIDONES : BloquesEstacion.MAQUINA;
				bloque(x, pozo + 1, z, var(b, x < dx ? Direction.EAST : Direction.WEST, azar(x, z, 32, 4)));
			}
			bloque(x0, pozo + 4, z, var(BloquesEstacion.LUZ_ROJA, Direction.EAST, 0));
			bloque(x1, pozo + 4, z + 3, var(BloquesEstacion.LUZ_ROJA, Direction.WEST, 0));
			bloque(x0, y + 2, z + 1, var(BloquesEstacion.TUBERIAS, Direction.EAST, 2));
			bloque(x1, y + 2, z + 1, var(BloquesEstacion.TUBERIAS, Direction.WEST, 3));
			int cx = azar(z, 7, 33, 2) == 0 ? x0 + 2 : x1 - 2;
			for (int yy = techoPozo - 1; yy >= techoPozo - 5; yy--) {
				bloque(cx, yy, z + 2, var(BloquesEstacion.CABLES, Direction.NORTH, yy % 2));
			}
		}
	}

	/* ------------------------------------------------------------ consultas */

	/** La sala donde esta ese punto, o null. Las de despues (mas pequenas) mandan. */
	public static Sala sala(double x, double y, double z) {
		Sala r = null;
		for (Sala s : SALAS) {
			if (s.dentro(x, y, z)) {
				r = s;
			}
		}
		return r;
	}

	/** La primera sala con ese nombre, o null. */
	public static Sala sala(String nombre) {
		for (Sala s : SALAS) {
			if (s.nombre().equals(nombre)) {
				return s;
			}
		}
		return null;
	}
}
