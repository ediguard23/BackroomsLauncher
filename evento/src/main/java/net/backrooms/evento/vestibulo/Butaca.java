package net.backrooms.evento.vestibulo;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnSuelo;
import net.backrooms.evento.mision.Entidades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Butaca del auditorio (y sofas del vestibulo): clic derecho para sentarse,
 * Mayus para levantarse. FACING es hacia donde mira quien se sienta.
 */
public class Butaca extends EnSuelo {
	public static final MapCodec<Butaca> CODEC = simpleCodec(Butaca::new);

	public Butaca(BlockBehaviour.Properties properties) {
		super(properties, Block.box(1, 0, 1, 15, 16, 15), Block.box(1, 0, 1, 15, 8, 15));
	}

	@Override
	protected MapCodec<? extends Butaca> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (!(nivel instanceof ServerLevel sl) || jugador.isPassenger() || jugador.isShiftKeyDown()) {
			return InteractionResult.SUCCESS;
		}
		if (!sl.getEntitiesOfClass(Asiento.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
			return InteractionResult.SUCCESS; // ocupada
		}
		Asiento a = new Asiento(Entidades.ASIENTO, sl);
		Direction mira = estado.getValue(FACING);
		a.setPos(pos.getX() + 0.5, pos.getY() + 0.42, pos.getZ() + 0.5);
		a.setYRot(mira.toYRot());
		sl.addFreshEntity(a);
		jugador.startRiding(a);
		sl.playSound(null, pos, net.backrooms.evento.Sonidos.BUTACA, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 0.9F + sl.getRandom().nextFloat() * 0.2F);
		jugador.setYRot(mira.toYRot());
		return InteractionResult.SUCCESS;
	}
}
