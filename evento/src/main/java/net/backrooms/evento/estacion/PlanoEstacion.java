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
 * El trazado de la Estacion Electrica (Nivel 3), como la de Backrooms: Escape Together: tres pisos
 * que se suben uno detras de otro. Cada piso es un laberinto de pasillos de hormigon partido en tres
 * zonas por lineas de REJAS: una reja se abre poniendo un fusible en su caja (en la pared, a su lado)
 * y se cierra si alguien lo saca. Al final de cada piso, un pasillo largo de lado a lado y en un
 * costado la puerta de la CENTRAL: alli se ponen fusibles, cinta y cable; con todo puesto (y
 * corriente) la central se activa y se abre la escalera al piso siguiente. La central del tercer
 * piso da corriente a la PASARELA; al otro lado, la puerta del final, con una cadena (la cizalla).
 *
 *   PISO 1 (y = 40): se llega cayendo por el techo de la nave del generador (al sur). El laberinto
 *                    va de sur a norte; la central al este del pasillo del norte; la escalera sube
 *                    hacia el norte.
 *   PISO 2 (y = 54): se llega por el norte (sala de llegada y pasillos). Laberinto de norte a sur;
 *                    la central al oeste del pasillo del sur; la escalera sube hacia el sur.
 *   PISO 3 (y = 68): se llega por el sur. Laberinto de sur a norte; la central al este del pasillo
 *                    del norte, y al este de la central la nave de la pasarela (sobre un pozo).
 *
 * Los laberintos tienen semilla fija (el mismo mapa siempre). GeneradorEstacion copia las cajas;
 * las de despues mandan.
 */
public final class PlanoEstacion {
	public static final int PISOS = 3;
	/** El suelo de cada piso (se pisa en suelo + 1; el techo, en suelo + ALTO). */
	public static final int[] SUELOS = {40, 54, 68};
	public static final int SUELO = SUELOS[0];
	public static final int ALTO = 5;
	public static final int CELDA = 4;
	/** El laberinto de cada piso: MW x MH celdas desde (MX0, MZ0) (lineas de muro). */
	public static final int MW = 19;
	public static final int MH = 15;
	public static final int MX0 = -38;
	public static final int MZ0 = -30;
	public static final int MX1 = MX0 + MW * CELDA;
	public static final int MZ1 = MZ0 + MH * CELDA;
	/** El fondo del pozo de la pasarela y su techo. */
	public static final int POZO = 34;
	public static final int TECHO_POZO = SUELOS[2] + 14;
	public static final String[] PISO = {"PISO 1", "PISO 2", "PISO 3"};

	/** Tipos de punto de recogida (Interactivos.TIPO). */
	public static final int CABLE = 0;
	public static final int CIZALLA = 1;
	public static final int CINTA = 2;
	public static final int BIDON = 3;
	public static final int FUSIBLE = 4;

	/** Lo que pide la central de cada piso: {fusibles, cintas, cables}. */
	public static final int[][] NECESITA = {{2, 1, 0}, {2, 1, 1}, {3, 2, 2}};

	public interface Patron {
		BlockState en(int x, int y, int z);
	}

