package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.piscinas.PlanoPiscinas;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.Mth;

/**
 * Un cuerpo: el modelo de un jugador (con las capas de la piel) y una cuerda (un hilo de
 * 1x16 que se estira) para los colgados. Las extremidades segun la postura, con un poco de
 * azar fijo por cuerpo para que no esten todos igual; la postura entera (tumbado, del reves)
 * la pone CadaverRenderer.
 */
public class ModeloCadaver extends EntityModel<CadaverRenderer.Estado> {
	private final ModelPart cabeza;
	private final ModelPart brazoD;
	private final ModelPart brazoI;
	private final ModelPart piernaD;
	private final ModelPart piernaI;
	private final ModelPart cuerda;

	public ModeloCadaver(ModelPart raiz) {
		super(raiz);
		this.cabeza = raiz.getChild("head");
		this.brazoD = raiz.getChild("right_arm");
		this.brazoI = raiz.getChild("left_arm");
		this.piernaD = raiz.getChild("right_leg");
		this.piernaI = raiz.getChild("left_leg");
		this.cuerda = raiz.getChild("cuerda");
	}

	public static LayerDefinition crear() {
		MeshDefinition malla = PlayerModel.createMesh(CubeDeformation.NONE, false);
		// la cuerda usa el hueco libre de la piel (56,16): 1x16x1
		malla.getRoot().addOrReplaceChild("cuerda", CubeListBuilder.create().texOffs(56, 16).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 16.0F, 1.0F), PartPose.ZERO);
		return LayerDefinition.create(malla, 64, 64);
	}

	@Override
	public void setupAnim(CadaverRenderer.Estado e) {
		super.setupAnim(e);
		float a = e.azar;
		float b = (a * 7.31F) % 1.0F;
		float c = (a * 13.7F) % 1.0F;
		this.brazoD.visible = (e.mutilado & PlanoPiscinas.SIN_BRAZO) == 0;
		this.piernaI.visible = (e.mutilado & PlanoPiscinas.SIN_PIERNA) == 0;
		this.cuerda.visible = false;
		switch (e.postura) {
			case PlanoPiscinas.BOCA_ARRIBA, PlanoPiscinas.BOCA_ABAJO -> {
				// brazos abiertos de cualquier manera, la cabeza ladeada
				this.brazoD.zRot = 0.3F + a * 1.2F;
				this.brazoI.zRot = -(0.2F + b * 1.3F);
				this.brazoD.xRot = (c - 0.5F) * 0.8F;
				this.piernaD.zRot = 0.08F + b * 0.25F;
				this.piernaI.zRot = -(0.05F + c * 0.3F);
				this.cabeza.yRot = (a - 0.5F) * 1.4F;
				this.cabeza.zRot = (b - 0.5F) * 0.4F;
			}
			case PlanoPiscinas.SENTADO -> {
				// las piernas hacia delante, los brazos muertos y la cabeza caida
				this.piernaD.xRot = -Mth.HALF_PI;
				this.piernaI.xRot = -Mth.HALF_PI;
				this.piernaD.yRot = 0.15F + a * 0.2F;
				this.piernaI.yRot = -(0.1F + b * 0.25F);
				this.brazoD.xRot = -0.25F;
				this.brazoI.xRot = -0.35F;
				this.brazoD.zRot = 0.12F;
				this.brazoI.zRot = -0.18F;
				this.cabeza.xRot = 0.6F + c * 0.35F;
				this.cabeza.zRot = (a - 0.5F) * 0.7F;
			}
			case PlanoPiscinas.COLGADO_PIES -> {
				// cabeza abajo: los brazos cuelgan (en el modelo, por encima de la cabeza)
				this.brazoD.xRot = Mth.PI + (a - 0.5F) * 0.3F;
				this.brazoI.xRot = Mth.PI + (b - 0.5F) * 0.3F;
				this.brazoD.zRot = -0.15F;
				this.brazoI.zRot = 0.15F;
				this.piernaD.zRot = -0.06F;
				this.piernaI.zRot = 0.06F;
				this.cuerda.visible = true;
				this.cuerda.setPos(0.0F, 24.0F, 0.0F);
				this.cuerda.yScale = e.cuerda;
			}
			case PlanoPiscinas.COLGADO_CUELLO -> {
				// la cabeza doblada, todo flojo, los pies al aire
				this.cabeza.zRot = 0.55F + a * 0.25F;
				this.cabeza.xRot = 0.25F;
				this.brazoD.zRot = 0.06F;
				this.brazoI.zRot = -0.06F;
				this.piernaD.xRot = 0.12F;
				this.piernaI.xRot = -0.05F;
				this.cuerda.visible = true;
				this.cuerda.setPos(0.0F, -8.0F, 0.0F);
				this.cuerda.xRot = Mth.PI;
				this.cuerda.yScale = e.cuerda;
			}
			default -> {
			}
		}
	}
}
