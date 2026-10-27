package net.backrooms.evento.estacion;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloques y objetos de la Estacion Electrica (Nivel 3). Texturas y modelos en
 * tools/texturas/estacion.js. Lo de la estructura no se rompe (es un nivel, no un mundo).
 */
public final class BloquesEstacion {
	/* ---------------------------------------------------------- estructura */

	/** Los pasos en el suelo de la estacion: hormigon (o chapa) con un poco de eco (Sonidos). */
	private static final SoundType PASOS_HORMIGON = new SoundType(0.8F, 1.0F, net.minecraft.sounds.SoundEvents.STONE_BREAK, net.backrooms.evento.Sonidos.ESTACION_PASO,
		net.minecraft.sounds.SoundEvents.STONE_PLACE, net.minecraft.sounds.SoundEvents.STONE_HIT, net.minecraft.sounds.SoundEvents.STONE_FALL);
	private static final SoundType PASOS_CHAPA = new SoundType(0.8F, 1.0F, net.minecraft.sounds.SoundEvents.METAL_BREAK, net.backrooms.evento.Sonidos.ESTACION_PASO_CHAPA,
		net.minecraft.sounds.SoundEvents.METAL_PLACE, net.minecraft.sounds.SoundEvents.METAL_HIT, net.minecraft.sounds.SoundEvents.METAL_FALL);

	/** Hormigon de las paredes, manchado de humedad y hollin. */
	public static final Block HORMIGON = registrar("hormigon_estacion", propiedades(MapColor.STONE, SoundType.STONE));
	/** La parte de abajo de las paredes, pintada de verde gris palido y desconchada. */
	public static final Block PINTADA = registrar("pared_pintada", propiedades(MapColor.COLOR_LIGHT_GREEN, SoundType.STONE));
	/** La franja pintada con el color del sector (COLOR: 0 rojo, 1 amarillo, 2 azul, 3 verde). */
	public static final Block FRANJA = conObjeto(Blocks.register(clave("franja_sector"), Interactivos.Franja::new, propiedades(MapColor.COLOR_RED, SoundType.STONE)));
	public static final Block SUELO = registrar("suelo_estacion", propiedades(MapColor.STONE, PASOS_HORMIGON));
	public static final Block SUELO_MANCHADO = registrar("suelo_estacion_manchado", propiedades(MapColor.STONE, PASOS_HORMIGON));
	public static final Block CHAPA = registrar("chapa_estacion", propiedades(MapColor.METAL, PASOS_CHAPA));
	public static final Block REJILLA = registrar("rejilla_estacion", propiedades(MapColor.METAL, PASOS_CHAPA).noOcclusion());
	public static final Block TECHO = registrar("techo_estacion", propiedades(MapColor.COLOR_GRAY, SoundType.STONE));
	/** El techo con la bandeja de cables y una tuberia por debajo. */
	public static final Block TECHO_TUBOS = registrar("techo_tubos", propiedades(MapColor.COLOR_GRAY, SoundType.METAL));
	public static final Block LAMPARA = registrar("lampara_estacion", propiedades(MapColor.SNOW, SoundType.GLASS).lightLevel(e -> 14));
	public static final Block LAMPARA_FUNDIDA = registrar("lampara_fundida", propiedades(MapColor.COLOR_GRAY, SoundType.GLASS));
	/** El marco fijo a los lados de una reja corrediza (la hoja se mete en el de la izquierda al abrirse). */
	public static final Block MARCO_REJA = registrar("marco_reja", propiedades(MapColor.METAL, SoundType.METAL));

	/* ---------------------------------------------------------- decorado */

