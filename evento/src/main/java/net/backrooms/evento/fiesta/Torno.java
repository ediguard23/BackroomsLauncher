package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Las puertas de la Fiesta (PlanoFiesta.PUERTAS): una hoja de 1 x 2 en la pared. TIPO 0 la verde
 * de las habitaciones (COSTE 0 se pasa gratis; 1-5 son monedas, que se pagan una vez por vuelta),
 * 1 la roja de la salida del laberinto (gratis) y 2 la del final, que pide los tickets del
 * objetivo (no los gasta). FACING mira hacia el lado desde el que se paga; desde detras siempre se
 * puede volver. No se abre nunca: al pagar (o al volver) se cruza al otro lado (JuegoFiesta#torno),
 * asi que para los demas sigue siendo pared y no hay que sincronizar choques por jugador.
 */
public class Torno extends HorizontalDirectionalBlock {
	public static final MapCodec<Torno> CODEC = simpleCodec(Torno::new);
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 1);
	public static final IntegerProperty COSTE = IntegerProperty.create("coste", 0, 5);
	public static final IntegerProperty TIPO = IntegerProperty.create("tipo", 0, 2);

	public Torno(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PARTE, 0).setValue(COSTE, 1).setValue(TIPO, 0));
	}

	@Override
	protected MapCodec<? extends Torno> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer sp) {
			BlockPos abajo = estado.getValue(PARTE) == 1 ? pos.below() : pos;
			JuegoFiesta.get().torno(sp, abajo, estado.getValue(FACING), estado.getValue(COSTE), estado.getValue(TIPO));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PARTE, COSTE, TIPO);
	}
}
