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
 * camitas y altavoces); lo de jugar (puertas de monedas, semaforos, localizador, marcador, noclip) y
 * lo del laberinto, el castillo de bolas y la sala central. Texturas y modelos: tools/texturas/fiesta.js
 * y tools/texturas/laberinto.js.
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
	/** El regalo (RegaloBloque): en la sala de los regalos se abre con clic derecho. */
	public static final Block REGALO = conObjeto(Blocks.register(clave("regalo"), RegaloBloque::new,
		propiedades(MapColor.COLOR_RED, SoundType.WOOL, 0.5F).noOcclusion()));
	public static final Block SILLITA = suelo("sillita", MapColor.COLOR_BLUE, SoundType.WOOD, Block.box(3, 0, 3, 13, 14, 13), Block.box(3, 0, 3, 13, 8, 13));
	public static final Block CONFETI = suelo("confeti", MapColor.NONE, SoundType.WOOL, Block.box(0, 0, 0, 16, 0.5, 16), Shapes.empty());
	public static final Block PELUCHE = suelo("peluche", MapColor.COLOR_BROWN, SoundType.WOOL, Block.box(4, 0, 4, 12, 10, 12), Shapes.empty());
	public static final Block OLLA = suelo("olla", MapColor.METAL, SoundType.METAL, Block.box(1, 0, 1, 15, 14, 15), Block.box(1, 0, 1, 15, 14, 15));
	public static final Block BANDEJA = suelo("bandeja", MapColor.METAL, SoundType.METAL, Block.box(1, 0, 3, 15, 2, 13), Shapes.empty());
	public static final Block CAMITA = suelo("camita", MapColor.COLOR_LIGHT_BLUE, SoundType.WOOL, Block.box(0, 0, 0, 16, 7, 16), Block.box(0, 0, 0, 16, 6, 16));

	/* -------------------------------------------------------- para jugar */

	/** Las puertas (Torno): la verde de monedas, la roja de la salida del laberinto y la del final, que pide tickets. */
	public static final Block PUERTA = conObjeto(Blocks.register(clave("puerta_fiesta"), Torno::new,
		propiedades(MapColor.COLOR_GREEN, SoundType.WOOD, -1.0F).explosionResistance(3600000.0F).noOcclusion().lightLevel(e -> 3)));
	/** El semaforo pequeno de los postes del castillo (SemaforoCastillo): LUZ 0 apagado, 1 rojo, 2 verde. */
	public static final Block SEMAFORO = conObjeto(Blocks.register(clave("semaforo_castillo"), SemaforoCastillo::new,
		propiedades(MapColor.COLOR_BLACK, SoundType.METAL, -1.0F).explosionResistance(3600000.0F).noOcclusion()
			.lightLevel(e -> e.getValue(SemaforoCastillo.LUZ) > 0 ? 12 : 0)));
	/** El boton azul de las salas seguras: por 2 monedas, la salida suena (JuegoFiesta#localizador). */
	public static final Block LOCALIZADOR = conObjeto(Blocks.register(clave("localizador_salida"), LocalizadorSalida::new,
		propiedades(MapColor.COLOR_BLUE, SoundType.METAL, -1.0F).explosionResistance(3600000.0F).noCollision().noOcclusion().lightLevel(e -> 6)));
	/** El cartel de LEDs de la sala central (Marcador): tus tickets, o el bote de la vuelta. */
	public static final Block MARCADOR = conObjeto(Blocks.register(clave("marcador_fiesta"), Marcador::new,
		propiedades(MapColor.COLOR_BLACK, SoundType.METAL, -1.0F).explosionResistance(3600000.0F).noCollision().noOcclusion().lightLevel(e -> 5)));
	public static final net.minecraft.world.level.block.entity.BlockEntityType<MarcadorEntidad> MARCADOR_ENTIDAD = net.minecraft.core.Registry.register(
		net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, BackroomsEvento.id("marcador_fiesta"),
		net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(MarcadorEntidad::new, MARCADOR).build());
	/** El cuadrado de noclip de la sala central: no tiene suelo; quien cae, vuelve al laberinto. */
	public static final Block NOCLIP = registrar("noclip", propiedades(MapColor.COLOR_BLACK, SoundType.WOOL, -1.0F)
		.explosionResistance(3600000.0F).noCollision().noOcclusion().lightLevel(e -> 3));

	/* ------------------------------------------- el laberinto, el castillo y la sala central */

	public static final Block PARED_LAB = registrar("pared_laberinto", propiedades(MapColor.SNOW, SoundType.STONE, 1.5F));
	public static final Block ZOCALO_LAB = registrar("zocalo_laberinto", propiedades(MapColor.COLOR_RED, SoundType.WOOD, 1.5F));
	public static final Block TECHO_LAB = registrar("techo_laberinto", propiedades(MapColor.COLOR_GRAY, SoundType.STONE, 1.0F));
	public static final Block[] COLUMNAS = {
		registrar("columna_roja", propiedades(MapColor.COLOR_RED, SoundType.STONE, 1.5F)),
		registrar("columna_amarilla", propiedades(MapColor.COLOR_YELLOW, SoundType.STONE, 1.5F)),
		registrar("columna_verde", propiedades(MapColor.COLOR_GREEN, SoundType.STONE, 1.5F)),
		registrar("columna_azul", propiedades(MapColor.COLOR_BLUE, SoundType.STONE, 1.5F)),
		registrar("columna_morada", propiedades(MapColor.COLOR_PURPLE, SoundType.STONE, 1.5F))};
	/** El suelo a cuadros del laberinto: blanco, azul y amarillo (PlanoFiesta#damero dice cual va en cada sitio). */
	public static final Block[] DAMEROS = {
		registrar("damero_blanco", propiedades(MapColor.SNOW, SoundType.STONE, 1.0F)),
		registrar("damero_azul", propiedades(MapColor.COLOR_BLUE, SoundType.STONE, 1.0F)),
		registrar("damero_amarillo", propiedades(MapColor.COLOR_YELLOW, SoundType.STONE, 1.0F))};
	/** El suelo a cuadros grandes del salon final: rojo y morado (con el blanco de DAMEROS). */
	public static final Block DAMERO_ROJO = registrar("damero_rojo", propiedades(MapColor.COLOR_RED, SoundType.STONE, 1.0F));
	public static final Block DAMERO_MORADO = registrar("damero_morado", propiedades(MapColor.COLOR_PURPLE, SoundType.STONE, 1.0F));
	/** Banderines colgando del techo de lado a lado del pasillo (FACING: el eje por el que van). */
	public static final Block BANDERINES = suelo("banderines", MapColor.COLOR_RED, SoundType.WOOL, Block.box(0, 8, 7, 16, 16, 9), Shapes.empty());
	/** La flecha de neon colgada del techo: apunta (FACING) hacia la salida del laberinto. */
	public static final Block FLECHA = conObjeto(Blocks.register(clave("flecha_techo"), p -> new SueloVariante(p, Block.box(1, 13, 1, 15, 16, 15), Shapes.empty()),
		propiedades(MapColor.COLOR_PURPLE, SoundType.GLASS, 0.5F).noOcclusion().noCollision().lightLevel(e -> 10)));
	/** Los rotulos que brillan: 0 SALIDA (verde, encima de la puerta roja), 1 CERRAMOS (rojo, el tobogan del final). */
	public static final Block LETRERO = pared("letrero_fiesta", MapColor.COLOR_GREEN, SoundType.METAL, 11);
	public static final Block NUBE = pared("nube_fiesta", MapColor.SNOW, SoundType.WOOL, 0);
	/** El castillo de bolas: postes amarillos acolchados y redes negras (finas, se ve a traves). */
	public static final Block POSTE = suelo("poste_castillo", MapColor.COLOR_YELLOW, GOMA, Block.box(4, 0, 4, 12, 16, 12), Block.box(4, 0, 4, 12, 16, 12));
	public static final Block RED = suelo("red_castillo", MapColor.COLOR_BLACK, SoundType.WOOL, Block.box(0, 0, 7, 16, 16, 9), Block.box(0, 0, 7, 16, 16, 9));
	public static final Block TOBOGAN = registrar("tobogan", propiedades(MapColor.COLOR_RED, SoundType.METAL, 1.0F));
	/** La sala central: moqueta de carreteras, pared verde agua con franja, y la valla blanca del salon final. */
	public static final Block MOQUETA_CIUDAD = registrar("moqueta_ciudad", propiedades(MapColor.COLOR_GREEN, GOMA, 1.0F));
	public static final Block PARED_CENTRAL = registrar("pared_central", propiedades(MapColor.COLOR_CYAN, SoundType.STONE, 1.5F));
	public static final Block PARED_CENTRAL_BAJA = registrar("pared_central_baja", propiedades(MapColor.COLOR_GREEN, SoundType.STONE, 1.5F));
	/** Un ramo de tres globos atados con sus hilos a un peso (en el suelo, nunca un globo suelto en el suelo). */
	public static final Block RAMO_GLOBOS = suelo("ramo_globos", MapColor.COLOR_RED, SoundType.WOOL, Block.box(2, 0, 2, 14, 16, 14), Shapes.empty());
	/** La franja de aviso (amarilla y negra) alrededor de los noclip de la sala central. */
	public static final Block FRANJA = registrar("franja_aviso", propiedades(MapColor.COLOR_YELLOW, SoundType.STONE, 1.0F).lightLevel(e -> 4));
	public static final Block VALLA = suelo("valla_fiesta", MapColor.SNOW, SoundType.WOOD, Block.box(0, 0, 6, 16, 14, 10), Block.box(0, 0, 6, 16, 24, 10));

	/* ---------------------------------------- suelos defectuosos, parkour y juegos */

	/** Cede si te quedas encima (SueloDefectuoso); se ve como el suelo de alrededor. Indestructible. */
	public static final Block SUELO_DEFECTUOSO = conObjeto(Blocks.register(clave("suelo_defectuoso"), SueloDefectuoso::new,
		propiedades(MapColor.COLOR_YELLOW, GOMA, -1.0F).explosionResistance(3600000.0F).noOcclusion()));
	public static final Block PISCINA_BOLAS = conObjeto(Blocks.register(clave("piscina_bolas"), PiscinaBolas::new,
		propiedades(MapColor.COLOR_RED, GOMA, 0.5F).noOcclusion()));
	/** La trampilla del final de cada parkour: brilla y sube (JuegoFiesta). */
	public static final Block TRAMPILLA = registrar("trampilla_fiesta", propiedades(MapColor.GOLD, SoundType.METAL, -1.0F)
		.explosionResistance(3600000.0F).lightLevel(e -> 10));
	public static final Block NOTA = conObjeto(Blocks.register(clave("nota_fiesta"), NotaFiesta::new,
		propiedades(MapColor.SNOW, SoundType.WOOL, 0.2F).noCollision().noOcclusion()));
	public static final Block BALDOSA_JUEGO = conObjeto(Blocks.register(clave("baldosa_juego"), BaldosaJuego::new,
		propiedades(MapColor.COLOR_RED, SoundType.GLASS, -1.0F).explosionResistance(3600000.0F)
			.lightLevel(e -> e.getValue(BaldosaJuego.LUZ) ? 13 : (e.getValue(BaldosaJuego.COLOR) == 4 ? 5 : 1))));

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

	/** Los que se ven a traves (redes, banderines, flechas, puertas...): para el cliente, capa CUTOUT. */
	public static Block[] recortados() {
		return new Block[] {GUIRNALDA, CARTEL, DIBUJO, ALTAVOZ, CONFETI, GLOBO, NOTA, PUERTA, SEMAFORO, LOCALIZADOR, MARCADOR, NOCLIP,
			BANDERINES, FLECHA, LETRERO, NUBE, POSTE, RED, VALLA, RAMO_GLOBOS, REGALO};
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
