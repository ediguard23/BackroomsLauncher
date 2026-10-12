package net.backrooms.evento.piscinas;

import java.util.Arrays;

/**
 * El trazado de las Piscinas (Nivel 37): salas de azulejo blanco conectadas sin
 * fin, con agua a distintas profundidades. No depende de Minecraft: dice que
 * material va en cada bloque de una columna (x, z), y GeneradorPiscinas lo pone.
 *
 * El mundo se parte en celdas de 24x24. La pared entre dos celdas es la linea
 * u = 0 (o v = 0) de la de mas al este (o al sur), de un bloque de grueso, y casi
 * siempre tiene una puerta en el medio. Cada celda es un tipo de sala, uno por
 * cada imagen de referencia del organizador:
 *
 *   PASILLOS  pasillos de azulejo de 3 de ancho, a veces con un canal de agua (lo que une todo)
 *   SALA      piscina de calles con columnas cuadradas y paneles en el techo
 *   INUNDADA  sala alta con el suelo bajo un palmo de agua, columnas redondas y claraboyas
 *   BOVEDAS   bosque de pilares y bovedas de arista sobre una piscina de borde curvo
 *   TUNEL     tubo redondo con anillos, encharcado, que se cruza con otros
 *   CARACOL   sala redonda con una escalera de caracol alrededor de una columna y un balcon
 *   TERRAZAS  terrazas curvas a dos alturas, arcos oscuros y un canal serpenteante
 *
 * Una de cada tres manzanas de 2x2 celdas es una sala grande (SALA, INUNDADA o
 * BOVEDAS de 48x48) sin paredes por dentro. CARACOL y TERRAZAS tienen planta alta
 * (se pisa en y = 49) y, si dos quedan juntas, se pasa de una a otra por arriba.
 *
 * El suelo seco esta en y = 40 (se pisa en 41) y el agua llega hasta y = 40, un
 * pelo por debajo del borde, como en una piscina de verdad. Ninguna columna deja
 * aire a la altura del agua al lado de un bloque de agua: no se derrama nunca.
 * Nada baja de y = 34: a menos de 32 bloques del fondo del mundo el cliente
 * oscurece la niebla (como en el Nivel 0).
 */
public final class PlanoPiscinas {
	/** Lado de una celda. */
	public static final int C = 24;
	/** Columna del medio de la celda (centro de las puertas). */
	public static final int M = 12;
	/** Primera capa que se escribe. */
	public static final int Y0 = 32;
	/** Bloque del suelo seco: se pisa en SUELO + 1. */
	public static final int SUELO = 40;
	public static final int PISO = SUELO + 1;
	/** Suelo de la planta alta: se pisa en PLANTA + 1. */
	public static final int PLANTA = 48;
	/** Ultima capa que puede tener algo. */
	public static final int TOPE = 59;
	public static final int ALTO = TOPE - Y0 + 1;

	/* ------------------------------------------------------- materiales */

	public static final byte AIRE = 0;
	public static final byte AZULEJO = 1;
	/** Azulejo de suelo o pared que el generador ensucia o rompe al azar. */
	public static final byte PARED = 2;
	public static final byte AZUL = 3;
	public static final byte FONDO = 4;
	public static final byte FRANJA = 5;
	public static final byte BORDE = 6;
	public static final byte LUZ = 7;
	public static final byte CLARABOYA = 8;
	public static final byte FOCO = 9;
	public static final byte REJILLA = 10;
	public static final byte AGUA = 11;
	/** Losa abajo (seca). */
	public static final byte LOSA = 12;
	public static final byte LOSA_ARRIBA = 13;
	/** Losa abajo anegada: el agua le llega por encima. */
	public static final byte LOSA_AGUA = 14;
	/** Luz invisible (bloque de luz de Minecraft) a 15. */
	public static final byte LUZ_OCULTA = 15;
	/** Luz invisible a 10, para las salas en penumbra. */
	public static final byte LUZ_TENUE = 16;
	/** Escalones (subiendo hacia N, E, S, O). */
	public static final byte ESCALON = 17;
	/** Escalones del reves, para los techos curvos (el escalon, abajo, hacia N, E, S, O). */
	public static final byte ESCALON_ARRIBA = 21;

	/* ------------------------------------------------------------ tipos */

	public static final int PASILLOS = 0;
	public static final int SALA = 1;
	public static final int INUNDADA = 2;
	public static final int BOVEDAS = 3;
	public static final int TUNEL = 4;
	public static final int CARACOL = 5;
	public static final int TERRAZAS = 6;
	public static final String[] TIPOS = {"pasillos", "piscina", "sala inundada", "bovedas", "tunel", "caracol", "terrazas"};
	/** Bloque del techo de cada tipo (la pared llega uno mas arriba). */
	private static final int[] TECHO = {45, 49, 52, 50, 48, 57, 57};

	public static final int NORTE = 0;
	public static final int ESTE = 1;
	public static final int SUR = 2;
	public static final int OESTE = 3;
	private static final int[] DX = {0, 1, 0, -1};
	private static final int[] DZ = {-1, 0, 1, 0};

	/**
	 * Una sala: su tipo y su rectangulo (x0, z0 son las lineas de pared del oeste y del
	 * norte; x1, z1 las del este y el sur, que ya son de la sala de al lado).
	 *
	 * @param giro   cuartos de vuelta del dibujo (CARACOL y TERRAZAS se dibujan en un marco y se giran)
	 * @param oscura sin luces (solo los focos de la piscina)
	 */
	public record Sala(int tipo, int x0, int z0, int x1, int z1, int giro, boolean oscura, int var) {
		public int ancho() {
			return this.x1 - this.x0;
		}

		public int largo() {
			return this.z1 - this.z0;
		}
	}

	private final long semilla;

	public PlanoPiscinas(long semilla) {
		this.semilla = semilla;
	}

	/* ------------------------------------------------------------- azar */

	private int hash(int a, int b, int c) {
		long h = this.semilla ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (c * 0x165667B19E3779F9L);
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 32;
		return (int) h & 0x7FFFFFFF;
	}

	private double azar(int a, int b, int c) {
		return this.hash(a, b, c) / 2147483648.0;
	}

	/* ------------------------------------------------------------ salas */

	/** Tipo de la sala grande de esa manzana de 2x2 celdas, o -1 si son cuatro salas sueltas. */
	private int grande(int mx, int mz) {
		if (this.azar(mx, mz, 11) >= 0.34) {
			return -1;
		}
		int k = this.hash(mx, mz, 12) % 3;
		return k == 0 ? SALA : k == 1 ? INUNDADA : BOVEDAS;
	}

	public Sala sala(int cx, int cz) {
		int mx = Math.floorDiv(cx, 2);
		int mz = Math.floorDiv(cz, 2);
		int g = this.grande(mx, mz);
		if (g >= 0) {
			return new Sala(g, mx * 2 * C, mz * 2 * C, (mx * 2 + 2) * C, (mz * 2 + 2) * C, 0,
				g == BOVEDAS && this.azar(mx, mz, 13) < 0.3, this.hash(mx, mz, 14));
		}
		double r = this.azar(cx, cz, 21);
		int tipo = r < 0.27 ? PASILLOS : r < 0.41 ? SALA : r < 0.51 ? INUNDADA : r < 0.65 ? BOVEDAS : r < 0.80 ? TUNEL : r < 0.90 ? CARACOL : TERRAZAS;
		boolean oscura = (tipo == PASILLOS || tipo == TUNEL || tipo == BOVEDAS) && this.azar(cx, cz, 22) < 0.14;
		return new Sala(tipo, cx * C, cz * C, (cx + 1) * C, (cz + 1) * C, this.hash(cx, cz, 23) % 4, oscura, this.hash(cx, cz, 24));
	}

	private static int techo(Sala s) {
		return TECHO[s.tipo()];
	}

	private static boolean esSala(Sala s) {
		return s.tipo() == SALA || s.tipo() == INUNDADA || s.tipo() == BOVEDAS;
	}

	/* ----------------------------------------------- paredes y puertas */

