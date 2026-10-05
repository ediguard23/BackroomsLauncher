package net.backrooms.evento.bloques;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Bloques del Nivel 0. Con textura propia dentro del mod: el jugador no
 * descarga ningun resource pack al entrar.
 *
 * Se pueden romper (el staff construye en creativo); los jugadores van en
 * modo aventura.
 */
public final class Bloques {
	public static final Block PAPEL_PINTADO = registrar("papel_pintado",
		propiedades(MapColor.COLOR_YELLOW, SoundType.WOOL, 1.5F));
	public static final Block PAPEL_PINTADO_SUCIO = registrar("papel_pintado_sucio",
		propiedades(MapColor.COLOR_YELLOW, SoundType.WOOL, 1.5F));
	public static final Block ZOCALO = registrar("zocalo",
		propiedades(MapColor.COLOR_YELLOW, SoundType.WOOD, 1.5F));
	public static final Block MOQUETA = registrar("moqueta",
		propiedades(MapColor.SAND, SoundType.WOOL, 0.8F));
	public static final Block MOQUETA_MOJADA = registrar("moqueta_mojada",
		propiedades(MapColor.DIRT, SoundType.WET_SPONGE, 0.8F));
	public static final Block TECHO = registrar("techo",
		propiedades(MapColor.SAND, SoundType.CALCITE, 1.0F));
	public static final Block FLUORESCENTE = registrar("fluorescente",
		propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> 15));
	public static final Block FLUORESCENTE_APAGADO = registrar("fluorescente_apagado",
		propiedades(MapColor.COLOR_GRAY, SoundType.GLASS, 0.6F));
	public static final Block FLUORESCENTE_PARPADEO = Blocks.register(clave("fluorescente_parpadeo"), TuboParpadeante::new,
		propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> e.getValue(TuboParpadeante.LIT) ? 15 : 0));

	/* ---------------------------------------------------- decoracion */

	public static final Block ENCHUFE = conObjeto(Blocks.register(clave("enchufe"), EnPared::new,
		propiedades(MapColor.SNOW, SoundType.STONE, 0.5F).noCollision().noOcclusion()));
	public static final Block ENCHUFE_MANCHADO = conObjeto(Blocks.register(clave("enchufe_manchado"), EnPared::new,
		propiedades(MapColor.SAND, SoundType.STONE, 0.5F).noCollision().noOcclusion()));
	public static final Block DIBUJO = conObjeto(Blocks.register(clave("dibujo"), Dibujo::new,
		propiedades(MapColor.NONE, SoundType.WOOL, 0.2F).noCollision().noOcclusion().replaceable()));
	public static final Block SALIDA = conObjeto(Blocks.register(clave("salida"), EnPared::new,
		propiedades(MapColor.COLOR_GREEN, SoundType.METAL, 0.8F).noCollision().noOcclusion().lightLevel(e -> 7)));
	public static final Block VENTILADOR = conObjeto(Blocks.register(clave("ventilador"), Ventilador::new,
		propiedades(MapColor.METAL, SoundType.METAL, 0.8F).noCollision().noOcclusion()));
	public static final Block VENTILADOR_GRANDE = conObjeto(Blocks.register(clave("ventilador_grande"), Ventilador::new,
		propiedades(MapColor.METAL, SoundType.METAL, 0.8F).noCollision().noOcclusion()));
	public static final Block NOTA = conObjeto(Blocks.register(clave("nota"), Nota::new,
		propiedades(MapColor.SNOW, SoundType.WOOL, 0.2F).noOcclusion()));
	public static final Block SILLA = conObjeto(Blocks.register(clave("silla"), Silla::new,
		propiedades(MapColor.COLOR_BLUE, SoundType.WOOD, 1.0F).noOcclusion()));
	public static final Block SENAL_ALTO = senal("senal_alto");
	public static final Block SENAL_PELIGRO = senal("senal_peligro");
	public static final Block SENAL_SIGA = senal("senal_siga");
	public static final Block SENAL_ALTO_REVES = senal("senal_alto_reves");
	public static final Block SENAL_SIGA_REVES = senal("senal_siga_reves");
	public static final Block BACILO = registrar("bacilo", propiedades(MapColor.COLOR_BLACK, SoundType.SCULK, 1.0F));
	public static final Block RAIZ_BACILO = registrar("raiz_bacilo", propiedades(MapColor.COLOR_BLACK, SoundType.SCULK, 1.0F));
	public static final Block CAPA_BACILO = conObjeto(Blocks.register(clave("capa_bacilo"), CarpetBlock::new,
		propiedades(MapColor.COLOR_BLACK, SoundType.SCULK_VEIN, 0.2F).noOcclusion()));
	public static final Block PARED_FINA = conObjeto(Blocks.register(clave("pared_fina"), IronBarsBlock::new,
		propiedades(MapColor.COLOR_YELLOW, SoundType.WOOL, 1.5F).noOcclusion()));
	public static final Block PARED_FINA_SUCIA = conObjeto(Blocks.register(clave("pared_fina_sucia"), IronBarsBlock::new,
		propiedades(MapColor.COLOR_YELLOW, SoundType.WOOL, 1.5F).noOcclusion()));

	/* ------------------------------------------- ascensor de salida */

	public static final Block ACERO = registrar("acero", propiedades(MapColor.METAL, SoundType.METAL, 3.0F));
	public static final Block ASCENSOR_SUELO = registrar("ascensor_suelo", propiedades(MapColor.METAL, SoundType.METAL, 3.0F));
	public static final Block LUZ_ASCENSOR = registrar("luz_ascensor",
		propiedades(MapColor.SNOW, SoundType.GLASS, 0.6F).lightLevel(e -> 15));
	public static final Block PANEL_ASCENSOR = conObjeto(Blocks.register(clave("panel_ascensor"), PanelAscensor::new,
		propiedades(MapColor.METAL, SoundType.METAL, 1.0F).noCollision().noOcclusion().lightLevel(e -> 5)));

	/* ------------------------------------------------------- vestibulo */

	public static final Block BUTACA = conObjeto(Blocks.register(clave("butaca"), net.backrooms.evento.vestibulo.Butaca::new,
		propiedades(MapColor.COLOR_RED, SoundType.WOOL, 1.0F).noOcclusion()));
	public static final Block SOFA = conObjeto(Blocks.register(clave("sofa"), net.backrooms.evento.vestibulo.Butaca::new,
		propiedades(MapColor.COLOR_GRAY, SoundType.WOOL, 1.0F).noOcclusion()));
	public static final Block MAQUINA_ABAJO = orientado("maquina_abajo", MapColor.COLOR_RED, SoundType.METAL, 0);
	public static final Block MAQUINA_ARRIBA = orientado("maquina_arriba", MapColor.COLOR_RED, SoundType.METAL, 8);
	public static final Block TAQUILLA = orientado("taquilla", MapColor.COLOR_GRAY, SoundType.METAL, 0);
	public static final Block PUERTA_ASCENSOR_IZQ = orientado("puerta_ascensor_izq", MapColor.METAL, SoundType.METAL, 0);
	public static final Block PUERTA_ASCENSOR_DER = orientado("puerta_ascensor_der", MapColor.METAL, SoundType.METAL, 0);

	/** Las senales en el orden del reparto del generador. */
	public static final Block[] SENALES = {SENAL_ALTO, SENAL_PELIGRO, SENAL_SIGA, SENAL_ALTO_REVES, SENAL_SIGA_REVES};

	static {
		Items.registerBlock(FLUORESCENTE_PARPADEO);
	}

	private static Block conObjeto(Block bloque) {
		Items.registerBlock(bloque);
		return bloque;
	}

	private static Block senal(String id) {
		return conObjeto(Blocks.register(clave(id),
			p -> new EnSuelo(p, Block.box(2, 0, 7, 14, 16, 10), Block.box(7, 0, 8, 9, 16, 10)),
			propiedades(MapColor.METAL, SoundType.METAL, 1.0F).noOcclusion()));
	}

	/** Bloque entero con frente (maquinas, taquillas, puertas): FACING es hacia donde mira el frente. */
	private static Block orientado(String id, MapColor color, SoundType sonido, int luz) {
		return conObjeto(Blocks.register(clave(id),
			p -> new EnSuelo(p, Block.box(0, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 16)),
			propiedades(color, sonido, 2.0F).lightLevel(e -> luz)));
	}

	private Bloques() {
	}

	private static ResourceKey<Block> clave(String id) {
		return ResourceKey.create(Registries.BLOCK, BackroomsEvento.id(id));
	}

	private static BlockBehaviour.Properties propiedades(MapColor color, SoundType sonido, float dureza) {
		return BlockBehaviour.Properties.of().mapColor(color).sound(sonido).strength(dureza);
	}

	private static Block registrar(String id, BlockBehaviour.Properties propiedades) {
		Block bloque = Blocks.register(clave(id), propiedades);
		Items.registerBlock(bloque);
		return bloque;
	}

	/** Fuerza la carga de la clase (y con ella el registro) desde el inicializador. */
	public static void iniciar() {
		BackroomsEvento.LOG.info("Bloques del Nivel 0 registrados");
	}
}
