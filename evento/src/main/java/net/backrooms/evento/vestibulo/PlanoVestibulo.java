package net.backrooms.evento.vestibulo;

import java.util.ArrayList;
import java.util.List;
import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.bloques.EnPared;
import net.backrooms.evento.bloques.EnSuelo;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * El "Centro de Expediciones": el vestibulo donde todos esperan antes del
 * /start. Se describe como una lista de cajas de bloques (las de despues
 * pisan a las de antes) que GeneradorVestibulo pone al generar cada chunk;
 * los rotulos y las vitrinas son entidades de texto y de objeto que pone
 * Vestibulo la primera vez.
 *
 * Planta (y = 64 el suelo; se pisa en 65; techo en 78):
 *  - Auditorio, al norte (z -68..-25): escenario con pantalla, telon y
 *    focos, y 200 butacas (10 filas de 20, pasillo central) para la
 *    conferencia inicial.
 *  - Vestibulo, al sur (z -23..32, 73 de ancho): suelo ajedrezado, columnas
 *    de cuarzo, mostrador de informacion en el centro, zonas de espera con
 *    sofas, plantas, maquinas de agua de almendras, taquillas, la vitrina
 *    del equipo y, al fondo, los ascensores al Nivel 0.
 * Unos 4000 m2 para 200 personas: ni apretados ni perdidos.
 */
public final class PlanoVestibulo {
	public static final int SUELO = 64;
	public static final int TECHO = 78;
	/** Limites del edificio (con sus muros). */
	public static final int X0 = -37;
	public static final int X1 = 37;
	public static final int Z0 = -69;
	/** Hasta el fondo de las cabinas de los ascensores (la pared sur del vestibulo esta en 33). */
	public static final int Z1 = 38;
	public static final int Y0 = 62;
	public static final int Y1 = 79;

	/** Donde aparece la gente: delante del mostrador, mirando al auditorio. */
	public static final double SPAWN_X = 0.5;
	public static final double SPAWN_Y = SUELO + 1;
	public static final double SPAWN_Z = 6.5;

	/**
	 * Centros de las puertas de los ascensores (en la pared sur, z = PUERTA_Z). Las puertas
	 * ocupan x = c-1 y c: fuera las de acero del rellano (PUERTA_Z) y dentro las de hierro
	 * de la cabina (PUERTA_Z + 1). La cabina, x = c-2..c+1 y z = CABINA_Z0..CABINA_Z1, es
	 * la misma que sale en la cinematica (cinematica.fsh): bloque de hierro con zocalo de
	 * roble oscuro, suelo de piedra lisa, lampara de redstone, el indicador de planta
	 * encima de las puertas y la botonera de piedra negra pulida a la derecha.
	 */
	public static final int[] ASCENSORES = {-24, -12, 0, 12, 24};
	public static final int PUERTA_Z = 33;
	public static final int CABINA_Z0 = 35;
	public static final int CABINA_Z1 = 37;
	/**
	 * El hueco de los ascensores, escondido bajo el vestibulo: quien sube a una cabina
	 * espera aqui (por dentro es otra cabina) a que le toque la cinematica y el viaje,
	 * para no tapar la puerta a los que vienen detras.
	 */
	public static final int HUECO_X0 = -30;
	public static final int HUECO_X1 = 30;
	public static final int HUECO_Y = 48;
	public static final int HUECO_Z = 36;
	/** Pedestales de la vitrina del equipo (x; z = VITRINA_Z). */
	public static final int[] VITRINA_X = {-22, -19, -16, -13};
	public static final int VITRINA_Z = -18;
	/** Columnas del vestibulo. */
	private static final int[] COL_X = {-24, -12, 12, 24};
	private static final int[] COL_Z = {-12, 2, 16};

	@FunctionalInterface
	public interface Patron {
		BlockState en(int x, int y, int z);
	}

