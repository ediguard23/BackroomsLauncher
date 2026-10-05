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
 * Objeto apoyado en el suelo y orientado: senales (con su poste, que si
 * choca) y lo que no choca (notas). La forma de seleccion y la de choque
 * se dan al registrar.
 */
public class EnSuelo extends HorizontalDirectionalBlock {
	public static final MapCodec<EnSuelo> CODEC = simpleCodec(EnSuelo::new);

	private final Map<Direction, VoxelShape> forma;
	private final Map<Direction, VoxelShape> choque;

	public EnSuelo(BlockBehaviour.Properties properties) {
		this(properties, Block.box(0, 0, 0, 16, 1, 16), Shapes.empty());
	}

	public EnSuelo(BlockBehaviour.Properties properties, VoxelShape forma, VoxelShape choque) {
		super(properties);
		this.forma = Shapes.rotateHorizontal(forma);
		this.choque = Shapes.rotateHorizontal(choque);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends EnSuelo> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return this.forma.get(estado.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return this.choque.get(estado.getValue(FACING));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
