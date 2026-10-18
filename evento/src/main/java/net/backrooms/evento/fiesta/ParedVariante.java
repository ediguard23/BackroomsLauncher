package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Algo pegado a la pared con varios dibujos (carteles, dibujos de ninos, guirnaldas, altavoces). */
public class ParedVariante extends EnPared {
	public static final MapCodec<ParedVariante> CODEC = simpleCodec(ParedVariante::new);
	public static final IntegerProperty VARIANTE = IntegerProperty.create("variante", 0, 7);

	public ParedVariante(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(VARIANTE, 0));
	}

	@Override
	protected MapCodec<? extends ParedVariante> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VARIANTE);
	}
}
