package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * El localizador de salida de las salas seguras del laberinto: un boton azul en la pared con su
 * placa ("LOCALIZAR SALIDA · 2 MONEDAS"). Al usarlo (y pagar) la puerta de la salida suena solo
 * para quien lo ha pulsado, desde donde esta: hay que ir hacia el sonido (JuegoFiesta#localizador).
 */
public class LocalizadorSalida extends EnPared {
	public static final MapCodec<LocalizadorSalida> CODEC = simpleCodec(LocalizadorSalida::new);

	public LocalizadorSalida(BlockBehaviour.Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected MapCodec<? extends LocalizadorSalida> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer sp) {
			JuegoFiesta.get().localizador(sp, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
