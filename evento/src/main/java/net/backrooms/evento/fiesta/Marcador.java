package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/**
 * El cartel de LEDs de la sala central, pegado a la pared: tres bloques (PARTE 0 izquierda, 1 el
 * centro, 2 derecha) de panel negro. El del centro lleva la entidad (MarcadorEntidad) y el cliente
 * escribe encima, en rojo, lo de cada uno (MarcadorRenderer): TIPO 0 "TICKETS 894/1500" con tus
 * tickets, 1 "BOTE 540", lo que te daria la vuelta si la acabaras ahora.
 */
public class Marcador extends EnPared implements EntityBlock {
	public static final MapCodec<Marcador> CODEC = simpleCodec(Marcador::new);
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 2);
	public static final IntegerProperty TIPO = IntegerProperty.create("tipo", 0, 1);

	public Marcador(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.defaultBlockState().setValue(PARTE, 1).setValue(TIPO, 0));
	}

	@Override
	protected MapCodec<? extends Marcador> codec() {
		return CODEC;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return estado.getValue(PARTE) == 1 ? new MarcadorEntidad(pos, estado) : null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(PARTE, TIPO);
	}
}
