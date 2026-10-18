package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Una baldosa de los juegos de la Fiesta: las cuatro de colores de la memoria (COLOR 0-3: roja,
 * amarilla, azul y verde) y los botones de empezar (COLOR 4, una estrella). LUZ: encendida (la
 * cambian JuegoMemoria y JuegoRegalos).
 */
public class BaldosaJuego extends Block {
	public static final MapCodec<BaldosaJuego> CODEC = simpleCodec(BaldosaJuego::new);
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 4);
	public static final BooleanProperty LUZ = BooleanProperty.create("luz");

	public BaldosaJuego(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, 0).setValue(LUZ, false));
	}

	@Override
	protected MapCodec<? extends BaldosaJuego> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COLOR, LUZ);
	}
}
