package net.backrooms.evento.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Silla de oficina "bugeada": medio hundida en la moqueta, derecha o
 * volcada, como si el mundo la hubiera cargado mal.
 */
public class Silla extends EnSuelo {
	public static final MapCodec<Silla> CODEC = simpleCodec(Silla::new);
	public static final BooleanProperty VOLCADA = BooleanProperty.create("volcada");

	public Silla(BlockBehaviour.Properties properties) {
		super(properties, Block.box(3, 0, 3, 13, 12, 14), Block.box(3, 0, 3, 13, 4, 13));
		this.registerDefaultState(this.defaultBlockState().setValue(VOLCADA, false));
	}

	@Override
	protected MapCodec<? extends Silla> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VOLCADA);
	}
}
