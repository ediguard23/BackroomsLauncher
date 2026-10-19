package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Un semaforo pequeno del castillo de bolas, en el sitio de una red (choca como ella): una caja
 * negra con la lampara roja arriba y la verde abajo, mirando (FACING) al pasillo. LUZ la cambia
 * JuegoSemaforo para todos a la vez: 0 apagado, 1 rojo, 2 verde.
 */
public class SemaforoCastillo extends HorizontalDirectionalBlock {
	public static final MapCodec<SemaforoCastillo> CODEC = simpleCodec(SemaforoCastillo::new);
	public static final IntegerProperty LUZ = IntegerProperty.create("luz", 0, 2);

	public SemaforoCastillo(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LUZ, 0));
	}

	@Override
	protected MapCodec<? extends SemaforoCastillo> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LUZ);
	}
}
