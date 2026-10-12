package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Una valvula de las Piscinas: un volante rojo en un tubo que sale de la pared. Hay 25 en
 * el mapa (PlanoPiscinas#valvulas) y a cada jugador le tocan 3 (Valvulas). Se cierra
 * manteniendo el clic derecho: el volante gira (ANGULO, un cuarto de vuelta de los radios
 * cada vez) mientras suena el metal. Lo que pasa al girarla lo decide Valvulas.
 */
public class BloqueValvula extends EnPared {
	public static final MapCodec<BloqueValvula> CODEC = simpleCodec(BloqueValvula::new);
	/** El giro del volante: 0, 22,5, 45 y 67,5 grados (tiene cuatro radios: con eso da la vuelta). */
	public static final IntegerProperty ANGULO = IntegerProperty.create("angulo", 0, 3);
	private static final Map<Direction, VoxelShape> FORMAS = Shapes.rotateHorizontal(Block.box(2, 2, 8, 14, 14, 16));

	public BloqueValvula(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(ANGULO, 0));
	}

	@Override
	protected MapCodec<? extends BloqueValvula> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return FORMAS.get(estado.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return FORMAS.get(estado.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer sp) {
			Valvulas.get().girar(sp, pos, estado);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ANGULO);
	}
}
