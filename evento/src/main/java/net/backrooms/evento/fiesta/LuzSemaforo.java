package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Una lampara del semaforo de la Fiesta, en la pared del fondo de su pasillo. ROJA y VERDE
 * dicen de que color es la lampara; LUZ si esta encendida (la cambia JuegoSemaforo).
 */
public class LuzSemaforo extends EnPared {
	public static final MapCodec<LuzSemaforo> CODEC = simpleCodec(LuzSemaforo::new);
	/** 0 la carcasa (sin lampara), 1 la roja, 2 la verde. */
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 2);
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty LUZ =
		net.minecraft.world.level.block.state.properties.BooleanProperty.create("luz");

	public LuzSemaforo(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(COLOR, 0).setValue(LUZ, false));
	}

	@Override
	protected MapCodec<? extends LuzSemaforo> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(COLOR, LUZ);
	}
}