	/** La pared x = cx*C entre (cx-1, cz) y (cx, cz) no existe: es la misma sala grande. */
	private boolean fusionV(int cx, int cz) {
		return Math.floorMod(cx, 2) == 1 && this.grande(Math.floorDiv(cx, 2), Math.floorDiv(cz, 2)) >= 0;
	}

	private boolean fusionH(int cx, int cz) {
		return Math.floorMod(cz, 2) == 1 && this.grande(Math.floorDiv(cx, 2), Math.floorDiv(cz, 2)) >= 0;
	}

	/** Cada celda tiene al menos una puerta: esta. */
	private int forzada(int cx, int cz) {
		return this.hash(cx, cz, 31) % 4;
	}

	private boolean puertaV(int cx, int cz) {
		return !this.fusionV(cx, cz) && (this.azar(cx, cz, 32) < 0.72 || this.forzada(cx - 1, cz) == ESTE || this.forzada(cx, cz) == OESTE);
	}

	private boolean puertaH(int cx, int cz) {
		return !this.fusionH(cx, cz) && (this.azar(cx, cz, 33) < 0.72 || this.forzada(cx, cz - 1) == SUR || this.forzada(cx, cz) == NORTE);
	}

	/** Hay puerta en ese lado de la celda. */
	public boolean puerta(int cx, int cz, int lado) {
		return switch (lado) {
			case NORTE -> this.puertaH(cx, cz);
			case SUR -> this.puertaH(cx, cz + 1);
			case OESTE -> this.puertaV(cx, cz);
			default -> this.puertaV(cx + 1, cz);
		};
	}

	/** La planta alta de la sala llega a ese lado. */
	private static boolean alta(Sala s, int lado) {
		return s.tipo() == CARACOL || s.tipo() == TERRAZAS && lado == (OESTE + s.giro()) % 4;
	}

	/** Puerta de la planta alta: las dos salas tienen planta alta en ese lado. */
	public boolean puertaAlta(int cx, int cz, int lado) {
		return alta(this.sala(cx, cz), lado) && alta(this.sala(cx + DX[lado], cz + DZ[lado]), (lado + 2) % 4);
	}

	/** Medio ancho de la puerta: 2 entre salas grandes, 1 en lo demas. */
	private static int medioAncho(Sala a, Sala b) {
		return esSala(a) && esSala(b) ? 2 : 1;
	}

	/* ------------------------------------------------------- utilidades */

	private static void rellenar(byte[] c, int y0, int y1, byte m) {
		for (int y = Math.max(y0, Y0); y <= Math.min(y1, TOPE); y++) {
			c[y - Y0] = m;
		}
	}

	private static void poner(byte[] c, int y, byte m) {
		if (y >= Y0 && y <= TOPE) {
			c[y - Y0] = m;
		}
	}

	public static byte en(byte[] c, int y) {
		return y < Y0 || y > TOPE ? AIRE : c[y - Y0];
	}

	private static double medio(double h) {
		return Math.round(h * 2.0) / 2.0;
	}

	/** Macizo hasta `tope`. */
	private static void macizo(byte[] c, int tope) {
		rellenar(c, Y0, tope, PARED);
	}

	/**
	 * Suelo con la superficie a la altura h (de medio en medio). Si queda por debajo
	 * de SUELO + 1, encima hay agua hasta SUELO; el ultimo bloque macizo es `fondo`.
	 */
	private static void suelo(byte[] c, double h, byte fondo) {
		double q = medio(h);
		int n = (int) Math.floor(q);
		rellenar(c, Y0, n - 1, PARED);
		if (q < PISO) {
			poner(c, n - 1, fondo);
		}
		int agua = n;
		if (q > n) {
			poner(c, n, n <= SUELO ? LOSA_AGUA : LOSA);
			agua = n + 1;
		}
		rellenar(c, agua, SUELO, AGUA);
	}

	/** Techo con la superficie a la altura h (de medio en medio), macizo hasta `tope`. */
	private static void techo(byte[] c, double h, int tope) {
		double q = medio(h);
		int n = (int) Math.floor(q);
		// el techo, de azulejo limpio: las piezas caidas arriba parecen manchas
		if (q > n) {
			poner(c, n, LOSA_ARRIBA);
			rellenar(c, n + 1, tope, AZULEJO);
		} else {
			rellenar(c, n, tope, AZULEJO);
		}
	}

	/**
	 * Techo curvo (bovedas, tubos): con la altura en las dos mitades de la columna a lo
	 * largo de x (ax, bx) y de z (az, bz). Si sube por dentro de la columna, un escalon
	 * del reves hace de chaflan: medio bloque mas bajo del lado bajo. Si no, como techo().
	 */
	private static void techoCurvo(byte[] c, double ax, double bx, double az, double bz, int tope) {
		boolean enX = Math.abs(bx - ax) >= Math.abs(bz - az);
		double a = enX ? ax : az;
		double b = enX ? bx : bz;
		double m = (a + b) / 2.0;
		if (Math.abs(a - b) < 0.3) {
			techo(c, m, tope);
			return;
		}
		int n = (int) Math.floor(m + 0.25);
		int bajo = a < b ? (enX ? OESTE : NORTE) : (enX ? ESTE : SUR);
		poner(c, n, (byte) (ESCALON_ARRIBA + bajo));
		rellenar(c, n + 1, tope, AZULEJO);
	}

	/** Primer bloque macizo del techo por encima de y. */
	private static int techoSobre(byte[] c, int y) {
		for (int k = y; k <= TOPE; k++) {
			byte m = en(c, k);
			// las luces invisibles no son techo: si no, la claraboya acaba flotando a media altura
			if (m != AIRE && m != AGUA && m != LUZ_OCULTA && m != LUZ_TENUE) {
				return k;
			}
		}
		return TOPE;
	}

	/**
	 * Borde de una piscina visto desde la columna seca de al lado: azulejo del fondo
	 * en la cara mojada, el borde azul arriba y, si cubre, un foco cada pocos bloques.
	 */
	private static void orilla(byte[] c, double profVecina, boolean foco) {
		if (profVecina <= 0) {
			return;
		}
		int abajo = (int) Math.floor(PISO - medio(profVecina));
		for (int y = Math.max(abajo, Y0); y < SUELO; y++) {
			poner(c, y, FONDO);
		}
		poner(c, SUELO, BORDE);
		if (foco && profVecina >= 2.0) {
			poner(c, SUELO - 1, FOCO);
		}
	}

	/* ---------------------------------------------------------- columna */

	/** Llena `col` (ALTO materiales, de Y0 a TOPE) con la columna (x, z). */
	public void columna(int x, int z, byte[] col) {
		Arrays.fill(col, AIRE);
		int cx = Math.floorDiv(x, C);
		int cz = Math.floorDiv(z, C);
		int u = x - cx * C;
		int v = z - cz * C;
		boolean paredO = u == 0 && !this.fusionV(cx, cz);
		boolean paredN = v == 0 && !this.fusionH(cx, cz);
		if (paredO || paredN) {
			this.pared(cx, cz, u, v, paredO, paredN, col);
			return;
		}
		Sala s = this.sala(cx, cz);
		switch (s.tipo()) {
			case PASILLOS -> this.pasillos(s, cx, cz, u, v, col);
			case SALA -> this.piscina(s, x, z, col);
			case INUNDADA -> this.inundada(s, x, z, col);
			case BOVEDAS -> this.bovedas(s, x, z, col);
			case TUNEL -> this.tunel(s, cx, cz, u, v, col);
			case CARACOL -> this.caracol(s, cx, cz, u, v, col);
			default -> this.terrazas(s, cx, cz, u, v, col);
		}
	}

