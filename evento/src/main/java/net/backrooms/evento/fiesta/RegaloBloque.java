package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Un regalo de la Fiesta (decoracion por todas partes). En la sala de los regalos, con clic
 * derecho se abre: si es uno de los de tu partida, lo encuentras (JuegoRegalos#abrir).
 */
public class RegaloBloque extends SueloVariante {
	public static final MapCodec<RegaloBloque> CODEC = simpleCodec(RegaloBloque::new);

	public RegaloBloque(BlockBehaviour.Properties propiedades) {
		super(propiedades, Block.box(2, 0, 2, 14, 12, 14), Block.box(2, 0, 2, 14, 12, 14));
	}

	@Override
	protected MapCodec<? extends RegaloBloque> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer sp) {
			JuegoRegalos.get().abrir(sp, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
