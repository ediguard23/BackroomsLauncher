package net.backrooms.evento.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ventilador colgado del techo. Las aspas giran con una textura animada (sin
 * entidad de bloque): cuesta lo mismo que un bloque normal aunque haya miles.
 */
public class Ventilador extends Block {
	public static final MapCodec<Ventilador> CODEC = simpleCodec(Ventilador::new);
	private static final VoxelShape FORMA = Block.box(5, 9, 5, 11, 16, 11);

	public Ventilador(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Ventilador> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return FORMA;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}
}