	/** Columna de pared entre dos celdas: maciza, con la puerta si la hay. */
	private void pared(int cx, int cz, int u, int v, boolean paredO, boolean paredN, byte[] col) {
		Sala s = this.sala(cx, cz);
		if (paredO && paredN) {
			int t = Math.max(Math.max(techo(s), techo(this.sala(cx - 1, cz))), Math.max(techo(this.sala(cx, cz - 1)), techo(this.sala(cx - 1, cz - 1))));
			macizo(col, t + 1);
			return;
		}
		int lado = paredO ? OESTE : NORTE;
		Sala n = this.sala(cx + DX[lado], cz + DZ[lado]);
		macizo(col, Math.max(techo(s), techo(n)) + 1);
		int o = (paredO ? v : u) - M;
		int a = Math.abs(o);
		if (this.puerta(cx, cz, lado)) {
			if (s.tipo() == TUNEL && n.tipo() == TUNEL) {
				// de tubo a tubo: el tubo sigue por la pared
				double[] p = perfilTubo(o, false);
				if (p != null) {
					vaciar(col, PISO, TECHO[TUNEL]);
					suelo(col, p[0] <= PISO + 0.25 ? PISO - 0.5 : p[0], FONDO);
					// el chaflan va de lado a lado del tubo: a lo largo de la pared
					double ya = alto(perfilTubo(o - 0.25, false));
					double yb = alto(perfilTubo(o + 0.25, false));
					if (paredO) {
						techoCurvo(col, p[1], p[1], ya, yb, TECHO[TUNEL] + 1);
					} else {
						techoCurvo(col, ya, yb, p[1], p[1], TECHO[TUNEL] + 1);
					}
				}
			} else {
				int w = medioAncho(s, n);
				if (a <= w) {
					int arriba = PISO + 2 + w - (a == w ? 1 : 0);
					vaciar(col, PISO, arriba);
					if (a == w) {
						poner(col, arriba + 1, LOSA_ARRIBA);
					}
				}
			}
		}
		if (a <= 1 && this.puertaAlta(cx, cz, lado)) {
			vaciar(col, PLANTA + 1, PLANTA + (a == 0 ? 4 : 3));
			if (a == 1) {
				poner(col, PLANTA + 4, LOSA_ARRIBA);
			}
		}
	}

	private static void vaciar(byte[] c, int y0, int y1) {
		rellenar(c, y0, y1, AIRE);
	}

	/* --------------------------------------------------------- pasillos */

	private void pasillos(Sala s, int cx, int cz, int u, int v, byte[] col) {
		int r = s.var();
		int uA = this.puerta(cx, cz, OESTE) ? 1 : 4;
		int uB = this.puerta(cx, cz, ESTE) ? 23 : 20;
		int vA = this.puerta(cx, cz, NORTE) ? 1 : 4;
		int vB = this.puerta(cx, cz, SUR) ? 23 : 20;
		boolean h = Math.abs(v - M) <= 1 && u >= uA && u <= uB;
		boolean vert = Math.abs(u - M) <= 1 && v >= vA && v <= vB;
		boolean anilloN = (r & 1) != 0 && Math.abs(v - 5) <= 1 && u >= 4 && u <= 20;
		boolean anilloS = (r & 2) != 0 && Math.abs(v - 19) <= 1 && u >= 4 && u <= 20;
		boolean anilloO = (r & 4) != 0 && Math.abs(u - 5) <= 1 && v >= 4 && v <= 20;
		boolean anilloE = (r & 8) != 0 && Math.abs(u - 19) <= 1 && v >= 4 && v <= 20;
		if (!(h || vert || anilloN || anilloS || anilloO || anilloE)) {
			macizo(col, TECHO[PASILLOS] + 1);
			return;
		}
		boolean eje = h && v == M || vert && u == M || anilloN && v == 5 || anilloS && v == 19 || anilloO && u == 5 || anilloE && u == 19;
		boolean canal = (r >> 4) % 3 == 0 && eje;
		suelo(col, canal ? SUELO : PISO, FONDO);
		if (!canal && eje && (u + v) % 7 == 0) {
			poner(col, SUELO, REJILLA);
		}
		techo(col, TECHO[PASILLOS], TECHO[PASILLOS] + 1);
		boolean luz = (u == M || u == 5 || u == 19) && v % 4 == 1 || (v == M || v == 5 || v == 19) && u % 4 == 1;
		if (!s.oscura() && eje && luz) {
			poner(col, TECHO[PASILLOS], LUZ);
		}
	}

	/* ------------------------------------------- piscina de calles (SALA) */

	/** Profundidad de la piscina de calles en (x, z), o 0. */
	private static double profPiscina(Sala s, int x, int z) {
		int rx = x - s.x0();
		int rz = z - s.z0();
		int w = s.ancho();
		int l = s.largo();
		if (rx < 6 || rz < 6 || rx >= w - 6 || rz >= l - 6) {
			return 0;
		}
		boolean aLoLargo = (s.var() & 1) == 0;
		double pos = aLoLargo ? rx : rz;
		double largo = (aLoLargo ? w : l) - 12;
		double t = (pos - 6 + 0.5) / largo * 2.0 - 1.0;
		return 1.5 + 2.0 * (1.0 - t * t);
	}

	private void piscina(Sala s, int x, int z, byte[] col) {
		int rx = x - s.x0();
		int rz = z - s.z0();
		int t = TECHO[SALA];
		if (pilar8(rx, rz, s)) {
			macizo(col, t + 1);
			return;
		}
		double p = profPiscina(s, x, z);
		if (p > 0) {
			boolean aLoLargo = (s.var() & 1) == 0;
			int lateral = aLoLargo ? rz : rx;
			int pos = aLoLargo ? rx : rz;
			int fin = (aLoLargo ? s.ancho() : s.largo()) - 6;
			boolean franja = (lateral - 6) % 3 == 1 && pos > 7 && pos < fin - 2;
			suelo(col, PISO - p, franja ? FRANJA : FONDO);
		} else {
			suelo(col, PISO, AZULEJO);
			orilla(col, this.vecina(s, x, z, PlanoPiscinas::profPiscina), (x + z) % 4 == 0);
		}
		techo(col, t, t + 1);
		if (!s.oscura() && Math.floorMod(rx, 4) == 2 && Math.floorMod(rz, 4) == 2) {
			poner(col, t, LUZ);
		}
		if (!s.oscura() && rx % 8 == 0 && rz % 8 == 0) {
			poner(col, PISO + 4, LUZ_OCULTA);
		}
	}

	/** Pilares cuadrados de 2x2 cada 8 bloques, sin tocar las paredes. */
	private static boolean pilar8(int rx, int rz, Sala s) {
		int mx = rx % 8;
		int mz = rz % 8;
		return (mx == 3 || mx == 4) && (mz == 3 || mz == 4) && rx > 2 && rz > 2 && rx < s.ancho() - 2 && rz < s.largo() - 2;
	}

	private interface Prof {
		double en(Sala s, int x, int z);
	}

	/** La mayor profundidad de las cuatro columnas de al lado (dentro de la misma sala). */
	private double vecina(Sala s, int x, int z, Prof f) {
		double p = 0;
		for (int k = 0; k < 4; k++) {
			p = Math.max(p, f.en(s, x + DX[k], z + DZ[k]));
		}
		return p;
	}

	/* --------------------------------------------- sala inundada (INUNDADA) */

	private void inundada(Sala s, int x, int z, byte[] col) {
		int rx = x - s.x0();
		int rz = z - s.z0();
		int t = TECHO[INUNDADA];
		// columnas redondas cada 12
		int px = Math.floorMod(rx, 12) - 6;
		int pz = Math.floorMod(rz, 12) - 6;
		if (px * px + pz * pz <= 4.6) {
			macizo(col, t + 1);
			return;
		}
		boolean orillaSala = rx == 1 || rz == 1 || rx == s.ancho() - 1 || rz == s.largo() - 1;
		suelo(col, orillaSala ? SUELO + 0.5 : SUELO, FONDO);
		techo(col, t, t + 1);
		// claraboyas entre las columnas
		int qx = Math.floorMod(rx + 6, 12) - 6;
		int qz = Math.floorMod(rz + 6, 12) - 6;
		int bx = Math.floorDiv(x + 6, 12);
		int bz = Math.floorDiv(z + 6, 12);
		// el pozo de la claraboya sube 4 mas: sus paredes (y el anillo de alrededor) macizas hasta arriba
		boolean pozo = Math.abs(qx) <= 3 && Math.abs(qz) <= 3 && rx > 3 && rz > 3 && rx < s.ancho() - 3 && rz < s.largo() - 3
			&& this.azar(bx, bz, 41) < 0.65;
		if (pozo) {
			techo(col, t, t + 5);
			if (Math.abs(qx) <= 2 && Math.abs(qz) <= 2) {
				vaciar(col, t, t + 3);
				poner(col, t + 4, Math.abs(qx) <= 1 && Math.abs(qz) <= 1 && !s.oscura() ? CLARABOYA : PARED);
			}
			if (qx == 0 && qz == 0 && !s.oscura()) {
				poner(col, PISO + 6, LUZ_OCULTA);
			}
		} else if (!s.oscura() && (Math.abs(qx) == 6 && qz == 0 || qx == 0 && Math.abs(qz) == 6)) {
			poner(col, PISO + 5, LUZ_OCULTA);
		}
	}

