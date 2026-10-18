package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnSuelo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Algo en el suelo, orientado y con varios colores o dibujos (globos, regalos, confeti...). */
public class SueloVariante extends EnSuelo {
	public static final MapCodec<SueloVariante> CODEC = simpleCodec(SueloVariante::new);
	public static final IntegerProperty VARIANTE = IntegerProperty.create("variante", 0, 3);

	public SueloVariante(BlockBehaviour.Properties propiedades) {
		this(propiedades, Block.box(0, 0, 0, 16, 1, 16), Shapes.empty());
	}

	public SueloVariante(BlockBehaviour.Properties propiedades, VoxelShape forma, VoxelShape choque) {
		super(propiedades, forma, choque);
		this.registerDefaultState(this.defaultBlockState().setValue(VARIANTE, 0));
	}

	@Override
	protected MapCodec<? extends SueloVariante> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VARIANTE);
	}
}
