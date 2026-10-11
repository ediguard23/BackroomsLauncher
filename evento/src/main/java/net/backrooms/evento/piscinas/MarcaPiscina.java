package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Lo que alguien dejo en una pared de las Piscinas (texturas en tools/texturas/piscinas.js,
 * en este mismo orden): CORRE pintado con sangre en tres bloques, una mano, dos manos que
 * resbalan, aranazos, palotes de alguien que contaba, una espiral, una mancha de humedad,
 * un brochazo de sangre y tres carteles viejos de piscina (los numeros, en PlanoPiscinas.M_*).
 */
public class MarcaPiscina extends EnPared {
	public static final MapCodec<MarcaPiscina> CODEC = simpleCodec(MarcaPiscina::new);
	public static final IntegerProperty MARCA = IntegerProperty.create("marca", 0, PlanoPiscinas.M_CUANTAS - 1);

	public MarcaPiscina(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(MARCA, PlanoPiscinas.M_MANO));
	}

	@Override
	protected MapCodec<? extends MarcaPiscina> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(MARCA);
	}
}
