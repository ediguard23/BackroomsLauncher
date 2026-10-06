package net.backrooms.evento.mundo;

/**
 * El trazado del Nivel 0: dice, para cada columna (x, z) del mundo, si hay
 * pared, que hay en el techo y como esta la moqueta. Es Java puro (sin
 * Minecraft) para poder dibujarlo en una imagen y revisarlo (ver Mapa).
 *
 * Todo sale de funciones de (x, z, semilla): cualquier chunk se genera sin
 * mirar a sus vecinos, en cualquier orden y en paralelo, y el mapa es tan
 * grande como haga falta.
 *
 * Las paredes van sobre una reticula de G bloques. Cada tramo entre dos nodos
 * existe o no segun la zona, y si existe puede tener un hueco de puerta. Las
 * zonas cambian despacio (ruido de baja frecuencia):
 *  - SALAS: el Nivel 0 de siempre, habitaciones irregulares que se abren unas
 *    a otras.
 *  - LABERINTO: tramos casi todos cerrados y pocas puertas; facil perderse.
 *  - PASILLOS: corredores largos y paralelos con cruces de vez en cuando.
 *  - SALON: salas enormes, solo pilares; donde se juntan los grupos.
 * Aparte, otra capa de ruido marca las zonas a oscuras (tubos apagados) y
 * la moqueta mojada.
 */
public final class Plano {
	/** Separacion de la reticula de paredes. */
	public static final int G = 6;
	/** Radio de la sala de llegada alrededor de (0, 0): sin paredes, solo pilares. */
	public static final int LLEGADA = 36;

	public static final int SALON = 0;
	public static final int SALAS = 1;
	public static final int LABERINTO = 2;
	public static final int PASILLOS = 3;

	/** Lo que hay en el techo de una columna. */
	public static final int TECHO = 0;
	public static final int TUBO = 1;
	public static final int TUBO_APAGADO = 2;
	public static final int TUBO_PARPADEO = 3;

	private final long semilla;

	public Plano(long semilla) {
		this.semilla = semilla;
	}

	/* --------------------------------------------------------- azar */

	private long hash(long a, long b, long canal) {
		long h = this.semilla ^ (canal * 0x9E3779B97F4A7C15L);
		h ^= a * 0xC2B2AE3D27D4EB4FL;
		h = Long.rotateLeft(h, 31) * 0x165667B19E3779F9L;
		h ^= b * 0x27D4EB2F165667C5L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return h;
	}

	/** Numero en [0, 1) fijo para (a, b, canal). */
	private double azar(long a, long b, long canal) {
		return (hash(a, b, canal) >>> 11) * 0x1.0p-53;
	}

	/** Ruido de valor suave en [0, 1): cambia poco a poco con (x, z). */
	private double ruido(double x, double z, long canal) {
		long x0 = (long) Math.floor(x);
		long z0 = (long) Math.floor(z);
		double fx = x - x0;
		double fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		double a = azar(x0, z0, canal);
		double b = azar(x0 + 1, z0, canal);
		double c = azar(x0, z0 + 1, canal);
		double d = azar(x0 + 1, z0 + 1, canal);
		return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
	}

	/** Dos octavas: zonas con borde algo irregular. */
	private double ruido2(double x, double z, long canal) {
		return ruido(x, z, canal) * 0.7 + ruido(x * 2.7, z * 2.7, canal + 100) * 0.3;
	}

	/* ------------------------------------------- ascensores de salida */

	/** Cada SUPER celdas de la reticula (642 bloques) hay un ascensor de salida. */
	public static final int SUPER = 107;

	/**
	 * Ascensor de salida: ocupa la celda (gx, gz) entera, con paredes de acero
	 * en sus cuatro lados y la puerta (2 de ancho) en el lado `mira`
	 * (0 N, 1 E, 2 S, 3 O). Alrededor siempre hay un salon iluminado, para que
	 * se vea desde lejos.
	 */
	public record Ascensor(int gx, int gz, int mira) {
		public int centroX() {
			return this.gx * G + 3;
		}

		public int centroZ() {
			return this.gz * G + 3;
		}
	}

	/** El ascensor de la super celda que contiene el nodo (gx, gz). */
	public Ascensor ascensor(int gx, int gz) {
		int sx = Math.floorDiv(gx, SUPER);
		int sz = Math.floorDiv(gz, SUPER);
		return new Ascensor(
			sx * SUPER + 16 + (int) (azar(sx, sz, 90) * (SUPER - 32)),
			sz * SUPER + 16 + (int) (azar(sx, sz, 91) * (SUPER - 32)),
			(int) (azar(sx, sz, 92) * 4));
	}

