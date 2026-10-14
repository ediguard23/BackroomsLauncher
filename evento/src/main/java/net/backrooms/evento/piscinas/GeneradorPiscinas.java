package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.bloques.EnPared;
import net.backrooms.evento.bloques.EnSuelo;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Genera las Piscinas segun PlanoPiscinas, columna a columna. La dimension mide 64
 * de alto y solo se escribe de y = 32 a y = 59. Como en el Nivel 0: sin estructuras,
 * cuevas ni decoracion, y la semilla del trazado sale de la del mundo.
 */
public class GeneradorPiscinas extends ChunkGenerator {
	public static final MapCodec<GeneradorPiscinas> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
			.apply(i, i.stable(GeneradorPiscinas::new)));

	public static final int ALTO = 64;

	private static final Direction[] LADOS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	/** Lo que va en cada material del plano (el azulejo de pared se ensucia al azar en poner). */
	private static final BlockState[] ESTADOS = new BlockState[PlanoPiscinas.ESCALON_ARRIBA + 4];

	static {
		BlockState losa = BloquesPiscinas.LOSA_AZULEJO.defaultBlockState();
		ESTADOS[PlanoPiscinas.AZULEJO] = BloquesPiscinas.AZULEJO.defaultBlockState();
		ESTADOS[PlanoPiscinas.PARED] = BloquesPiscinas.AZULEJO.defaultBlockState();
		ESTADOS[PlanoPiscinas.AZUL] = BloquesPiscinas.AZULEJO_AZUL.defaultBlockState();
		ESTADOS[PlanoPiscinas.FONDO] = BloquesPiscinas.FONDO_PISCINA.defaultBlockState();
		ESTADOS[PlanoPiscinas.FRANJA] = BloquesPiscinas.FRANJA_PISCINA.defaultBlockState();
		ESTADOS[PlanoPiscinas.BORDE] = BloquesPiscinas.BORDE_PISCINA.defaultBlockState();
		ESTADOS[PlanoPiscinas.LUZ] = BloquesPiscinas.LUZ_TECHO.defaultBlockState();
		ESTADOS[PlanoPiscinas.CLARABOYA] = BloquesPiscinas.CLARABOYA.defaultBlockState();
		ESTADOS[PlanoPiscinas.FOCO] = BloquesPiscinas.FOCO_PISCINA.defaultBlockState();
		ESTADOS[PlanoPiscinas.REJILLA] = BloquesPiscinas.REJILLA.defaultBlockState();
		ESTADOS[PlanoPiscinas.AGUA] = Blocks.WATER.defaultBlockState();
		ESTADOS[PlanoPiscinas.LOSA] = losa;
		ESTADOS[PlanoPiscinas.LOSA_ARRIBA] = losa.setValue(SlabBlock.TYPE, SlabType.TOP);
		ESTADOS[PlanoPiscinas.LOSA_AGUA] = losa.setValue(SlabBlock.WATERLOGGED, true);
		ESTADOS[PlanoPiscinas.LUZ_OCULTA] = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
		ESTADOS[PlanoPiscinas.LUZ_TENUE] = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 10);
		for (int k = 0; k < 4; k++) {
			BlockState e = BloquesPiscinas.ESCALON_AZULEJO.defaultBlockState().setValue(StairBlock.FACING, LADOS[k]);
			ESTADOS[PlanoPiscinas.ESCALON + k] = e;
			ESTADOS[PlanoPiscinas.ESCALON_ARRIBA + k] = e.setValue(StairBlock.HALF, Half.TOP);
		}
	}

	private volatile RandomState estadoPlano;
	private volatile PlanoPiscinas plano;

	public GeneradorPiscinas(BiomeSource biomas) {
		super(biomas);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	public PlanoPiscinas plano(RandomState estado) {
		PlanoPiscinas p = this.plano;
		if (p == null || this.estadoPlano != estado) {
			p = new PlanoPiscinas(estado.getOrCreateRandomFactory(BackroomsEvento.id("piscinas")).at(0, 0, 0).nextLong());
			this.plano = p;
			this.estadoPlano = estado;
		}
		return p;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState estado, StructureManager estructuras, ChunkAccess chunk) {
		PlanoPiscinas p = this.plano(estado);
		ChunkPos cp = chunk.getPos();
		int bx = cp.getMinBlockX();
		int bz = cp.getMinBlockZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Heightmap fondo = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap superficie = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		byte[] col = new byte[PlanoPiscinas.ALTO];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = bx + x;
				int wz = bz + z;
				p.columna(wx, wz, col);
				for (int k = 0; k < col.length; k++) {
					byte m = col[k];
					if (m == PlanoPiscinas.AIRE) {
						continue;
					}
					int y = PlanoPiscinas.Y0 + k;
					BlockState e = m == PlanoPiscinas.PARED ? pared(wx, y, wz)
						: m == PlanoPiscinas.LUZ && azar(wx, y, wz) % 75 == 0 ? BloquesPiscinas.LUZ_PARPADEO.defaultBlockState()
						: ESTADOS[m];
					pos.set(x, y, z);
					chunk.setBlockState(pos, e);
					fondo.update(x, y, z, e);
					superficie.update(x, y, z, e);
				}
				for (PlanoPiscinas.Detalle d : p.detallesEn(wx, wz)) {
					BlockState e = detalle(d);
					pos.set(x, d.y(), z);
					chunk.setBlockState(pos, e);
					fondo.update(x, d.y(), z, e);
					superficie.update(x, d.y(), z, e);
					if (d.tipo() == PlanoPiscinas.D_VALVULA) {
						// el volante (lo que gira) es un block entity: al generar no se crea solo
						chunk.setBlockEntity(new ValvulaEntidad(new BlockPos(wx, d.y(), wz), e));
					}
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	/** El bloque de un detalle del plano (marca, mancha, objeto o azulejo cambiado). */
	private static BlockState detalle(PlanoPiscinas.Detalle d) {
		Direction mira = LADOS[d.lado()];
		return switch (d.tipo()) {
			case PlanoPiscinas.D_MARCA -> BloquesPiscinas.MARCA.defaultBlockState().setValue(EnPared.FACING, mira).setValue(MarcaPiscina.MARCA, d.variante());
			case PlanoPiscinas.D_MANCHA -> BloquesPiscinas.MANCHA.defaultBlockState().setValue(EnSuelo.FACING, mira).setValue(ManchaSuelo.MANCHA, d.variante());
			case PlanoPiscinas.D_OBJETO -> BloquesPiscinas.OBJETOS[d.variante()].defaultBlockState().setValue(EnSuelo.FACING, mira);
			case PlanoPiscinas.D_VALVULA -> BloquesPiscinas.VALVULA.defaultBlockState().setValue(EnPared.FACING, mira);
			case PlanoPiscinas.D_PUERTA -> BloquesPiscinas.PUERTA_SALIDA.defaultBlockState().setValue(PuertaSalida.FACING, mira).setValue(PuertaSalida.PARTE, d.variante());
			case PlanoPiscinas.D_LECTOR -> BloquesPiscinas.LECTOR.defaultBlockState().setValue(EnPared.FACING, mira).setValue(LectorTarjeta.COLOR, d.variante());
			case PlanoPiscinas.D_CARTEL -> BloquesPiscinas.CARTEL_SALIDA.defaultBlockState().setValue(EnPared.FACING, mira).setValue(CartelSalida.VARIANTE, d.variante());
			default -> (d.variante() == 0 ? BloquesPiscinas.AZULEJO_MANCHADO : BloquesPiscinas.AZULEJO_SANGRE).defaultBlockState();
		};
	}

	private static int azar(int x, int y, int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h = (h ^ (h >>> 13)) * 0x5bd1e995;
		return (h ^ (h >>> 15)) & 0x7FFFFFFF;
	}

	/** Azulejo de pared o suelo: alguno sucio y alguno con piezas caidas. */
	private static BlockState pared(int x, int y, int z) {
		int r = azar(x, y, z) & 1023;
		if (r < 30) {
			return BloquesPiscinas.AZULEJO_SUCIO.defaultBlockState();
		}
		if (r < 36) {
			return BloquesPiscinas.AZULEJO_ROTO.defaultBlockState();
		}
		return BloquesPiscinas.AZULEJO.defaultBlockState();
	}

	/**
	 * Un sitio donde estar de pie (seco, con dos de aire encima) cerca de (x, z), en la
	 * planta de abajo y en una sala con luz. Sale del plano: no hace falta el chunk.
	 */
	public BlockPos puntoLibre(RandomState estado, int x, int z) {
		PlanoPiscinas p = this.plano(estado);
		byte[] col = new byte[PlanoPiscinas.ALTO];
		for (int r = 0; r <= PlanoPiscinas.C * 3; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int cx = x + dx;
					int cz = z + dz;
					PlanoPiscinas.Sala s = p.sala(Math.floorDiv(cx, PlanoPiscinas.C), Math.floorDiv(cz, PlanoPiscinas.C));
					if (s.oscura()) {
						continue;
					}
					p.columna(cx, cz, col);
					if (p.pisable(cx, PlanoPiscinas.PISO, cz, col)) {
						return new BlockPos(cx, PlanoPiscinas.PISO, cz);
					}
				}
			}
		}
		return new BlockPos(x, PlanoPiscinas.PISO, z);
	}

	@Override
	public int getSpawnHeight(LevelHeightAccessor alturas) {
		return PlanoPiscinas.PISO;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types tipo, LevelHeightAccessor alturas, RandomState estado) {
		return PlanoPiscinas.TOPE + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor alturas, RandomState estado) {
		PlanoPiscinas p = this.plano(estado);
		byte[] col = new byte[PlanoPiscinas.ALTO];
		p.columna(x, z, col);
		BlockState[] estados = new BlockState[PlanoPiscinas.TOPE + 1];
		BlockState aire = Blocks.AIR.defaultBlockState();
		for (int y = 0; y <= PlanoPiscinas.TOPE; y++) {
			byte m = PlanoPiscinas.en(col, y);
			estados[y] = m == PlanoPiscinas.AIRE ? aire : m == PlanoPiscinas.PARED ? BloquesPiscinas.AZULEJO.defaultBlockState() : ESTADOS[m];
		}
		return new NoiseColumn(alturas.getMinY(), estados);
	}

	@Override
	public void addDebugScreenInfo(List<String> lineas, RandomState estado, BlockPos pos) {
		PlanoPiscinas.Sala s = this.plano(estado).sala(Math.floorDiv(pos.getX(), PlanoPiscinas.C), Math.floorDiv(pos.getZ(), PlanoPiscinas.C));
		lineas.add("Piscinas: " + PlanoPiscinas.TIPOS[s.tipo()] + (s.oscura() ? " (a oscuras)" : ""));
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
		return PlanoPiscinas.SUELO;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
