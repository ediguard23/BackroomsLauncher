package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.Sonidos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Panel de luz de las Piscinas que falla (uno de cada ~75, ver GeneradorPiscinas):
 * casi siempre encendido; muy de vez en cuando chasquea, se apaga un instante y
 * vuelve, a veces con un par de parpadeos mas. No todos fallan igual: cada panel
 * tiene su propia probabilidad (sale de su posicion), y cada rafaga es distinta.
 * Como el tubo del Nivel 0, solo trabaja cuando parpadea (ticks aleatorios).
 */
public class LuzParpadeante extends Block {
	public static final MapCodec<LuzParpadeante> CODEC = simpleCodec(LuzParpadeante::new);
	public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

	public LuzParpadeante(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(LIT, true));
	}

	@Override
	public MapCodec<LuzParpadeante> codec() {
		return CODEC;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState estado) {
		return estado.getValue(LIT);
	}

	/** 0,15..0,5: lo nervioso que es este panel (un tick aleatorio llega ~1 vez por minuto). */
	private static float nervio(BlockPos pos) {
		int h = pos.getX() * 73856093 ^ pos.getY() * 19349663 ^ pos.getZ() * 83492791;
		h = (h ^ (h >>> 13)) * 0x5bd1e995;
		return 0.15F + ((h ^ (h >>> 15)) & 255) / 255.0F * 0.35F;
	}

	@Override
	protected void randomTick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource azar) {
		if (!estado.getValue(LIT) || azar.nextFloat() > nervio(pos)) {
			return;
		}
		nivel.setBlock(pos, estado.setValue(LIT, false), Block.UPDATE_CLIENTS);
		nivel.playSound(null, pos, Sonidos.PISCINAS_CHISPA, SoundSource.BLOCKS, 0.9F, 0.85F + azar.nextFloat() * 0.3F);
		nivel.scheduleTick(pos, this, 1 + azar.nextInt(4));
	}

	/** Cada tick programado cambia de estado; al encenderse, a veces sigue la rafaga (pocas veces y corta). */
	@Override
	protected void tick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource azar) {
		boolean encendido = !estado.getValue(LIT);
		nivel.setBlock(pos, estado.setValue(LIT, encendido), Block.UPDATE_CLIENTS);
		if (!encendido) {
			nivel.scheduleTick(pos, this, 1 + azar.nextInt(4));
		} else if (azar.nextFloat() < 0.35F) {
			nivel.scheduleTick(pos, this, 2 + azar.nextInt(10));
			if (azar.nextFloat() < 0.4F) {
				nivel.playSound(null, pos, Sonidos.PISCINAS_CHISPA, SoundSource.BLOCKS, 0.6F, 0.9F + azar.nextFloat() * 0.3F);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
