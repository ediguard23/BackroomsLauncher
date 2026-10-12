package net.backrooms.evento.piscinas;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.bloques.EnSuelo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloques de las Piscinas (Nivel 37): azulejo blanco con su junta (limpio, sucio y
 * con piezas caidas), el fondo de la piscina con las causticas animadas, las franjas
 * de las calles, el borde, los paneles de luz (y el que falla), las claraboyas y los
 * focos de debajo del agua; y los detalles que se encuentran muy de vez en cuando:
 * marcas en la pared, manchas en el suelo y cosas que alguien dejo. Texturas y
 * modelos: tools/texturas/piscinas.js.
 */
public final class BloquesPiscinas {
	/** El azulejo suena como el de pizarra salvo al pisarlo: pasos secos con toda la sala detras (Sonidos). */
	private static final SoundType AZULEJOS = new SoundType(1.3F, 1.0F, SoundEvents.DEEPSLATE_TILES_BREAK, Sonidos.PISCINAS_PASO,
		SoundEvents.DEEPSLATE_TILES_PLACE, SoundEvents.DEEPSLATE_TILES_HIT, SoundEvents.DEEPSLATE_TILES_FALL);

	public static final Block AZULEJO = registrar("azulejo", propiedades(MapColor.SNOW, AZULEJOS, 1.5F));
	public static final Block AZULEJO_SUCIO = registrar("azulejo_sucio", propiedades(MapColor.SNOW, AZULEJOS, 1.5F));
	public static final Block AZULEJO_ROTO = registrar("azulejo_roto", propiedades(MapColor.SNOW, AZULEJOS, 1.5F));
	public static final Block AZULEJO_AZUL = registrar("azulejo_azul", propiedades(MapColor.COLOR_CYAN, AZULEJOS, 1.5F));
	public static final Block FONDO_PISCINA = registrar("fondo_piscina", propiedades(MapColor.COLOR_LIGHT_BLUE, AZULEJOS, 1.5F));
	public static final Block FRANJA_PISCINA = registrar("franja_piscina", propiedades(MapColor.COLOR_BLUE, AZULEJOS, 1.5F));
	public static final Block BORDE_PISCINA = registrar("borde_piscina", propiedades(MapColor.COLOR_CYAN, AZULEJOS, 1.5F));
	public static final Block REJILLA = registrar("rejilla", propiedades(MapColor.METAL, SoundType.METAL, 1.5F));
	public static final Block LUZ_TECHO = registrar("luz_techo", propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> 15));
	public static final Block CLARABOYA = registrar("claraboya", propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> 15));
	public static final Block FOCO_PISCINA = registrar("foco_piscina", propiedades(MapColor.COLOR_LIGHT_BLUE, SoundType.GLASS, 0.6F).lightLevel(e -> 15));
	public static final Block LOSA_AZULEJO = conObjeto(Blocks.register(clave("losa_azulejo"), SlabBlock::new,
		propiedades(MapColor.SNOW, AZULEJOS, 1.5F)));
	public static final Block ESCALON_AZULEJO = conObjeto(Blocks.register(clave("escalon_azulejo"),
		p -> new StairBlock(AZULEJO.defaultBlockState(), p), propiedades(MapColor.SNOW, AZULEJOS, 1.5F)));
	public static final Block LUZ_PARPADEO = conObjeto(Blocks.register(clave("luz_techo_parpadeo"), LuzParpadeante::new,
		propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> e.getValue(LuzParpadeante.LIT) ? 15 : 0)));

	/* ------------------------------------------------- detalles (raros) */

	public static final Block AZULEJO_MANCHADO = registrar("azulejo_manchado", propiedades(MapColor.SNOW, AZULEJOS, 1.5F));
	public static final Block AZULEJO_SANGRE = registrar("azulejo_sangre", propiedades(MapColor.SNOW, AZULEJOS, 1.5F));
	public static final Block MARCA = conObjeto(Blocks.register(clave("marca"), MarcaPiscina::new,
		propiedades(MapColor.NONE, SoundType.STONE, 0.2F).noCollision().noOcclusion().replaceable()));
	public static final Block MANCHA = conObjeto(Blocks.register(clave("mancha"), ManchaSuelo::new,
		propiedades(MapColor.NONE, SoundType.STONE, 0.2F).noCollision().noOcclusion().replaceable()));
	public static final Block FLOTADOR = objeto("flotador", MapColor.COLOR_YELLOW, SoundType.WOOL, Block.box(1, 0, 0, 15, 8, 15));
	public static final Block TOALLA = objeto("toalla", MapColor.COLOR_LIGHT_BLUE, SoundType.WOOL, Block.box(2, 0, 2, 14, 2, 13));
	public static final Block CHANCLAS = objeto("chanclas", MapColor.COLOR_RED, SoundType.WOOL, Block.box(3, 0, 3, 14, 2, 13));
	public static final Block MOCHILA = objeto("mochila", MapColor.COLOR_GREEN, SoundType.WOOL, Block.box(3, 0, 2, 13, 7, 12));
	public static final Block[] OBJETOS = {FLOTADOR, TOALLA, CHANCLAS, MOCHILA};

	/* ------------------------------------------------------- misiones */

	/** No se rompen (ni en creativo por error): son de las misiones. */
	public static final Block VALVULA = conObjeto(Blocks.register(clave("valvula"), BloqueValvula::new,
		propiedades(MapColor.COLOR_RED, SoundType.METAL, -1.0F).explosionResistance(3600000.0F).noOcclusion()));
	public static final Block LECTOR = conObjeto(Blocks.register(clave("lector_tarjeta"), LectorTarjeta::new,
		propiedades(MapColor.METAL, SoundType.METAL, -1.0F).explosionResistance(3600000.0F).noOcclusion()
			.lightLevel(e -> e.getValue(LectorTarjeta.LIT) ? 6 : 2)));
	public static final Block PUERTA_SALIDA = conObjeto(Blocks.register(clave("puerta_salida"), PuertaSalida::new,
		propiedades(MapColor.COLOR_GRAY, SoundType.METAL, -1.0F).explosionResistance(3600000.0F)));
	public static final Block CARTEL_SALIDA = conObjeto(Blocks.register(clave("cartel_salida"), CartelSalida::new,
		propiedades(MapColor.COLOR_GREEN, SoundType.GLASS, -1.0F).explosionResistance(3600000.0F).noCollision().noOcclusion()
			.lightLevel(e -> 7)));

	private BloquesPiscinas() {
	}

	/** Algo tirado en el suelo y orientado; no choca (no estorba ni a la gente ni a la Serpiente). */
	private static Block objeto(String id, MapColor color, SoundType sonido, VoxelShape forma) {
		return conObjeto(Blocks.register(clave(id), p -> new EnSuelo(p, forma, Shapes.empty()),
			propiedades(color, sonido, 0.5F).noCollision().noOcclusion()));
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
		BackroomsEvento.LOG.info("Bloques de las Piscinas registrados");
	}
}
