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

	/* -------------------------------------------------------- zonas */

	/** Zona de un nodo de la reticula. */
	public int zona(int gx, int gz) {
		int x = gx * G;
		int z = gz * G;
		if (x * x + z * z <= LLEGADA * LLEGADA) {
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
		if (x * x + z * z <= (LLEGADA + 20) * (LLEGADA + 20)) {
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

	/** Papel pintado manchado: cerca de la moqueta mojada y algun lamparon suelto. */
	public boolean sucio(int x, int y, int z) {
		double m = ruido2(x / 22.0, z / 22.0, 40);
		if (m > 0.66 && y <= 2) {
			return true;
		}
		return ruido(x / 7.0, z / 7.0 + y * 0.35, 41) > 0.82;
	}
}
