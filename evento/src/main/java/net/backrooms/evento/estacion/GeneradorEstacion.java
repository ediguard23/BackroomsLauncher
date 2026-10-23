package net.backrooms.evento.estacion;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Genera la Estacion Electrica (PlanoEstacion) en su dimension; fuera del edificio no hay nada. Como todo sale del plano, se puede borrar la
 * dimension y se vuelve a generar igual. Las cajas se reparten por chunk una vez (son miles).
 */
public class GeneradorEstacion extends ChunkGenerator {
	public static final MapCodec<GeneradorEstacion> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
			.apply(i, i.stable(GeneradorEstacion::new)));

	private static final java.util.Map<Long, List<PlanoEstacion.Caja>> POR_CHUNK = new java.util.HashMap<>();

	/** Las cajas que tocan ese chunk, en el orden del plano (las de despues pisan a las de antes). */
	private static synchronized List<PlanoEstacion.Caja> cajas(int cx, int cz) {
		if (POR_CHUNK.isEmpty()) {
			for (PlanoEstacion.Caja c : PlanoEstacion.CAJAS) {
				for (int x = c.x0() >> 4; x <= c.x1() >> 4; x++) {
					for (int z = c.z0() >> 4; z <= c.z1() >> 4; z++) {
						POR_CHUNK.computeIfAbsent(((long) x << 32) ^ (z & 0xFFFFFFFFL), k -> new java.util.ArrayList<>()).add(c);
					}
				}
			}
		}
		return POR_CHUNK.getOrDefault(((long) cx << 32) ^ (cz & 0xFFFFFFFFL), List.of());
	}

	public GeneradorEstacion(BiomeSource biomas) {
		super(biomas);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState estado, StructureManager estructuras, ChunkAccess chunk) {
		ChunkPos cp = chunk.getPos();
		int cx0 = cp.getMinBlockX();
		int cz0 = cp.getMinBlockZ();
		int cx1 = cx0 + 15;
		int cz1 = cz0 + 15;
		if (cx1 < PlanoEstacion.X0 || cx0 > PlanoEstacion.X1 || cz1 < PlanoEstacion.Z0 || cz0 > PlanoEstacion.Z1) {
			return CompletableFuture.completedFuture(chunk);
		}
		Heightmap fondo = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap superficie = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (PlanoEstacion.Caja c : cajas(cp.x, cp.z)) {
			int x0 = Math.max(c.x0(), cx0);
			int x1 = Math.min(c.x1(), cx1);
			int z0 = Math.max(c.z0(), cz0);
			int z1 = Math.min(c.z1(), cz1);
			if (x0 > x1 || z0 > z1) {
				continue;
			}
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					for (int y = c.y0(); y <= c.y1(); y++) {
						BlockState s = c.patron().en(x, y, z);
						pos.set(x, y, z);
						chunk.setBlockState(pos, s);
						fondo.update(x & 15, y, z & 15, s);
						superficie.update(x & 15, y, z & 15, s);
						if (s.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb) {
							// al generar, la entidad de bloque no se crea sola: se pone en el chunk (como las valvulas)
							net.minecraft.world.level.block.entity.BlockEntity be = eb.newBlockEntity(new BlockPos(x, y, z), s);
							if (be != null) {
								chunk.setBlockEntity(be);
							}
						}
					}
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getSpawnHeight(LevelHeightAccessor alturas) {
		return PlanoEstacion.SUELO + 1;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types tipo, LevelHeightAccessor alturas, RandomState estado) {
		return PlanoEstacion.BASES[PlanoEstacion.PISOS - 1] + 2 * PlanoEstacion.ZONA_SUBE + 14;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor alturas, RandomState estado) {
		BlockState[] col = new BlockState[0];
		return new NoiseColumn(alturas.getMinY(), col);
	}

	@Override
	public void addDebugScreenInfo(List<String> lineas, RandomState estado, BlockPos pos) {
		PlanoEstacion.Sala s = PlanoEstacion.sala(pos.getX(), pos.getY(), pos.getZ());
		lineas.add("Estacion: " + PlanoEstacion.PISO[PlanoEstacion.piso(pos.getZ())] + " · " + (s == null ? "fuera" : s.nombre()));
	}

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
		return 128;
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
