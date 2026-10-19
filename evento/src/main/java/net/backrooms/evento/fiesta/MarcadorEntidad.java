package net.backrooms.evento.fiesta;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Solo para que el cliente dibuje el texto del marcador (MarcadorRenderer); no guarda nada. */
public class MarcadorEntidad extends BlockEntity {
	public MarcadorEntidad(BlockPos pos, BlockState estado) {
		super(BloquesFiesta.MARCADOR_ENTIDAD, pos, estado);
	}
}
