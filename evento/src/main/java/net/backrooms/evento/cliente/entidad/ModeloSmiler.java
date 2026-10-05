package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * El Smiler (malla en MallaSmiler, generada): una sombra que flota y se
 * mece, con los brazos colgando y la cabeza ladeada.
 */
public class ModeloSmiler extends EntityModel<LivingEntityRenderState> {
	private final ModelPart cuerpo;
	private final ModelPart cabeza;
	private final ModelPart brazoIzq;
	private final ModelPart brazoDer;

	public ModeloSmiler(ModelPart raiz) {
		super(raiz);
		this.cuerpo = raiz.getChild("cuerpo");
		this.cabeza = this.cuerpo.getChild("cabeza");
		this.brazoIzq = this.cuerpo.getChild("brazo_izq");
		this.brazoDer = this.cuerpo.getChild("brazo_der");
	}

	@Override
	public void setupAnim(LivingEntityRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		this.cuerpo.y += Mth.sin(t * 0.08F) * 0.8F;
		this.cuerpo.zRot += Mth.sin(t * 0.05F) * 0.05F;
		this.cabeza.yRot += s.yRot * Mth.DEG_TO_RAD;
		this.cabeza.xRot += s.xRot * Mth.DEG_TO_RAD;
		this.cabeza.zRot += 0.18F + Mth.sin(t * 0.03F) * 0.08F;
		this.brazoIzq.xRot += Mth.sin(t * 0.06F) * 0.12F;
		this.brazoDer.xRot += Mth.sin(t * 0.06F + 2.0F) * 0.12F;
	}
}
