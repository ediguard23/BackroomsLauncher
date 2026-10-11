package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * La Serpiente (malla en MallaSerpiente, generada). Sin animaciones todavia: se
 * queda en la postura del modelo, alzada y mirando hacia abajo.
 */
public class ModeloSerpiente extends EntityModel<LivingEntityRenderState> {
	public ModeloSerpiente(ModelPart raiz) {
		super(raiz);
	}
}