	public record Caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron patron) {
	}

	/** Una sala o zona, con su piso (para el staff y la entidad). */
	public record Sala(String nombre, int x0, int z0, int x1, int z1, int piso) {
		public boolean dentro(double x, double y, double z) {
			return x >= this.x0 && x < this.x1 + 1 && z >= this.z0 && z < this.z1 + 1 && PlanoEstacion.piso(y) == this.piso;
		}
	}

	/** Donde se coge algo: el bloque, el tipo (CABLE, CIZALLA...) y su piso. */
	public record Punto(int id, int x, int y, int z, int tipo, int piso) {
	}

	/**
	 * Una puerta de 3x3 (persiana o reja): el bloque de abajo a un lado, hacia donde se extiende
	 * (`lado`) y hacia donde mira; y = el suelo. Las rejas llevan su caja de fusible (`caja`).
	 */
	public record Puerta(int id, int x, int y, int z, Direction mira, Direction lado, int piso, int @org.jspecify.annotations.Nullable [] caja) {
		public int[] bloque(int parte) {
			int col = parte / 3;
			return new int[] {this.x + this.lado.getStepX() * col, this.y + 1 + parte % 3, this.z + this.lado.getStepZ() * col};
		}

		public boolean es(int bx, int by, int bz) {
			for (int k = 0; k < 9; k++) {
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
	/** Las rejas de los laberintos (con su caja de fusible). */
	public static final List<Puerta> REJAS = new ArrayList<>();
	/** Las persianas de las escaleras (id = piso), la de la pasarela (id 2) y la puerta encadenada. */
	public static final Puerta[] ESCALERAS = new Puerta[PISOS];
	public static Puerta puertaFinal;
	public static final Central[] CENTRALES = new Central[PISOS];
	/** Los focos de la pasarela, en el orden en que se encienden. */
	public static final List<int[]> FOCOS = new ArrayList<>();
	/** La boca del generador y su centro (de ahi sale su zumbido). */
	public static final int[] BOCA = {0, SUELO + 2, 44};
	public static final double[] GENERADOR = {0.5, SUELO + 2.5, 40.5};
	/** Por donde ronda la entidad: {x, z, piso}, el centro de las celdas de los laberintos. */
	public static final List<int[]> RONDA = new ArrayList<>();
	/** Donde se llega a cada piso (x, z, giro). El 0, cayendo por el techo de la nave. */
	public static final double[][] LLEGADAS = {{0.5, 46.5, 180.0}, {45.5, -63.5, 90.0}, {-44.5, 63.5, 270.0}};
	public static final double[] LLEGADA = LLEGADAS[0];
	/** La comida: {x, z, tipo de Plano.C_*, y del suelo}. */
	public static final List<int[]> COMIDA = new ArrayList<>();
	/** Detras de la puerta del final: quien llega aqui sale del nivel {x0, z0, x1, z1}. */
	public static final int[] FINAL = {114, -34, 117, -32};

	/** Limites del edificio, para el generador: salen de las cajas. */
	public static final int X0;
	public static final int X1;
	public static final int Z0;
	public static final int Z1;

	private PlanoEstacion() {
	}

	/** El piso de una altura (las escaleras cuentan como el de abajo hasta medio camino). */
	public static int piso(double y) {
		return y < SUELOS[1] - 1 ? 0 : y < SUELOS[2] - 1 ? 1 : 2;
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
	 * cada `luces`. Interior x0..x1, z0..z1; los muros alrededor.
	 */
	private static void sala(String nombre, int piso, int y, int x0, int z0, int x1, int z1, int alto, Patron suelo, int color, int luces) {
		if (nombre != null) {
			SALAS.add(new Sala(nombre, x0, z0, x1, z1, piso));
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
	private static Puerta puerta(Block b, int id, int x, int y, int z, Direction mira, Direction lado, int piso, int[] caja) {
		Puerta p = new Puerta(id, x, y, z, mira, lado, piso, caja);
		for (int k = 0; k < 9; k++) {
			int[] c = p.bloque(k);
			bloque(c[0], c[1], c[2], mira(b, mira).setValue(Interactivos.PARTE, k));
		}
		return p;
	}

	private static void punto(int x, int y, int z, int tipo, Direction mira, int piso) {
		PUNTOS.add(new Punto(PUNTOS.size(), x, y + 1, z, tipo, piso));
		bloque(x, y + 1, z, mira(BloquesEstacion.PUNTO, mira).setValue(Interactivos.TIPO, tipo));
	}

	private static void rotulo(int x, int y, int z, Direction mira, int color, int num) {
		bloque(x, y, z, mira(BloquesEstacion.ROTULO, mira).setValue(Interactivos.COLOR, color).setValue(Interactivos.Rotulo.NUM, num));
	}

	/* ------------------------------------------------------------- el plano */

	static {
		nave();
		for (int p = 0; p < PISOS; p++) {
			laberinto(p, p != 1);
		}
		final0();
		escalera(0);
		llegada1();
		final1();
		escalera(1);
		llegada2();
		pasarela();
		final2();
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

	/** La nave del generador (piso 1, al sur del laberinto): donde se cae por el techo. */
	private static void nave() {
		int y = SUELOS[0];
		sala("nave", 0, y, -14, 32, 14, 48, 10, (x, yy, z) -> Math.abs(x) <= 6 && z >= 36 && z <= 46 ? s(BloquesEstacion.CHAPA) : SUELO_H.en(x, yy, z), 0, 5);
		// al laberinto, por el norte (el muro de la nave y el del laberinto)
		hueco(-1, y, 30, 1, 31);
		// el generador: carcasa de 9 x 6 x 4 con radiadores, tapa y dos escapes hasta el techo
		caja(-4, y + 1, 38, 4, y + 4, 43, (x, yy, z) -> {
			if (yy == y + 4) {
				return s(BloquesEstacion.GENERADOR_TAPA);
			}
			if (Math.abs(x) == 4) {
				return mira(BloquesEstacion.GENERADOR_RADIADOR, x < 0 ? Direction.WEST : Direction.EAST);
			}
			return s(BloquesEstacion.GENERADOR);
		});
		caja(-3, y + 2, 39, 3, y + 3, 42, s(BloquesEstacion.MACIZO));
		bloque(BOCA[0], BOCA[1], BOCA[2], mira(BloquesEstacion.BOCA, Direction.SOUTH));
		for (int x : new int[] {-2, 2}) {
			caja(x, y + 5, 40, x, y + 9, 40, s(BloquesEstacion.ESCAPE));
		}
		bloque(-6, y + 1, 44, var(BloquesEstacion.BIDONES, Direction.SOUTH, 1));
		bloque(6, y + 1, 44, var(BloquesEstacion.BIDONES, Direction.SOUTH, 2));
		bloque(-1, y + 1, 45, var(BloquesEstacion.SENAL, Direction.SOUTH, 3));
		// el agujero por donde se cae (en el techo, al sur del generador)
		aire(0, y + 10, 46, 1, y + 10, 47);
		caja(-1, y + 11, 45, 2, y + 11, 48, s(BloquesEstacion.CHAPA));
		// taquillas, estanterias, maquinas y cuadros por las paredes
		for (int z = 34; z <= 46; z += 2) {
			bloque(-14, y + 1, z, var(BloquesEstacion.ARMARIO, Direction.EAST, 3));
			bloque(14, y + 1, z, var(BloquesEstacion.TRANSFORMADOR, Direction.WEST, Math.floorMod(z, 3)));
			bloque(-14, y + 3, z + 1, var(BloquesEstacion.CUADRO, Direction.EAST, 4 + Math.floorMod(z, 4)));
			bloque(14, y + 3, z + 1, var(BloquesEstacion.CUADRO, Direction.WEST, Math.floorMod(z, 4)));
			bloque(-14, y + 6, z, var(BloquesEstacion.TUBERIAS, Direction.EAST, 1));
			bloque(14, y + 6, z, var(BloquesEstacion.TUBERIAS, Direction.WEST, 1));
		}
		for (int z = 35; z <= 47; z += 6) {
			bloque(-15, y + 5, z, s(BloquesEstacion.LAMPARA));
			bloque(15, y + 5, z, s(BloquesEstacion.LAMPARA));
		}
		bloque(-9, y + 1, 32, var(BloquesEstacion.ESTANTERIA, Direction.SOUTH, 2));
		bloque(9, y + 1, 32, var(BloquesEstacion.ESTANTERIA, Direction.SOUTH, 0));
		bloque(-10, y + 1, 48, var(BloquesEstacion.MAQUINA, Direction.NORTH, 3));
		bloque(10, y + 1, 48, var(BloquesEstacion.MAQUINA, Direction.NORTH, 1));
		// el tablon de instrucciones (en la pared sur, mirando al norte) y los carteles
		bloque(-4, y + 2, 48, var(BloquesEstacion.SENAL, Direction.NORTH, 4));
		bloque(-3, y + 2, 48, var(BloquesEstacion.SENAL, Direction.NORTH, 5));
		bloque(-2, y + 2, 48, var(BloquesEstacion.SENAL, Direction.NORTH, 6));
		bloque(-3, y + 4, 32, var(BloquesEstacion.SENAL, Direction.SOUTH, 1));
		bloque(3, y + 4, 32, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
		rotulo(0, y + 5, 32, Direction.SOUTH, 0, 0);
		// los primeros bidones y algo de comer
		punto(-12, y, 47, BIDON, Direction.NORTH, 0);
		punto(12, y, 47, BIDON, Direction.NORTH, 0);
		COMIDA.add(new int[] {-12, 34, net.backrooms.evento.mundo.Plano.C_GALLETAS, y + 1});
		COMIDA.add(new int[] {12, 34, net.backrooms.evento.mundo.Plano.C_AGUA, y + 1});
	}

	/* ------------------------------------------------------------ los laberintos */

	/**
	 * El laberinto del piso `p`: MW x MH celdas, tres zonas (de la entrada a la salida) separadas por
	 * dos lineas de rejas (dos rejas en cada linea), dos trasteros cerrados con reja y lo que se coge
	 * en los callejones sin salida. `haciaNorte`: se entra por el sur y se sale por el norte.
	 */
	private static void laberinto(int p, boolean haciaNorte) {
		int y = SUELOS[p];
		int techo = y + ALTO;
		Random azar = new Random(7310L + p * 1931L);
		SALAS.add(new Sala("laberinto_" + (p + 1), MX0 + 1, MZ0 + 1, MX1 - 1, MZ1 - 1, p));
		int[] zona = new int[MH];
		for (int cz = 0; cz < MH; cz++) {
			int desdeEntrada = haciaNorte ? MH - 1 - cz : cz;
			zona[cz] = desdeEntrada < 5 ? 0 : desdeEntrada < 10 ? 1 : 2;
		}
		// cada zona, su laberinto (backtracking), y despues algunos pasos de mas
		boolean[][] este = new boolean[MW][MH];
		boolean[][] sur = new boolean[MW][MH];
		boolean[][] visto = new boolean[MW][MH];
		int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int z0 = 0; z0 < MH; z0 += 5) {
			int[] pila = new int[MW * MH * 2];
			int tope = 0;
			int sx = azar.nextInt(MW);
			pila[tope++] = sx;
			pila[tope++] = z0;
			visto[sx][z0] = true;
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
					int nx = cx + dir[o][0];
					int nz = cz + dir[o][1];
					if (nx < 0 || nz < 0 || nx >= MW || nz >= MH || visto[nx][nz] || zona[nz] != zona[cz]) {
						continue;
					}
					abrir(este, sur, cx, cz, nx, nz);
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
		}
		for (int cx = 0; cx < MW; cx++) {
			for (int cz = 0; cz < MH; cz++) {
				if (cx + 1 < MW && !este[cx][cz] && azar.nextInt(100) < 10) {
					este[cx][cz] = true;
				}
				if (cz + 1 < MH && zona[cz + 1] == zona[cz] && !sur[cx][cz] && azar.nextInt(100) < 10) {
					sur[cx][cz] = true;
				}
			}
		}
		// las dos lineas de rejas (entre las filas 4-5 y 9-10): dos columnas en cada una, separadas
		List<int[]> rejas = new ArrayList<>();   // {cx, cz de arriba, 0 = paso sur}
		for (int fila : new int[] {4, 9}) {
			int c1 = 1 + azar.nextInt(MW / 2 - 2);
			int c2 = MW / 2 + 1 + azar.nextInt(MW / 2 - 2);
			for (int c : new int[] {c1, c2}) {
				sur[c][fila] = true;
				rejas.add(new int[] {c, fila, 0});
			}
		}
		// la entrada y la salida, en la columna del medio
		int medio = MW / 2;
		int filaEntrada = haciaNorte ? MH - 1 : 0;
		int filaSalida = haciaNorte ? 0 : MH - 1;
		// los callejones sin salida de cada zona (sin contar entrada ni salida)
		List<List<int[]>> callejones = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			callejones.add(new ArrayList<>());
		}
		for (int cx = 0; cx < MW; cx++) {
			for (int cz = 0; cz < MH; cz++) {
				if (cx == medio && (cz == filaEntrada || cz == filaSalida)) {
					continue;
				}
				boolean[] ab = abiertos(este, sur, cx, cz);
				int n = 0;
				for (boolean b : ab) {
					n += b ? 1 : 0;
				}
				boolean junto = false;
				for (int[] r : rejas) {
					junto |= r[0] == cx && (r[1] == cz || r[1] + 1 == cz);
				}
				if (n == 1 && !junto) {
					callejones.get(zona[cz]).add(new int[] {cx, cz});
				}
			}
		}
		for (List<int[]> l : callejones) {
			Collections.shuffle(l, azar);
		}
		// dos trasteros (zonas 1 y 2): un callejon cuyo paso se cierra con una reja
		List<int[]> trasteros = new ArrayList<>();
		for (int zz = 1; zz <= 2; zz++) {
			List<int[]> l = callejones.get(zz);
			for (int i = 0; i < l.size(); i++) {
				int[] c = l.get(i);
				boolean[] ab = abiertos(este, sur, c[0], c[1]);
				// el paso: 0 norte, 1 este, 2 sur, 3 oeste
				int lado = ab[0] ? 0 : ab[1] ? 1 : ab[2] ? 2 : 3;
				trasteros.add(new int[] {c[0], c[1], lado});
				l.remove(i);
				break;
			}
		}

		int color = p;
		// todo macizo y luego se excava
		caja(MX0, y, MZ0, MX1, y, MZ1, SUELO_H);
		caja(MX0, y + 1, MZ0, MX1, techo - 1, MZ1, pared(color, y));
		for (int cx = 0; cx < MW; cx++) {
			for (int cz = 0; cz < MH; cz++) {
				int x0 = MX0 + 1 + cx * CELDA;
				int z0 = MZ0 + 1 + cz * CELDA;
				aire(x0, y + 1, z0, x0 + 2, techo - 1, z0 + 2);
				if (este[cx][cz]) {
					aire(x0 + 3, y + 1, z0, x0 + 3, techo - 1, z0 + 2);
				}
				if (sur[cx][cz]) {
					aire(x0, y + 1, z0 + 3, x0 + 2, techo - 1, z0 + 3);
				}
				RONDA.add(new int[] {x0 + 1, z0 + 1, p});
			}
		}
		// el techo: lamparas en una de cada dos celdas (alguna fundida), zonas enteras sin luz
		caja(MX0, techo, MZ0, MX1, techo, MZ1, (x, yy, z) -> {
			int cx = Math.floorDiv(x - MX0 - 1, CELDA);
			int cz = Math.floorDiv(z - MZ0 - 1, CELDA);
			int u = Math.floorMod(x - MX0 - 1, CELDA);
			int v = Math.floorMod(z - MZ0 - 1, CELDA);
			boolean oscura = azar(cx / 4 + p * 7, cz / 3, 41, 4) == 0;
			if (u == 1 && v == 1 && ((cx + cz) & 1) == 0 && !oscura) {
				return azar(cx, cz, 42 + p, 5) == 0 ? s(BloquesEstacion.LAMPARA_FUNDIDA) : s(BloquesEstacion.LAMPARA);
			}
			return u == 1 ? s(BloquesEstacion.TECHO_TUBOS) : s(BloquesEstacion.TECHO);
		});
		// la entrada y la salida (en el muro de fuera)
		int ex = MX0 + 1 + medio * CELDA;
		aire(ex, y + 1, haciaNorte ? MZ1 : MZ0, ex + 2, techo - 1, haciaNorte ? MZ1 : MZ0);
		aire(ex, y + 1, haciaNorte ? MZ0 : MZ1, ex + 2, techo - 1, haciaNorte ? MZ0 : MZ1);
		// las rejas: en el hueco (3 de alto: arriba queda el dintel) y su caja en el muro de al lado
		for (int[] r : rejas) {
			int x0 = MX0 + 1 + r[0] * CELDA;
			int zm = MZ0 + 1 + r[1] * CELDA + 3;
			caja(x0, techo - 1, zm, x0 + 2, techo - 1, zm, pared(color, y));
			reja(x0, y, zm, Direction.EAST, p, new int[] {x0 - 1, y + 2, zm});
			rotulo(x0 - 1, y + 4, zm + (haciaNorte ? 1 : -1), haciaNorte ? Direction.SOUTH : Direction.NORTH, color, 10 + REJAS.size() % 6);
		}
		for (int[] t : trasteros) {
			int x0 = MX0 + 1 + t[0] * CELDA;
			int z0 = MZ0 + 1 + t[1] * CELDA;
			switch (t[2]) {
				case 0 -> {
					caja(x0, techo - 1, z0 - 1, x0 + 2, techo - 1, z0 - 1, pared(color, y));
					reja(x0, y, z0 - 1, Direction.EAST, p, new int[] {x0 - 1, y + 2, z0 - 1});
				}
				case 2 -> {
					caja(x0, techo - 1, z0 + 3, x0 + 2, techo - 1, z0 + 3, pared(color, y));
					reja(x0, y, z0 + 3, Direction.EAST, p, new int[] {x0 - 1, y + 2, z0 + 3});
				}
				case 1 -> {
					caja(x0 + 3, techo - 1, z0, x0 + 3, techo - 1, z0 + 2, pared(color, y));
					reja(x0 + 3, y, z0, Direction.SOUTH, p, new int[] {x0 + 3, y + 2, z0 - 1});
				}
				default -> {
					caja(x0 - 1, techo - 1, z0, x0 - 1, techo - 1, z0 + 2, pared(color, y));
					reja(x0 - 1, y, z0, Direction.SOUTH, p, new int[] {x0 - 1, y + 2, z0 - 1});
				}
			}
			// dentro del trastero: estanterias y lo que se guarda
			caja(x0, y, z0, x0 + 2, y, z0 + 2, s(BloquesEstacion.CHAPA));
		}
		// lo que se coge, por zonas: fusibles para las rejas y la central, gasolina, cinta, cable y la cizalla
		int[][][] reparto = {
			{{FUSIBLE, FUSIBLE, BIDON, FUSIBLE}, {FUSIBLE, FUSIBLE, BIDON, CINTA}, {FUSIBLE, FUSIBLE, CINTA, BIDON}},
			{{FUSIBLE, FUSIBLE, BIDON, FUSIBLE}, {FUSIBLE, FUSIBLE, CABLE, BIDON}, {FUSIBLE, CINTA, CABLE, FUSIBLE}},
			{{FUSIBLE, FUSIBLE, BIDON, CINTA}, {FUSIBLE, FUSIBLE, CABLE, BIDON}, {FUSIBLE, FUSIBLE, CABLE, CINTA}}
		};
		int[][] enTrasteros = {{CINTA, FUSIBLE}, {CABLE, CINTA}, {CIZALLA, CABLE}};
		Direction[] lados = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		boolean[][] usado = new boolean[MW][MH];
		for (int i = 0; i < trasteros.size(); i++) {
			int[] t = trasteros.get(i);
			usado[t[0]][t[1]] = true;
			int x0 = MX0 + 1 + t[0] * CELDA;
			int z0 = MZ0 + 1 + t[1] * CELDA;
			// el punto contra la pared del fondo (la contraria al paso)
			int fondo = (t[2] + 2) % 4;
			int bx = x0 + 1 + (fondo == 1 ? 1 : fondo == 3 ? -1 : 0);
			int bz = z0 + 1 + (fondo == 2 ? 1 : fondo == 0 ? -1 : 0);
			punto(bx, y, bz, enTrasteros[p][i], lados[t[2]], p);
			int ex2 = x0 + 1 + (fondo % 2 == 0 ? -1 : 0);
			int ez2 = z0 + 1 + (fondo % 2 == 1 ? -1 : 0);
			bloque(ex2, y + 1, ez2, var(BloquesEstacion.ESTANTERIA, lados[t[2]], i));
		}
		for (int zz = 0; zz < 3; zz++) {
			List<int[]> l = callejones.get(zz);
			int[] cosas = reparto[p][zz];
			for (int i = 0; i < cosas.length; i++) {
				int[] c = i < l.size() ? l.get(i) : celdaLibre(azar, zona, zz, usado);
				usado[c[0]][c[1]] = true;
				int[] b = pegado(c[0], c[1], este, sur, azar);
				punto(b[0], y, b[1], cosas[i], lados[b[2]], p);
			}
		}
		// por los pasillos: tuberias, cuadros, senales, rotulos, bidones vacios, cables y luces rojas
		for (int cx = 0; cx < MW; cx++) {
			for (int cz = 0; cz < MH; cz++) {
				if (usado[cx][cz]) {
					continue;
				}
				boolean[] ab = abiertos(este, sur, cx, cz);
				int ladoPared = -1;
				for (int i = 0; i < 4; i++) {
					int l = (azar(cx, cz, 50 + p, 4) + i) % 4;
					if (!ab[l]) {
						ladoPared = l;
						break;
					}
				}
				if (ladoPared < 0) {
					continue;
				}
				int x0 = MX0 + 1 + cx * CELDA;
				int z0 = MZ0 + 1 + cz * CELDA;
				int bx = x0 + 1 + (ladoPared == 1 ? 1 : ladoPared == 3 ? -1 : 0);
				int bz = z0 + 1 + (ladoPared == 2 ? 1 : ladoPared == 0 ? -1 : 0);
				Direction dentro = lados[(ladoPared + 2) % 4];
				int r = azar(cx, cz, 60 + p, 20);
				if (r < 4) {
					bloque(bx, y + 2, bz, var(BloquesEstacion.TUBERIAS, dentro, r));
				} else if (r < 7) {
					bloque(bx, y + 2, bz, var(BloquesEstacion.CUADRO, dentro, r));
				} else if (r < 8) {
					bloque(bx, y + 2, bz, var(BloquesEstacion.SENAL, dentro, r == 7 ? 0 : 7));
				} else if (r < 10) {
					rotulo(bx, y + 3, bz, dentro, color, 1 + Math.floorMod(cx / 3 + cz / 3 * 5, 9));
				} else if (r < 11) {
					bloque(bx, y + 1, bz, var(BloquesEstacion.BIDONES, dentro, 2));
				} else if (r < 12) {
					bloque(x0 + 1, techo - 1, z0 + 1, var(BloquesEstacion.CABLES, Direction.NORTH, 0));
				} else if (r < 13) {
					bloque(bx, y + 3, bz, var(BloquesEstacion.LUZ_ROJA, dentro, 0));
				} else if (r < 14 && cz % 5 == 2) {
					COMIDA.add(new int[] {x0 + 1, z0 + 1, azar(cx, cz, 70, 2) == 0 ? net.backrooms.evento.mundo.Plano.C_AGUA : net.backrooms.evento.mundo.Plano.C_GALLETAS, y + 1});
				}
			}
		}
	}

	/** Donde poner algo pegado a una pared de la celda (una sin paso): {x, z, hacia donde mira (0 N, 1 E, 2 S, 3 O)}. */
	private static int[] pegado(int cx, int cz, boolean[][] este, boolean[][] sur, Random azar) {
		boolean[] ab = abiertos(este, sur, cx, cz);
		int x0 = MX0 + 1 + cx * CELDA;
		int z0 = MZ0 + 1 + cz * CELDA;
		for (int k = 0; k < 4; k++) {
			int l = (azar.nextInt(4) + k) % 4;
			if (ab[l]) {
				continue;
			}
			int bx = x0 + 1 + (l == 1 ? 1 : l == 3 ? -1 : 0);
			int bz = z0 + 1 + (l == 2 ? 1 : l == 0 ? -1 : 0);
			return new int[] {bx, bz, (l + 2) % 4};
		}
		// sin pared libre: en medio
		return new int[] {x0 + 1, z0 + 1, 0};
	}

	private static int[] celdaLibre(Random azar, int[] zona, int zz, boolean[][] usado) {
		for (int t = 0; t < 200; t++) {
			int cx = azar.nextInt(MW);
			int cz = azar.nextInt(MH);
			if (zona[cz] == zz && !usado[cx][cz] && cx != MW / 2) {
				return new int[] {cx, cz};
			}
		}
		return new int[] {0, 0};
	}

	/** Los pasos de una celda: norte, este, sur, oeste. */
	private static boolean[] abiertos(boolean[][] este, boolean[][] sur, int cx, int cz) {
		return new boolean[] {cz > 0 && sur[cx][cz - 1], cx + 1 < MW && este[cx][cz], cz + 1 < MH && sur[cx][cz], cx > 0 && este[cx - 1][cz]};
	}

	private static void reja(int x, int y, int z, Direction lado, int piso, int[] caja) {
		Direction mira = lado == Direction.EAST ? Direction.NORTH : Direction.EAST;
		REJAS.add(puerta(BloquesEstacion.REJA, REJAS.size(), x, y, z, mira, lado, piso, caja));
		bloque(caja[0], caja[1], caja[2], s(BloquesEstacion.CAJA_REJA));
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

	/* ------------------------------------------------------------ los finales de cada piso */

	/**
	 * El pasillo largo del final de un piso (de lado a lado, pegado al muro del laberinto) y, en un
	 * costado, la sala de la central. `norte`: el pasillo esta al norte del laberinto. `este`: la
	 * central esta al este.
	 */
	private static void pasilloFinal(int p, boolean norte, boolean este) {
		int y = SUELOS[p];
		int z0 = norte ? MZ0 - 3 : MZ1 + 1;
		sala("pasillo_final_" + (p + 1), p, y, MX0 + 1, z0, MX1 - 1, z0 + 2, ALTO, SUELO_H, p, 6);
		// la salida del laberinto (en medio)
		int ex = MX0 + 1 + MW / 2 * CELDA;
		aire(ex, y + 1, norte ? MZ0 : MZ1, ex + 2, y + ALTO - 1, norte ? MZ0 : MZ1);
		Direction alPasillo = norte ? Direction.SOUTH : Direction.NORTH;
		// carteles y cosas por el pasillo; al otro costado, una puerta tapiada
		int muro = norte ? z0 - 1 : z0 + 3;
		for (int x = MX0 + 4; x <= MX1 - 4; x += 5) {
			int r = azar(x, p, 80, 5);
			if (r == 0) {
				bloque(x, y + 2, muro, var(BloquesEstacion.CUADRO, alPasillo, 4 + Math.floorMod(x, 4)));
			} else if (r == 1) {
				bloque(x, y + 2, muro, var(BloquesEstacion.TUBERIAS, alPasillo, 2));
			} else if (r == 2) {
				bloque(x, y + 3, muro, var(BloquesEstacion.LUZ_ROJA, alPasillo, 0));
			}
		}
		int tapiada = este ? MX0 : MX1;
		caja(tapiada, y + 1, z0, tapiada, y + 3, z0 + 2, s(BloquesEstacion.HORMIGON));
		bloque(este ? MX0 + 1 : MX1 - 1, y + 1, z0 + 1, var(BloquesEstacion.BIDONES, este ? Direction.EAST : Direction.WEST, 1));
		bloque(este ? MX0 + 1 : MX1 - 1, y + 3, z0 + 1, var(BloquesEstacion.SENAL, este ? Direction.EAST : Direction.WEST, 7));
	}

	/**
	 * La sala de la central de un piso: interior cx0..cx1, cz0..cz1, la puerta al pasillo final y el
	 * cuadro de la central en la pared de `pared` (mirando hacia `mira`).
	 */
	private static void salaCentral(int p, int cx0, int cz0, int cx1, int cz1, int[] puerta, int[] cuadro, Direction mira) {
		int y = SUELOS[p];
		sala("central_" + (p + 1), p, y, cx0, cz0, cx1, cz1, ALTO + 1, (x, yy, z) -> s(BloquesEstacion.CHAPA), p, 4);
		hueco(puerta[0], y, puerta[1], puerta[2], puerta[3]);
		// el cuadro de la central y a sus lados cuadros con relojes; encima, la pantalla
		int x = cuadro[0];
		int z = cuadro[1];
		bloque(x, y + 2, z, mira(BloquesEstacion.PANEL, mira).setValue(Interactivos.TIPO, p));
		Direction lado = mira.getClockWise();
		for (int k = 1; k <= 3; k++) {
			bloque(x + lado.getStepX() * k, y + 2, z + lado.getStepZ() * k, var(BloquesEstacion.CUADRO, mira, 4 + k));
			bloque(x - lado.getStepX() * k, y + 2, z - lado.getStepZ() * k, var(BloquesEstacion.CUADRO, mira, k));
			bloque(x + lado.getStepX() * k, y + 1, z + lado.getStepZ() * k, var(BloquesEstacion.ARMARIO, mira, k));
			bloque(x - lado.getStepX() * k, y + 1, z - lado.getStepZ() * k, var(BloquesEstacion.ARMARIO, mira, k + 1));
		}
		bloque(x - lado.getStepX() * 4, y + 3, z - lado.getStepZ() * 4, var(BloquesEstacion.SENAL, mira, 0));
		bloque(x + lado.getStepX() * 4, y + 3, z + lado.getStepZ() * 4, var(BloquesEstacion.SENAL, mira, 0));
		// la pantalla: 0,05 por delante del muro, a la altura de los ojos y un poco mas
		double px = x + 0.5 - mira.getStepX() * 0.43;
		double pz = z + 0.5 - mira.getStepZ() * 0.43;
		CENTRALES[p] = new Central(p, x, y + 2, z, new double[] {px, y + 3.9, pz, mira.toYRot()});
		rotulo(x, y + 5, z, mira, p, 9);
		// consolas en medio, mirando al cuadro
		int mx = (cx0 + cx1) / 2;
		int mz = (cz0 + cz1) / 2;
		for (int k = -2; k <= 2; k += 2) {
			bloque(mx + lado.getStepX() * k, y + 1, mz + lado.getStepZ() * k, var(BloquesEstacion.CONSOLA, mira, Math.floorMod(k, 4)));
		}
		COMIDA.add(new int[] {mx + mira.getStepX() * 3, mz + mira.getStepZ() * 3, net.backrooms.evento.mundo.Plano.C_AGUA, y + 1});
	}

	/** Piso 1: pasillo final al norte y la central al este. */
	private static void final0() {
		pasilloFinal(0, true, true);
		salaCentral(0, 40, -40, 50, -26, new int[] {38, -33, 39, -31}, new int[] {50, -33}, Direction.WEST);
		ESCALERAS[0] = puerta(BloquesEstacion.PERSIANA, 0, 44, SUELOS[0], -41, Direction.NORTH, Direction.EAST, 0, null);
	}

	/** Piso 2: pasillo final al sur y la central al oeste. */
	private static void final1() {
		pasilloFinal(1, false, false);
		salaCentral(1, -50, 26, -40, 40, new int[] {-39, 31, -38, 33}, new int[] {-50, 33}, Direction.EAST);
		ESCALERAS[1] = puerta(BloquesEstacion.PERSIANA, 1, -46, SUELOS[1], 41, Direction.SOUTH, Direction.EAST, 1, null);
	}

	/** Piso 3: pasillo final al norte, la central al este y, al este de la central, la pasarela. */
	private static void final2() {
		pasilloFinal(2, true, true);
		salaCentral(2, 40, -40, 50, -26, new int[] {38, -33, 39, -31}, new int[] {45, -40}, Direction.SOUTH);
		// la persiana de la pasarela, en la pared este de la central (y el muro de la nave de la pasarela)
		hueco(51, SUELOS[2], -34, 52, -32);
		ESCALERAS[2] = puerta(BloquesEstacion.PERSIANA, 2, 51, SUELOS[2], -34, Direction.EAST, Direction.SOUTH, 2, null);
	}

	/**
	 * La escalera del piso p al p + 1: de la persiana de la central sube recta 13 peldanos hasta el
	 * rellano del piso de arriba, y de ahi a su sala de llegada.
	 */
	private static void escalera(int p) {
		int y = SUELOS[p];
		int arriba = SUELOS[p + 1];
		boolean norte = p == 0;
		int x0 = norte ? 44 : -46;
		int s = norte ? -1 : 1;
		int z0 = norte ? -42 : 42;
		int zFin = z0 + s * 14;
		// el hueco: de la persiana hasta el rellano, del suelo de abajo al techo de arriba
		caja(x0 - 1, y, Math.min(z0, zFin), x0 + 3, arriba + ALTO, Math.max(z0, zFin), s(BloquesEstacion.HORMIGON));
		aire(x0, y + 1, Math.min(z0, zFin), x0 + 2, arriba + ALTO - 1, Math.max(z0, zFin));
		Direction sube = norte ? Direction.NORTH : Direction.SOUTH;
		for (int k = 0; k < 13; k++) {
			int z = z0 + s * k;
			caja(x0, y + 1, z, x0 + 2, y + k, z, s(BloquesEstacion.HORMIGON));
			caja(x0, y + 1 + k, z, x0 + 2, y + 1 + k, z, Blocks.POLISHED_ANDESITE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, sube));
		}
		caja(x0, y + 1, z0 + s * 13, x0 + 2, arriba, zFin, s(BloquesEstacion.CHAPA));
		// luces en las paredes del hueco y un rotulo con el piso
		for (int k = 2; k < 13; k += 4) {
			bloque(x0 - 1, y + k + 3, z0 + s * k, s(BloquesEstacion.LAMPARA));
			bloque(x0 + 3, y + k + 3, z0 + s * k, s(BloquesEstacion.LAMPARA));
		}
		rotulo(x0 + 1, arriba + 4, zFin + s, norte ? Direction.SOUTH : Direction.NORTH, p + 1, 0);
	}

	/** Piso 2: la sala de llegada (al norte, sobre la escalera) y los pasillos hasta el laberinto. */
	private static void llegada1() {
		int y = SUELOS[1];
		// los pasillos: del medio del laberinto al norte y luego al este, hasta la sala
		sala(null, 1, y, -1, -61, 1, -31, ALTO, SUELO_H, 1, 4);
		sala(null, 1, y, -1, -64, 38, -62, ALTO, SUELO_H, 1, 4);
		aire(-1, y + 1, -62, 1, y + ALTO - 1, -61);
		hueco(-1, y, MZ0, 1, MZ0);
		sala("llegada_2", 1, y, 40, -69, 50, -58, ALTO + 1, SUELO_H, 1, 4);
		hueco(39, y, -64, 39, -62);
		hueco(44, y, -57, 46, -57);
		llegadaDecorado(1, 40, -69, 50, -58, Direction.WEST);
	}

	/** Piso 3: la sala de llegada (al sur, sobre la escalera) y los pasillos hasta el laberinto. */
	private static void llegada2() {
		int y = SUELOS[2];
		sala(null, 2, y, -1, 31, 1, 61, ALTO, SUELO_H, 2, 4);
		sala(null, 2, y, -38, 62, 1, 64, ALTO, SUELO_H, 2, 4);
		aire(-1, y + 1, 61, 1, y + ALTO - 1, 62);
		hueco(-1, y, MZ1, 1, MZ1);
		sala("llegada_3", 2, y, -50, 58, -40, 69, ALTO + 1, SUELO_H, 2, 4);
		hueco(-39, y, 62, -39, 64);
		hueco(-46, y, 57, -44, 57);
		llegadaDecorado(2, -50, 58, -40, 69, Direction.EAST);
	}

	private static void llegadaDecorado(int p, int x0, int z0, int x1, int z1, Direction hacia) {
		int y = SUELOS[p];
		for (int z = z0 + 1; z <= z1 - 1; z += 3) {
			bloque(hacia == Direction.WEST ? x1 : x0, y + 1, z, var(BloquesEstacion.ARMARIO, hacia, Math.floorMod(z, 4)));
			bloque(hacia == Direction.WEST ? x1 : x0, y + 3, z + 1, var(BloquesEstacion.CUADRO, hacia, 4 + Math.floorMod(z, 4)));
		}
		punto((x0 + x1) / 2, y, z0 + 1, BIDON, Direction.SOUTH, p);
		COMIDA.add(new int[] {(x0 + x1) / 2 + 2, (z0 + z1) / 2, net.backrooms.evento.mundo.Plano.C_PIZZA, y + 1});
		bloque((x0 + x1) / 2, y + 3, z0, var(BloquesEstacion.SENAL, Direction.SOUTH, 2));
	}

	/** La nave de la pasarela: un pozo hondo y la pasarela de rejilla hasta la puerta encadenada. */
	private static void pasarela() {
		int y = SUELOS[2];
		int x0 = 53;
		int x1 = 112;
		int z0 = -45;
		int z1 = -21;
		SALAS.add(new Sala("pasarela", x0, z0, x1, z1, 2));
		Patron muro = (x, yy, z) -> yy < y ? s(BloquesEstacion.HORMIGON) : pared(-1, y).en(x, yy, z);
		caja(x0 - 1, POZO, z0 - 1, x1 + 1, POZO, z1 + 1, s(BloquesEstacion.SUELO_MANCHADO));
		caja(x0 - 1, POZO + 1, z0 - 1, x1 + 1, TECHO_POZO - 1, z0 - 1, muro);
		caja(x0 - 1, POZO + 1, z1 + 1, x1 + 1, TECHO_POZO - 1, z1 + 1, muro);
		caja(x0 - 1, POZO + 1, z0, x0 - 1, TECHO_POZO - 1, z1, muro);
		caja(x1 + 1, POZO + 1, z0, x1 + 1, TECHO_POZO - 1, z1, muro);
		aire(x0, POZO + 1, z0, x1, TECHO_POZO - 1, z1);
		techo(x0, z0, x1, z1, TECHO_POZO, 10);
		// la pasarela de rejilla, su barandilla y los focos apagados a los lados
		caja(x0, y, -34, 106, y, -32, s(BloquesEstacion.REJILLA));
		for (int x = x0; x <= 106; x++) {
			bloque(x, y + 1, -35, mira(BloquesEstacion.BARANDILLA, Direction.SOUTH));
			bloque(x, y + 1, -31, mira(BloquesEstacion.BARANDILLA, Direction.NORTH));
			bloque(x, y, -35, s(BloquesEstacion.CHAPA));
			bloque(x, y, -31, s(BloquesEstacion.CHAPA));
			if (Math.floorMod(x - x0, 5) == 2) {
				bloque(x, y, -35, BloquesEstacion.FOCO.defaultBlockState());
				bloque(x, y, -31, BloquesEstacion.FOCO.defaultBlockState());
				FOCOS.add(new int[] {x, y, -35});
				FOCOS.add(new int[] {x, y, -31});
			}
		}
		for (int x = x0 + 6; x <= 104; x += 12) {
			caja(x, POZO + 1, -35, x, y - 1, -35, s(BloquesEstacion.CHAPA));
			caja(x, POZO + 1, -31, x, y - 1, -31, s(BloquesEstacion.CHAPA));
		}
		// la plataforma del final y la puerta encadenada (al este); detras, oscuro
		caja(107, y, -38, x1, y, -28, s(BloquesEstacion.CHAPA));
		for (int z = -38; z <= -28; z++) {
			if (z < -35 || z > -31) {
				bloque(107, y + 1, z, mira(BloquesEstacion.BARANDILLA, Direction.WEST));
			}
		}
		hueco(x1 + 1, y, -34, x1 + 1, -32);
		puertaFinal = puerta(BloquesEstacion.REJA, 0, x1 + 1, y, -34, Direction.EAST, Direction.SOUTH, 2, null);
		caja(x1 + 2, y, -36, x1 + 6, y + 4, -30, s(Blocks.BLACK_CONCRETE));
		aire(x1 + 2, y + 1, -34, x1 + 5, y + 3, -32);
		bloque(x1, y + 4, -33, var(BloquesEstacion.LUZ_ROJA, Direction.WEST, 0));
		bloque(x1, y + 3, -36, var(BloquesEstacion.SENAL, Direction.WEST, 2));
		// el fondo: maquinas, transformadores, bidones, cables colgando y luces rojas
		for (int x = x0 + 4; x <= x1 - 2; x += 7) {
			for (int z : new int[] {-43, -40, -26, -23}) {
				int k = azar(x, z, 31, 4);
				Block b = k == 0 ? BloquesEstacion.TRANSFORMADOR : k == 1 ? BloquesEstacion.BIDONES : BloquesEstacion.MAQUINA;
				bloque(x, POZO + 1, z, var(b, z < -33 ? Direction.SOUTH : Direction.NORTH, azar(x, z, 32, 4)));
			}
			bloque(x, POZO + 4, z0, var(BloquesEstacion.LUZ_ROJA, Direction.SOUTH, 0));
			bloque(x + 3, POZO + 4, z1, var(BloquesEstacion.LUZ_ROJA, Direction.NORTH, 0));
			bloque(x + 1, y + 2, z0, var(BloquesEstacion.TUBERIAS, Direction.SOUTH, 2));
			bloque(x + 1, y + 2, z1, var(BloquesEstacion.TUBERIAS, Direction.NORTH, 3));
			int cz = azar(x, 7, 33, 2) == 0 ? -40 : -26;
			for (int yy = TECHO_POZO - 1; yy >= TECHO_POZO - 6; yy--) {
				bloque(x + 2, yy, cz, var(BloquesEstacion.CABLES, Direction.NORTH, yy % 2));
			}
		}
	}

	/* ------------------------------------------------------------ consultas */

	/** La sala (por nombre) donde esta ese punto, o null. Las de despues (mas pequenas) mandan. */
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