	/* ------------------------------------------------------ bovedas */

	/**
	 * Altura del techo de una boveda de 6 de luz entre pilares, en la posicion continua p
	 * (dentro del paso de 8: los pilares ocupan 0..2). En la linea de pilares, el arranque.
	 */
	private static double boveda(double p) {
		double o = p - 8.0 * Math.floor(p / 8.0) - 5.0;
		if (Math.abs(o) >= 3.0) {
			return 45.0;
		}
		return 45.0 + 1.45 * Math.sqrt(9.0 - o * o);
	}

	/** Techo de la sala de bovedas en (px, pz) continuos, relativos a la sala: la mas alta de las dos bovedas que se cruzan. */
	private static double techoBovedas(double px, double pz) {
		return Math.max(boveda(px), boveda(pz));
	}

	/** Profundidad de la piscina de borde curvo (dos o tres circulos fundidos), o 0. */
	private static double profBovedas(Sala s, int x, int z) {
		int rx = x - s.x0();
		int rz = z - s.z0();
		int w = s.ancho();
		int l = s.largo();
		if (rx < 3 || rz < 3 || rx > w - 3 || rz > l - 3) {
			return 0;
		}
		int r = s.var();
		boolean grande = w > C;
		double f = 0;
		int n = 2 + (r >>> 3) % 2;
		for (int k = 0; k < n; k++) {
			int hk = r * 31 + k * 977;
			double cxk = 7 + Math.floorMod(hk >>> 4, w - 14);
			double czk = 7 + Math.floorMod(hk >>> 11, l - 14);
			double rad = grande ? 7 + Math.floorMod(hk >>> 17, 6) : 4 + Math.floorMod(hk >>> 17, 3);
			double d = Math.hypot(rx + 0.5 - cxk, rz + 0.5 - czk);
			f = Math.max(f, 1.0 - d / rad);
		}
		if (f <= 0) {
			return 0;
		}
		return Math.min(4.0, 0.5 + f * 7.0);
	}

	private void bovedas(Sala s, int x, int z, byte[] col) {
		int rx = x - s.x0();
		int rz = z - s.z0();
		int mx = Math.floorMod(rx, 8);
		int mz = Math.floorMod(rz, 8);
		boolean pilX = mx <= 1;
		boolean pilZ = mz <= 1;
		int t = TECHO[BOVEDAS];
		if (pilX && pilZ) {
			macizo(col, t + 1);
			return;
		}
		double p = profBovedas(s, x, z);
		if (p > 0) {
			suelo(col, PISO - p, FONDO);
		} else {
			suelo(col, PISO, AZULEJO);
			orilla(col, this.vecina(s, x, z, PlanoPiscinas::profBovedas), (x + 2 * z) % 3 == 0);
		}
		double cx = rx + 0.5;
		double cz = rz + 0.5;
		techoCurvo(col, techoBovedas(cx - 0.25, cz), techoBovedas(cx + 0.25, cz),
			techoBovedas(cx, cz - 0.25), techoBovedas(cx, cz + 0.25), t + 1);
		if (!s.oscura() && mx == 4 && mz == 4) {
			poner(col, PISO + 4, LUZ_TENUE);
		}
	}

	/* ------------------------------------------------------------ tunel */

	private static final double RADIO_TUBO = 4.5;
	private static final double CENTRO_TUBO = 43.5;

	/** {abajo, arriba} del tubo a `o` del eje (null si es pared). Los anillos estrechan el tubo. */
	private static double[] perfilTubo(double o, boolean anillo) {
		double r = anillo ? RADIO_TUBO - 0.8 : RADIO_TUBO;
		if (Math.abs(o) >= r) {
			return null;
		}
		double s = Math.sqrt(r * r - o * o);
		return new double[] {CENTRO_TUBO - s, CENTRO_TUBO + s};
	}

	/**
	 * {abajo, arriba} de los tubos de la celda en la columna (u, v) desplazada (du, dv)
	 * dentro de ella, o null si ahi es macizo. Un tubo va del centro a cada puerta; en el
	 * centro, una cupula (el cruce, o el final si solo hay una puerta).
	 */
	private double[] tubos(int cx, int cz, int u, int v, double du, double dv) {
		double lo = Double.MAX_VALUE;
		double hi = -1;
		double uc = u + 0.5 + du;
		double vc = v + 0.5 + dv;
		for (int lado = 0; lado < 4; lado++) {
			if (!this.puerta(cx, cz, lado)) {
				continue;
			}
			boolean x = lado == ESTE || lado == OESTE;
			int pos = x ? u : v;
			int a = lado == OESTE || lado == NORTE ? pos : C - pos;
			if (a > M) {
				continue;
			}
			double[] p = perfilTubo((x ? vc : uc) - (M + 0.5), a % 4 == 0 && a < M - 1);
			if (p != null) {
				lo = Math.min(lo, p[0]);
				hi = Math.max(hi, p[1]);
			}
		}
		double[] cupula = perfilTubo(Math.hypot(uc - (M + 0.5), vc - (M + 0.5)), false);
		if (cupula != null) {
			lo = Math.min(lo, cupula[0]);
			hi = Math.max(hi, cupula[1]);
		}
		return hi < 0 ? null : new double[] {lo, hi};
	}

	/** Alto del techo del tubo; fuera del tubo, su arranque (para el chaflan del borde). */
	private static double alto(double[] p) {
		return p == null ? CENTRO_TUBO : p[1];
	}

	private void tunel(Sala s, int cx, int cz, int u, int v, byte[] col) {
		int t = TECHO[TUNEL];
		double[] p = this.tubos(cx, cz, u, v, 0, 0);
		if (p == null) {
			macizo(col, t + 1);
			return;
		}
		suelo(col, p[0] <= PISO + 0.25 ? PISO - 0.5 : p[0], FONDO);
		techoCurvo(col, alto(this.tubos(cx, cz, u, v, -0.25, 0)), alto(this.tubos(cx, cz, u, v, 0.25, 0)),
			alto(this.tubos(cx, cz, u, v, 0, -0.25)), alto(this.tubos(cx, cz, u, v, 0, 0.25)), t + 1);
		// en el eje de cada tubo: a que distancia de su puerta esta (para las luces)
		int eje = -1;
		for (int lado = 0; lado < 4; lado++) {
			boolean x = lado == ESTE || lado == OESTE;
			int pos = x ? u : v;
			int a = lado == OESTE || lado == NORTE ? pos : C - pos;
			if ((x ? v : u) == M && a <= M && this.puerta(cx, cz, lado)) {
				eje = a;
			}
		}
		if (!s.oscura() && eje > 0) {
			if (eje % 4 == 2) {
				poner(col, PISO + 3, LUZ_OCULTA);
			}
			if (eje % 8 == 6) {
				poner(col, techoSobre(col, PISO), CLARABOYA);
			}
		}
	}

	/* ------------------------------------------------- dibujo girado */

	/** Coordenadas del marco de dibujo (a, b) a partir de las de la celda (u, v). */
	private static int[] marco(int u, int v, int giro) {
		return switch (giro) {
			case 1 -> new int[] {v, C - u};
			case 2 -> new int[] {C - u, C - v};
			case 3 -> new int[] {C - v, u};
			default -> new int[] {u, v};
		};
	}

	/** El lado real de un lado del marco de dibujo. */
	private static int ladoReal(int lado, int giro) {
		return (lado + giro) % 4;
	}

	/* ---------------------------------------------------------- caracol */

	private static final double COLUMNA = 2.3;
	private static final double ESCALERA = 6.0;
	private static final double AGUA_CARACOL = 9.0;
	private static final double SALA_CARACOL = 11.6;
	private static final int PELDANOS = 18;

