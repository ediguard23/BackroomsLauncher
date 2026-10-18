package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.Sonidos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La piscina de bolas del fondo de las zonas de parkour: se hunde uno hasta la cintura (la
 * caja es mas baja que un bloque) y amortigua las caidas (sin dano). Quien cae aqui fuera del
 * sitio de llegada vuelve al principio del recorrido (JuegoFiesta).
 */
public class PiscinaBolas extends Block {
	public static final MapCodec<PiscinaBolas> CODEC = simpleCodec(PiscinaBolas::new);
	private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 6, 16);

	public PiscinaBolas(BlockBehaviour.Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected MapCodec<? extends PiscinaBolas> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return FORMA;
	}

	@Override
	public void fallOn(Level nivel, BlockState estado, BlockPos pos, Entity quien, double distancia) {
		quien.resetFallDistance();
		if (nivel instanceof ServerLevel s && distancia > 1.5) {
			s.playSound(null, pos, Sonidos.PARKOUR_BOLAS, SoundSource.BLOCKS, 1.0F, 0.9F + s.getRandom().nextFloat() * 0.2F);
		}
	}
}