	/** En la pared: tuberias (0..3), cuadros electricos con relojes (4..7). */
	public static final Block TUBERIAS = pared("tuberias_pared", 0, Block.box(0, 4, 10, 16, 15, 16));
	public static final Block CUADRO = pared("cuadro_electrico", 1, Block.box(1, 1, 12, 15, 15, 16));
	/** Senales y carteles: PELIGRO ALTA TENSION, sector, salida, prohibido, extintor, combustible... */
	public static final Block SENAL = pared("senal_estacion", 0, Block.box(0, 0, 15, 16, 16, 16));
	/** Luz roja de emergencia enjaulada, en la pared. */
	public static final Block LUZ_ROJA = pared("luz_roja", 8, Block.box(4, 3, 9, 12, 11, 16));
	public static final Block ROTULO = conObjeto(Blocks.register(clave("rotulo_sector"), Interactivos.Rotulo::new,
		propiedades(MapColor.COLOR_YELLOW, SoundType.METAL).noOcclusion()));
	/** Un buzon de chapa en la pared: con un fusible (que se queda dentro) se abre y da lo que tenga. */
	public static final Block BUZON = conObjeto(Blocks.register(clave("buzon_estacion"), Interactivos.Buzon::new,
		propiedades(MapColor.COLOR_GRAY, SoundType.METAL).noOcclusion()));
	/** Cables que cuelgan del techo (no chocan). */
	public static final Block CABLES = suelo("cables_colgando", Block.box(2, 0, 2, 14, 16, 14), Block.box(4, 0, 4, 12, 16, 12));
	/** En el suelo, macizos: armario electrico, transformador, maquina, bidones, estanteria, carro, banco, consola. */
	public static final Block ARMARIO = suelo("armario_electrico", Block.box(0, 0, 2, 16, 16, 16), Block.box(0, 0, 2, 16, 16, 16));
	public static final Block TRANSFORMADOR = suelo("transformador", Block.box(1, 0, 1, 15, 16, 15), Block.box(1, 0, 1, 15, 16, 15));
	/** Lo de las centrales: racks de servidores abandonados y cables gruesos tirados por el suelo (no chocan). */
	public static final Block RACK = suelo("rack_servidor", Block.box(1, 0, 1, 15, 16, 15), Block.box(1, 0, 1, 15, 16, 15));
	public static final Block CABLES_SUELO = suelo("cables_suelo", Block.box(0, 0, 0, 16, 3, 16), net.minecraft.world.phys.shapes.Shapes.empty());
	public static final Block MAQUINA = suelo("maquina_estacion", Block.box(0, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 16));
	public static final Block BIDONES = suelo("bidones", Block.box(1, 0, 1, 15, 14, 15), Block.box(1, 0, 1, 15, 14, 15));
	public static final Block ESTANTERIA = suelo("estanteria_estacion", Block.box(0, 0, 4, 16, 16, 16), Block.box(0, 0, 4, 16, 16, 16));
	public static final Block CONSOLA = suelo("consola_control", Block.box(0, 0, 2, 16, 14, 16), Block.box(0, 0, 2, 16, 14, 16));
	public static final Block BARANDILLA = suelo("barandilla", Block.box(0, 0, 14, 16, 16, 16), Block.box(0, 0, 14, 16, 24, 16));

	/* ---------------------------------------------------------- lo que se usa */