	public record Caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron patron) {
	}

	public static final List<Caja> CAJAS = new ArrayList<>();

	private PlanoVestibulo() {
	}

	private static void caja(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		CAJAS.add(new Caja(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), (x, y, z) -> s));
	}

	private static void caja(int x0, int y0, int z0, int x1, int y1, int z1, Patron p) {
		CAJAS.add(new Caja(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), p));
	}

	private static void punto(int x, int y, int z, BlockState s) {
		caja(x, y, z, x, y, z, s);
	}

	private static BlockState b(Block b) {
		return b.defaultBlockState();
	}

	private static BlockState escalera(Block b, Direction mira) {
		return b.defaultBlockState().setValue(StairBlock.FACING, mira);
	}

	private static BlockState losa(Block b) {
		return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
	}

	private static BlockState mira(Block b, Direction d) {
		return b.defaultBlockState().setValue(EnSuelo.FACING, d);
	}

	private static BlockState pared(Block b, Direction d) {
		return b.defaultBlockState().setValue(EnPared.FACING, d);
	}

	/** Una hoja cerrada de la puerta de acero de un rellano: 0 la izquierda (x = c-1), 1 la derecha (x = c). */
	public static BlockState puerta(int hoja) {
		return mira(hoja == 0 ? Bloques.PUERTA_ASCENSOR_IZQ : Bloques.PUERTA_ASCENSOR_DER, Direction.NORTH);
	}

	/**
	 * Media hoja de la puerta de hierro de una cabina (dos de alto, como en la cinematica).
	 * Miran a la cabina; al abrirse cada una gira hacia su lado.
	 */
	public static BlockState puertaCabina(int hoja, boolean arriba, boolean abierta) {
		return Blocks.IRON_DOOR.defaultBlockState()
			.setValue(DoorBlock.FACING, Direction.NORTH)
			.setValue(DoorBlock.HINGE, hoja == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT)
			.setValue(DoorBlock.HALF, arriba ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER)
			.setValue(DoorBlock.OPEN, abierta);
	}

	/** Lampara de redstone encendida (lleva un bloque de redstone encima para que no se apague). */
	private static BlockState lampara() {
		return Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
	}

	/** El ascensor (indice de ASCENSORES) en cuya cabina esta ese punto, o -1. */
	public static int cabina(double x, double y, double z) {
		if (y < SUELO + 1 || y >= SUELO + 5 || z < CABINA_Z0 || z >= CABINA_Z1 + 1) {
			return -1;
		}
		for (int i = 0; i < ASCENSORES.length; i++) {
			if (x >= ASCENSORES[i] - 2 && x < ASCENSORES[i] + 2) {
				return i;
			}
		}
		return -1;
	}

	static {
		BlockState aire = b(Blocks.AIR);
		BlockState relleno = b(Blocks.SMOOTH_STONE);

		// ---------------------------------------------------------- casco
		caja(X0, Y0, Z0, X1, Y1, Z1, relleno);
		caja(-26, SUELO + 1, -68, 26, TECHO - 1, -25, aire);    // auditorio
		caja(-36, SUELO + 1, -23, 36, TECHO - 1, 32, aire);     // vestibulo
		caja(-10, SUELO + 1, -24, 10, SUELO + 8, -24, aire);    // gran puerta entre los dos

		// -------------------------------------------------- vestibulo: suelo y techo
		caja(-36, SUELO, -23, 36, SUELO, 32, (x, y, z) -> {
			if (x <= -35 || x >= 35 || z <= -22 || z >= 31) {
				return b(Blocks.POLISHED_DEEPSLATE);
			}
			return ((Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) & 1) == 0 ? b(Blocks.WHITE_CONCRETE) : b(Blocks.LIGHT_GRAY_CONCRETE);
		});
		caja(-36, TECHO, -23, 36, TECHO, 32, (x, y, z) -> {
			int mx = Math.floorMod(x, 6);
			int mz = Math.floorMod(z, 6);
			return (mx == 2 || mx == 3) && (mz == 2 || mz == 3) ? b(Bloques.FLUORESCENTE) : b(Blocks.SMOOTH_QUARTZ);
		});
		// paredes: zocalo de madera, moldura y el papel amarillo de siempre
		Patron paredVestibulo = (x, y, z) -> {
			if (y <= SUELO + 3) {
				return b(Blocks.DARK_OAK_PLANKS);
			}
			if (y == SUELO + 4) {
				return b(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, x == -37 || x == 37 ? Direction.Axis.Z : Direction.Axis.X);
			}
			if (y == TECHO - 1) {
				return b(Blocks.SMOOTH_QUARTZ);
			}
			return b(Bloques.PAPEL_PINTADO);
		};
		caja(-37, SUELO + 1, -23, -37, TECHO - 1, 32, paredVestibulo);
		caja(37, SUELO + 1, -23, 37, TECHO - 1, 32, paredVestibulo);
		caja(-36, SUELO + 1, 33, 36, TECHO - 1, 33, paredVestibulo);
		caja(-36, SUELO + 1, -24, -11, TECHO - 1, -24, paredVestibulo);
		caja(11, SUELO + 1, -24, 36, TECHO - 1, -24, paredVestibulo);
		caja(-10, SUELO + 9, -24, 10, TECHO - 1, -24, paredVestibulo);
		// marco de la gran puerta
		caja(-11, SUELO + 1, -24, -11, SUELO + 9, -24, b(Blocks.QUARTZ_PILLAR));
		caja(11, SUELO + 1, -24, 11, SUELO + 9, -24, b(Blocks.QUARTZ_PILLAR));
		caja(-11, SUELO + 9, -24, 11, SUELO + 9, -24, b(Blocks.CHISELED_QUARTZ_BLOCK));

		// --------------------------------------------------------- columnas
		for (int cx : COL_X) {
			for (int cz : COL_Z) {
				caja(cx - 1, SUELO + 1, cz - 1, cx + 1, TECHO - 1, cz + 1, b(Blocks.QUARTZ_PILLAR));
				caja(cx - 1, SUELO + 1, cz - 1, cx + 1, SUELO + 1, cz + 1, b(Blocks.POLISHED_DEEPSLATE));
				caja(cx - 1, TECHO - 1, cz - 1, cx + 1, TECHO - 1, cz + 1, b(Blocks.CHISELED_QUARTZ_BLOCK));
				// luz en las cuatro caras y estandartes negro y amarillo
				for (Direction d : Direction.Plane.HORIZONTAL) {
					int fx = cx + d.getStepX();
					int fz = cz + d.getStepZ();
					punto(fx, SUELO + 6, fz, b(Blocks.OCHRE_FROGLIGHT));
					Block estandarte = ((cx + cz) / 2 + d.get2DDataValue()) % 2 == 0 ? Blocks.YELLOW_WALL_BANNER : Blocks.BLACK_WALL_BANNER;
					punto(fx + d.getStepX(), SUELO + 10, fz + d.getStepZ(), estandarte.defaultBlockState().setValue(WallBannerBlock.FACING, d));
				}
			}
		}

		// ------------------------------------------- mostrador de informacion
		caja(-5, SUELO + 1, 14, 5, SUELO + 1, 22, b(Blocks.DARK_OAK_PLANKS));
		caja(-5, SUELO + 2, 14, 5, SUELO + 2, 22, losa(Blocks.SMOOTH_QUARTZ_SLAB));
		caja(-3, SUELO + 1, 16, 3, SUELO + 2, 20, aire);
		caja(-1, SUELO + 1, 22, 1, SUELO + 2, 22, aire);           // entrada del personal
		caja(-3, SUELO, 16, 3, SUELO, 20, b(Blocks.DARK_OAK_PLANKS));
		punto(-2, SUELO + 1, 17, mira(Bloques.SOFA, Direction.NORTH));
		punto(2, SUELO + 1, 17, mira(Bloques.SOFA, Direction.NORTH));
		punto(-2, SUELO + 1, 19, mira(Bloques.SOFA, Direction.SOUTH));
		punto(2, SUELO + 1, 19, mira(Bloques.SOFA, Direction.SOUTH));
		punto(0, SUELO + 1, 18, b(Blocks.LECTERN));
		punto(-5, SUELO + 3, 14, b(Blocks.POTTED_FERN));
		punto(5, SUELO + 3, 14, b(Blocks.POTTED_FERN));
		punto(-5, SUELO + 3, 22, b(Blocks.POTTED_AZALEA));
		punto(5, SUELO + 3, 22, b(Blocks.POTTED_AZALEA));
		// lampara colgando encima
		caja(0, SUELO + 9, 18, 0, TECHO - 1, 18, b(Blocks.IRON_CHAIN));
		punto(0, SUELO + 8, 18, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));

		// ----------------------------------------------------- zonas de espera
		for (int sx : new int[] {-18, 18}) {
			for (int sz : new int[] {-6, 24}) {
				zonaEspera(sx, sz);
			}
		}

		// ------------------------------------------------ arboles en macetas
		for (int[] p : new int[][] {{-30, -18}, {30, -18}, {-30, 10}, {30, 10}, {-6, -18}, {6, -18}}) {
			maceta(p[0], p[1]);
		}

		// -------------------------------------- maquinas de agua de almendras
		for (int z = -2; z <= 3; z++) {
			punto(36, SUELO + 1, z, mira(Bloques.MAQUINA_ABAJO, Direction.WEST));
			punto(36, SUELO + 2, z, mira(Bloques.MAQUINA_ARRIBA, Direction.WEST));
		}
		caja(35, SUELO, -3, 35, SUELO, 4, b(Blocks.POLISHED_DEEPSLATE));

		// ------------------------------------------------------------ taquillas
		for (int z = -4; z <= 7; z++) {
			punto(-36, SUELO + 1, z, mira(Bloques.TAQUILLA, Direction.EAST));
			punto(-36, SUELO + 2, z, mira(Bloques.TAQUILLA, Direction.EAST));
		}
		caja(-33, SUELO + 1, -3, -33, SUELO + 1, 6, losa(Blocks.SPRUCE_SLAB));

		// ------------------------------------------------- vitrina del equipo
		for (int i = 0; i < 4; i++) {
			int x = -22 + i * 3;
			punto(x, SUELO + 1, -18, b(Blocks.QUARTZ_PILLAR));
			caja(x, SUELO + 2, -18, x, SUELO + 3, -18, b(Blocks.GLASS));
		}

		// ------------------------------------------------------- ascensores
		caja(-36, SUELO, 29, 36, SUELO, 32, (x, y, z) -> z == 29 && ((x & 1) == 0) ? b(Blocks.YELLOW_CONCRETE)
			: z == 29 ? b(Blocks.BLACK_CONCRETE) : b(Blocks.POLISHED_DEEPSLATE));
		BlockState acero = b(Bloques.ACERO);
		for (int cx : ASCENSORES) {
			caja(cx - 2, SUELO + 1, PUERTA_Z, cx + 1, SUELO + 5, PUERTA_Z, acero);
			caja(cx - 1, SUELO + 1, PUERTA_Z, cx, SUELO + 3, PUERTA_Z, (x, y, z) -> puerta(x - cx + 1));
			punto(cx + 2, SUELO + 2, PUERTA_Z - 1, pared(Bloques.PANEL_ASCENSOR, Direction.NORTH));
			cabina(cx, SUELO, CABINA_Z0, CABINA_Z1, cx - 2, cx + 1);
			// la pared de las puertas por dentro: hierro, las dos hojas y el indicador negro encima
			caja(cx - 2, SUELO + 1, PUERTA_Z + 1, cx + 1, SUELO + 4, PUERTA_Z + 1, b(Blocks.IRON_BLOCK));
			for (int hoja = 0; hoja < 2; hoja++) {
				punto(cx - 1 + hoja, SUELO + 1, PUERTA_Z + 1, puertaCabina(hoja, false, false));
				punto(cx - 1 + hoja, SUELO + 2, PUERTA_Z + 1, puertaCabina(hoja, true, false));
			}
			caja(cx - 1, SUELO + 3, PUERTA_Z + 1, cx, SUELO + 3, PUERTA_Z + 1, b(Blocks.BLACK_CONCRETE));
			// la botonera, en la pared derecha junto a la puerta
			for (int by = SUELO + 2; by <= SUELO + 3; by++) {
				punto(cx + 1, by, CABINA_Z0, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
					.setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.WEST));
			}
		}
		// el hueco de espera, bajo el vestibulo (ver HUECO_*): por dentro, otra cabina larga
		cabina(0, HUECO_Y - 1, HUECO_Z - 1, HUECO_Z + 1, HUECO_X0, HUECO_X1);

		// ------------------------------------------------- auditorio: sala
		caja(-26, SUELO, -68, 26, SUELO, -25, (x, y, z) -> {
			if (z >= -54 && x >= -12 && x <= 12) {
				return b(Blocks.RED_WOOL);
			}
			return b(Blocks.DARK_OAK_PLANKS);
		});
		caja(-26, TECHO, -68, 26, TECHO, -25, (x, y, z) -> {
			// encima del escenario, focos muy juntos; en la sala, una rejilla de luz calida
			if (z <= -57 && x >= -18 && x <= 18 && Math.floorMod(x, 3) == 0 && Math.floorMod(z, 2) == 0) {
				return b(Blocks.PEARLESCENT_FROGLIGHT);
			}
			return Math.floorMod(x, 4) == 1 && Math.floorMod(z, 4) == 1 ? b(Blocks.OCHRE_FROGLIGHT) : b(Blocks.BLACK_CONCRETE);
		});
		Patron paredAuditorio = (x, y, z) -> {
			if (y <= SUELO + 2) {
				return b(Blocks.DARK_OAK_PLANKS);
			}
			if (y == SUELO + 6 && Math.floorMod(z, 6) == 0) {
				return b(Blocks.OCHRE_FROGLIGHT);
			}
			return Math.floorMod(z, 4) < 2 ? b(Blocks.BROWN_WOOL) : b(Blocks.DARK_OAK_PLANKS);
		};
		caja(-27, SUELO + 1, -68, -27, TECHO - 1, -25, paredAuditorio);
		caja(27, SUELO + 1, -68, 27, TECHO - 1, -25, paredAuditorio);
		caja(-26, SUELO + 1, -69, 26, TECHO - 1, -69, b(Blocks.BLACK_CONCRETE));
		// salidas de emergencia
		punto(-26, SUELO + 4, -30, pared(Bloques.SALIDA, Direction.EAST));
		punto(26, SUELO + 4, -30, pared(Bloques.SALIDA, Direction.WEST));

		// escenario
		caja(-18, SUELO + 1, -68, 18, SUELO + 2, -57, b(Blocks.DARK_OAK_PLANKS));
		caja(-18, SUELO + 1, -56, 18, SUELO + 2, -56, b(Blocks.POLISHED_BLACKSTONE));
		caja(-4, SUELO + 1, -55, 4, SUELO + 1, -55, escalera(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		caja(-4, SUELO + 2, -56, 4, SUELO + 2, -56, escalera(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		// pantalla con su marco (el logo lo pone Vestibulo)
		caja(-16, SUELO + 3, -68, 16, TECHO - 1, -68, b(Blocks.POLISHED_BLACKSTONE));
		caja(-15, SUELO + 4, -68, 15, TECHO - 2, -68, b(Blocks.BLACK_CONCRETE));
		// telon a los lados y bambalina arriba
		caja(-18, SUELO + 3, -67, -16, TECHO - 1, -66, b(Blocks.RED_WOOL));
		caja(16, SUELO + 3, -67, 18, TECHO - 1, -66, b(Blocks.RED_WOOL));
		caja(-18, TECHO - 2, -58, 18, TECHO - 1, -58, b(Blocks.RED_WOOL));
		// candilejas al borde del escenario
		for (int x = -17; x <= 17; x += 2) {
			if (Math.abs(x) > 4) {
				punto(x, SUELO + 3, -57, b(Blocks.LANTERN));
			}
		}
		// atril y altavoces
		punto(7, SUELO + 3, -60, b(Blocks.LECTERN));
		for (int sx : new int[] {-21, 21}) {
			caja(sx - 1, SUELO + 1, -61, sx, SUELO + 6, -60, b(Blocks.BLACK_CONCRETE));
			punto(sx, SUELO + 3, -59, b(Blocks.NOTE_BLOCK));
			punto(sx - 1, SUELO + 5, -59, b(Blocks.NOTE_BLOCK));
		}
		// focos colgados de una cercha
		caja(-18, TECHO - 3, -61, 18, TECHO - 3, -61, b(Blocks.IRON_BARS));
		for (int x = -16; x <= 16; x += 4) {
			punto(x, TECHO - 4, -61, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
		}
		// estandartes a los lados del escenario
		for (int x : new int[] {-24, -20, 20, 24}) {
			punto(x, SUELO + 9, -68, (x % 8 == 0 ? Blocks.BLACK_WALL_BANNER : Blocks.YELLOW_WALL_BANNER).defaultBlockState()
				.setValue(WallBannerBlock.FACING, Direction.SOUTH));
		}

		// 200 butacas: 10 filas de 20, pasillo central de 5
		for (int fila = 0; fila < 10; fila++) {
			int z = -50 + fila * 2;
			caja(-12, SUELO + 1, z, -3, SUELO + 1, z, mira(Bloques.BUTACA, Direction.NORTH));
			caja(3, SUELO + 1, z, 12, SUELO + 1, z, mira(Bloques.BUTACA, Direction.NORTH));
		}
		// alfombra del pasillo central y luces de suelo en los extremos de las filas
		caja(-2, SUELO + 1, -54, 2, SUELO + 1, -25, b(Blocks.RED_CARPET));
		for (int fila = 0; fila < 10; fila++) {
			int z = -50 + fila * 2;
			punto(-13, SUELO + 1, z, b(Blocks.LANTERN));
			punto(13, SUELO + 1, z, b(Blocks.LANTERN));
		}
		// plantas en los rincones del auditorio
		for (int[] p : new int[][] {{-24, -27}, {24, -27}, {-24, -52}, {24, -52}}) {
			maceta(p[0], p[1]);
		}
	}

	/**
	 * Una cabina como la de la cinematica, de x0..x1 y z0..z1 por dentro, con el suelo en
	 * y = suelo: bloque de hierro, zocalo de roble oscuro, suelo de piedra lisa, tres de
	 * alto y lamparas de redstone en el techo (con su bloque de redstone encima).
	 */
	private static void cabina(int cx, int suelo, int z0, int z1, int x0, int x1) {
		caja(x0 - 1, suelo, z0 - 1, x1 + 1, suelo + 5, z1 + 1, b(Blocks.IRON_BLOCK));
		caja(x0 - 1, suelo + 1, z0, x1 + 1, suelo + 1, z1 + 1, b(Blocks.DARK_OAK_PLANKS));
		caja(x0, suelo, z0, x1, suelo, z1, b(Blocks.SMOOTH_STONE));
		caja(x0, suelo + 1, z0, x1, suelo + 3, z1, b(Blocks.AIR));
		int zm = (z0 + z1) / 2;
		for (int x = x0; x <= x1; x++) {
			// en las cabinas, las dos del centro; en el hueco largo, una de cada tres
			boolean luz = x1 - x0 < 6 ? Math.abs(2 * x - (x0 + x1)) <= 1 : Math.floorMod(x - cx, 3) == 0;
			if (luz) {
				punto(x, suelo + 4, zm, lampara());
				punto(x, suelo + 5, zm, b(Blocks.REDSTONE_BLOCK));
			}
		}
	}

	/** Sofas en U alrededor de una mesa baja, con alfombra y plantas. */
	private static void zonaEspera(int cx, int cz) {
		caja(cx - 3, SUELO + 1, cz - 3, cx + 3, SUELO + 1, cz + 3, b(Blocks.BROWN_CARPET));
		caja(cx - 2, SUELO + 1, cz - 3, cx + 2, SUELO + 1, cz - 3, mira(Bloques.SOFA, Direction.SOUTH));
		caja(cx - 2, SUELO + 1, cz + 3, cx + 2, SUELO + 1, cz + 3, mira(Bloques.SOFA, Direction.NORTH));
		caja(cx - 3, SUELO + 1, cz - 1, cx - 3, SUELO + 1, cz + 1, mira(Bloques.SOFA, Direction.EAST));
		caja(cx - 1, SUELO + 1, cz - 1, cx + 1, SUELO + 1, cz, b(Blocks.SPRUCE_FENCE));
		caja(cx - 1, SUELO + 2, cz - 1, cx + 1, SUELO + 2, cz, losa(Blocks.SPRUCE_SLAB));
		punto(cx, SUELO + 3, cz, b(Blocks.POTTED_RED_TULIP));
		punto(cx - 3, SUELO + 1, cz - 3, b(Blocks.POTTED_AZALEA));
		punto(cx + 3, SUELO + 1, cz - 3, b(Blocks.POTTED_FLOWERING_AZALEA));
		punto(cx - 3, SUELO + 1, cz + 3, b(Blocks.POTTED_FLOWERING_AZALEA));
		punto(cx + 3, SUELO + 1, cz + 3, b(Blocks.POTTED_AZALEA));
		// lampara de pie
		caja(cx + 3, SUELO + 1, cz, cx + 3, SUELO + 2, cz, b(Blocks.DARK_OAK_FENCE));
		punto(cx + 3, SUELO + 3, cz, b(Blocks.LANTERN));
	}

	/** Un arbolito de azalea en un macetero de piedra negra. */
	private static void maceta(int x, int z) {
		caja(x - 1, SUELO + 1, z - 1, x + 1, SUELO + 1, z + 1, b(Blocks.POLISHED_BLACKSTONE));
		punto(x, SUELO + 1, z, b(Blocks.MOSS_BLOCK));
		punto(x, SUELO + 2, z, b(Blocks.DARK_OAK_LOG));
		caja(x - 1, SUELO + 3, z - 1, x + 1, SUELO + 4, z + 1, b(Blocks.FLOWERING_AZALEA_LEAVES).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
		punto(x, SUELO + 5, z, b(Blocks.AZALEA_LEAVES).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
	}
}
