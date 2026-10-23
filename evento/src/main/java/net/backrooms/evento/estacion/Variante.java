package net.backrooms.evento.estacion;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.backrooms.evento.bloques.EnSuelo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bloques de la Estacion con varios dibujos (VARIANTE 0..7): pegados a la pared o en el suelo. */
public final class Variante {
	public static final IntegerProperty VARIANTE = IntegerProperty.create("variante", 0, 7);

	private Variante() {
	}

	/** Algo pegado a la pared (tuberias, cuadros electricos, senales...), con su forma y su colision. */
	public static class Pared extends EnPared {
		public static final MapCodec<Pared> CODEC = simpleCodec(p -> new Pared(p, Block.box(0, 0, 15, 16, 16, 16)));
		private final java.util.Map<net.minecraft.core.Direction, VoxelShape> forma;

		public Pared(BlockBehaviour.Properties p, VoxelShape forma) {
			super(p);
			this.forma = net.minecraft.world.phys.shapes.Shapes.rotateHorizontal(forma);
			this.registerDefaultState(this.defaultBlockState().setValue(VARIANTE, 0));
		}

		@Override
		protected VoxelShape getShape(BlockState e, net.minecraft.world.level.BlockGetter n, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
			return this.forma.get(e.getValue(FACING));
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState e, net.minecraft.world.level.BlockGetter n, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
			return this.forma.get(e.getValue(FACING));
		}

		@Override
		protected MapCodec<? extends Pared> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			super.createBlockStateDefinition(b);
			b.add(VARIANTE);
		}
	}

	/** Un bloque entero que mira a un lado (el radiador del generador). */
	public static class Orientado extends net.minecraft.world.level.block.HorizontalDirectionalBlock {
		public static final MapCodec<Orientado> CODEC = simpleCodec(Orientado::new);

		public Orientado(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
		}

		@Override
		protected MapCodec<? extends Orientado> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			b.add(FACING);
		}
	}

	/** Algo en el suelo y orientado (armarios, transformadores, bidones, estanterias...). */
	public static class Suelo extends EnSuelo {
		public static final MapCodec<Suelo> CODEC = simpleCodec(p -> new Suelo(p, Block.box(0, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 16)));

		public Suelo(BlockBehaviour.Properties p, VoxelShape forma, VoxelShape choque) {
			super(p, forma, choque);
			this.registerDefaultState(this.defaultBlockState().setValue(VARIANTE, 0));
		}

		@Override
		protected MapCodec<? extends Suelo> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			super.createBlockStateDefinition(b);
			b.add(VARIANTE);
		}
	}
}
