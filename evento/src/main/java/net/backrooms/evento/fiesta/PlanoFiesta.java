package net.backrooms.evento.fiesta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El trazado de la Fiesta (Nivel Fun, las Feeding Rooms): un edificio cerrado, hecho a mano,
 * que GeneradorFiesta pone al generar cada chunk a partir de una lista de cajas de bloques
 * (como el vestibulo; las de despues pisan a las de antes). Suelo en y = SUELO (se pisa en
 * SUELO + 1), lejos del fondo del mundo para que la niebla no se vuelva negra.
 *
 * Planta (x hacia el este, z hacia el sur):
 *
 *             COCINA (1 moneda)        PREMIO          SIESTA (2 monedas)
 *             x -44..-9, z -64..-42   x -6..6         x 9..44, z -64..-42
 *                   |  pasillo        z -44..-26           pasillo  |
 *   LABERINTO       +-- torno ---- SALON ---- torno ---------------+
 *   x -69..-27      (abierto)    x -24..24     (2 monedas) SEMAFORO x 26..70, z -7..5
 *   z -39..27  -----------------  z -24..28  -------------
 *   (sala secreta: 3 monedas)        |
 *                                 LLEGADA x -8..8, z 30..46 (+ el ascensor de las Piscinas)
 *
 * Se llega en el ascensor (Fiesta#llegar): la llegada es un vestibulo de fiesta con su arco de
 * globos y la taquilla; el salon, la fiesta de verdad (mesas con tartas, regalos, el escenario
 * y, al norte, la puerta del Gran Premio, que pide los tickets del objetivo). Las monedas y los
 * tickets estan en sitios fijos (MONEDAS, TICKETS: los coge cada jugador una vez, JuegoFiesta).
 */
public final class PlanoFiesta {
	public static final int SUELO = 40;
	/** Techo normal (se puede estar de pie de SUELO + 1 a TECHO - 1). */
	public static final int TECHO = 45;
	public static final int TECHO_SALON = 52;
	public static final int TECHO_SEMAFORO = 50;
	/** Limites del edificio (con sus muros), para el generador. */
	public static final int X0 = -71;
	public static final int X1 = 72;
	public static final int Z0 = -66;
	public static final int Z1 = 54;
	public static final int Y0 = SUELO;
	public static final int Y1 = TECHO_SALON;

	/** Donde aparece quien llega: dentro del ascensor, mirando a la llegada (norte). */
	public static final double LLEGADA_X = 0.5;
	public static final double LLEGADA_Y = SUELO + 1;
	public static final double LLEGADA_Z = 50.5;

	/* el laberinto: celdas de CELDA (3 de paso y 1 de pared) */
	public static final int LAB_X = -70;
	public static final int LAB_Z = -40;
	public static final int LAB_W = 11;
	public static final int LAB_H = 17;
	public static final int CELDA = 4;

	/* el pasillo del semaforo */
	public static final int SEM_X0 = 26;
	public static final int SEM_X1 = 70;
	public static final int SEM_Z0 = -7;
	public static final int SEM_Z1 = 5;
	/** Se sale de la zona de salida al cruzar x = SEM_SALIDA; se gana al llegar a x = SEM_META. */
	public static final int SEM_SALIDA = 31;
	public static final int SEM_META = 64;

	/* la sala del Gran Premio: quien se queda en el regalo gigante (x -2..2, z -44..-41) pasa */
	public static final int PREMIO_X0 = -2;
	public static final int PREMIO_X1 = 2;
	public static final int PREMIO_Z0 = -44;
	public static final int PREMIO_Z1 = -41;

	@FunctionalInterface
	public interface Patron {
		BlockState en(int x, int y, int z);
	}

	public record Caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron patron) {
	}

	/** Un punto donde hay algo que coger: id fijo (lo que se guarda), sitio y cuanto vale. */
	public record Punto(int id, double x, double y, double z, int valor) {
	}

	/** Un torno: la casilla de abajo, hacia donde mira (el lado que paga) y lo que cuesta. */
	public record Puerta(int id, int x, int y, int z, Direction mira, int coste, String nombre) {
	}

	public static final List<Caja> CAJAS = new ArrayList<>();
	public static final List<Punto> MONEDAS = new ArrayList<>();
	public static final List<Punto> TICKETS = new ArrayList<>();
	public static final List<Puerta> TORNOS = new ArrayList<>();
	/** Altavoces (x, y, z): de ahi sale la musica lejana y los anuncios de la megafonia. */
	public static final List<int[]> ALTAVOCES = new ArrayList<>();
	/** Sitios por donde puede aparecer y pasear el Anfitrion (x, z, a la altura del suelo). */
	public static final List<int[]> RONDA = new ArrayList<>();
	/** Las lamparas del semaforo (x, y, z), en la pared del fondo del pasillo. */
	public static final List<int[]> SEMAFORO_ROJO = new ArrayList<>();
	public static final List<int[]> SEMAFORO_VERDE = new ArrayList<>();
	/** Las salas, para saber donde esta cada uno (sonidos, HUD): nombre y caja x0, z0, x1, z1. */
	public record Sala(String nombre, int x0, int z0, int x1, int z1) {
		public boolean dentro(double x, double z) {
			return x >= this.x0 && x < this.x1 + 1 && z >= this.z0 && z < this.z1 + 1;
		}
	}

	public static final List<Sala> SALAS = new ArrayList<>();

	/* ------------------------------------------------ suelos defectuosos y parkour */

	/** El suelo de las zonas de parkour (debajo del de la Fiesta): se pisa en SUB + 1. */
	public static final int SUB = SUELO - 8;

	/**
	 * Una zona de parkour escondida bajo un parche de suelo defectuoso: la caja de dentro, donde
	 * se cae (debajo del parche), la trampilla del final y adonde sube; `premio` son los tickets
	 * de la primera vez.
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

	public static final List<Parkour> PARKOURS = new ArrayList<>();
	/** Los parches de suelo defectuoso: x0, z0, x1, z1 y la variante (0 goma, 1 moqueta). */
	private static final List<int[]> PARCHES = new ArrayList<>();

	/* ------------------------------------------------------- la sala de juegos */

	/** Las baldosas de la memoria (x0, z0 de cada una, de 2x2): roja, amarilla, azul y verde. */
	public static final int[][] MEMORIA = {{28, 12}, {32, 12}, {28, 16}, {32, 16}};
	/** El boton de empezar la memoria (x0, z0, de 2x1) y el de los regalos. */
	public static final int[] MEMORIA_BOTON = {30, 20};
	public static final int[] REGALOS_BOTON = {39, 25};
	/** La sala de los regalos (interior) y los sitios donde puede salir un regalo. */
	public static final int[] REGALOS_SALA = {38, 9, 46, 27};
	public static final List<double[]> REGALOS_SITIOS = new ArrayList<>();

	private static int siguienteId;

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

	/**
	 * Una sala: suelo, paredes (zocalo abajo, papel encima) y techo con luces cada `luces`
	 * bloques (alguna que falla). Interior de x0..x1, z0..z1; los muros van alrededor.
	 */
	private static void sala(String nombre, int x0, int z0, int x1, int z1, int techo, Block suelo, Block papel, int luces) {
		SALAS.add(new Sala(nombre, x0, z0, x1, z1));
		caja(x0 - 1, SUELO, z0 - 1, x1 + 1, SUELO, z1 + 1, s(suelo));
		Patron pared = (x, y, z) -> y == SUELO + 1 ? s(BloquesFiesta.ZOCALO)
			: (papel == BloquesFiesta.PAPEL && (Math.floorMod(hash(x, z, y / 3), 41) == 0) ? s(BloquesFiesta.PAPEL_SUCIO) : s(papel));
		caja(x0 - 1, SUELO + 1, z0 - 1, x1 + 1, techo - 1, z0 - 1, pared);
		caja(x0 - 1, SUELO + 1, z1 + 1, x1 + 1, techo - 1, z1 + 1, pared);
		caja(x0 - 1, SUELO + 1, z0, x0 - 1, techo - 1, z1, pared);
		caja(x1 + 1, SUELO + 1, z0, x1 + 1, techo - 1, z1, pared);
		int ox = Math.floorMod(x0 + 1, luces);
		int oz = Math.floorMod(z0 + 1, luces);
		caja(x0 - 1, techo, z0 - 1, x1 + 1, techo, z1 + 1, (x, y, z) -> {
			if (x >= x0 && x <= x1 && z >= z0 && z <= z1 && Math.floorMod(x - ox, luces) == 0 && Math.floorMod(z - oz, luces) == 0) {
				return Math.floorMod(hash(x, z, 7), 9) == 0 ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.LUZ);
			}
			return s(BloquesFiesta.TECHO);
		});
	}

	/** Un hueco de paso en un muro (de 3 de alto). */
	private static void hueco(int x0, int z0, int x1, int z1) {
		aire(x0, SUELO + 1, z0, x1, SUELO + 3, z1);
	}

	/** Un torno en un muro: la hoja de 1 x 2 en (x, z) y lo que queda del hueco tapado. */
	private static void torno(int x, int z, Direction mira, int coste, String nombre) {
		BlockState base = BloquesFiesta.TORNO.defaultBlockState().setValue(Torno.FACING, mira).setValue(Torno.COSTE, coste);
		caja(x, SUELO + 1, z, x, SUELO + 1, z, base.setValue(Torno.PARTE, 0));
		caja(x, SUELO + 2, z, x, SUELO + 2, z, base.setValue(Torno.PARTE, 1));
		TORNOS.add(new Puerta(TORNOS.size(), x, SUELO + 1, z, mira, coste, nombre));
	}

	private static void moneda(double x, double z) {
		MONEDAS.add(new Punto(siguienteId++, x, SUELO + 1, z, 1));
	}

	private static void moneda(double x, double y, double z) {
		MONEDAS.add(new Punto(siguienteId++, x, y, z, 1));
	}

	private static void tickets(double x, double z, int valor) {
		TICKETS.add(new Punto(siguienteId++, x, SUELO + 1, z, valor));
	}

	private static void tickets(double x, double y, double z, int valor) {
		TICKETS.add(new Punto(siguienteId++, x, y, z, valor));
	}

	/** Globos de colores alrededor de (x, z): un racimo. */
	private static void racimo(int x, int z, int y, int semilla) {
		int[][] r = {{0, 0}, {1, 0}, {0, 1}, {-1, 0}};
		for (int i = 0; i < r.length; i++) {
			BlockState g = variante(BloquesFiesta.GLOBO, Direction.from2DDataValue(i), Math.floorMod(semilla + i, 4));
			caja(x + r[i][0], y, z + r[i][1], x + r[i][0], y, z + r[i][1], g);
		}
	}

	private static void bloque(int x, int y, int z, BlockState s) {
		caja(x, y, z, x, y, z, s);
	}

	/* ----------------------------------------------------------- el plano */

	static {
		parkours();
		llegada();
		salon();
		laberinto();
		semaforo();
		cocina();
		siesta();
		premio();
		juegos();
		conexiones();
		notas();
	}

	/** La llegada: el ascensor de las Piscinas se abre a un vestibulo de fiesta. */
	private static void llegada() {
		sala("llegada", -8, 30, 8, 46, TECHO + 1, BloquesFiesta.SUELO_GOMA, BloquesFiesta.PAPEL, 4);
		// el ascensor (cabina de acero, como la de las Piscinas) y su hueco abierto a la llegada
		caja(-3, SUELO, 47, 3, SUELO + 4, 53, s(Blocks.IRON_BLOCK));
		aire(-2, SUELO + 1, 48, 2, SUELO + 3, 52);
		caja(-2, SUELO + 4, 48, 2, SUELO + 4, 52, (x, y, z) -> (x == 0 && z == 50) ? s(BloquesFiesta.LUZ) : s(Blocks.IRON_BLOCK));
		hueco(-2, 47, 2, 47);
		// el arco de globos de la entrada y el cartel de bienvenida encima
		for (int x = -3; x <= 3; x++) {
			int y = SUELO + 4 + (Math.abs(x) < 2 ? 1 : 0);
			bloque(x, y, 44, variante(BloquesFiesta.GLOBO, Direction.NORTH, Math.floorMod(x, 4)));
		}
		for (int z = 41; z <= 44; z++) {
			bloque(-3, SUELO + 1 + (44 - z), z, variante(BloquesFiesta.GLOBO, Direction.NORTH, Math.floorMod(z, 4)));
			bloque(3, SUELO + 1 + (44 - z), z, variante(BloquesFiesta.GLOBO, Direction.NORTH, Math.floorMod(z + 2, 4)));
		}
		bloque(0, SUELO + 4, 30, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 0));
		bloque(-6, SUELO + 2, 30, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 2));
		bloque(6, SUELO + 2, 30, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, 0));
		// la taquilla de los tickets (al oeste) con su moneda detras
		caja(-8, SUELO + 1, 36, -6, SUELO + 1, 40, s(Blocks.STRIPPED_CHERRY_LOG));
		bloque(-7, SUELO + 2, 38, variante(BloquesFiesta.TARTA, Direction.EAST, 1));
		moneda(-7.5, 35.5);
		tickets(6.5, 34.5, 25);
		// guirnaldas por las paredes, confeti y globos sueltos
		for (int z = 32; z <= 44; z += 4) {
			bloque(-8, TECHO - 1, z, variante(BloquesFiesta.GUIRNALDA, Direction.EAST, z % 2));
			bloque(8, TECHO - 1, z, variante(BloquesFiesta.GUIRNALDA, Direction.WEST, z % 2));
		}
		bloque(4, SUELO + 1, 33, variante(BloquesFiesta.CONFETI, Direction.NORTH, 0));
		bloque(-3, SUELO + 1, 35, variante(BloquesFiesta.CONFETI, Direction.EAST, 1));
		racimo(6, 44, SUELO + 1, 1);
		bloque(8, SUELO + 3, 40, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {7, SUELO + 3, 40});
		RONDA.add(new int[] {0, 36});
	}

	/** El salon: la fiesta. Mesas largas con mantel y tartas, regalos, el escenario y el premio. */
	private static void salon() {
		sala("salon", -24, -24, 24, 28, TECHO_SALON, BloquesFiesta.MOQUETA_CONFETI, BloquesFiesta.PAPEL_ESTRELLAS, 6);
		// cuatro mesas largas con sus sillitas, tartas y alguna moneda debajo
		for (int mx : new int[] {-14, -8, 8, 14}) {
			for (int z = -8; z <= 16; z++) {
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
		// junto a las mesas (no dentro: las mesas chocan)
		moneda(-12.5, 3.5);
		moneda(13.5, -5.5);
		tickets(-5.5, 12.5, 25);
		tickets(9.5, -2.5, 25);
		tickets(20.5, 22.5, 25);
		// el escenario al oeste: tarima, telon rojo y el carton del Anfitrion
		caja(-24, SUELO + 1, -4, -19, SUELO + 1, 10, s(Blocks.DARK_OAK_PLANKS));
		caja(-24, SUELO + 2, -4, -24, TECHO_SALON - 1, 10, (x, y, z) -> s(Blocks.RED_WOOL));
		bloque(-23, SUELO + 4, 3, variante(BloquesFiesta.CARTEL, Direction.EAST, 4));
		bloque(-19, SUELO + 2, -4, variante(BloquesFiesta.REGALO, Direction.EAST, 2));
		bloque(-19, SUELO + 2, 10, variante(BloquesFiesta.REGALO, Direction.EAST, 3));
		bloque(-22, SUELO + 2, 6, s(Blocks.AIR));
		// regalos amontonados en las esquinas
		int[][] regalos = {{-23, -23}, {-22, -23}, {-23, -22}, {23, -23}, {22, -23}, {23, 27}, {22, 27}, {23, 26}, {-23, 27}};
		for (int i = 0; i < regalos.length; i++) {
			bloque(regalos[i][0], SUELO + 1, regalos[i][1], variante(BloquesFiesta.REGALO, Direction.from2DDataValue(i % 4), i % 4));
		}
		bloque(-23, SUELO + 2, -23, variante(BloquesFiesta.REGALO, Direction.SOUTH, 1));
		// globos: racimos sueltos y en el techo
		racimo(-2, 20, SUELO + 1, 0);
		racimo(19, -16, SUELO + 1, 2);
		for (int x = -20; x <= 20; x += 5) {
			for (int z = -20; z <= 24; z += 7) {
				if (Math.floorMod(hash(x, z, 31), 3) != 0) {
					bloque(x, TECHO_SALON - 1, z, variante(BloquesFiesta.GLOBO, Direction.from2DDataValue(Math.floorMod(x + z, 4)), Math.floorMod(hash(x, z, 32), 4)));
				}
			}
		}
		// carteles, dibujos y guirnaldas
		bloque(0, SUELO + 6, -24, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 1));
		bloque(-6, SUELO + 3, -24, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, 1));
		bloque(6, SUELO + 3, -24, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, 2));
		bloque(24, SUELO + 3, 10, variante(BloquesFiesta.DIBUJO, Direction.WEST, 3));
		bloque(24, SUELO + 3, 18, variante(BloquesFiesta.CARTEL, Direction.WEST, 3));
		bloque(24, SUELO + 4, -3, variante(BloquesFiesta.CARTEL, Direction.WEST, 5));
		bloque(-24, SUELO + 4, 18, variante(BloquesFiesta.DIBUJO, Direction.EAST, 4));
		bloque(-10, SUELO + 3, 28, variante(BloquesFiesta.CARTEL, Direction.NORTH, 6));
		bloque(10, SUELO + 3, 28, variante(BloquesFiesta.DIBUJO, Direction.NORTH, 5));
		for (int x = -22; x <= 22; x += 4) {
			bloque(x, SUELO + 8, -24, variante(BloquesFiesta.GUIRNALDA, Direction.SOUTH, Math.floorMod(x, 2)));
			bloque(x, SUELO + 8, 28, variante(BloquesFiesta.GUIRNALDA, Direction.NORTH, Math.floorMod(x, 2)));
		}
		// confeti por la moqueta y peluches olvidados
		for (int i = 0; i < 18; i++) {
			int x = -20 + Math.floorMod(hash(i, 1, 41), 41);
			int z = -20 + Math.floorMod(hash(i, 2, 41), 45);
			if (Math.abs(x - 8) > 1 && Math.abs(x + 8) > 1 && Math.abs(x - 14) > 1 && Math.abs(x + 14) > 1) {
				bloque(x, SUELO + 1, z, variante(BloquesFiesta.CONFETI, Direction.from2DDataValue(i % 4), i % 4));
			}
		}
		bloque(2, SUELO + 1, 24, variante(BloquesFiesta.PELUCHE, Direction.SOUTH, 0));
		bloque(-19, SUELO + 1, 22, variante(BloquesFiesta.PELUCHE, Direction.EAST, 1));
		bloque(-24, SUELO + 4, -14, variante(BloquesFiesta.ALTAVOZ, Direction.EAST, 0));
		bloque(24, SUELO + 4, 22, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {-23, SUELO + 4, -14});
		ALTAVOCES.add(new int[] {23, SUELO + 4, 22});
		RONDA.add(new int[] {0, 0});
		RONDA.add(new int[] {-18, 20});
		RONDA.add(new int[] {18, -18});
		RONDA.add(new int[] {0, -20});
		bloque(24, SUELO + 4, -1, variante(BloquesFiesta.CARTEL, Direction.WEST, 7));
		// el mostrador de premios alrededor de su puerta
		caja(-4, SUELO + 1, -24, -2, SUELO + 1, -23, s(Blocks.STRIPPED_CHERRY_LOG));
		caja(2, SUELO + 1, -24, 4, SUELO + 1, -23, s(Blocks.STRIPPED_CHERRY_LOG));
		bloque(-3, SUELO + 2, -23, variante(BloquesFiesta.REGALO, Direction.SOUTH, 0));
		bloque(3, SUELO + 2, -23, variante(BloquesFiesta.PELUCHE, Direction.SOUTH, 2));
		bloque(0, SUELO + 3, -24, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 2));
	}

	/**
	 * El laberinto de juegos: paredes acolchadas de colores, techo bajo, celdas de 3 x 3. Sale
	 * de un recorrido en profundidad con semilla fija (siempre el mismo) y algunos atajos para
	 * que no sea un arbol. Monedas en callejones sin salida, tickets en otros, y una sala secreta
	 * (3 monedas) con premio y una nota.
	 */
	private static void laberinto() {
		int x0 = LAB_X;
		int z0 = LAB_Z;
		int ancho = LAB_W * CELDA;
		int alto = LAB_H * CELDA;
		SALAS.add(new Sala("laberinto", x0 + 1, z0 + 1, x0 + ancho - 1, z0 + alto - 1));
		caja(x0, SUELO, z0, x0 + ancho, SUELO, z0 + alto, (x, y, z) -> Math.floorMod(hash(x, z, 51), 13) == 0
			? s(BloquesFiesta.SUELO_GOMA_SUCIO) : s(BloquesFiesta.SUELO_GOMA));
		caja(x0, TECHO, z0, x0 + ancho, TECHO, z0 + alto, (x, y, z) -> {
			int cx = Math.floorDiv(x - x0, CELDA);
			int cz = Math.floorDiv(z - z0, CELDA);
			if (Math.floorMod(x - x0, CELDA) == 2 && Math.floorMod(z - z0, CELDA) == 2 && (cx + cz) % 2 == 0) {
				return Math.floorMod(hash(cx, cz, 53), 6) == 0 ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.LUZ);
			}
			return s(BloquesFiesta.TECHO);
		});
		// la sala secreta ocupa las celdas (1..3, 1..2): fuera del recorrido
		boolean[][] visto = new boolean[LAB_W][LAB_H];
		for (int cx = 1; cx <= 3; cx++) {
			for (int cz = 1; cz <= 2; cz++) {
				visto[cx][cz] = true;
			}
		}
		// paredes entre celdas: este[x][z] (entre x y x+1) y sur[x][z] (entre z y z+1)
		boolean[][] este = new boolean[LAB_W][LAB_H];
		boolean[][] sur = new boolean[LAB_W][LAB_H];
		for (int cx = 0; cx < LAB_W; cx++) {
			for (int cz = 0; cz < LAB_H; cz++) {
				este[cx][cz] = true;
				sur[cx][cz] = true;
			}
		}
		Random azar = new Random(1337);
		List<int[]> pila = new ArrayList<>();
		pila.add(new int[] {LAB_W - 1, 10});
		visto[LAB_W - 1][10] = true;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!pila.isEmpty()) {
			int[] c = pila.get(pila.size() - 1);
			List<int[]> libres = new ArrayList<>();
			for (int[] d : dirs) {
				int nx = c[0] + d[0];
				int nz = c[1] + d[1];
				if (nx >= 0 && nz >= 0 && nx < LAB_W && nz < LAB_H && !visto[nx][nz]) {
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
			if (d[0] == 1) {
				este[c[0]][c[1]] = false;
			} else if (d[0] == -1) {
				este[nx][nz] = false;
			} else if (d[1] == 1) {
				sur[c[0]][c[1]] = false;
			} else {
				sur[nx][nz] = false;
			}
			visto[nx][nz] = true;
			pila.add(new int[] {nx, nz});
		}
		// atajos: quita algunas paredes para que haya vueltas (no en la sala secreta)
		for (int i = 0; i < 22; i++) {
			int cx = azar.nextInt(LAB_W - 1);
			int cz = azar.nextInt(LAB_H - 1);
			if (cx <= 3 && cz <= 2 || cx == 0 && cz <= 3) {
				continue;
			}
			if (azar.nextBoolean()) {
				este[cx][cz] = false;
			} else {
				sur[cx][cz] = false;
			}
		}
		// dentro de la sala secreta no hay paredes
		for (int cx = 1; cx <= 3; cx++) {
			for (int cz = 1; cz <= 2; cz++) {
				if (cx < 3) {
					este[cx][cz] = false;
				}
				if (cz < 2) {
					sur[cx][cz] = false;
				}
			}
		}
		// las paredes acolchadas: cada tramo de un color
		final boolean[][] fe = este;
		final boolean[][] fs = sur;
		caja(x0, SUELO + 1, z0, x0 + ancho, TECHO - 1, z0 + alto, (x, y, z) -> {
			int lx = x - x0;
			int lz = z - z0;
			boolean px = Math.floorMod(lx, CELDA) == 0;
			boolean pz = Math.floorMod(lz, CELDA) == 0;
			int cx = Math.floorDiv(lx, CELDA);
			int cz = Math.floorDiv(lz, CELDA);
			boolean muro;
			if (lx == 0 || lz == 0 || lx == ancho || lz == alto) {
				muro = true;
			} else if (px && pz) {
				muro = true;
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
			int color = Math.floorMod(hash(px ? lx : cx, pz ? lz : cz, 61), BloquesFiesta.ACOLCHADOS.length);
			return s(BloquesFiesta.ACOLCHADOS[color]);
		});
		// la sala secreta: suelo de confeti, regalos, tickets y una nota; su torno da a la celda (2, 3)
		int sx0 = x0 + CELDA + 1;
		int sz0 = z0 + CELDA + 1;
		SALAS.add(new Sala("secreta", sx0, sz0, x0 + 4 * CELDA - 1, z0 + 3 * CELDA - 1));
		caja(sx0, SUELO, sz0, x0 + 4 * CELDA - 1, SUELO, z0 + 3 * CELDA - 1, s(BloquesFiesta.MOQUETA_CONFETI));
		tickets(sx0 + 1.5, sz0 + 1.5, 50);
		tickets(sx0 + 9.5, sz0 + 1.5, 50);
		tickets(sx0 + 1.5, sz0 + 5.5, 50);
		tickets(sx0 + 9.5, sz0 + 5.5, 50);
		bloque(sx0 + 5, SUELO + 1, sz0, variante(BloquesFiesta.REGALO, Direction.SOUTH, 3));
		bloque(sx0 + 4, SUELO + 1, sz0, variante(BloquesFiesta.REGALO, Direction.SOUTH, 1));
		bloque(sx0 + 5, SUELO + 2, sz0 - 1, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, 6));
		bloque(sx0 + 2, SUELO + 2, sz0 - 1, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, 7));
		bloque(sx0 + 8, SUELO + 1, sz0 + 3, variante(BloquesFiesta.PELUCHE, Direction.WEST, 3));
		// los callejones sin salida: monedas en 8 y tickets en otros 8
		List<int[]> sinSalida = new ArrayList<>();
		for (int cx = 0; cx < LAB_W; cx++) {
			for (int cz = 0; cz < LAB_H; cz++) {
				if (cx >= 1 && cx <= 3 && cz >= 1 && cz <= 2) {
					continue;
				}
				int salidas = 0;
				if (cx < LAB_W - 1 && !este[cx][cz]) {
					salidas++;
				}
				if (cx > 0 && !este[cx - 1][cz]) {
					salidas++;
				}
				if (cz < LAB_H - 1 && !sur[cx][cz]) {
					salidas++;
				}
				if (cz > 0 && !sur[cx][cz - 1]) {
					salidas++;
				}
				if (salidas == 1) {
					sinSalida.add(new int[] {cx, cz});
				}
			}
		}
		java.util.Collections.shuffle(sinSalida, new Random(4242));
		int monedas = 0;
		int conTickets = 0;
		for (int[] c : sinSalida) {
			double x = x0 + c[0] * CELDA + 2.5;
			double z = z0 + c[1] * CELDA + 2.5;
			if (monedas < 8) {
				moneda(x, z);
				monedas++;
			} else if (conTickets < 8) {
				tickets(x, z, 25 + 25 * (conTickets % 2));
				conTickets++;
			}
		}
		// algun peluche y dibujo por los pasillos, y por donde pasa el Anfitrion
		for (int i = 0; i < 10; i++) {
			int cx = Math.floorMod(hash(i, 3, 71), LAB_W);
			int cz = Math.floorMod(hash(i, 4, 71), LAB_H);
			if (cx >= 1 && cx <= 3 && cz >= 1 && cz <= 2) {
				continue;
			}
			RONDA.add(new int[] {x0 + cx * CELDA + 2, z0 + cz * CELDA + 2});
			if (i % 3 == 0) {
				bloque(x0 + cx * CELDA + 1, SUELO + 1, z0 + cz * CELDA + 1, variante(BloquesFiesta.PELUCHE, Direction.from2DDataValue(i % 4), i % 4));
			}
		}
		ALTAVOCES.add(new int[] {x0 + 22, TECHO - 1, z0 + 34});
		ALTAVOCES.add(new int[] {x0 + 6, TECHO - 1, z0 + 58});
	}

	/** El pasillo del semaforo: zona de salida, la carrera de 33 bloques y la meta con su podio. */
	private static void semaforo() {
		sala("semaforo", SEM_X0, SEM_Z0, SEM_X1, SEM_Z1, TECHO_SEMAFORO, BloquesFiesta.SUELO_GOMA, BloquesFiesta.PAPEL, 5);
		for (int z = SEM_Z0; z <= SEM_Z1; z++) {
			bloque(SEM_SALIDA, SUELO + 1, z, variante(BloquesFiesta.LINEA, Direction.NORTH, 0));
			bloque(SEM_META, SUELO + 1, z, variante(BloquesFiesta.LINEA, Direction.NORTH, 1));
		}
		// el semaforo: carcasa de 3 x 5 en la pared del fondo (este), rojo arriba y verde abajo
		int xs = SEM_X1;
		for (int z = -2; z <= 0; z++) {
			for (int y = SUELO + 4; y <= SUELO + 8; y++) {
				// una fila de lamparas rojas (y + 7) y otra verde (y + 5); lo demas, la carcasa negra
				int color = y == SUELO + 7 ? 1 : y == SUELO + 5 ? 2 : 0;
				BlockState l = BloquesFiesta.LUZ_SEMAFORO.defaultBlockState().setValue(LuzSemaforo.FACING, Direction.WEST).setValue(LuzSemaforo.COLOR, color);
				bloque(xs, y, z, l);
				if (color == 1) {
					SEMAFORO_ROJO.add(new int[] {xs, y, z});
				} else if (color == 2) {
					SEMAFORO_VERDE.add(new int[] {xs, y, z});
				}
			}
		}
		// el podio de la meta y los dibujos del pasillo
		caja(67, SUELO + 1, -2, 69, SUELO + 1, 0, s(Blocks.GOLD_BLOCK));
		bloque(SEM_X0, SUELO + 4, 3, variante(BloquesFiesta.CARTEL, Direction.EAST, 7));
		for (int x = 34; x <= 60; x += 7) {
			bloque(x, SUELO + 3, SEM_Z0, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, Math.floorMod(x, 8)));
			bloque(x + 3, TECHO_SEMAFORO - 2, SEM_Z1, variante(BloquesFiesta.GUIRNALDA, Direction.NORTH, Math.floorMod(x, 2)));
		}
		bloque(SEM_X1, SUELO + 10, 3, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {SEM_X1 - 1, SUELO + 10, 3});
	}

	/**
	 * La cocina, al fondo de su pasillo: baldosa blanca y negra, encimeras de acero, ollas
	 * enormes y bandejas con nombres. Lo que mas dice de que va esta fiesta.
	 */
	private static void cocina() {
		sala("pasillo_cocina", -13, -41, -11, -26, TECHO, BloquesFiesta.SUELO_GOMA, BloquesFiesta.PAPEL, 4);
		sala("cocina", -44, -64, -9, -42, TECHO + 1, BloquesFiesta.BALDOSA_COCINA, BloquesFiesta.PAPEL_SUCIO, 5);
		// encimeras a lo largo de las paredes y una isla en medio
		caja(-43, SUELO + 1, -63, -10, SUELO + 1, -63, s(Blocks.IRON_BLOCK));
		caja(-43, SUELO + 1, -62, -43, SUELO + 1, -43, s(Blocks.IRON_BLOCK));
		caja(-34, SUELO + 1, -55, -18, SUELO + 1, -52, s(Blocks.IRON_BLOCK));
		for (int x = -33; x <= -19; x += 2) {
			bloque(x, SUELO + 2, -55, variante(BloquesFiesta.BANDEJA, Direction.SOUTH, Math.floorMod(x, 4)));
			bloque(x, SUELO + 2, -52, variante(BloquesFiesta.BANDEJA, Direction.NORTH, Math.floorMod(x + 1, 4)));
		}
		for (int x = -40; x <= -14; x += 5) {
			bloque(x, SUELO + 2, -63, variante(BloquesFiesta.OLLA, Direction.SOUTH, Math.floorMod(x, 4)));
		}
		bloque(-30, SUELO + 1, -47, variante(BloquesFiesta.OLLA, Direction.NORTH, 3));
		bloque(-26, SUELO + 3, -64, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 6));
		bloque(-38, SUELO + 3, -42, variante(BloquesFiesta.DIBUJO, Direction.NORTH, 6));
		bloque(-44, SUELO + 3, -50, variante(BloquesFiesta.DIBUJO, Direction.EAST, 7));
		moneda(-42.5, -44.5);
		moneda(-11.5, SUELO + 2, -63.5);
		tickets(-26.5, SUELO + 2, -53.5, 50);
		tickets(-16.5, -45.5, 50);
		tickets(-40.5, -60.5, 50);
		bloque(-9, SUELO + 4, -50, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {-10, SUELO + 4, -50});
		RONDA.add(new int[] {-26, -48});
		RONDA.add(new int[] {-12, -33});
	}

	/** La sala de la siesta: camitas en fila, luz baja y una cajita de musica. */
	private static void siesta() {
		sala("pasillo_siesta", 11, -41, 13, -26, TECHO, BloquesFiesta.SUELO_GOMA, BloquesFiesta.PAPEL, 4);
		sala("siesta", 9, -64, 44, -42, TECHO, BloquesFiesta.SUELO_GOMA, BloquesFiesta.PAPEL_ESTRELLAS, 7);
		for (int x = 12; x <= 42; x += 3) {
			for (int z : new int[] {-62, -58, -50, -46}) {
				bloque(x, SUELO + 1, z, variante(BloquesFiesta.CAMITA, Direction.NORTH, Math.floorMod(x + z, 4)));
				if (Math.floorMod(hash(x, z, 81), 4) == 0) {
					bloque(x, SUELO + 2, z, variante(BloquesFiesta.PELUCHE, Direction.SOUTH, Math.floorMod(x, 4)));
				}
			}
		}
		bloque(26, SUELO + 3, -64, variante(BloquesFiesta.DIBUJO, Direction.SOUTH, 4));
		bloque(44, SUELO + 3, -54, variante(BloquesFiesta.DIBUJO, Direction.WEST, 5));
		bloque(9, SUELO + 3, -54, variante(BloquesFiesta.CARTEL, Direction.EAST, 4));
		moneda(42.5, -63.5);
		moneda(10.5, -43.5);
		tickets(27.5, -54.5, 50);
		tickets(36.5, -47.5, 50);
		tickets(15.5, -60.5, 50);
		bloque(44, SUELO + 3, -44, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {43, SUELO + 3, -44});
		RONDA.add(new int[] {26, -54});
		RONDA.add(new int[] {12, -33});
	}

	/** La sala del Gran Premio: confeti dorado y el regalo gigante que se abre hacia lo siguiente. */
	private static void premio() {
		sala("premio", -6, -44, 6, -26, TECHO + 3, BloquesFiesta.MOQUETA_CONFETI, BloquesFiesta.PAPEL_ESTRELLAS, 4);
		caja(PREMIO_X0 - 1, SUELO + 1, PREMIO_Z0, PREMIO_X1 + 1, SUELO + 6, PREMIO_Z1 + 1, (x, y, z) -> {
			boolean dentro = x >= PREMIO_X0 && x <= PREMIO_X1 && z <= PREMIO_Z1 && y <= SUELO + 4;
			// la entrada del regalo, por delante (sur): 3 de ancho y 3 de alto
			boolean entrada = z == PREMIO_Z1 + 1 && x >= -1 && x <= 1 && y <= SUELO + 3;
			if (dentro || entrada) {
				return Blocks.AIR.defaultBlockState();
			}
			// la caja de regalo: rojo con lazo dorado en cruz
			return x == 0 || y == SUELO + 3 ? s(Blocks.GOLD_BLOCK) : s(Blocks.RED_CONCRETE);
		});
		caja(PREMIO_X0, SUELO, PREMIO_Z0, PREMIO_X1, SUELO, PREMIO_Z1, s(Blocks.GOLD_BLOCK));
		for (int z = -40; z <= -27; z += 3) {
			bloque(-5, SUELO + 1, z, variante(BloquesFiesta.CONFETI, Direction.NORTH, 2));
			bloque(5, SUELO + 1, z, variante(BloquesFiesta.CONFETI, Direction.SOUTH, 3));
			racimo(-5, z, SUELO + 4, z);
			racimo(5, z, SUELO + 4, z + 1);
		}
		bloque(0, SUELO + 5, -27, variante(BloquesFiesta.CARTEL, Direction.NORTH, 1));
	}

	/**
	 * Las tres zonas de parkour, debajo del suelo de la Fiesta (lo primero: las salas de encima
	 * ponen su suelo, que es el techo de estas). Paredes de papel despegado, una piscina de bolas
	 * en el fondo y plataformas acolchadas hasta la trampilla del final.
	 */
	private static void parkours() {
		// bajo el salon (parche de moqueta), hacia el este; sube al salon
		parkour("salon", 16, 18, 41, 24, 18, 20, 1, 1, 38, 21, 20.5, 17.5, 0.0F, 100, 11);
		// bajo un callejon del laberinto, hacia el oeste; sube a la sala secreta (sin pagar)
		parkour("laberinto", -63, 15, -34, 21, -36, 17, -1, 0, -61, 18, -62.5, -32.5, 180.0F, 100, 12);
		// bajo la sala de la siesta, hacia el oeste; sube a la siesta
		parkour("siesta", 19, -51, 43, -45, 40, -48, -1, 0, 21, -48, 24.5, -44.5, 180.0F, 100, 13);
	}

	/**
	 * Una zona de parkour de x0..x1, z0..z1 (interior) con el parche en (px, pz) (2x2), hacia
	 * `dir` (1 este, -1 oeste) y la trampilla en (fx, fz) (2x2).
	 */
	private static void parkour(String nombre, int x0, int z0, int x1, int z1, int px, int pz, int dir, int variante,
		int fx, int fz, double sx, double sz, float giro, int premio, long semilla) {
		SALAS.add(new Sala("parkour_" + nombre, x0, z0, x1, z1));
		caja(x0 - 1, SUB, z0 - 1, x1 + 1, SUB, z1 + 1, s(Blocks.GRAY_CONCRETE));
		Patron pared = (x, y, z) -> y == SUB + 1 ? s(BloquesFiesta.ZOCALO) : s(BloquesFiesta.PAPEL_SUCIO);
		caja(x0 - 1, SUB + 1, z0 - 1, x1 + 1, SUELO - 1, z0 - 1, pared);
		caja(x0 - 1, SUB + 1, z1 + 1, x1 + 1, SUELO - 1, z1 + 1, pared);
		caja(x0 - 1, SUB + 1, z0, x0 - 1, SUELO - 1, z1, pared);
		caja(x1 + 1, SUB + 1, z0, x1 + 1, SUELO - 1, z1, pared);
		caja(x0 - 1, SUELO, z0 - 1, x1 + 1, SUELO, z1 + 1, s(BloquesFiesta.TECHO));
		caja(x0, SUB + 1, z0, x1, SUB + 1, z1, s(BloquesFiesta.PISCINA_BOLAS));
		// alguna luz colgando, que falla
		for (int x = x0 + 3; x <= x1 - 3; x += 7) {
			bloque(x, SUELO - 1, (z0 + z1) / 2, s(BloquesFiesta.LUZ_PARPADEO));
		}
		// las plataformas: de la llegada a la trampilla, a saltos de 2-3, subiendo poco a poco
		Random azar = new Random(semilla);
		int x = px + (dir > 0 ? 2 : -1);
		int z = pz;
		int y = SUB + 1;
		int fin = dir > 0 ? fx - 2 : fx + 3;
		while (dir > 0 ? x < fin : x > fin) {
			bloque(x, y, z, s(BloquesFiesta.ACOLCHADOS[Math.floorMod(x + z, BloquesFiesta.ACOLCHADOS.length)]));
			if (azar.nextInt(4) == 0) {
				bloque(x, y, z + 1, s(BloquesFiesta.ACOLCHADOS[Math.floorMod(x + z + 1, BloquesFiesta.ACOLCHADOS.length)]));
			}
			int alto = Math.max(SUB + 1, Math.min(SUB + 4, y + (azar.nextInt(3) - (y >= SUB + 3 ? 1 : 0))));
			int salto = alto > y ? 2 : 2 + azar.nextInt(2);
			x += dir * salto;
			z = Math.max(z0 + 1, Math.min(z1 - 1, z + azar.nextInt(3) - 1));
			y = alto;
		}
		// la plataforma del final con la trampilla y su premio
		caja(fx - 1, SUB + 1, fz - 1, fx + 2, SUB + 3, fz + 2, s(BloquesFiesta.ACOLCHADO_AMARILLO));
		caja(fx, SUB + 3, fz, fx + 1, SUB + 3, fz + 1, s(BloquesFiesta.TRAMPILLA));
		tickets(fx + (dir > 0 ? -0.5 : 2.5), SUB + 4, fz + 1.0, 50);
		PARKOURS.add(new Parkour(PARKOURS.size(), nombre, x0, z0, x1, z1, px + 1.0, pz + 1.0, fx, fz, fx + 1, fz + 1,
			sx, sz, giro, premio));
		PARCHES.add(new int[] {px, pz, px + 1, pz + 1, variante});
	}

	/**
	 * La sala de juegos, al este del salon (1 moneda): la memoria de colores (cuatro baldosas que
	 * se encienden en orden y hay que pisar igual) y, detras de un tabique, la sala de los regalos,
	 * casi a oscuras, para buscarlos a contrarreloj.
	 */
	private static void juegos() {
		sala("juegos", 26, 9, 46, 27, TECHO + 1, BloquesFiesta.SUELO_GOMA, BloquesFiesta.PAPEL_ESTRELLAS, 5);
		// las baldosas de colores y el boton, sobre una alfombra oscura para que se distingan
		caja(27, SUELO, 11, 35, SUELO, 21, s(BloquesFiesta.MOQUETA_CONFETI));
		for (int c = 0; c < 4; c++) {
			int[] b = MEMORIA[c];
			BlockState e = BloquesFiesta.BALDOSA_JUEGO.defaultBlockState().setValue(BaldosaJuego.COLOR, c);
			caja(b[0], SUELO, b[1], b[0] + 1, SUELO, b[1] + 1, e);
		}
		caja(MEMORIA_BOTON[0], SUELO, MEMORIA_BOTON[1], MEMORIA_BOTON[0] + 1, SUELO, MEMORIA_BOTON[1],
			BloquesFiesta.BALDOSA_JUEGO.defaultBlockState().setValue(BaldosaJuego.COLOR, 4));
		bloque(31, SUELO + 4, 9, variante(BloquesFiesta.CARTEL, Direction.SOUTH, 3));
		bloque(26, SUELO + 3, 14, variante(BloquesFiesta.DIBUJO, Direction.EAST, 3));
		racimo(35, 25, SUELO + 1, 2);
		// el tabique de la sala de los regalos, con su puerta al sur
		caja(37, SUELO + 1, 9, 37, TECHO, 27, (x, y, z) -> y == SUELO + 1 ? s(BloquesFiesta.ZOCALO) : s(BloquesFiesta.PAPEL_SUCIO));
		aire(37, SUELO + 1, 24, 37, SUELO + 3, 26);
		// dentro, casi a oscuras: el techo sin luces salvo dos que fallan, y cosas apiladas
		caja(38, TECHO + 1, 9, 46, TECHO + 1, 27, (x, y, z) -> (x == 42 && (z == 13 || z == 22)) ? s(BloquesFiesta.LUZ_PARPADEO) : s(BloquesFiesta.TECHO));
		int[][] montones = {{40, 11}, {44, 11}, {42, 15}, {39, 18}, {45, 18}, {42, 21}, {40, 23}};
		for (int i = 0; i < montones.length; i++) {
			int[] m = montones[i];
			bloque(m[0], SUELO + 1, m[1], variante(BloquesFiesta.REGALO, Direction.from2DDataValue(i % 4), i % 4));
			bloque(m[0], SUELO + 2, m[1], variante(BloquesFiesta.REGALO, Direction.from2DDataValue((i + 1) % 4), (i + 2) % 4));
			bloque(m[0] + 1, SUELO + 1, m[1], variante(BloquesFiesta.PELUCHE, Direction.from2DDataValue(i % 4), i % 4));
		}
		caja(REGALOS_BOTON[0], SUELO, REGALOS_BOTON[1], REGALOS_BOTON[0], SUELO, REGALOS_BOTON[1],
			BloquesFiesta.BALDOSA_JUEGO.defaultBlockState().setValue(BaldosaJuego.COLOR, 4));
		bloque(38, SUELO + 3, 27, variante(BloquesFiesta.CARTEL, Direction.NORTH, 2));
		// los sitios de los regalos: suelo libre de la sala, lejos de la puerta
		for (int gx = 38; gx <= 46; gx += 2) {
			for (int gz = 9; gz <= 23; gz += 2) {
				boolean ocupado = false;
				for (int[] m : montones) {
					ocupado |= Math.abs(m[0] - gx) <= 1 && Math.abs(m[1] - gz) <= 0;
				}
				if (!ocupado) {
					REGALOS_SITIOS.add(new double[] {gx + 0.5, SUELO + 1, gz + 0.5});
				}
			}
		}
		bloque(46, SUELO + 3, 18, variante(BloquesFiesta.ALTAVOZ, Direction.WEST, 0));
		ALTAVOCES.add(new int[] {45, SUELO + 3, 18});
		RONDA.add(new int[] {31, 24});
		RONDA.add(new int[] {42, 25});
	}

	/** Las notas, cada una donde dice algo (NotaFiesta). */
	private static void notas() {
		int[][] n = {{-20, -60, 0}, {28, 3, 1}, {-48, 10, 2}, {16, 22, 3}, {-38, -44, 4}, {30, -60, 5}, {5, 42, 6}, {-4, -30, 7}};
		for (int[] a : n) {
			bloque(a[0], SUELO + 1, a[1], BloquesFiesta.NOTA.defaultBlockState().setValue(NotaFiesta.FACING, Direction.from2DDataValue(a[2] % 4))
				.setValue(NotaFiesta.VARIANTE, a[2]));
		}
	}

	/**
	 * Los pasos entre salas, al final: cada sala pone sus muros enteros y los de la de al lado
	 * los pisarian. Huecos abiertos (la llegada, el laberinto, los pasillos) y los tornos.
	 */
	private static void conexiones() {
		hueco(-2, 29, 2, 29);
		hueco(LAB_X + LAB_W * CELDA, LAB_Z + 10 * CELDA + 1, -25, LAB_Z + 10 * CELDA + 3);
		hueco(-13, -41, -11, -41);
		hueco(11, -41, 13, -41);
		torno(25, -1, Direction.WEST, 2, "el semáforo");
		torno(-12, -25, Direction.SOUTH, 1, "la cocina");
		torno(12, -25, Direction.SOUTH, 2, "la sala de la siesta");
		torno(0, -25, Direction.SOUTH, 0, "el Gran Premio");
		torno(LAB_X + 2 * CELDA + 2, LAB_Z + 3 * CELDA, Direction.SOUTH, 3, "la sala secreta");
		torno(25, 18, Direction.WEST, 1, "la sala de juegos");
		// los parches de suelo defectuoso, encima de cada parkour
		for (int[] p : PARCHES) {
			caja(p[0], SUELO, p[1], p[2], SUELO, p[3], BloquesFiesta.SUELO_DEFECTUOSO.defaultBlockState().setValue(SueloDefectuoso.VARIANTE, p[4]));
		}
	}

	/** La sala (por nombre) donde esta ese punto, o null. */
	public static Sala sala(double x, double z) {
		for (int i = SALAS.size() - 1; i >= 0; i--) {
			if (SALAS.get(i).dentro(x, z)) {
				return SALAS.get(i);
			}
		}
		return null;
	}

	public static Puerta torno(int x, int y, int z) {
		for (Puerta p : TORNOS) {
			if (p.x() == x && p.y() == y && p.z() == z) {
				return p;
			}
		}
		return null;
	}
}
