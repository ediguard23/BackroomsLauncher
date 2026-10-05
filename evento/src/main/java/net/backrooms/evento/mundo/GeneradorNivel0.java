package net.backrooms.evento.mundo;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.bloques.Dibujo;
import net.backrooms.evento.bloques.EnPared;
import net.backrooms.evento.bloques.EnSuelo;
import net.backrooms.evento.bloques.Silla;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Genera el Nivel 0 segun el Plano: moqueta en y=32, pasillo de 4 bloques de
 * alto (y=33..36) y techo en y=37 con sus tubos. La
 * dimension mide 48 de alto y solo la seccion de arriba tiene bloques, asi que
 * cada chunk cuesta casi nada de generar, guardar y enviar: es lo que permite
 * un mapa enorme con 200 jugadores.
 *
 * El suelo no esta en y=0 a proposito: a menos de 32 bloques del fondo del
 * mundo el cliente oscurece la niebla (voidDarknessOnsetRange) y la distancia
 * se veria negra en vez de amarilla.
 *
 * Sin estructuras, cuevas ni decoracion de bioma. La semilla del trazado sale
 * de la del mundo: cada mundo nuevo tiene otro laberinto.
 */
public class GeneradorNivel0 extends ChunkGenerator {
	public static final MapCodec<GeneradorNivel0> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(
				BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
				com.mojang.serialization.Codec.INT.optionalFieldOf("variante", 1).forGetter(g -> g.variante))
			.apply(i, i.stable(GeneradorNivel0::new))
	);

	public static final int SUELO = 32;
	public static final int TECHO_Y = 37;
	public static final int ALTO = 48;

	private volatile RandomState estadoPlano;
	private volatile Plano plano;

	/** Cada fase es otra dimension con este generador y otra variante: otro laberinto con la misma semilla. */
	private final int variante;

	public GeneradorNivel0(BiomeSource biomas, int variante) {
		super(biomas);
		this.variante = variante;
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	/** El plano de este mundo: su semilla se deriva de la del mundo. */
	public Plano plano(RandomState estado) {
		Plano p = this.plano;
		if (p == null || this.estadoPlano != estado) {
			long semilla = estado.getOrCreateRandomFactory(BackroomsEvento.id("plano")).at(0, 0, 0).nextLong();
			if (this.variante != 1) {
				semilla ^= this.variante * 0x9E3779B97F4A7C15L;
			}
			p = new Plano(semilla);
			this.plano = p;
			this.estadoPlano = estado;
		}
		return p;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState estado, StructureManager estructuras, ChunkAccess chunk) {
		Plano p = this.plano(estado);
		ChunkPos cp = chunk.getPos();
		int bx = cp.getMinBlockX();
		int bz = cp.getMinBlockZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Heightmap fondo = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap superficie = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = bx + x;
				int wz = bz + z;
				int asc = p.ascensorEn(wx, wz);
				if (asc != Plano.ASC_NO) {
					this.ascensor(chunk, pos, p, x, z, wx, wz, asc, fondo, superficie);
					continue;
				}
				poner(chunk, pos.set(x, SUELO, z), p.mojado(wx, wz) ? Bloques.MOQUETA_MOJADA.defaultBlockState() : Bloques.MOQUETA.defaultBlockState(), fondo, superficie);
				int letrero = p.letreroAscensor(wx, wz);
				if (letrero >= 0) {
					poner(chunk, pos.set(x, TECHO_Y - 1, z), Bloques.SALIDA.defaultBlockState().setValue(EnPared.FACING, MIRA[letrero]), fondo, superficie);
				}
				if (p.pared(wx, wz)) {
					this.pared(chunk, pos, p, x, z, wx, wz, fondo, superficie);
				} else {
					this.decorar(chunk, pos, p, x, z, wx, wz, fondo, superficie);
				}
				int ventilador = p.ventilador(wx, wz);
				if (ventilador > 0) {
					poner(chunk, pos.set(x, TECHO_Y - 1, z), (ventilador == 2 ? Bloques.VENTILADOR_GRANDE : Bloques.VENTILADOR).defaultBlockState(), fondo, superficie);
				}
				BlockState techo = switch (p.techo(wx, wz)) {
					case Plano.TUBO -> Bloques.FLUORESCENTE.defaultBlockState();
					case Plano.TUBO_APAGADO -> Bloques.FLUORESCENTE_APAGADO.defaultBlockState();
					case Plano.TUBO_PARPADEO -> Bloques.FLUORESCENTE_PARPADEO.defaultBlockState();
					default -> Bloques.TECHO.defaultBlockState();
				};
				poner(chunk, pos.set(x, TECHO_Y, z), techo, fondo, superficie);
				// una capa mas encima: nadie ve el vacio si rompe una placa en creativo
				poner(chunk, pos.set(x, TECHO_Y + 1, z), Bloques.TECHO.defaultBlockState(), fondo, superficie);
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	private static final Direction[] MIRA = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	/**
	 * Columna del ascensor de salida: paredes de acero, suelo de chapa, luz en
	 * el techo, el hueco de la puerta con su dintel y el panel de botones en
	 * la pared de enfrente.
	 */
	private void ascensor(ChunkAccess chunk, BlockPos.MutableBlockPos pos, Plano p, int x, int z, int wx, int wz, int asc, Heightmap a, Heightmap b) {
		BlockState acero = Bloques.ACERO.defaultBlockState();
		poner(chunk, pos.set(x, SUELO, z), asc == Plano.ASC_PARED ? acero : Bloques.ASCENSOR_SUELO.defaultBlockState(), a, b);
		if (asc == Plano.ASC_PARED) {
			for (int y = SUELO + 1; y < TECHO_Y; y++) {
				poner(chunk, pos.set(x, y, z), acero, a, b);
			}
		} else if (asc == Plano.ASC_PUERTA) {
			poner(chunk, pos.set(x, TECHO_Y - 1, z), acero, a, b);
		} else {
			int panel = p.panelAscensor(wx, wz);
			if (panel >= 0) {
				poner(chunk, pos.set(x, SUELO + 2, z), Bloques.PANEL_ASCENSOR.defaultBlockState().setValue(EnPared.FACING, MIRA[panel]), a, b);
			}
		}
		Plano.Ascensor as = p.ascensor(Math.floorDiv(wx, Plano.G), Math.floorDiv(wz, Plano.G));
		int lx = wx - as.gx() * Plano.G;
		int lz = wz - as.gz() * Plano.G;
		boolean luz = asc == Plano.ASC_DENTRO && lx >= 2 && lx <= 4 && lz >= 2 && lz <= 4;
		poner(chunk, pos.set(x, TECHO_Y, z), luz ? Bloques.LUZ_ASCENSOR.defaultBlockState() : acero, a, b);
		poner(chunk, pos.set(x, TECHO_Y + 1, z), Bloques.TECHO.defaultBlockState(), a, b);
	}

	/** Columna de pared: maciza (zocalo + papel), tabique fino o cubierta de Hay Bacillus. */
	private void pared(ChunkAccess chunk, BlockPos.MutableBlockPos pos, Plano p, int x, int z, int wx, int wz, Heightmap a, Heightmap b) {
		if (p.paredFina(wx, wz) != 0) {
			boolean sucia = p.sucio(wx, 2, wz);
			BlockState fina = (sucia ? Bloques.PARED_FINA_SUCIA : Bloques.PARED_FINA).defaultBlockState()
				.setValue(CrossCollisionBlock.NORTH, p.pared(wx, wz - 1))
				.setValue(CrossCollisionBlock.SOUTH, p.pared(wx, wz + 1))
				.setValue(CrossCollisionBlock.EAST, p.pared(wx + 1, wz))
				.setValue(CrossCollisionBlock.WEST, p.pared(wx - 1, wz));
			for (int y = SUELO + 1; y < TECHO_Y; y++) {
				poner(chunk, pos.set(x, y, z), fina, a, b);
			}
			return;
		}
		boolean bacilo = p.paredBacilo(wx, wz);
		poner(chunk, pos.set(x, SUELO + 1, z), (bacilo ? Bloques.RAIZ_BACILO : Bloques.ZOCALO).defaultBlockState(), a, b);
		for (int y = SUELO + 2; y < TECHO_Y; y++) {
			Block bloque = bacilo ? Bloques.BACILO : p.sucio(wx, y - SUELO, wz) ? Bloques.PAPEL_PINTADO_SUCIO : Bloques.PAPEL_PINTADO;
			poner(chunk, pos.set(x, y, z), bloque.defaultBlockState(), a, b);
		}
	}

	/** Columna libre: lo que haya pegado a la pared de al lado o tirado en el suelo. */
	private void decorar(ChunkAccess chunk, BlockPos.MutableBlockPos pos, Plano p, int x, int z, int wx, int wz, Heightmap a, Heightmap b) {
		Plano.Deco d = p.decoracion(wx, wz);
		Direction mira = MIRA[d.mira()];
		switch (d.tipo()) {
			case Plano.D_ENCHUFE, Plano.D_ENCHUFE_MANCHADO -> poner(chunk, pos.set(x, SUELO + 1, z),
				(d.tipo() == Plano.D_ENCHUFE ? Bloques.ENCHUFE : Bloques.ENCHUFE_MANCHADO).defaultBlockState().setValue(EnPared.FACING, mira), a, b);
			case Plano.D_DIBUJO -> poner(chunk, pos.set(x, SUELO + 2, z),
				Bloques.DIBUJO.defaultBlockState().setValue(EnPared.FACING, mira).setValue(Dibujo.DIBUJO, d.variante()), a, b);
			case Plano.D_SALIDA -> poner(chunk, pos.set(x, TECHO_Y - 1, z),
				Bloques.SALIDA.defaultBlockState().setValue(EnPared.FACING, mira), a, b);
			case Plano.D_NOTA -> poner(chunk, pos.set(x, SUELO + 1, z),
				Bloques.NOTA.defaultBlockState().setValue(EnSuelo.FACING, mira), a, b);
			case Plano.D_SILLA, Plano.D_SILLA_VOLCADA -> poner(chunk, pos.set(x, SUELO + 1, z),
				Bloques.SILLA.defaultBlockState().setValue(EnSuelo.FACING, mira).setValue(Silla.VOLCADA, d.tipo() == Plano.D_SILLA_VOLCADA), a, b);
			case Plano.D_SENAL -> poner(chunk, pos.set(x, SUELO + 1, z),
				Bloques.SENALES[d.variante()].defaultBlockState().setValue(EnSuelo.FACING, mira), a, b);
			case Plano.D_VENA -> poner(chunk, pos.set(x, SUELO + 1, z), Bloques.CAPA_BACILO.defaultBlockState(), a, b);
			default -> {
			}
		}
	}

	private static void poner(ChunkAccess chunk, BlockPos pos, BlockState estado, Heightmap a, Heightmap b) {
		chunk.setBlockState(pos, estado);
		a.update(pos.getX(), pos.getY(), pos.getZ(), estado);
		b.update(pos.getX(), pos.getY(), pos.getZ(), estado);
	}

	/**
	 * Columna libre (sin pared) mas cercana a (x, z), con los pies sobre la
	 * moqueta. Sale del plano, asi que no hace falta que el chunk este cargado.
	 */
	public BlockPos puntoLibre(RandomState estado, int x, int z) {
		Plano p = this.plano(estado);
		for (int r = 0; r <= Plano.G; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) == r && !p.pared(x + dx, z + dz)) {
						return new BlockPos(x + dx, SUELO + 1, z + dz);
					}
				}
			}
		}
		return new BlockPos(x, SUELO + 1, z);
	}

	/** La gente aparece de pie sobre la moqueta, no encima del techo. */
	@Override
	public int getSpawnHeight(LevelHeightAccessor alturas) {
		return SUELO + 1;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types tipo, LevelHeightAccessor alturas, RandomState estado) {
		return TECHO_Y + 2;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor alturas, RandomState estado) {
		Plano p = this.plano(estado);
		BlockState[] col = new BlockState[TECHO_Y + 2];
		BlockState aire = Blocks.AIR.defaultBlockState();
		java.util.Arrays.fill(col, aire);
		boolean pared = p.pared(x, z);
		col[SUELO] = Bloques.MOQUETA.defaultBlockState();
		for (int y = SUELO + 1; y < TECHO_Y; y++) {
			col[y] = pared ? Bloques.PAPEL_PINTADO.defaultBlockState() : aire;
		}
		col[TECHO_Y] = Bloques.TECHO.defaultBlockState();
		col[TECHO_Y + 1] = Bloques.TECHO.defaultBlockState();
		return new NoiseColumn(alturas.getMinY(), col);
	}

	@Override
	public void addDebugScreenInfo(List<String> lineas, RandomState estado, BlockPos pos) {
		Plano p = this.plano(estado);
		String[] zonas = {"salon", "salas", "laberinto", "pasillos"};
		int zona = p.zona(Math.floorDiv(pos.getX(), Plano.G), Math.floorDiv(pos.getZ(), Plano.G));
		lineas.add("Nivel 0: zona " + zonas[zona] + ", oscuridad " + p.oscuridad(pos.getX(), pos.getZ()));
	}

	/* -------------------------------------------- nada de lo de Minecraft */

	@Override
	public void createStructures(RegistryAccess registros, ChunkGeneratorStructureState estructuras, StructureManager gestor, ChunkAccess chunk,
		StructureTemplateManager plantillas, ResourceKey<Level> nivel) {
	}

	@Override
	public void createReferences(WorldGenLevel nivel, StructureManager gestor, ChunkAccess chunk) {
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel nivel, ChunkAccess chunk, StructureManager gestor) {
	}

	@Override
	public void applyCarvers(WorldGenRegion region, long semilla, RandomState estado, BiomeManager biomas, StructureManager gestor, ChunkAccess chunk) {
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager gestor, RandomState estado, ChunkAccess chunk) {
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return ALTO;
	}

	@Override
	public int getSeaLevel() {
		return SUELO + 1;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
