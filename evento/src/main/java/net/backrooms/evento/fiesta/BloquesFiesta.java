package net.backrooms.evento.fiesta;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.piscinas.LuzParpadeante;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloques de la Fiesta (Nivel Fun, las Feeding Rooms): el papel pintado de colores (limpio,
 * con estrellas y el que ya se despega), el zocalo de rayas, el suelo de goma de puzle, la
 * moqueta de confeti, el techo de estrellas y sus luces, las paredes acolchadas del
 * laberinto, las baldosas de la cocina; la decoracion (globos, guirnaldas, carteles, dibujos
 * de ninos, mesas con mantel, tartas, regalos, sillitas, confeti, peluches, ollas, bandejas,
 * camitas y altavoces); y lo de jugar: los tornos de monedas, la puerta del Gran Premio, las
 * lamparas del semaforo y sus lineas de salida y meta. Texturas y modelos:
 * tools/texturas/fiesta.js.
 */
public final class BloquesFiesta {
	private static final SoundType GOMA = SoundType.WOOL;

	public static final Block PAPEL = registrar("papel_fiesta", propiedades(MapColor.COLOR_LIGHT_GREEN, SoundType.WOOD, 1.5F));
	public static final Block PAPEL_ESTRELLAS = registrar("papel_estrellas", propiedades(MapColor.COLOR_PINK, SoundType.WOOD, 1.5F));
	public static final Block PAPEL_SUCIO = registrar("papel_fiesta_sucio", propiedades(MapColor.COLOR_LIGHT_GREEN, SoundType.WOOD, 1.5F));
	public static final Block ZOCALO = registrar("zocalo_fiesta", propiedades(MapColor.COLOR_RED, SoundType.WOOD, 1.5F));
	public static final Block SUELO_GOMA = registrar("suelo_goma", propiedades(MapColor.COLOR_YELLOW, GOMA, 1.0F));
	public static final Block SUELO_GOMA_SUCIO = registrar("suelo_goma_sucio", propiedades(MapColor.COLOR_YELLOW, GOMA, 1.0F));
	public static final Block MOQUETA_CONFETI = registrar("moqueta_confeti", propiedades(MapColor.COLOR_BLUE, GOMA, 1.0F));
	public static final Block BALDOSA_COCINA = registrar("baldosa_cocina", propiedades(MapColor.QUARTZ, SoundType.STONE, 1.5F));
	public static final Block TECHO = registrar("techo_fiesta", propiedades(MapColor.SNOW, SoundType.WOOD, 1.0F));
	public static final Block LUZ = registrar("luz_fiesta", propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> 15));
	public static final Block LUZ_PARPADEO = conObjeto(Blocks.register(clave("luz_fiesta_parpadeo"), LuzParpadeante::new,
		propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> e.getValue(LuzParpadeante.LIT) ? 15 : 0)));
	public static final Block ACOLCHADO_ROJO = registrar("acolchado_rojo", propiedades(MapColor.COLOR_RED, GOMA, 1.0F));
	public static final Block ACOLCHADO_AMARILLO = registrar("acolchado_amarillo", propiedades(MapColor.COLOR_YELLOW, GOMA, 1.0F));
	public static final Block ACOLCHADO_AZUL = registrar("acolchado_azul", propiedades(MapColor.COLOR_BLUE, GOMA, 1.0F));
	public static final Block ACOLCHADO_VERDE = registrar("acolchado_verde", propiedades(MapColor.COLOR_GREEN, GOMA, 1.0F));
	public static final Block[] ACOLCHADOS = {ACOLCHADO_ROJO, ACOLCHADO_AMARILLO, ACOLCHADO_AZUL, ACOLCHADO_VERDE};

	/* ------------------------------------------------------- decoracion */

	/** Globo con su cuerda: 4 colores. No choca (se cruza entre ellos). */
	public static final Block GLOBO = suelo("globo", MapColor.COLOR_RED, SoundType.WOOL, Block.box(4, 0, 4, 12, 16, 12), Shapes.empty());
	public static final Block GUIRNALDA = pared("guirnalda", MapColor.COLOR_YELLOW, SoundType.WOOL, 0);
	public static final Block CARTEL = pared("cartel_fiesta", MapColor.COLOR_ORANGE, SoundType.WOOD, 0);
	public static final Block DIBUJO = pared("dibujo_fiesta", MapColor.SNOW, SoundType.WOOD, 0);
	public static final Block ALTAVOZ = pared("altavoz_fiesta", MapColor.COLOR_GRAY, SoundType.METAL, 0);
	/** La mesa con mantel: choca como una mesa (el tablero arriba). */
	public static final Block MESA = suelo("mesa_fiesta", MapColor.COLOR_PINK, SoundType.WOOD, Block.box(0, 0, 0, 16, 13, 16), Block.box(0, 11, 0, 16, 13, 16));
	public static final Block TARTA = suelo("tarta", MapColor.COLOR_PINK, SoundType.WOOL, Block.box(3, 0, 3, 13, 9, 13), Shapes.empty());
	public static final Block REGALO = suelo("regalo", MapColor.COLOR_RED, SoundType.WOOL, Block.box(2, 0, 2, 14, 12, 14), Block.box(2, 0, 2, 14, 12, 14));
	public static final Block SILLITA = suelo("sillita", MapColor.COLOR_BLUE, SoundType.WOOD, Block.box(3, 0, 3, 13, 14, 13), Block.box(3, 0, 3, 13, 8, 13));
	public static final Block CONFETI = suelo("confeti", MapColor.NONE, SoundType.WOOL, Block.box(0, 0, 0, 16, 0.5, 16), Shapes.empty());
	public static final Block PELUCHE = suelo("peluche", MapColor.COLOR_BROWN, SoundType.WOOL, Block.box(4, 0, 4, 12, 10, 12), Shapes.empty());
	public static final Block OLLA = suelo("olla", MapColor.METAL, SoundType.METAL, Block.box(1, 0, 1, 15, 14, 15), Block.box(1, 0, 1, 15, 14, 15));
	public static final Block BANDEJA = suelo("bandeja", MapColor.METAL, SoundType.METAL, Block.box(1, 0, 3, 15, 2, 13), Shapes.empty());
	public static final Block CAMITA = suelo("camita", MapColor.COLOR_LIGHT_BLUE, SoundType.WOOL, Block.box(0, 0, 0, 16, 7, 16), Block.box(0, 0, 0, 16, 6, 16));

	/* -------------------------------------------------------- para jugar */

	public static final Block TORNO = conObjeto(Blocks.register(clave("torno_fiesta"), Torno::new,
		propiedades(MapColor.COLOR_ORANGE, SoundType.METAL, -1.0F).explosionResistance(3600000.0F).lightLevel(e -> 4)));
	public static final Block LUZ_SEMAFORO = conObjeto(Blocks.register(clave("luz_semaforo"), LuzSemaforo::new,
		propiedades(MapColor.COLOR_BLACK, SoundType.METAL, -1.0F).explosionResistance(3600000.0F).noOcclusion()
			.lightLevel(e -> e.getValue(LuzSemaforo.LUZ) ? 15 : 0)));
	/** Las lineas del semaforo pintadas en el suelo: variante 0 la salida, 1 la meta. */
	public static final Block LINEA = suelo("linea_fiesta", MapColor.SNOW, SoundType.WOOL, Block.box(0, 0, 0, 16, 0.5, 16), Shapes.empty());

	private BloquesFiesta() {
	}

	private static Block suelo(String id, MapColor color, SoundType sonido, VoxelShape forma, VoxelShape choque) {
		return conObjeto(Blocks.register(clave(id), p -> new SueloVariante(p, forma, choque),
			propiedades(color, sonido, 0.5F).noOcclusion()));
	}

	private static Block pared(String id, MapColor color, SoundType sonido, int luz) {
		return conObjeto(Blocks.register(clave(id), ParedVariante::new,
			propiedades(color, sonido, 0.5F).noCollision().noOcclusion().lightLevel(e -> luz)));
	}

	private static ResourceKey<Block> clave(String id) {
		return ResourceKey.create(Registries.BLOCK, BackroomsEvento.id(id));
	}

	private static BlockBehaviour.Properties propiedades(MapColor color, SoundType sonido, float dureza) {
		return BlockBehaviour.Properties.of().mapColor(color).sound(sonido).strength(dureza);
	}

	private static Block conObjeto(Block bloque) {
		Items.registerBlock(bloque);
		return bloque;
	}

	private static Block registrar(String id, BlockBehaviour.Properties propiedades) {
		return conObjeto(Blocks.register(clave(id), propiedades));
	}

	/** Fuerza la carga de la clase (y con ella el registro) desde el inicializador. */
	public static void iniciar() {
		BackroomsEvento.LOG.info("Bloques de la Fiesta registrados");
	}
}
