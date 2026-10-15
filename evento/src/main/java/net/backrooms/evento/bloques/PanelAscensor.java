package net.backrooms.evento.bloques;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.fase.Fases;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * El panel de botones de un ascensor de salida. Quien ha completado sus
 * misiones baja a la siguiente fase (o escapa, si era la ultima); a los
 * demas no les responde.
 */
public class PanelAscensor extends EnPared {
	public static final MapCodec<PanelAscensor> CODEC = simpleCodec(PanelAscensor::new);

	public PanelAscensor(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends PanelAscensor> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer sp) {
			Fases.get().pulsar(sp, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
