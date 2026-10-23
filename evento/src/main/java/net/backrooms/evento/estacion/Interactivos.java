package net.backrooms.evento.estacion;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.backrooms.evento.bloques.EnSuelo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Los bloques de la Estacion con los que se hace algo. Lo que pasa lo decide JuegoEstacion (en el
 * servidor); el estado del bloque solo lo ensena (y JuegoEstacion lo pone al dia).
 */
public final class Interactivos {
	public static final IntegerProperty TIPO = IntegerProperty.create("tipo", 0, 7);
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 3);
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 8);
	/** Una puerta que se esta moviendo: no se dibuja (la dibuja la animacion) pero sigue chocando. */
	public static final BooleanProperty OCULTA = BooleanProperty.create("oculta");
	public static final BooleanProperty VACIO = BooleanProperty.create("vacio");
	public static final BooleanProperty HECHO = BooleanProperty.create("hecho");
	public static final BooleanProperty ABIERTA = BooleanProperty.create("abierta");
	public static final BooleanProperty LIT = BooleanProperty.create("lit");

	private Interactivos() {
	}

	/**
	 * Donde se coge algo (TIPO: 0 cable, 1 caja de herramientas, 2 cinta aislante, 3 bidon de
	 * combustible, 4..7 el fusible de cada color). VACIO mientras se repone.
	 */
	public static class Punto extends EnSuelo {
		public static final MapCodec<Punto> CODEC = simpleCodec(Punto::new);

		public Punto(BlockBehaviour.Properties p) {
			super(p, Block.box(1, 0, 1, 15, 14, 15), Block.box(1, 0, 1, 15, 14, 15));
			this.registerDefaultState(this.defaultBlockState().setValue(TIPO, 0).setValue(VACIO, false));
		}

		@Override
		protected MapCodec<? extends Punto> codec() {
			return CODEC;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState e, Level nivel, BlockPos pos, Player j, BlockHitResult golpe) {
			if (j instanceof ServerPlayer sp) {
				JuegoEstacion.get().recoger(sp, pos, e);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack s, BlockState e, Level nivel, BlockPos pos, Player j, InteractionHand mano, BlockHitResult golpe) {
			return this.useWithoutItem(e, nivel, pos, j, golpe);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			super.createBlockStateDefinition(b);
			b.add(TIPO, VACIO);
		}
	}

	/**
	 * Un panel averiado en la pared (TIPO: lo que hace falta para arreglarlo: 0 cable, 1
	 * herramientas, 2 cinta). Echa chispas hasta que esta HECHO.
	 */
	public static class Panel extends EnPared {
		public static final MapCodec<Panel> CODEC = simpleCodec(Panel::new);
		private static final java.util.Map<Direction, VoxelShape> FORMA = Shapes.rotateHorizontal(Block.box(1, 0, 11, 15, 16, 16));

		public Panel(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.defaultBlockState().setValue(TIPO, 0).setValue(HECHO, false));
		}

		@Override
		protected MapCodec<? extends Panel> codec() {
			return CODEC;
		}

		@Override
		protected VoxelShape getShape(BlockState e, BlockGetter n, BlockPos pos, CollisionContext ctx) {
			return FORMA.get(e.getValue(FACING));
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState e, BlockGetter n, BlockPos pos, CollisionContext ctx) {
			return FORMA.get(e.getValue(FACING));
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState e, Level nivel, BlockPos pos, Player j, BlockHitResult golpe) {
			if (j instanceof ServerPlayer sp) {
				JuegoEstacion.get().central(sp, pos);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack s, BlockState e, Level nivel, BlockPos pos, Player j, InteractionHand mano, BlockHitResult golpe) {
			return this.useWithoutItem(e, nivel, pos, j, golpe);
		}

		@Override
		public void animateTick(BlockState e, Level nivel, BlockPos pos, RandomSource r) {
			if (e.getValue(HECHO) || r.nextInt(4) != 0) {
				return;
			}
			Direction d = e.getValue(FACING);
			double x = pos.getX() + 0.5 + d.getStepX() * 0.35 + (r.nextDouble() - 0.5) * 0.6;
			double y = pos.getY() + 0.3 + r.nextDouble() * 0.6;
			double z = pos.getZ() + 0.5 + d.getStepZ() * 0.35 + (r.nextDouble() - 0.5) * 0.6;
			nivel.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, d.getStepX() * 0.08, 0.02, d.getStepZ() * 0.08);
			if (r.nextInt(6) == 0) {
				nivel.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.03, 0.0);
			}
			if (r.nextInt(5) == 0) {
				nivel.playLocalSound(x, y, z, net.backrooms.evento.Sonidos.ESTACION_CHISPAS, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F + r.nextFloat() * 0.4F, 0.8F + r.nextFloat() * 0.5F, false);
			}
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			super.createBlockStateDefinition(b);
			b.add(TIPO, HECHO);
		}
	}

	/**
	 * La caja de fusible de una reja: un bloque entero metido en el muro, con la caja a los dos lados.
	 * HECHO: tiene su fusible (y la reja esta abierta). Clic derecho: lo pone (si llevas uno) o lo saca.
	 */
	public static class CajaReja extends Block {
		public static final MapCodec<CajaReja> CODEC = simpleCodec(CajaReja::new);

		public CajaReja(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.stateDefinition.any().setValue(HECHO, false));
		}

		@Override
		protected MapCodec<? extends CajaReja> codec() {
			return CODEC;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState e, Level nivel, BlockPos pos, Player j, BlockHitResult golpe) {
			if (j instanceof ServerPlayer sp) {
				JuegoEstacion.get().cajaReja(sp, pos);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack s, BlockState e, Level nivel, BlockPos pos, Player j, InteractionHand mano, BlockHitResult golpe) {
			return this.useWithoutItem(e, nivel, pos, j, golpe);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			b.add(HECHO);
		}
	}

	/**
	 * Una persiana metalica (o una reja) de 3x3 (PARTE = columna * 3 + fila): cerrada choca; ABIERTA esta
	 * recogida arriba y no se ve ni choca. OCULTA: se esta moviendo (la dibuja la animacion de
	 * JuegoEstacion) y sigue chocando como cerrada. Es fina (4 px): no tapa la caja de al lado.
	 */
	public static class Persiana extends HorizontalDirectionalBlock {
		public static final MapCodec<Persiana> CODEC = simpleCodec(Persiana::new);
		private static final java.util.Map<Direction, VoxelShape> FINA = Shapes.rotateHorizontal(Block.box(0, 0, 6, 16, 16, 10));

		public Persiana(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PARTE, 0).setValue(ABIERTA, false).setValue(OCULTA, false));
		}

		@Override
		protected MapCodec<? extends Persiana> codec() {
			return CODEC;
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState e, BlockGetter n, BlockPos pos, CollisionContext ctx) {
			return e.getValue(ABIERTA) ? Shapes.empty() : FINA.get(e.getValue(FACING));
		}

		@Override
		protected VoxelShape getShape(BlockState e, BlockGetter n, BlockPos pos, CollisionContext ctx) {
			return e.getValue(ABIERTA) ? Shapes.empty() : FINA.get(e.getValue(FACING));
		}

		@Override
		protected RenderShape getRenderShape(BlockState e) {
			return e.getValue(ABIERTA) || e.getValue(OCULTA) ? RenderShape.INVISIBLE : RenderShape.MODEL;
		}

		@Override
		protected VoxelShape getOcclusionShape(BlockState e) {
			return Shapes.empty();
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState e, Level nivel, BlockPos pos, Player j, BlockHitResult golpe) {
			if (j instanceof ServerPlayer sp && !e.getValue(ABIERTA)) {
				JuegoEstacion.get().persianaCerrada(sp, pos);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack s, BlockState e, Level nivel, BlockPos pos, Player j, InteractionHand mano, BlockHitResult golpe) {
			return this.useWithoutItem(e, nivel, pos, j, golpe);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			b.add(FACING, PARTE, ABIERTA, OCULTA);
		}
	}

	/**
	 * Un buzon de chapa en la pared, con una ranura para fusible. Con un fusible (que se queda dentro:
	 * no se recupera) se abre y da lo que tenga dentro (JuegoEstacion#buzon). ABIERTA: ya vacio.
	 */
	public static class Buzon extends EnPared {
		public static final MapCodec<Buzon> CODEC = simpleCodec(Buzon::new);
		private static final java.util.Map<Direction, VoxelShape> FORMA = Shapes.rotateHorizontal(Block.box(3, 2, 10, 13, 14, 16));

		public Buzon(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.defaultBlockState().setValue(ABIERTA, false));
		}

		@Override
		protected MapCodec<? extends Buzon> codec() {
			return CODEC;
		}

		@Override
		protected VoxelShape getShape(BlockState e, BlockGetter n, BlockPos pos, CollisionContext ctx) {
			return FORMA.get(e.getValue(FACING));
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState e, BlockGetter n, BlockPos pos, CollisionContext ctx) {
			return FORMA.get(e.getValue(FACING));
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState e, Level nivel, BlockPos pos, Player j, BlockHitResult golpe) {
			if (j instanceof ServerPlayer sp) {
				JuegoEstacion.get().buzon(sp, pos);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack s, BlockState e, Level nivel, BlockPos pos, Player j, InteractionHand mano, BlockHitResult golpe) {
			return this.useWithoutItem(e, nivel, pos, j, golpe);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			super.createBlockStateDefinition(b);
			b.add(ABIERTA);
		}
	}

	/** Un foco de la pasarela: apagado hasta que la pasarela tiene corriente. */
	public static class Foco extends Block {
		public static final MapCodec<Foco> CODEC = simpleCodec(Foco::new);

		public Foco(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
		}

		@Override
		protected MapCodec<? extends Foco> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			b.add(LIT);
		}
	}

	/** Un rotulo de sector en la pared: su letra (SECTOR 0..3: A, B, C, D) y su numero (0..15). */
	public static class Rotulo extends EnPared {
		public static final MapCodec<Rotulo> CODEC = simpleCodec(Rotulo::new);
		public static final IntegerProperty NUM = IntegerProperty.create("num", 0, 15);

		public Rotulo(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.defaultBlockState().setValue(COLOR, 0).setValue(NUM, 0));
		}

		@Override
		protected MapCodec<? extends Rotulo> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			super.createBlockStateDefinition(b);
			b.add(COLOR, NUM);
		}
	}

	/** Un bloque de pared con el color de un sector (la franja pintada). */
	public static class Franja extends Block {
		public static final MapCodec<Franja> CODEC = simpleCodec(Franja::new);

		public Franja(BlockBehaviour.Properties p) {
			super(p);
			this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, 0));
		}

		@Override
		protected MapCodec<? extends Franja> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
			b.add(COLOR);
		}
	}
}
