package net.backrooms.evento.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Tubo fluorescente que falla: casi siempre encendido, pero de vez en cuando
 * (en un tick aleatorio, ~1 vez por minuto) se apaga y vuelve en una rafaga
 * corta de parpadeos. Solo hace trabajo cuando parpadea: no tiene entidad de
 * bloque ni cuenta ticks.
 */
public class TuboParpadeante extends Block {
	public static final MapCodec<TuboParpadeante> CODEC = simpleCodec(TuboParpadeante::new);
	public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

	public TuboParpadeante(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LIT, true));
	}

	@Override
	public MapCodec<TuboParpadeante> codec() {
		return CODEC;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState estado) {
		return estado.getValue(LIT);
	}

	/** Empieza una rafaga: se apaga y programa la vuelta. */
	@Override
	protected void randomTick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource azar) {
		if (estado.getValue(LIT)) {
			nivel.setBlock(pos, estado.setValue(LIT, false), Block.UPDATE_CLIENTS);
			nivel.scheduleTick(pos, this, 2 + azar.nextInt(5));
		}
	}

	/** Cada tick programado cambia de estado; al encenderse, a veces sigue la rafaga. */
	@Override
	protected void tick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource azar) {
		boolean encendido = !estado.getValue(LIT);
		nivel.setBlock(pos, estado.setValue(LIT, encendido), Block.UPDATE_CLIENTS);
		if (!encendido) {
			nivel.scheduleTick(pos, this, 2 + azar.nextInt(5));
		} else if (azar.nextFloat() < 0.55F) {
			nivel.scheduleTick(pos, this, 3 + azar.nextInt(8));
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
