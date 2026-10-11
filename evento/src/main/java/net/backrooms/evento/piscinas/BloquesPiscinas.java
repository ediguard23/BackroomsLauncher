package net.backrooms.evento.piscinas;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Bloques de las Piscinas (Nivel 37): azulejo blanco con su junta (limpio, sucio y
 * con piezas caidas), el fondo de la piscina con las causticas animadas, las franjas
 * de las calles, el borde, los paneles de luz, las claraboyas y los focos de debajo
 * del agua. Texturas y modelos: tools/texturas/piscinas.js.
 */
public final class BloquesPiscinas {
	private static final SoundType AZULEJOS = SoundType.DEEPSLATE_TILES;

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

	private BloquesPiscinas() {
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
