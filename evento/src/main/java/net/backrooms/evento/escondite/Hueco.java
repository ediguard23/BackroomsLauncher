package net.backrooms.evento.escondite;

import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import net.backrooms.evento.bloques.Bloques;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Un bloque de pared hueca (ver Plano#celdaHueca), como en Escape the Backrooms:
 * algunos tramos de pared son dobles y estan huecos por dentro de punta a punta.
 * Cada bloque es aire por dentro con, en cada cara, nada (sigue el hueco), un
 * tabique de pladur entero (2 px) o el tabique roto del boquete por donde se entra.
 *
 * El boquete (dos columnas de ancho, del suelo a 1,6 de alto) se cruza AGACHADO;
 * dentro hay toda la altura y se puede estar de pie. Ni la Bacteria (3,2 de alto) ni
 * el Smiler caben por el. La luz no pasa por los tabiques (por dentro esta oscuro) y
 * quien esta dentro esta escondido (Escondites). El dibujo sale de
 * tools/texturas/huecos.js.
 */
public class Hueco extends Block {
	public static final MapCodec<Hueco> CODEC = simpleCodec(Hueco::new);

	public enum Cara implements StringRepresentable {
		NADA("nada"), ENTERO("entero"), ROTO("roto");

		private final String nombre;

		Cara(String nombre) {
			this.nombre = nombre;
		}

		@Override
		public String getSerializedName() {
			return this.nombre;
		}
	}

	/** El cuarto del boquete que cae en este bloque (izquierda vista desde fuera). */
	public enum Parte implements StringRepresentable {
		ABAJO_IZQ("abajo_izq", false, true),
		ABAJO_DER("abajo_der", false, false),
		ARRIBA_IZQ("arriba_izq", true, true),
		ARRIBA_DER("arriba_der", true, false);

		private final String nombre;
		public final boolean arriba;
		public final boolean izq;

		Parte(String nombre, boolean arriba, boolean izq) {
			this.nombre = nombre;
			this.arriba = arriba;
			this.izq = izq;
		}

		public static Parte de(boolean arriba, boolean izq) {
			return arriba ? (izq ? ARRIBA_IZQ : ARRIBA_DER) : (izq ? ABAJO_IZQ : ABAJO_DER);
		}

		@Override
		public String getSerializedName() {
			return this.nombre;
		}
	}

	/** Suelo (con el zocalo y escombros), medio, o techo (travesano y aislante). */
	public enum Altura implements StringRepresentable {
		SUELO("suelo"), MEDIO("medio"), TECHO("techo");

		private final String nombre;

		Altura(String nombre) {
			this.nombre = nombre;
		}

		@Override
		public String getSerializedName() {
			return this.nombre;
		}
	}

	public static final EnumProperty<Cara> NORTE = EnumProperty.create("norte", Cara.class);
	public static final EnumProperty<Cara> ESTE = EnumProperty.create("este", Cara.class);
	public static final EnumProperty<Cara> SUR = EnumProperty.create("sur", Cara.class);
	public static final EnumProperty<Cara> OESTE = EnumProperty.create("oeste", Cara.class);
	public static final EnumProperty<Parte> PARTE = EnumProperty.create("parte", Parte.class);
	public static final EnumProperty<Altura> ALTURA = EnumProperty.create("altura", Altura.class);

	/** Las caras en el orden N, E, S, O (el de Plano). */
	public static final EnumProperty<Cara>[] CARAS = caras();
	private static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private final Map<BlockState, VoxelShape> formas = new IdentityHashMap<>();

	public Hueco(Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(NORTE, Cara.NADA).setValue(ESTE, Cara.NADA).setValue(SUR, Cara.NADA).setValue(OESTE, Cara.NADA)
			.setValue(PARTE, Parte.ABAJO_IZQ).setValue(ALTURA, Altura.MEDIO));
		// todas las formas de una vez: getShape se llama desde varios hilos
		for (BlockState e : this.stateDefinition.getPossibleStates()) {
			this.formas.put(e, forma(e));
		}
	}

	@SuppressWarnings("unchecked")
	private static EnumProperty<Cara>[] caras() {
		return new EnumProperty[] {NORTE, ESTE, SUR, OESTE};
	}

	@Override
	protected MapCodec<? extends Hueco> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTE, ESTE, SUR, OESTE, PARTE, ALTURA);
	}

	/** Gira una forma un cuarto de vuelta como el estado del bloque (norte -> este). */
	private static VoxelShape girar(VoxelShape s, int veces) {
		for (int k = 0; k < veces; k++) {
			VoxelShape[] out = {Shapes.empty()};
			s.forAllBoxes((x1, y1, z1, x2, y2, z2) -> out[0] = Shapes.or(out[0], Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
			s = out[0];
		}
		return s;
	}

	/**
	 * Lo macizo de un estado: cada tabique entero es una lamina de 2 px en su cara; el
	 * roto deja solo el montante del borde y, en la parte de arriba, el dintel a 1,6
	 * del suelo (9,6 px de este bloque). Hecho en la cara norte y girado.
	 */
	private static VoxelShape forma(BlockState estado) {
		VoxelShape s = Shapes.empty();
		for (int k = 0; k < 4; k++) {
			Cara c = estado.getValue(CARAS[k]);
			VoxelShape norte;
			if (c == Cara.ENTERO) {
				norte = Block.box(0, 0, 0, 16, 16, 2);
			} else if (c == Cara.ROTO) {
				Parte p = estado.getValue(PARTE);
				norte = p.izq ? Block.box(0, 0, 0, 2, 16, 4) : Block.box(14, 0, 0, 16, 16, 4);
				if (p.arriba) {
					norte = Shapes.or(norte, Block.box(0, 9.6, 0, 16, 16, 2));
				}
			} else {
				continue;
			}
			s = Shapes.or(s, girar(norte, k));
		}
		return s;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return this.formas.getOrDefault(estado, Shapes.empty());
	}

	/** La luz no atraviesa los tabiques: por dentro de la pared esta oscuro. */
	@Override
	protected boolean useShapeForLightOcclusion(BlockState estado) {
		return true;
	}

	@Override
	protected boolean isPathfindable(BlockState estado, PathComputationType tipo) {
		// los mobs no lo cuentan como camino: nadie de ellos cabe
		return false;
	}

	/** Alguna de sus caras es el boquete. */
	public static boolean roto(BlockState estado) {
		for (EnumProperty<Cara> c : CARAS) {
			if (estado.getValue(c) == Cara.ROTO) {
				return true;
			}
		}
		return false;
	}

	/** De vez en cuando cae un poco de polvo de yeso del borde roto. */
	@Override
	public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource azar) {
		if (!estado.getValue(PARTE).arriba || azar.nextInt(12) != 0) {
			return;
		}
		for (int k = 0; k < 4; k++) {
			if (estado.getValue(CARAS[k]) == Cara.ROTO) {
				Direction d = DIRS[k];
				double a = 0.15 + azar.nextDouble() * 0.7;
				double x = pos.getX() + 0.5 + d.getStepX() * 0.45 + (d.getStepX() == 0 ? a - 0.5 : 0);
				double z = pos.getZ() + 0.5 + d.getStepZ() * 0.45 + (d.getStepZ() == 0 ? a - 0.5 : 0);
				nivel.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Bloques.PAPEL_PINTADO.defaultBlockState()),
					x, pos.getY() + 0.55, z, 0.0, 0.0, 0.0);
				return;
			}
		}
	}
}