	private void caracol(Sala s, int cx, int cz, int u, int v, byte[] col) {
		int[] ab = marco(u, v, s.giro());
		double da = ab[0] - M;
		double db = ab[1] - M;
		double d = Math.hypot(da, db);
		int t = TECHO[CARACOL];
		if (d > SALA_CARACOL || d <= COLUMNA) {
			macizo(col, t + 1);
			return;
		}
		// abajo: agua poco honda y un anillo seco junto a las paredes
		if (d < AGUA_CARACOL) {
			suelo(col, SUELO, FONDO);
		} else {
			suelo(col, PISO, AZULEJO);
			if (d < AGUA_CARACOL + 0.9) {
				poner(col, SUELO, BORDE);
			}
		}
		techo(col, t, t + 1);
		// la escalera: una vuelta de 18 peldanos flotando alrededor de la columna
		double ang = Math.atan2(db, da);
		double vuelta = Math.floorMod((int) Math.floor((ang + Math.PI) / (2 * Math.PI) * PELDANOS * 8), PELDANOS * 8) / 8.0;
		int k = (int) vuelta;
		if (d < ESCALERA) {
			double h = SUELO + 0.5 + k * 0.5;
			if (h <= PISO) {
				// los primeros peldanos salen del agua
				suelo(col, h, FONDO);
			} else {
				double q = medio(h);
				int n = (int) Math.floor(q);
				poner(col, q > n ? n : n - 1, q > n ? LOSA : LOSA_ARRIBA);
			}
		} else {
			// el balcon de la planta alta (deja ver la escalera por el medio)
			rellenar(col, PLANTA, PLANTA, PARED);
			if (d < ESCALERA + 0.9 && k < PELDANOS - 3) {
				poner(col, PLANTA + 1, AZULEJO);
			}
		}
		if (!s.oscura()) {
			if (d >= ESCALERA && d < ESCALERA + 1.0 && Math.floorMod(u + v, 5) == 0) {
				poner(col, t, LUZ);
			}
			// luces sin fuente: bajo el balcon, sobre el balcon y en el hueco de la escalera
			boolean eje = ab[0] == M || ab[1] == M;
			boolean diagonal = Math.abs(Math.abs(da) - Math.abs(db)) < 0.5;
			if (Math.abs(d - 8.5) < 0.5 && (eje || diagonal)) {
				poner(col, PISO + 3, LUZ_OCULTA);
				poner(col, PLANTA + 4, LUZ_OCULTA);
			}
			if (Math.abs(d - 4.2) < 0.5 && eje) {
				poner(col, PLANTA + 5, LUZ_OCULTA);
			}
		}
	}

	/* --------------------------------------------------------- terrazas */

	/** Frente de la terraza baja (b a partir del cual esta), en el marco de dibujo. */
	private static int frente1(Sala s, int a) {
		if (a >= 15 && a <= 21) {
			return 17;
		}
		return 17 + (int) Math.round(1.6 * Math.sin(a * 0.45 + (s.var() & 15)));
	}

	/** Frente de la terraza alta (a hasta el que esta). */
	private static int frente2(Sala s, int b) {
		if (b >= 18 && b <= 22) {
			return 5;
		}
		return 5 + (int) Math.round(1.6 * Math.sin(b * 0.4 + (s.var() >>> 4 & 15)));
	}

	private static double profCanal(Sala s, int a, int b) {
		double centro = 9 + 3 * Math.sin(a * 0.33 + (s.var() >>> 8 & 15));
		double o = Math.abs(b - centro);
		// lejos de la terraza alta y de la puerta del este
		if (o > 2.6 || a <= frente2(s, b) + 1 || a >= 21) {
			return 0;
		}
		return o < 1.4 ? 2.0 : 1.0;
	}

	private void terrazas(Sala s, int cx, int cz, int u, int v, byte[] col) {
		int g = s.giro();
		int[] ab = marco(u, v, g);
		int a = ab[0];
		int b = ab[1];
		int t = TECHO[TERRAZAS];
		boolean alta = a <= frente2(s, b);
		boolean baja = b >= frente1(s, a);
		techo(col, t, t + 1);
		// escaleras: suelo -> terraza baja (a 17..19, subiendo hacia +b) y baja -> alta (b 19..21, hacia -a)
		if (!alta && a >= 17 && a <= 19 && b >= 13 && b <= 16) {
			int y = PISO + (b - 13);
			rellenar(col, Y0, y - 1, PARED);
			poner(col, y, (byte) (ESCALON + ladoReal(SUR, g)));
			return;
		}
		if (baja && b >= 19 && b <= 21 && a >= 6 && a <= 9) {
			int y = PISO + 4 + (9 - a);
			rellenar(col, Y0, y - 1, PARED);
			poner(col, y, (byte) (ESCALON + ladoReal(OESTE, g)));
			return;
		}
		if (alta) {
			rellenar(col, Y0, PLANTA, PARED);
			boolean borde = a == frente2(s, b) && !(b >= 19 && b <= 21);
			if (borde) {
				poner(col, PLANTA + 1, AZULEJO);
			}
		} else if (baja) {
			rellenar(col, Y0, PLANTA - 4, PARED);
			boolean borde = b == frente1(s, a) && !(a >= 17 && a <= 19);
			if (borde) {
				poner(col, PLANTA - 3, AZULEJO);
			}
		} else {
			double p = profCanal(s, a, b);
			if (p > 0) {
				suelo(col, PISO - p, FONDO);
			} else {
				suelo(col, PISO, AZULEJO);
				double vec = 0;
				for (int k = 0; k < 4; k++) {
					int[] n = marco(u + DX[k], v + DZ[k], g);
					vec = Math.max(vec, profCanal(s, n[0], n[1]));
				}
				orilla(col, vec, (a + b) % 4 == 0);
			}
		}
		// arcos oscuros en el frente de la terraza baja y pasos bajo las terrazas hacia las puertas
		// (bajo la baja caben 3 de alto: su suelo esta en y = 44)
		boolean nicho = baja && !alta && (a == 8 || a == 9 || a == 22 || a == 23) && b >= frente1(s, a) && b <= frente1(s, a) + 2;
		boolean pasoS = baja && !alta && this.puerta(cx, cz, ladoReal(SUR, g)) && Math.abs(a - M) <= 1 && b >= frente1(s, a);
		boolean pasoO = alta && this.puerta(cx, cz, ladoReal(OESTE, g)) && Math.abs(b - M) <= 1;
		if (nicho || pasoS) {
			vaciar(col, PISO, PISO + 1);
			poner(col, PISO + 2, nicho || a != M ? LOSA_ARRIBA : AIRE);
		} else if (pasoO) {
			vaciar(col, PISO, PISO + 2);
			poner(col, PISO + 3, b == M ? AIRE : LOSA_ARRIBA);
		}
		if (!s.oscura()) {
			if (a % 8 == 4 && b % 8 == 4) {
				poner(col, alta ? PLANTA + 4 : baja ? PLANTA : PISO + 5, LUZ_OCULTA);
			}
			if (a % 6 == 3 && b % 6 == 3) {
				poner(col, t, LUZ);
			}
		}
	}

	/* -------------------------------------------------------- detalles */

	// las marcas (MarcaPiscina) y las manchas (ManchaSuelo), en el orden de tools/texturas/piscinas.js
	public static final int M_CORRE = 0;   // 0, 1, 2: de izquierda a derecha
	public static final int M_MANO = 3;
	public static final int M_MANOS = 4;
	public static final int M_ARANAZOS = 5;
	public static final int M_CONTEO = 6;
	public static final int M_ESPIRAL = 7;
	public static final int M_MANCHA = 8;
	public static final int M_SANGRE = 9;
	public static final int M_CARTEL = 10; // 10, 11, 12: no correr, profundidad, prohibido saltar
	public static final int M_CUANTAS = 13;
	public static final int S_SUCIEDAD = 0;
	public static final int S_CHARCO = 1;
	public static final int S_GOTAS_SANGRE = 2;
	public static final int S_ESCOMBROS = 3;
	public static final int S_HUELLAS = 4;
	public static final int S_CUANTAS = 5;

