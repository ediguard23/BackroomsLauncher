package net.backrooms.evento.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Pintada en la pared: sonrisa, flechas, CORRE, NO, HUYE, AQUI, "?", una mano,
 * marcas de conteo, un ojo y dos tipos de cinta (ver tools/texturas/decoracion.js).
 */
public class Dibujo extends EnPared {
	public static final MapCodec<Dibujo> CODEC = simpleCodec(Dibujo::new);
	public static final int CUANTOS = 14;
	public static final IntegerProperty DIBUJO = IntegerProperty.create("dibujo", 0, CUANTOS - 1);

	/** Indices de las cintas (no son pintadas, van aparte en el reparto). */
	public static final int CINTA_X = 12;
	public static final int CINTA_PRECAUCION = 13;

	public Dibujo(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(DIBUJO, 0));
	}

	@Override
	protected MapCodec<? extends Dibujo> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(DIBUJO);
	}
}
