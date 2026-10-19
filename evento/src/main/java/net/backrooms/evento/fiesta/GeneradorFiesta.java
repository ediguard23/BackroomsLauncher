package net.backrooms.evento.fiesta;

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
 * Genera la Fiesta (PlanoFiesta) en su dimension; fuera del edificio no hay nada. Como todo sale del plano, se puede borrar la dimension y se
 * vuelve a generar igual.
 */
public class GeneradorFiesta extends ChunkGenerator {
	public static final MapCodec<GeneradorFiesta> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
			.apply(i, i.stable(GeneradorFiesta::new)));

	public GeneradorFiesta(BiomeSource biomas) {
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
		if (cx1 < PlanoFiesta.X0 || cx0 > PlanoFiesta.X1 || cz1 < PlanoFiesta.Z0 || cz0 > PlanoFiesta.Z1) {
			return CompletableFuture.completedFuture(chunk);
		}
		Heightmap fondo = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap superficie = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (PlanoFiesta.Caja c : PlanoFiesta.CAJAS) {
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
		return PlanoFiesta.SUELO + 1;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types tipo, LevelHeightAccessor alturas, RandomState estado) {
		return PlanoFiesta.Y1 + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor alturas, RandomState estado) {
		BlockState[] col = new BlockState[0];
		return new NoiseColumn(alturas.getMinY(), col);
	}

	@Override
	public void addDebugScreenInfo(List<String> lineas, RandomState estado, BlockPos pos) {
		PlanoFiesta.Sala s = PlanoFiesta.sala(pos.getX(), pos.getZ());
		lineas.add("Fiesta: " + (s == null ? "fuera" : s.nombre()));
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