	public static final Block PUNTO = conObjeto(Blocks.register(clave("punto_recogida"), Interactivos.Punto::new,
		propiedades(MapColor.COLOR_YELLOW, SoundType.METAL).noOcclusion()));
	public static final Block PANEL = conObjeto(Blocks.register(clave("panel_averiado"), Interactivos.Panel::new,
		propiedades(MapColor.COLOR_GRAY, SoundType.METAL).noOcclusion().lightLevel(e -> e.getValue(Interactivos.HECHO) ? 4 : 2)));
	/** La caja de fusible de una reja, en el muro (se ve por los dos lados). */
	public static final Block CAJA_REJA = conObjeto(Blocks.register(clave("caja_fusible"), Interactivos.CajaReja::new,
		propiedades(MapColor.COLOR_GRAY, SoundType.METAL).lightLevel(e -> e.getValue(Interactivos.HECHO) ? 5 : 1)));
	/** Las rejas de los laberintos (3x3, como la persiana) y la puerta encadenada del final. */
	public static final Block REJA = conObjeto(Blocks.register(clave("reja_estacion"), Interactivos.Persiana::new,
		propiedades(MapColor.METAL, SoundType.METAL).noOcclusion()));
	public static final Block PERSIANA = conObjeto(Blocks.register(clave("persiana_estacion"), Interactivos.Persiana::new,
		propiedades(MapColor.METAL, SoundType.METAL).noOcclusion()));
	public static final Block FOCO = conObjeto(Blocks.register(clave("foco_pasarela"), Interactivos.Foco::new,
		propiedades(MapColor.COLOR_YELLOW, SoundType.GLASS).lightLevel(e -> e.getValue(Interactivos.LIT) ? 15 : 0)));

	/* ------------------------------------------------------------- objetos */

	public static final Item CABLE = objeto("cable_electrico", 4);
	/** Para la cadena de la puerta del final (solo hay una en todo el nivel). */
	public static final Item CIZALLA = objeto("cizalla", 1);
	public static final Item CINTA = objeto("cinta_aislante", 4);
	public static final Item FUSIBLE = objeto("fusible", 4);
	/** Lo que da cada punto de recogida (PlanoEstacion.CABLE, CIZALLA, CINTA, -, FUSIBLE). */
	private static final Item[] POR_TIPO = {CABLE, CIZALLA, CINTA, FUSIBLE, FUSIBLE};

	private BloquesEstacion() {
	}

	/** El objeto que da un punto de recogida (Interactivos.TIPO). */
	public static Item deTipo(int tipo) {
		return POR_TIPO[Math.min(tipo, POR_TIPO.length - 1)];
	}

	/** Si es un objeto de la Estacion (material, bidon o fusible): no sale del nivel. */
	public static boolean esDeLaEstacion(ItemStack s) {
		if (s.isEmpty()) {
			return false;
		}
		for (Item i : POR_TIPO) {
			if (s.is(i)) {
				return true;
			}
		}
		return false;
	}

	/** Los que se ven a traves (rejilla, cables, barandilla...): para el cliente, capa CUTOUT. */
	public static Block[] recortados() {
		return new Block[] {REJILLA, TUBERIAS, CUADRO, SENAL, LUZ_ROJA, ROTULO, CABLES, BARANDILLA, PUNTO, PANEL, REJA, BUZON,
			ESTANTERIA, CONSOLA, ARMARIO, TRANSFORMADOR, MAQUINA, BIDONES, LAMPARA, LAMPARA_FUNDIDA};
	}

	private static Block pared(String id, int luz, VoxelShape forma) {
		return conObjeto(Blocks.register(clave(id), p -> new Variante.Pared(p, forma),
			propiedades(MapColor.COLOR_GRAY, SoundType.METAL).noOcclusion().lightLevel(e -> luz)));
	}

	private static Block suelo(String id, VoxelShape forma, VoxelShape choque) {
		return conObjeto(Blocks.register(clave(id), p -> new Variante.Suelo(p, forma, choque), propiedades(MapColor.METAL, SoundType.METAL).noOcclusion()));
	}

	private static Item objeto(String id, int pila) {
		return Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id(id)), Item::new, new Item.Properties().stacksTo(pila));
	}

	private static ResourceKey<Block> clave(String id) {
		return ResourceKey.create(Registries.BLOCK, BackroomsEvento.id(id));
	}

	private static BlockBehaviour.Properties propiedades(MapColor color, SoundType sonido) {
		return BlockBehaviour.Properties.of().mapColor(color).sound(sonido).strength(-1.0F, 3600000.0F);
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
		BackroomsEvento.LOG.info("Bloques de la Estacion registrados");
	}
}
