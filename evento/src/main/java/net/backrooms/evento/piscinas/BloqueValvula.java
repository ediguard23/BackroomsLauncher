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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Una valvula de las Piscinas: un volante rojo en un tubo que sale de la pared. Hay 25 en
 * el mapa (PlanoPiscinas#valvulas) y a cada jugador le tocan 3 (Valvulas). Se cierra
 * manteniendo el clic derecho. El bloque es el tubo; el volante lo dibuja ValvulaRenderer,
 * que lo hace girar suave mientras alguien lo gira (ValvulaEntidad).
 */
public class BloqueValvula extends EnPared implements EntityBlock {
	public static final MapCodec<BloqueValvula> CODEC = simpleCodec(BloqueValvula::new);
	private static final Map<Direction, VoxelShape> FORMAS = Shapes.rotateHorizontal(Block.box(2, 2, 8, 14, 14, 16));

	public BloqueValvula(BlockBehaviour.Properties propiedades) {
		super(propiedades);
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
			Valvulas.get().girar(sp, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new ValvulaEntidad(pos, estado);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
		return nivel.isClientSide() && tipo == BloquesPiscinas.VALVULA_ENTIDAD
			? (n, p, e, be) -> ValvulaEntidad.tickCliente(n, p, e, (ValvulaEntidad) be) : null;
	}

	@Override
	protected boolean triggerEvent(BlockState estado, Level nivel, BlockPos pos, int id, int dato) {
		BlockEntity be = nivel.getBlockEntity(pos);
		return be != null && be.triggerEvent(id, dato);
	}
}
