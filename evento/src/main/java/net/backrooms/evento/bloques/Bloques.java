package net.backrooms.evento.bloques;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

	static {
		Items.registerBlock(FLUORESCENTE_PARPADEO);
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
