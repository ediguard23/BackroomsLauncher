package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnSuelo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Una mancha pegada al azulejo del suelo (no choca): suciedad acumulada, un charco
 * oscuro seco, un rastro de gotas de sangre, escombros de azulejo, huellas descalzas y el rastro
 * de algo arrastrado (tres seguidas).
 * Texturas en tools/texturas/piscinas.js, en este orden (los numeros, en PlanoPiscinas.S_*).
 */
public class ManchaSuelo extends EnSuelo {
	public static final MapCodec<ManchaSuelo> CODEC = simpleCodec(ManchaSuelo::new);
	public static final IntegerProperty MANCHA = IntegerProperty.create("mancha", 0, PlanoPiscinas.S_CUANTAS - 1);

	public ManchaSuelo(BlockBehaviour.Properties propiedades) {
		super(propiedades, Block.box(0, 0, 0, 16, 1, 16), Shapes.empty());
		this.registerDefaultState(this.defaultBlockState().setValue(MANCHA, PlanoPiscinas.S_SUCIEDAD));
	}

	@Override
	protected MapCodec<? extends ManchaSuelo> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(MANCHA);
	}
}
