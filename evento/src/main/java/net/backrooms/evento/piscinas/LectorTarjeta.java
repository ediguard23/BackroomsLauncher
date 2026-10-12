package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Un lector de tarjeta junto a la puerta de salida: uno por color (COLOR, PlanoPiscinas.T_*).
 * Quien lleva esa tarjeta pulsa el lector y la mete (Tarjetas#meter); entonces se enciende
 * (LIT) y ya no se apaga. Con los tres encendidos se abre la puerta.
 */
public class LectorTarjeta extends EnPared {
	public static final MapCodec<LectorTarjeta> CODEC = simpleCodec(LectorTarjeta::new);
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 2);
	public static final BooleanProperty LIT = BooleanProperty.create("lit");

	public LectorTarjeta(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(COLOR, 0).setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends LectorTarjeta> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, net.minecraft.world.phys.BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer sp) {
			Tarjetas.get().meter(sp, estado.getValue(COLOR), pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(COLOR, LIT);
	}
}