	/** Algo en una pared (MarcaPiscina, `variante` = la marca). */
	public static final int D_MARCA = 0;
	/** Una mancha pegada al suelo (ManchaSuelo). */
	public static final int D_MANCHA = 1;
	/** Un objeto tirado (BloquesPiscinas.OBJETOS). */
	public static final int D_OBJETO = 2;
	/** Un azulejo de la pared cambiado: 0 manchado, 1 con sangre. */
	public static final int D_AZULEJO = 3;

	/**
	 * Lo que dice que alguien estuvo aqui antes. `lado` es hacia donde mira (N, E, S, O):
	 * las marcas, al contrario que la pared que tienen detras.
	 */
	public record Detalle(int x, int y, int z, int tipo, int variante, int lado) {
	}

	private final java.util.concurrent.ConcurrentHashMap<Long, java.util.List<Detalle>> detalles = new java.util.concurrent.ConcurrentHashMap<>();

	/** Los detalles que caen en la columna (x, z). */
	public java.util.List<Detalle> detallesEn(int x, int z) {
		java.util.List<Detalle> todos = this.detallesCelda(Math.floorDiv(x, C), Math.floorDiv(z, C));
		if (todos.isEmpty()) {
			return todos;
		}
		java.util.List<Detalle> aqui = new java.util.ArrayList<>(1);
		for (Detalle d : todos) {
			if (d.x() == x && d.z() == z) {
				aqui.add(d);
			}
		}
		return aqui;
	}

	public java.util.List<Detalle> detallesCelda(int cx, int cz) {
		long k = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		java.util.List<Detalle> l = this.detalles.get(k);
		if (l == null) {
			l = this.calcularDetalles(cx, cz);
			if (this.detalles.size() > 20000) {
				this.detalles.clear();
			}
			this.detalles.put(k, l);
		}
		return l;
	}

	/**
	 * En 3 de cada 10 celdas, uno (a veces dos) de estos, cada uno con su peso: un cartel
	 * viejo de piscina, humedad, suciedad o escombros, algo abandonado, aranazos o palotes,
	 * manos, sangre y, muy pocas veces, CORRE. Lo demas, limpio.
	 */
	private java.util.List<Detalle> calcularDetalles(int cx, int cz) {
		// lo de las misiones va primero: la puerta de salida, las valvulas y los carteles que llevan a ella
		Salida sal = this.salida();
		if (cx == sal.celdaX() && cz == sal.celdaZ()) {
			return this.detallesSalida(sal);
		}
		java.util.List<Detalle> mision = new java.util.ArrayList<>();
		for (Valvula v : this.valvulas()) {
			if (Math.floorDiv(v.x(), C) == cx && Math.floorDiv(v.z(), C) == cz) {
				mision.add(new Detalle(v.x(), v.y(), v.z(), D_VALVULA, v.id(), v.lado()));
			}
		}
		if (!mision.isEmpty()) {
			return java.util.List.copyOf(mision);
		}
		if (Math.max(Math.abs(cx - sal.celdaX()), Math.abs(cz - sal.celdaZ())) <= CARTELES && this.azar(cx, cz, 97) < 0.5) {
			int[] p = this.pared(cx, cz, 8, PISO + 2, 1);
			if (p != null) {
				// mirando la pared (hacia p[2]), la derecha es (p[2] + 1): la flecha va hacia el lado de la puerta
				int[] puerta = sal.columna(1);
				long lateral = (long) (puerta[0] - p[0]) * DX[(p[2] + 1) % 4] + (long) (puerta[1] - p[1]) * DZ[(p[2] + 1) % 4];
				return java.util.List.of(new Detalle(p[0], PISO + 2, p[1], D_CARTEL, lateral >= 0 ? CARTEL_DERECHA : CARTEL_IZQUIERDA, (p[2] + 2) % 4));
			}
		}
		double r = this.azar(cx, cz, 61);
		if (r >= 0.30) {
			return java.util.List.of();
		}
		Sala s = this.sala(cx, cz);
		boolean piscina = s.tipo() == SALA || s.tipo() == BOVEDAS || s.tipo() == CARACOL || s.tipo() == INUNDADA;
		java.util.List<Detalle> out = new java.util.ArrayList<>();
		int cuantos = r < 0.05 ? 2 : 1;
		for (int k = 0; k < cuantos; k++) {
			double q = this.azar(cx, cz, 62 + k);
			int h = this.hash(cx, cz, 63 + k);
			if (q < 0.05) {
				this.marcaAncha(out, cx, cz, k, M_CORRE, 3);
			} else if (q < 0.25) {
				int cartel = piscina ? M_CARTEL + h % 3 : M_CONTEO;
				this.marca(out, cx, cz, k, cartel, PISO + 2);
			} else if (q < 0.43) {
				if (h % 3 == 0) {
					this.azulejoPared(out, cx, cz, k, 0);
				} else {
					this.marca(out, cx, cz, k, M_MANCHA, PISO + 1);
				}
			} else if (q < 0.60) {
				int[] suelos = {S_SUCIEDAD, S_CHARCO, S_ESCOMBROS, S_HUELLAS};
				this.suelo(out, cx, cz, k, D_MANCHA, suelos[h % suelos.length]);
			} else if (q < 0.75) {
				this.suelo(out, cx, cz, k, D_OBJETO, h % 4);
			} else if (q < 0.87) {
				int[] rayas = {M_ARANAZOS, M_CONTEO, M_ESPIRAL};
				this.marca(out, cx, cz, k, rayas[h % rayas.length], PISO + (h % 2));
			} else if (q < 0.94) {
				this.marca(out, cx, cz, k, h % 2 == 0 ? M_MANO : M_MANOS, PISO + 1);
			} else {
				switch (h % 3) {
					case 0 -> this.marca(out, cx, cz, k, M_SANGRE, PISO);
					case 1 -> this.suelo(out, cx, cz, k, D_MANCHA, S_GOTAS_SANGRE);
					default -> this.azulejoPared(out, cx, cz, k, 1);
				}
			}
		}
		return java.util.List.copyOf(out);
	}

	/** Columna (x, z) de la celda donde probar el intento t del detalle k. */
	private int[] candidata(int cx, int cz, int k, int t) {
		int u = 2 + Math.floorMod(this.hash(cx, cz, 70 + k * 101 + t * 7), C - 3);
		int v = 2 + Math.floorMod(this.hash(cx, cz, 71 + k * 101 + t * 7), C - 3);
		return new int[] {cx * C + u, cz * C + v};
	}

	private static boolean hueco(byte[] col, int y) {
		return en(col, y) == AIRE && en(col, y + 1) == AIRE && en(col, PISO - 1) != AIRE;
	}

	private static boolean macizoEn(byte[] col, int y) {
		byte m = en(col, y);
		return m == PARED || m == AZULEJO;
	}

	/** Busca una pared con la columna de delante libre a la altura y; devuelve {x, z, lado de la pared} o null. */
	private int[] pared(int cx, int cz, int k, int y, int ancho) {
		byte[] col = new byte[ALTO];
		byte[] vec = new byte[ALTO];
		for (int t = 0; t < 70; t++) {
			int[] c = this.candidata(cx, cz, k, t);
			int primero = this.hash(cx, cz, 72 + k + t) % 4;
			for (int i = 0; i < 4; i++) {
				int lado = (primero + i) % 4;
				boolean vale = true;
				// a lo largo de la pared (para CORRE, varias columnas seguidas)
				int tx = DX[(lado + 1) % 4];
				int tz = DZ[(lado + 1) % 4];
				for (int a = 0; a < ancho && vale; a++) {
					int x = c[0] + tx * a;
					int z = c[1] + tz * a;
					if (Math.floorDiv(x, C) != cx || Math.floorDiv(z, C) != cz) {
						vale = false;
						break;
					}
					this.columna(x, z, col);
					this.columna(x + DX[lado], z + DZ[lado], vec);
					vale = hueco(col, y) && macizoEn(vec, y) && macizoEn(vec, y + 1);
				}
				if (vale) {
					return new int[] {c[0], c[1], lado};
				}
			}
		}
		return null;
	}

