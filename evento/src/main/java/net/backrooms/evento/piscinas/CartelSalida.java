package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Cartel verde de salida de emergencia (con luz propia, como los de verdad): el pequeno, con
 * la flecha hacia donde queda la puerta (izquierda o derecha de quien lo mira), por las
 * paredes de alrededor; y el grande de encima de la puerta, en dos mitades
 * (PlanoPiscinas.CARTEL_*).
 */
public class CartelSalida extends EnPared {
	public static final MapCodec<CartelSalida> CODEC = simpleCodec(CartelSalida::new);
	public static final IntegerProperty VARIANTE = IntegerProperty.create("variante", 0, 3);

	public CartelSalida(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(VARIANTE, 0));
	}

	@Override
	protected MapCodec<? extends CartelSalida> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VARIANTE);
	}
}
