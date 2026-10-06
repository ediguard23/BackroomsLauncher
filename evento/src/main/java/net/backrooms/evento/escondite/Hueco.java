package net.backrooms.evento.escondite;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.Bloques;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Boquete en el zocalo de una pared del Nivel 0 (lo pone el generador, ver
 * Plano#hueco). Se pasa arrastrandose (0,6 de alto): el paso mide 14 px de ancho
 * y 13 de alto y encima sigue la pared. Ni la Bacteria (3,2 de alto) ni el Smiler
 * caben. Quien esta dentro esta escondido (Escondites).
 *
 * EJE es hacia donde se pasa. El dibujo (bordes rotos, yeso, papel que cuelga)
 * sale de tools/texturas/huecos.js; aqui, el polvo que cae del borde.
 */
public class Hueco extends Block {
	public static final MapCodec<Hueco> CODEC = simpleCodec(Hueco::new);
	public static final EnumProperty<Direction.Axis> EJE = BlockStateProperties.HORIZONTAL_AXIS;

	// el marco que queda: los dos lados (1 px) y el dintel (de 13 a 16 px)
	private static final VoxelShape PASO_Z = Shapes.or(Block.box(0, 0, 0, 1, 16, 16), Block.box(15, 0, 0, 16, 16, 16), Block.box(0, 13, 0, 16, 16, 16));
	private static final VoxelShape PASO_X = Shapes.or(Block.box(0, 0, 0, 16, 16, 1), Block.box(0, 0, 15, 16, 16, 16), Block.box(0, 13, 0, 16, 16, 16));

	public Hueco(Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(EJE, Direction.Axis.Z));
	}

	@Override
	protected MapCodec<? extends Hueco> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(EJE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(EJE, ctx.getHorizontalDirection().getAxis());
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return estado.getValue(EJE) == Direction.Axis.Z ? PASO_Z : PASO_X;
	}

	@Override
	protected boolean isPathfindable(BlockState estado, PathComputationType tipo) {
		// los mobs no lo cuentan como camino: nadie de ellos cabe
		return false;
	}

	/** Polvo de yeso que cae del borde roto, a ratos. */
	@Override
	public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource azar) {
		if (azar.nextInt(3) != 0) {
			return;
		}
		boolean z = estado.getValue(EJE) == Direction.Axis.Z;
		double a = 0.1 + azar.nextDouble() * 0.8;
		double cara = azar.nextBoolean() ? 0.06 : 0.94;
		double x = pos.getX() + (z ? a : cara);
		double zz = pos.getZ() + (z ? cara : a);
		nivel.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Bloques.PAPEL_PINTADO.defaultBlockState()),
			x, pos.getY() + 0.78, zz, 0.0, 0.0, 0.0);
	}
}