	private void marca(java.util.List<Detalle> out, int cx, int cz, int k, int marca, int y) {
		int[] p = this.pared(cx, cz, k, y, 1);
		if (p != null) {
			out.add(new Detalle(p[0], y, p[1], D_MARCA, marca, (p[2] + 2) % 4));
		}
	}

	/** Una marca de varios bloques (CORRE): la parte 0 a la izquierda de quien la mira. */
	private void marcaAncha(java.util.List<Detalle> out, int cx, int cz, int k, int primera, int ancho) {
		int y = PISO + 1;
		int[] p = this.pared(cx, cz, k, y, ancho);
		if (p == null) {
			return;
		}
		int mira = (p[2] + 2) % 4;
		// las columnas van de p hacia (lado + 1): mirando la pared (hacia `lado`), de izquierda a derecha
		for (int a = 0; a < ancho; a++) {
			int x = p[0] + DX[(p[2] + 1) % 4] * a;
			int z = p[1] + DZ[(p[2] + 1) % 4] * a;
			out.add(new Detalle(x, y, z, D_MARCA, primera + a, mira));
		}
	}

	private void azulejoPared(java.util.List<Detalle> out, int cx, int cz, int k, int cual) {
		int y = PISO + 1;
		int[] p = this.pared(cx, cz, k, y, 1);
		if (p == null) {
			return;
		}
		int x = p[0] + DX[p[2]];
		int z = p[1] + DZ[p[2]];
		// el azulejo es de la pared: solo si esa pared es de esta celda (si no, nadie lo pondria)
		if (Math.floorDiv(x, C) == cx && Math.floorDiv(z, C) == cz) {
			out.add(new Detalle(x, y, z, D_AZULEJO, cual, p[2]));
		}
	}

	/** En un sitio seco del suelo, lejos de las puertas. */
	private void suelo(java.util.List<Detalle> out, int cx, int cz, int k, int tipo, int variante) {
		byte[] col = new byte[ALTO];
		for (int t = 0; t < 70; t++) {
			int[] c = this.candidata(cx, cz, k + 5, t);
			int u = c[0] - cx * C;
			int v = c[1] - cz * C;
			if (Math.abs(u - M) <= 3 && (v <= 3 || v >= C - 3) || Math.abs(v - M) <= 3 && (u <= 3 || u >= C - 3)) {
				continue;
			}
			this.columna(c[0], c[1], col);
			if (this.pisable(c[0], PISO, c[1], col) && en(col, PISO - 1) != LOSA && en(col, PISO + 2) == AIRE) {
				out.add(new Detalle(c[0], PISO, c[1], tipo, variante, this.hash(cx, cz, 90 + k) % 4));
				return;
			}
		}
	}

	/* -------------------------------------------------------- misiones */

	/** Medio lado del cuadrado donde se reparte a quien llega (Piscinas): las valvulas lo cubren. */
	public static final int MAPA = 900;
	public static final int VALVULAS = 25;
	/** Una valvula en la pared (`variante` = su numero). */
	public static final int D_VALVULA = 4;
	/** Un trozo de la puerta de salida, en la pared: `variante` = columna * 3 + fila (0 abajo). */
	public static final int D_PUERTA = 5;
	/** Un lector de tarjeta junto a la puerta: `variante` = color (T_*). */
	public static final int D_LECTOR = 6;
	/** Un cartel de salida de emergencia: `variante` = CARTEL_*. */
	public static final int D_CARTEL = 7;
	public static final int CARTEL_IZQUIERDA = 0;
	public static final int CARTEL_DERECHA = 1;
	/** El grande de encima de la puerta, en dos mitades: 2 la izquierda, 3 la derecha. */
	public static final int CARTEL_GRANDE = 2;
	public static final int T_ROJA = 0;
	public static final int T_AMARILLA = 1;
	public static final int T_VERDE = 2;
	/** Celdas alrededor de la puerta con carteles que llevan a ella (la mitad tiene uno). */
	private static final int CARTELES = 10;
	/** Columnas de pared que ocupa la salida: margen, puerta (2) y los tres lectores. */
	private static final int ANCHO_SALIDA = 6;

	/** Una valvula: donde esta (el bloque de delante de la pared) y hacia donde mira. */
	public record Valvula(int id, int x, int y, int z, int lado) {
	}

	/**
	 * La salida de emergencia: (x, z) es la columna de delante de la pared donde empieza y
	 * `lado` hacia la pared. A lo largo (hacia lado + 1, la derecha de quien la mira): un
	 * margen, la puerta (columnas 1 y 2, en la pared, 3 de alto) con el cartel grande encima, y
	 * los lectores (3, 4 y 5: roja, amarilla y verde) a la altura del pecho.
	 */
	public record Salida(int x, int z, int lado) {
		/** La columna de delante de la pared en la posicion a (0..5). */
		public int[] columna(int a) {
			return new int[] {this.x + DX[(this.lado + 1) % 4] * a, this.z + DZ[(this.lado + 1) % 4] * a};
		}

		/** La de la pared misma en la posicion a (donde va la puerta). */
		public int[] pared(int a) {
			int[] c = this.columna(a);
			return new int[] {c[0] + DX[this.lado], c[1] + DZ[this.lado]};
		}

		/** Hacia donde miran la puerta, los lectores y los carteles. */
		public int mira() {
			return (this.lado + 2) % 4;
		}

		public int celdaX() {
			return Math.floorDiv(this.x, C);
		}

		public int celdaZ() {
			return Math.floorDiv(this.z, C);
		}

		/** El lector de ese color: {x, y, z}. */
		public int[] lector(int color) {
			int[] c = this.columna(3 + color);
			return new int[] {c[0], PISO + 1, c[1]};
		}
	}

	private volatile Salida salida;
	private volatile java.util.List<Valvula> valvulas;
	private final int[][] tarjetas = new int[3][];

	/** La salida de emergencia: la pared libre mas cercana al centro del mapa donde cabe. */
	public Salida salida() {
		Salida s = this.salida;
		if (s == null) {
			s = this.buscarSalida();
			this.salida = s;
		}
		return s;
	}