	/** El ascensor mas cercano a la columna (x, z). */
	public Ascensor ascensorCercano(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		int sx = Math.floorDiv(gx, SUPER);
		int sz = Math.floorDiv(gz, SUPER);
		Ascensor mejor = null;
		long dMejor = Long.MAX_VALUE;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				Ascensor a = this.ascensor((sx + dx) * SUPER, (sz + dz) * SUPER);
				long ax = a.centroX() - x;
				long az = a.centroZ() - z;
				long d = ax * ax + az * az;
				if (d < dMejor) {
					dMejor = d;
					mejor = a;
				}
			}
		}
		return mejor;
	}

	private boolean cercaAscensor(int gx, int gz, int radio) {
		Ascensor a = this.ascensor(gx, gz);
		return Math.max(Math.abs(gx - a.gx), Math.abs(gz - a.gz)) <= radio;
	}

	public static final int ASC_NO = 0;
	public static final int ASC_DENTRO = 1;
	public static final int ASC_PARED = 2;
	public static final int ASC_PUERTA = 3;

	/** Que parte del ascensor hay en la columna (x, z): nada, dentro, pared o el hueco de la puerta. */
	public int ascensorEn(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		Ascensor a = this.ascensor(gx, gz);
		int lx = x - a.gx * G;
		int lz = z - a.gz * G;
		if (lx < 0 || lz < 0 || lx > G || lz > G) {
			return ASC_NO;
		}
		if (lx > 0 && lx < G && lz > 0 && lz < G) {
			return ASC_DENTRO;
		}
		return this.pared(x, z) ? ASC_PARED : ASC_PUERTA;
	}

	private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

	/**
	 * Si (x, z) es la columna de fuera, delante de la puerta de un ascensor,
	 * hacia donde mira el cartel de SALIDA que va encima (0 N .. 3 O); si no, -1.
	 */
	public int letreroAscensor(int x, int z) {
		Ascensor a = this.ascensor(Math.floorDiv(x, G), Math.floorDiv(z, G));
		int[] d = DIRS[a.mira];
		// columna del hueco de la puerta que queda detras de (x, z)
		int px = x - d[0];
		int pz = z - d[1];
		return this.ascensorEn(px, pz) == ASC_PUERTA && this.ascensor(Math.floorDiv(px, G), Math.floorDiv(pz, G)).equals(a) ? a.mira : -1;
	}

	/** Si (x, z) es donde va el panel de botones dentro del ascensor, hacia donde mira; si no, -1. */
	public int panelAscensor(int x, int z) {
		Ascensor a = this.ascensor(Math.floorDiv(x, G), Math.floorDiv(z, G));
		int cx = a.gx * G + 3;
		int cz = a.gz * G + 3;
		// en la pared de enfrente de la puerta, mirando hacia ella
		int[] d = DIRS[a.mira];
		int px = cx - d[0] * 2;
		int pz = cz - d[1] * 2;
		return x == px && z == pz ? a.mira : -1;
	}

	/* -------------------------------------------------------- comida */

	public static final int C_NADA = 0;
	public static final int C_AGUA = 1;
	public static final int C_GALLETAS = 2;
	public static final int C_PIZZA = 3;

	/**
	 * Comida tirada en (x, z): agua de almendras (la unica que sube la
	 * cordura), galletas o pizza. Como mucho una por celda y en suelo libre.
	 * Es fija para cada mundo; lo que ya se ha cogido lo apunta Comida.
	 */
	public int comida(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		if (x - gx * G != 1 + (int) (azar(gx, gz, 95) * 5) || z - gz * G != 1 + (int) (azar(gx, gz, 96) * 5)) {
			return C_NADA;
		}
		double r = azar(gx, gz, 97);
		// el agua de almendras ya no sale del plano: van 2 por jugador junto a donde llega (Comida.repartirAguas)
		int c = r < 0.0035 ? C_NADA : r < 0.0105 ? C_GALLETAS : r < 0.014 ? C_PIZZA : C_NADA;
		if (c == C_NADA || this.cercaAscensor(gx, gz, 1) || this.pared(x, z) || this.decoracion(x, z).tipo() != D_NADA) {
			return C_NADA;
		}
		return c;
	}

	/** La comida de la celda (gx, gz): {x, z, tipo}, o null si no tiene. */
	public int[] comidaEnCelda(int gx, int gz) {
		int x = gx * G + 1 + (int) (azar(gx, gz, 95) * 5);
		int z = gz * G + 1 + (int) (azar(gx, gz, 96) * 5);
		int c = this.comida(x, z);
		return c == C_NADA ? null : new int[] {x, z, c};
	}

	/* -------------------------------------------------------- zonas */

	/** Zona de un nodo de la reticula. */
	public int zona(int gx, int gz) {
		int x = gx * G;
		int z = gz * G;
		if (x * x + z * z <= LLEGADA * LLEGADA || this.cercaAscensor(gx, gz, 3)) {
			return SALON;
		}
		double r = ruido2(x / 110.0, z / 110.0, 1);
		if (r < 0.24) {
			return SALON;
		}
		if (r < 0.62) {
			return SALAS;
		}
		if (r < 0.8) {
			return LABERINTO;
		}
		return PASILLOS;
	}

	/** Probabilidad de que exista un tramo de pared, por zona y orientacion. */
	private static double probTramo(int zona, boolean alongX) {
		return switch (zona) {
			case SALON -> 0.0;
			case SALAS -> 0.46;
			case LABERINTO -> 0.68;
			default -> alongX ? 0.1 : 0.92; // PASILLOS: corredores a lo largo de z
		};
	}

	private static double probPuerta(int zona) {
		return switch (zona) {
			case SALAS -> 0.55;
			case LABERINTO -> 0.3;
			default -> 0.18;
		};
	}

	/**
	 * Tramo de la reticula que sale del nodo (gx, gz): a lo largo de x si
	 * `alongX`, si no a lo largo de z. Devuelve -2 si no hay pared, -1 si es
	 * pared entera y >= 1 la posicion (dentro del tramo) donde empieza el hueco
	 * de puerta de 2 bloques.
	 */
	private int tramo(int gx, int gz, boolean alongX) {
		// las cuatro paredes del ascensor, con la puerta en su lado
		Ascensor a = this.ascensor(gx, gz);
		if (alongX && gx == a.gx && (gz == a.gz || gz == a.gz + 1)) {
			return a.mira == (gz == a.gz ? 0 : 2) ? 2 : -1;
		}
		if (!alongX && gz == a.gz && (gx == a.gx || gx == a.gx + 1)) {
			return a.mira == (gx == a.gx ? 3 : 1) ? 2 : -1;
		}
		// la zona del tramo es la de su punto medio, para que no dependa de que nodo se mire
		int zona = alongX ? zonaMedia(gx, gz, gx + 1, gz) : zonaMedia(gx, gz, gx, gz + 1);
		long canal = alongX ? 11 : 12;
		if (azar(gx, gz, canal) >= probTramo(zona, alongX)) {
			return -2;
		}
		// la celda (gx, gz) tiene este tramo de pared norte (alongX) u oeste: si es su salida
		// obligada, lleva puerta
		boolean salida = abreAlNorte(gx, gz) == alongX;
		if (salida || azar(gx, gz, canal + 2) < probPuerta(zona)) {
			return 1 + (int) (azar(gx, gz, canal + 4) * (G - 3));
		}
		return -1;
	}

	/**
	 * Salida obligada de cada celda: al norte o al oeste (laberinto en "arbol
	 * binario"). Cada celda queda unida a otra mas al noroeste, asi que todo el
	 * mapa esta conectado y no hay bolsas cerradas donde aparecer atrapado. En
	 * los pasillos casi siempre es al norte, para no partir los corredores.
	 */
	private boolean abreAlNorte(int gx, int gz) {
		double sesgo = zona(gx, gz) == PASILLOS ? 0.9 : 0.5;
		return azar(gx, gz, 50) < sesgo;
	}

	private int zonaMedia(int ax, int az, int bx, int bz) {
		int za = zona(ax, az);
		int zb = zona(bx, bz);
		// en la frontera manda la zona mas abierta: no quedan paredes sueltas en un salon
		return Math.min(za, zb) == SALON ? SALON : za;
	}

	/** true si en la columna (x, z) hay pared o pilar. */
	public boolean pared(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		int lx = x - gx * G;
		int lz = z - gz * G;
		if (lx == 0 && lz == 0) {
			return pilar(gx, gz);
		}
		if (lz == 1 && lx >= 1 && lx <= 5 && this.tramoHueco(gx, gz, true)) {
			return true;
		}
		if (lx == 1 && lz >= 1 && lz <= 5 && this.tramoHueco(gx, gz, false)) {
			return true;
		}
		if (lz == 0) {
			int t = tramo(gx, gz, true);
			return t == -1 || (t >= 1 && (lx < t || lx > t + 1));
		}
		if (lx == 0) {
			int t = tramo(gx, gz, false);
			return t == -1 || (t >= 1 && (lz < t || lz > t + 1));
		}
		return false;
	}

	/** Nodo de la reticula: pilar si llega alguna pared, o los pilares sueltos de los salones. */
	private boolean pilar(int gx, int gz) {
		if (tramo(gx, gz, true) != -2 || tramo(gx, gz, false) != -2
			|| tramo(gx - 1, gz, true) != -2 || tramo(gx, gz - 1, false) != -2) {
			return true;
		}
		if (zona(gx, gz) == SALON) {
			return Math.floorMod(gx, 2) == 0 && Math.floorMod(gz, 2) == 0 && azar(gx, gz, 20) < 0.75;
		}
		return false;
	}

	/* --------------------------------------------------- techo y suelo */

	/** 0 = bien iluminado, 1 = transicion (parpadeos), 2 = a oscuras. */
	public int oscuridad(int x, int z) {
		if (x * x + z * z <= (LLEGADA + 20) * (LLEGADA + 20)
			|| this.cercaAscensor(Math.floorDiv(x, G), Math.floorDiv(z, G), 4)) {
			return 0;
		}
		double o = ruido2(x / 64.0, z / 64.0, 7);
		return o > 0.7 ? 2 : o > 0.6 ? 1 : 0;
	}

	/** Lo que hay en el techo encima de (x, z). Los tubos son placas de 2x2 en el centro de cada celda. */
	public int techo(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		int lx = x - gx * G;
		int lz = z - gz * G;
		if ((lz != 2 && lz != 3) || (lx != 2 && lx != 3)) {
			return TECHO;
		}
		int cx = gx * G + 3;
		int cz = gz * G + 3;
		double r = azar(gx, gz, 30);
		return switch (oscuridad(cx, cz)) {
			case 2 -> r < 0.12 ? TUBO_PARPADEO : r < 0.85 ? TUBO_APAGADO : TECHO;
			case 1 -> r < 0.3 ? TUBO_PARPADEO : r < 0.62 ? TUBO_APAGADO : TUBO;
			default -> r < 0.04 ? TUBO_PARPADEO : r < 0.07 ? TUBO_APAGADO : TUBO;
		};
	}

	/** Moqueta mojada: charcos grandes e irregulares. */
	public boolean mojado(int x, int z) {
		return ruido2(x / 22.0, z / 22.0, 40) > 0.72;
	}

	/* ------------------------------------------------------ decoracion */

	public static final int D_NADA = 0;
	public static final int D_ENCHUFE = 1;
	public static final int D_ENCHUFE_MANCHADO = 2;
	public static final int D_DIBUJO = 3;
	public static final int D_SALIDA = 4;
	public static final int D_NOTA = 5;
	public static final int D_SILLA = 6;
	public static final int D_SILLA_VOLCADA = 7;
	public static final int D_SENAL = 8;
	public static final int D_VENA = 9;

	/** Decoracion de una columna libre: tipo, hacia donde mira (0 N, 1 E, 2 S, 3 O) y variante. */
	public record Deco(int tipo, int mira, int variante) {
	}

	public static final Deco NADA = new Deco(D_NADA, 0, 0);

	/** Pared maciza (no fina) en (x, z): donde se puede pegar algo (y que no tape un hueco). */
	private boolean paredMaciza(int x, int z) {
		return this.pared(x, z) && this.paredFina(x, z) == 0 && this.celdaHueca(x, z) == null;
	}

	/**
	 * Que hay en el suelo o en la pared de una columna libre. Pegado a una
	 * pared: enchufes, pintadas, cintas y (muy raro) un cartel de salida. En
	 * medio: notas, sillas hundidas y senales. A oscuras hay mas pintadas y el
	 * suelo se llena de venas de Hay Bacillus.
	 */
	public Deco decoracion(int x, int z) {
		if (this.cercaAscensor(Math.floorDiv(x, G), Math.floorDiv(z, G), 1)) {
			return NADA;
		}
		boolean oscuro = this.oscuridad(x, z) == 2;
		if (oscuro && ruido2(x / 9.0, z / 9.0, 70) > 0.71) {
			return new Deco(D_VENA, 0, 0);
		}
		int[] lados = new int[4];
		int n = 0;
		if (this.paredMaciza(x, z - 1)) {
			lados[n++] = 2;
		}
		if (this.paredMaciza(x + 1, z)) {
			lados[n++] = 3;
		}
		if (this.paredMaciza(x, z + 1)) {
			lados[n++] = 0;
		}
		if (this.paredMaciza(x - 1, z)) {
			lados[n++] = 1;
		}
		double r = azar(x, z, 60);
		if (n > 0) {
			int mira = lados[(int) (azar(x, z, 61) * n)];
			double pintadas = oscuro ? 0.022 : 0.011;
			if (r < 0.009) {
				return new Deco(this.sucio(x, 2, z) || azar(x, z, 62) < 0.25 ? D_ENCHUFE_MANCHADO : D_ENCHUFE, mira, 0);
			}
			if (r < 0.009 + pintadas) {
				return new Deco(D_DIBUJO, mira, (int) (azar(x, z, 63) * 12));
			}
			if (r < 0.009 + pintadas + 0.004) {
				return new Deco(D_DIBUJO, mira, azar(x, z, 64) < 0.5 ? 12 : 13);
			}
			if (r < 0.009 + pintadas + 0.0042) {
				return new Deco(D_SALIDA, mira, 0);
			}
			return NADA;
		}
		int mira = (int) (azar(x, z, 65) * 4);
		if (r < 0.0010) {
			return new Deco(D_NOTA, mira, 0);
		}
		if (r < 0.0026) {
			return new Deco(azar(x, z, 66) < 0.4 ? D_SILLA_VOLCADA : D_SILLA, mira, 0);
		}
		if (r < 0.0031) {
			return new Deco(D_SENAL, mira, (int) (azar(x, z, 67) * 5));
		}
		return NADA;
	}

	/** Ventilador colgado del techo en (x, z): 0 ninguno, 1 normal, 2 grande. Uno como mucho por celda. */
	public int ventilador(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		if (x - gx * G != 4 || z - gz * G != 4 || this.cercaAscensor(gx, gz, 1)) {
			return 0;
		}
		boolean salon = this.zona(gx, gz) == SALON;
		if (azar(gx, gz, 68) >= (salon ? 0.08 : 0.045)) {
			return 0;
		}
		return salon && azar(gx, gz, 69) < 0.4 ? 2 : 1;
	}

	/**
	 * Pared fina: algunos tramos de las salas son un tabique delgado en vez
	 * de pared maciza. 0 = no, 1 = tramo a lo largo de x, 2 = a lo largo de z.
	 */
	public int paredFina(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		int lx = x - gx * G;
		int lz = z - gz * G;
		if (lx == 0 && lz == 0) {
			return 0;
		}
		boolean alongX = lz == 0;
		if (!alongX && lx != 0) {
			return 0;
		}
		int zona = alongX ? zonaMedia(gx, gz, gx + 1, gz) : zonaMedia(gx, gz, gx, gz + 1);
		if (zona != SALAS || azar(gx, gz, alongX ? 71 : 72) >= 0.2) {
			return 0;
		}
		return alongX ? 1 : 2;
	}

	/* ----------------------------------------------------- paredes huecas */

	public static final int CARA_NADA = 0;
	public static final int CARA_ENTERA = 1;
	public static final int CARA_ROTA = 2;

	/**
	 * Un bloque de pared hueca: que hay en cada cara (CARA_*, en el orden N, E, S, O)
	 * y, si una cara es el boquete, si este es su cuarto izquierdo visto desde fuera.
	 */
	public record CeldaHueca(int[] caras, boolean izq) {
	}

	/**
	 * Tramo de pared DOBLE y hueco por dentro (escondite/Hueco), como en Escape the
	 * Backrooms: en vez de una fila de pared ocupa dos (la suya y la siguiente hacia el
	 * sur o el este) en las columnas 1..5 del tramo, y por dentro es aire de punta a
	 * punta. Solo en tramos enteros (sin puerta) de salas, laberinto o pasillos, lejos
	 * de los ascensores, en uno de cada 6 o 7. Del mismo nodo no salen dos (se
	 * cruzarian en la esquina).
	 */
	public boolean tramoHueco(int gx, int gz, boolean alongX) {
		if (this.tramo(gx, gz, alongX) != -1 || this.cercaAscensor(gx, gz, 2)) {
			return false;
		}
		if (azar(gx, gz, alongX ? 93 : 94) >= 0.16) {
			return false;
		}
		// ni tabique fino ni bacilo
		int x = gx * G + (alongX ? 3 : 0);
		int z = gz * G + (alongX ? 0 : 3);
		if (this.paredFina(x, z) != 0 || this.paredBacilo(x, z)) {
			return false;
		}
		return alongX || !this.tramoHueco(gx, gz, true);
	}

	/**
	 * El boquete de un lado (0 norte u oeste, 1 sur o este) de un tramo hueco: la
	 * columna donde empieza (ocupa esa y la siguiente, 1..5), o -1 si ese lado esta
	 * entero. Al menos uno de los dos lados tiene boquete si hay suelo libre delante.
	 */
	private int boquete(int gx, int gz, boolean alongX, int lado) {
		long c = alongX ? 95 : 96;
		int principal = azar(gx, gz, c + 10) < 0.5 ? 0 : 1;
		if (lado != principal && azar(gx, gz, c + 20 + lado) >= 0.45) {
			return -1;
		}
		int p = 1 + (int) (azar(gx, gz, c + 30 + lado) * 4);
		// delante del boquete tiene que haber suelo
		for (int k = 0; k < 2; k++) {
			int a = (alongX ? gx : gz) * G + p + k;
			int fila = (alongX ? gz : gx) * G + (lado == 0 ? -1 : 2);
			if (alongX ? this.pared(a, fila) : this.pared(fila, a)) {
				return -1;
			}
		}
		return p;
	}

	/** Que tiene el bloque de pared hueca de la columna (x, z), o null si no es pared hueca. */
	public CeldaHueca celdaHueca(int x, int z) {
		int gx = Math.floorDiv(x, G);
		int gz = Math.floorDiv(z, G);
		int lx = x - gx * G;
		int lz = z - gz * G;
		if (lx >= 1 && lx <= 5 && (lz == 0 || lz == 1) && this.tramoHueco(gx, gz, true)) {
			// tramo a lo largo de x: fila 0 da al norte, fila 1 al sur
			int[] caras = {lz == 0 ? CARA_ENTERA : CARA_NADA, lx == 5 ? CARA_ENTERA : CARA_NADA,
				lz == 1 ? CARA_ENTERA : CARA_NADA, lx == 1 ? CARA_ENTERA : CARA_NADA};
			int p = this.boquete(gx, gz, true, lz);
			boolean izq = false;
			if (p > 0 && (lx == p || lx == p + 1)) {
				caras[lz == 0 ? 0 : 2] = CARA_ROTA;
				// visto desde el norte la izquierda es la x menor; desde el sur, la mayor
				izq = (lx == p) == (lz == 0);
			}
			return new CeldaHueca(caras, izq);
		}
		if (lz >= 1 && lz <= 5 && (lx == 0 || lx == 1) && this.tramoHueco(gx, gz, false)) {
			// tramo a lo largo de z: columna 0 da al oeste, columna 1 al este
			int[] caras = {lz == 1 ? CARA_ENTERA : CARA_NADA, lx == 1 ? CARA_ENTERA : CARA_NADA,
				lz == 5 ? CARA_ENTERA : CARA_NADA, lx == 0 ? CARA_ENTERA : CARA_NADA};
			int p = this.boquete(gx, gz, false, lx);
			boolean izq = false;
			if (p > 0 && (lz == p || lz == p + 1)) {
				caras[lx == 0 ? 3 : 1] = CARA_ROTA;
				// visto desde el este la izquierda es la z menor; desde el oeste, la mayor
				izq = (lz == p) == (lx == 1);
			}
			return new CeldaHueca(caras, izq);
		}
		return null;
	}

	/** Pared cubierta de Hay Bacillus (solo a oscuras). */
	public boolean paredBacilo(int x, int z) {
		return this.oscuridad(x, z) == 2 && ruido2(x / 7.0, z / 7.0, 73) > 0.6;
	}

	/** Papel pintado manchado: cerca de la moqueta mojada y algun lamparon suelto. */
	public boolean sucio(int x, int y, int z) {
		double m = ruido2(x / 22.0, z / 22.0, 40);
		if (m > 0.66 && y <= 2) {
			return true;
		}
		return ruido(x / 7.0, z / 7.0 + y * 0.35, 41) > 0.82;
	}
}
