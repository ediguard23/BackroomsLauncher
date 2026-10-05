package net.backrooms.evento.mundo;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.bloques.Bloques;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Genera el Nivel 0 segun el Plano: moqueta en y=32, pasillo de 3 bloques de
 * alto (y=33..35, como una oficina) y techo en y=36 con sus tubos. La
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
		i -> i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
			.apply(i, i.stable(GeneradorNivel0::new))
	);

	public static final int SUELO = 32;
	public static final int TECHO_Y = 36;
	public static final int ALTO = 48;

	private volatile RandomState estadoPlano;
	private volatile Plano plano;

	public GeneradorNivel0(BiomeSource biomas) {
		super(biomas);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	/** El plano de este mundo: su semilla se deriva de la del mundo. */
	private Plano plano(RandomState estado) {
		Plano p = this.plano;
		if (p == null || this.estadoPlano != estado) {
			long semilla = estado.getOrCreateRandomFactory(BackroomsEvento.id("plano")).at(0, 0, 0).nextLong();
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
				poner(chunk, pos.set(x, SUELO, z), p.mojado(wx, wz) ? Bloques.MOQUETA_MOJADA.defaultBlockState() : Bloques.MOQUETA.defaultBlockState(), fondo, superficie);
				if (p.pared(wx, wz)) {
					poner(chunk, pos.set(x, SUELO + 1, z), Bloques.ZOCALO.defaultBlockState(), fondo, superficie);
					for (int y = SUELO + 2; y < TECHO_Y; y++) {
						BlockState papel = p.sucio(wx, y - SUELO, wz) ? Bloques.PAPEL_PINTADO_SUCIO.defaultBlockState() : Bloques.PAPEL_PINTADO.defaultBlockState();
						poner(chunk, pos.set(x, y, z), papel, fondo, superficie);
					}
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