	private Salida buscarSalida() {
		byte[] col = new byte[ALTO];
		byte[] vec = new byte[ALTO];
		for (int r = 0; r < 40; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r || this.sala(dx, dz).oscura()) {
						continue;
					}
					for (int t = 0; t < 70; t++) {
						int[] c = this.candidata(dx, dz, 9, t);
						for (int lado = 0; lado < 4; lado++) {
							Salida s = new Salida(c[0], c[1], lado);
							if (this.cabeSalida(s, dx, dz, col, vec)) {
								return s;
							}
						}
					}
				}
			}
		}
		return new Salida(0, 0, NORTE);
	}

	/** Las seis columnas en la celda, secas y altas, con pared maciza detras de 4 de alto. */
	private boolean cabeSalida(Salida s, int cx, int cz, byte[] col, byte[] vec) {
		for (int a = 0; a < ANCHO_SALIDA; a++) {
			int[] c = s.columna(a);
			int[] p = s.pared(a);
			if (Math.floorDiv(c[0], C) != cx || Math.floorDiv(c[1], C) != cz || Math.floorDiv(p[0], C) != cx || Math.floorDiv(p[1], C) != cz) {
				return false;
			}
			this.columna(c[0], c[1], col);
			this.columna(p[0], p[1], vec);
			if (!this.pisable(c[0], PISO, c[1], col) || en(col, PISO + 2) != AIRE || en(col, PISO + 3) != AIRE) {
				return false;
			}
			for (int y = PISO; y <= PISO + 3; y++) {
				if (!macizoEn(vec, y)) {
					return false;
				}
			}
			// delante, al menos dos columnas por donde andar (los pasillos son de 3)
			for (int d = 1; d <= 2; d++) {
				int x = c[0] + DX[s.mira()] * d;
				int z = c[1] + DZ[s.mira()] * d;
				this.columna(x, z, vec);
				if (this.dePie(vec) < 0 || en(vec, PISO + 2) != AIRE) {
					return false;
				}
			}
		}
		return true;
	}

	private java.util.List<Detalle> detallesSalida(Salida s) {
		java.util.List<Detalle> out = new java.util.ArrayList<>();
		int mira = s.mira();
		for (int a = 1; a <= 2; a++) {
			int[] p = s.pared(a);
			for (int f = 0; f < 3; f++) {
				out.add(new Detalle(p[0], PISO + f, p[1], D_PUERTA, (a - 1) * 3 + f, mira));
			}
			int[] c = s.columna(a);
			out.add(new Detalle(c[0], PISO + 3, c[1], D_CARTEL, CARTEL_GRANDE + a - 1, mira));
		}
		for (int color = 0; color < 3; color++) {
			int[] l = s.lector(color);
			out.add(new Detalle(l[0], l[1], l[2], D_LECTOR, color, mira));
		}
		return java.util.List.copyOf(out);
	}

	/**
	 * Las VALVULAS valvulas: una en cada casilla de una cuadricula de 5x5 sobre el mapa (unas
	 * 360 bloques entre vecinas), movida un poco al azar, en una pared con suelo seco delante.
	 */
	public java.util.List<Valvula> valvulas() {
		java.util.List<Valvula> l = this.valvulas;
		if (l == null) {
			java.util.List<Valvula> nuevas = new java.util.ArrayList<>();
			Salida s = this.salida();
			double paso = 2.0 * MAPA / 5;
			for (int id = 0; id < VALVULAS; id++) {
				int tx = (int) Math.round(-MAPA + (id % 5 + 0.5) * paso + (this.azar(id, 1, 98) - 0.5) * 160);
				int tz = (int) Math.round(-MAPA + (id / 5 + 0.5) * paso + (this.azar(id, 2, 98) - 0.5) * 160);
				Valvula v = this.buscarValvula(id, Math.floorDiv(tx, C), Math.floorDiv(tz, C), s);
				if (v != null) {
					nuevas.add(v);
				}
			}
			l = java.util.List.copyOf(nuevas);
			this.valvulas = l;
		}
		return l;
	}

	private Valvula buscarValvula(int id, int cx0, int cz0, Salida s) {
		byte[] col = new byte[ALTO];
		byte[] vec = new byte[ALTO];
		for (int r = 0; r <= 5; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int cx = cx0 + dx;
					int cz = cz0 + dz;
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r || this.sala(cx, cz).oscura() || cx == s.celdaX() && cz == s.celdaZ()) {
						continue;
					}
					for (int t = 0; t < 70; t++) {
						int[] c = this.candidata(cx, cz, 600 + id, t);
						this.columna(c[0], c[1], col);
						if (!this.pisable(c[0], PISO, c[1], col) || en(col, PISO + 2) != AIRE) {
							continue;
						}
						for (int lado = 0; lado < 4; lado++) {
							this.columna(c[0] + DX[lado], c[1] + DZ[lado], vec);
							if (macizoEn(vec, PISO) && macizoEn(vec, PISO + 1) && macizoEn(vec, PISO + 2)) {
								return new Valvula(id, c[0], PISO + 1, c[1], (lado + 2) % 4);
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Donde esta escondida la tarjeta de ese color: {x, y, z} (el bloque donde queda tirada).
	 * Cada una en una direccion desde la puerta y cada vez mas dificil: la verde a ~260 bloques
	 * en una sala con luz; la amarilla a ~480, en una sala a oscuras; la roja a ~700, en el
	 * fondo de una piscina honda (hay que bucear) y con una Serpiente que la guarda (Tarjetas).
	 */
	public int[] tarjeta(int color) {
		int[] t = this.tarjetas[color];
		if (t == null) {
			t = this.buscarTarjeta(color);
			this.tarjetas[color] = t;
		}
		return t;
	}

	private int[] buscarTarjeta(int color) {
		Salida s = this.salida();
		double base = this.azar(1, 2, 99) * Math.PI * 2;
		double dist = color == T_VERDE ? 260 : color == T_AMARILLA ? 480 : 700;
		byte[] col = new byte[ALTO];
		for (int intento = 0; intento < 400; intento++) {
			// en espiral alrededor del punto ideal: primero cerca de la direccion y la distancia
			double ang = base + color * Math.PI * 2 / 3 + (intento % 2 == 0 ? 1 : -1) * (intento / 2) * 0.012;
			double d = dist + ((intento / 7) % 5 - 2) * 30;
			int cx = Math.floorDiv(s.x() + (int) Math.round(Math.cos(ang) * d), C);
			int cz = Math.floorDiv(s.z() + (int) Math.round(Math.sin(ang) * d), C);
			Sala sala = this.sala(cx, cz);
			boolean vale = switch (color) {
				case T_ROJA -> sala.tipo() == SALA && !sala.oscura();
				case T_AMARILLA -> sala.oscura();
				default -> !sala.oscura();
			};
			if (!vale) {
				continue;
			}
			for (int t = 0; t < 70; t++) {
				int[] c = this.candidata(cx, cz, 700 + color, t);
				this.columna(c[0], c[1], col);
				if (color == T_ROJA) {
					// agua de 3 o mas: la tarjeta queda en el fondo
					if (en(col, SUELO) == AGUA && en(col, SUELO - 1) == AGUA && en(col, SUELO - 2) == AGUA) {
						int y = SUELO - 2;
						while (y > Y0 && en(col, y - 1) == AGUA) {
							y--;
						}
						return new int[] {c[0], y, c[1]};
					}
				} else if (this.pisable(c[0], PISO, c[1], col) && en(col, PISO + 2) == AIRE) {
					return new int[] {c[0], PISO, c[1]};
				}
			}
		}
		int[] c = s.columna(0);
		return new int[] {c[0], PISO, c[1]};
	}

	/* ----------------------------------------------------------- comida */

	/** Lo que hay tirado para comer (los mismos numeros que Plano: 2 galletas, 3 pizza). */
	public static final int C_GALLETAS = 2;
	public static final int C_PIZZA = 3;
	private static final int[] SIN_COMIDA = new int[0];
	private final java.util.concurrent.ConcurrentHashMap<Long, int[]> comidas = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * La comida tirada en la celda: {x, z, tipo} en un sitio seco, o null. Con la misma
	 * densidad que en el Nivel 0 (unas 0,17 cosas por cada 24x24): galletas o pizza. El agua
	 * de almendras no sale de aqui: la reparte Comida alrededor de donde llega cada uno.
	 */
	public int[] comidaEnCelda(int cx, int cz) {
		long k = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		int[] c = this.comidas.get(k);
		if (c == null) {
			c = SIN_COMIDA;
			double r = this.azar(cx, cz, 95);
			int tipo = r < 0.11 ? C_GALLETAS : r < 0.17 ? C_PIZZA : 0;
			if (tipo != 0) {
				java.util.List<Detalle> sitio = new java.util.ArrayList<>();
				this.suelo(sitio, cx, cz, 9, D_MANCHA, 0);
				if (!sitio.isEmpty()) {
					c = new int[] {sitio.get(0).x(), sitio.get(0).z(), tipo};
				}
			}
			if (this.comidas.size() > 20000) {
				this.comidas.clear();
			}
			this.comidas.put(k, c);
		}
		return c.length == 0 ? null : c;
	}

	/* ---------------------------------------------------------- llegada */

	/** Se puede estar de pie en (x, y, z): suelo macizo y seco, y dos de aire encima. */
	public boolean pisable(int x, int y, int z, byte[] col) {
		byte suelo = en(col, y - 1);
		return suelo != AIRE && suelo != AGUA && suelo != LOSA_AGUA && en(col, y) == AIRE && en(col, y + 1) == AIRE;
	}

	/**
	 * Altura a la que se queda de pie en la planta de abajo, mojandose los pies si hace
	 * falta (agua de un palmo o suelo encharcado), o -1 si ahi no se puede estar.
	 */
	public double dePie(byte[] col) {
		byte s = en(col, SUELO);
		if (en(col, PISO) != AIRE || en(col, PISO + 1) != AIRE) {
			return -1;
		}
		if (s == LOSA_AGUA) {
			return SUELO + 0.5;
		}
		if (s == AGUA) {
			byte f = en(col, SUELO - 1);
			return f != AGUA && f != AIRE ? SUELO : -1;
		}
		return s != AIRE ? PISO : -1;
	}
}
