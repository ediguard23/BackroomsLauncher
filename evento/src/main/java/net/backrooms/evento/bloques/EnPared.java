package net.backrooms.evento.bloques;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Algo pegado a una pared (enchufes, dibujos, cintas, el cartel de salida).
 * Ocupa el aire delante de la pared y no choca con nada. FACING es hacia
 * donde mira: la pared queda detras, en la direccion opuesta.
 */
public class EnPared extends HorizontalDirectionalBlock {
	public static final MapCodec<EnPared> CODEC = simpleCodec(EnPared::new);
	private static final Map<Direction, VoxelShape> FORMAS = Shapes.rotateHorizontal(Block.box(0, 0, 15, 16, 16, 16));

	public EnPared(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends EnPared> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return FORMAS.get(estado.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction cara = ctx.getClickedFace();
		Direction mira = cara.getAxis().isHorizontal() ? cara : ctx.getHorizontalDirection().getOpposite();
		return this.defaultBlockState().setValue(FACING, mira);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
