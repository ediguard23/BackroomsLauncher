package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Malla de bacteria. GENERADO por tools/texturas/entidades.js: no
 * editar a mano (se pierde al regenerar); cambiar alli y volver a generar.
 */
public final class MallaBacteria {
	private MallaBacteria() {
	}

	@SuppressWarnings("unused")
	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
		PartDefinition cadera = raiz.addOrReplaceChild("cadera", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3F, -1F, -1.5F, 6F, 2F, 3F)
				.texOffs(18, 0).addBox(-1F, -4F, -1F, 2F, 4F, 2F),
			PartPose.offsetAndRotation(0F, -2F, 2F, 0F, 0F, 0F));
		PartDefinition espina = cadera.addOrReplaceChild("espina", CubeListBuilder.create()
				.texOffs(26, 0).addBox(-1F, -10F, -1F, 2F, 10F, 2F)
				.texOffs(34, 0).addBox(-4F, -7F, -0.5F, 8F, 1F, 1F)
				.texOffs(52, 0).addBox(-3.5F, -3F, -0.5F, 7F, 1F, 1F)
				.texOffs(68, 0).addBox(1F, -9F, -2F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(0F, -3F, 0F, 0.314F, 0F, 0F));
		PartDefinition pecho = espina.addOrReplaceChild("pecho", CubeListBuilder.create()
				.texOffs(72, 0).addBox(-1F, -9F, -1F, 2F, 9F, 2F)
				.texOffs(80, 0).addBox(-5F, -8F, -0.5F, 10F, 1F, 1F)
				.texOffs(102, 0).addBox(-6F, -5F, -0.5F, 12F, 1F, 1F)
				.texOffs(0, 12).addBox(-4.5F, -2F, -0.5F, 9F, 1F, 1F)
				.texOffs(20, 12).addBox(-2F, -7F, -1.5F, 4F, 5F, 3F),
			PartPose.offsetAndRotation(0F, -10F, 0F, 0.244F, 0F, 0F));
		PartDefinition hombros = pecho.addOrReplaceChild("hombros", CubeListBuilder.create()
				.texOffs(34, 12).addBox(-11F, -1.5F, -1.5F, 22F, 3F, 3F)
				.texOffs(84, 12).addBox(-13F, -4F, -1F, 3F, 4F, 2F)
				.texOffs(94, 12).addBox(10F, -4F, -1F, 3F, 4F, 2F)
				.texOffs(104, 12).addBox(-3F, -3F, -1F, 6F, 2F, 2F)
				.texOffs(120, 12).addBox(-8F, 1.5F, -0.5F, 1F, 9F, 1F)
				.texOffs(124, 12).addBox(6F, 1.5F, -0.5F, 1F, 7F, 1F)
				.texOffs(0, 22).addBox(-5F, 1.5F, 0.5F, 1F, 5F, 1F),
			PartPose.offsetAndRotation(0F, -9F, 0F, -0.175F, 0F, 0F));
		PartDefinition cuello = hombros.addOrReplaceChild("cuello", CubeListBuilder.create()
				.texOffs(4, 22).addBox(-1F, -6F, -1F, 2F, 6F, 2F)
				.texOffs(12, 22).addBox(-2F, -4F, 0F, 4F, 1F, 1F),
			PartPose.offsetAndRotation(0F, -1.5F, 0F, -0.489F, 0F, 0F));
		PartDefinition cabeza = cuello.addOrReplaceChild("cabeza", CubeListBuilder.create()
				.texOffs(22, 22).addBox(-5.5F, -9F, -12F, 11F, 7F, 13F)
				.texOffs(70, 22).addBox(-5F, -2F, -11.5F, 10F, 2F, 1F)
				.texOffs(92, 22).addBox(-5F, -2F, -10.5F, 1F, 2F, 8F)
				.texOffs(110, 22).addBox(4F, -2F, -10.5F, 1F, 2F, 8F)
				.texOffs(0, 42).addBox(-4.5F, -2.01F, -10.5F, 9F, 0.01F, 9F)
				.texOffs(36, 42).addBox(-5F, -10F, 0F, 10F, 4F, 2F)
				.texOffs(60, 42).addBox(-1F, -13F, -6F, 2F, 4F, 2F)
				.texOffs(68, 42).addBox(2.5F, -12F, -2F, 1F, 3F, 1F)
				.texOffs(72, 42).addBox(-4F, -11F, -9F, 1F, 2F, 1F)
				.texOffs(76, 42).addBox(-6.5F, -7F, -8F, 1F, 6F, 1F)
				.texOffs(80, 42).addBox(5.5F, -6F, -4F, 1F, 7F, 1F),
			PartPose.offsetAndRotation(0F, -6F, 0F, 0.524F, 0F, 0F));
		PartDefinition mandibula = cabeza.addOrReplaceChild("mandibula", CubeListBuilder.create()
				.texOffs(84, 42).addBox(-5F, 0F, -11F, 10F, 2F, 11F)
				.texOffs(0, 55).addBox(-4.5F, -2F, -10.5F, 9F, 2F, 1F)
				.texOffs(20, 55).addBox(-4.5F, -2F, -9.5F, 1F, 2F, 8F)
				.texOffs(38, 55).addBox(3.5F, -2F, -9.5F, 1F, 2F, 8F)
				.texOffs(56, 55).addBox(-4F, 0F, -10F, 8F, 0.01F, 9F)
				.texOffs(90, 55).addBox(-1F, 2F, -8F, 1F, 5F, 1F),
			PartPose.offsetAndRotation(0F, -2F, 0F, 0.454F, 0F, 0F));
		PartDefinition brazo_izq = hombros.addOrReplaceChild("brazo_izq", CubeListBuilder.create()
				.texOffs(94, 55).addBox(-1F, 0F, -1F, 2F, 17F, 2F)
				.texOffs(102, 55).addBox(1F, 4F, -0.5F, 1F, 7F, 1F),
			PartPose.offsetAndRotation(11F, -1F, 0F, -0.14F, 0F, -0.14F));
		PartDefinition antebrazo_izq = brazo_izq.addOrReplaceChild("antebrazo_izq", CubeListBuilder.create()
				.texOffs(106, 55).addBox(-1F, 0F, -1F, 2F, 19F, 2F)
				.texOffs(114, 55).addBox(-1.5F, 6F, -1.5F, 3F, 3F, 3F),
			PartPose.offsetAndRotation(0F, 16.5F, 0F, -0.244F, 0F, 0.07F));
		PartDefinition mano_izq = antebrazo_izq.addOrReplaceChild("mano_izq", CubeListBuilder.create()
				.texOffs(0, 76).addBox(-2F, 0F, -1.5F, 4F, 2F, 3F),
			PartPose.offsetAndRotation(0F, 18.5F, 0F, 0.419F, 0F, 0F));
		PartDefinition garra_izq_a = mano_izq.addOrReplaceChild("garra_izq_a", CubeListBuilder.create()
				.texOffs(14, 76).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F),
			PartPose.offsetAndRotation(0F, 1.5F, -1F, -0.314F, 0F, 0F));
		PartDefinition garra_izq_b = mano_izq.addOrReplaceChild("garra_izq_b", CubeListBuilder.create()
				.texOffs(18, 76).addBox(-0.5F, 0F, -0.5F, 1F, 8F, 1F),
			PartPose.offsetAndRotation(-1.5F, 1.5F, 0.5F, 0.175F, 0F, 0.384F));
		PartDefinition garra_izq_c = mano_izq.addOrReplaceChild("garra_izq_c", CubeListBuilder.create()
				.texOffs(22, 76).addBox(-0.5F, 0F, -0.5F, 1F, 8F, 1F),
			PartPose.offsetAndRotation(1.5F, 1.5F, 0.5F, 0.175F, 0F, -0.384F));
		PartDefinition brazo_der = hombros.addOrReplaceChild("brazo_der", CubeListBuilder.create()
				.texOffs(26, 76).addBox(-1F, 0F, -1F, 2F, 17F, 2F)
				.texOffs(34, 76).addBox(-2F, 4F, -0.5F, 1F, 7F, 1F),
			PartPose.offsetAndRotation(-11F, -1F, 0F, -0.14F, 0F, 0.14F));
		PartDefinition antebrazo_der = brazo_der.addOrReplaceChild("antebrazo_der", CubeListBuilder.create()
				.texOffs(38, 76).addBox(-1F, 0F, -1F, 2F, 19F, 2F)
				.texOffs(46, 76).addBox(-1.5F, 6F, -1.5F, 3F, 3F, 3F),
			PartPose.offsetAndRotation(0F, 16.5F, 0F, -0.244F, 0F, -0.07F));
		PartDefinition mano_der = antebrazo_der.addOrReplaceChild("mano_der", CubeListBuilder.create()
				.texOffs(58, 76).addBox(-2F, 0F, -1.5F, 4F, 2F, 3F),
			PartPose.offsetAndRotation(0F, 18.5F, 0F, 0.419F, 0F, 0F));
		PartDefinition garra_der_a = mano_der.addOrReplaceChild("garra_der_a", CubeListBuilder.create()
				.texOffs(72, 76).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F),
			PartPose.offsetAndRotation(0F, 1.5F, -1F, -0.314F, 0F, 0F));
		PartDefinition garra_der_b = mano_der.addOrReplaceChild("garra_der_b", CubeListBuilder.create()
				.texOffs(76, 76).addBox(-0.5F, 0F, -0.5F, 1F, 8F, 1F),
			PartPose.offsetAndRotation(-1.5F, 1.5F, 0.5F, 0.175F, 0F, 0.384F));
		PartDefinition garra_der_c = mano_der.addOrReplaceChild("garra_der_c", CubeListBuilder.create()
				.texOffs(80, 76).addBox(-0.5F, 0F, -0.5F, 1F, 8F, 1F),
			PartPose.offsetAndRotation(1.5F, 1.5F, 0.5F, 0.175F, 0F, -0.384F));
		PartDefinition muslo_izq = cadera.addOrReplaceChild("muslo_izq", CubeListBuilder.create()
				.texOffs(84, 76).addBox(-1F, 0F, -1F, 2F, 11F, 2F)
				.texOffs(92, 76).addBox(-1.5F, 3F, -1.5F, 3F, 4F, 3F),
			PartPose.offsetAndRotation(3F, 0F, 0F, -0.454F, 0F, -0.105F));
		PartDefinition pierna_izq = muslo_izq.addOrReplaceChild("pierna_izq", CubeListBuilder.create()
				.texOffs(104, 76).addBox(-1F, 0F, -1F, 2F, 12F, 2F)
				.texOffs(112, 76).addBox(0.5F, 3F, 0.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(0F, 10.5F, 0F, 0.838F, 0F, 0F));
		PartDefinition pie_izq = pierna_izq.addOrReplaceChild("pie_izq", CubeListBuilder.create()
				.texOffs(116, 76).addBox(-1F, 0F, -1F, 2F, 3F, 2F)
				.texOffs(0, 97).addBox(-1.5F, 2F, -5F, 1F, 1F, 5F)
				.texOffs(12, 97).addBox(0.5F, 2F, -5F, 1F, 1F, 5F),
			PartPose.offsetAndRotation(0F, 11.5F, 0F, -0.384F, 0F, 0F));
		PartDefinition muslo_der = cadera.addOrReplaceChild("muslo_der", CubeListBuilder.create()
				.texOffs(24, 97).addBox(-1F, 0F, -1F, 2F, 11F, 2F)
				.texOffs(32, 97).addBox(-1.5F, 3F, -1.5F, 3F, 4F, 3F),
			PartPose.offsetAndRotation(-3F, 0F, 0F, -0.454F, 0F, 0.105F));
		PartDefinition pierna_der = muslo_der.addOrReplaceChild("pierna_der", CubeListBuilder.create()
				.texOffs(44, 97).addBox(-1F, 0F, -1F, 2F, 12F, 2F)
				.texOffs(52, 97).addBox(-1.5F, 3F, 0.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(0F, 10.5F, 0F, 0.838F, 0F, 0F));
		PartDefinition pie_der = pierna_der.addOrReplaceChild("pie_der", CubeListBuilder.create()
				.texOffs(56, 97).addBox(-1F, 0F, -1F, 2F, 3F, 2F)
				.texOffs(64, 97).addBox(-1.5F, 2F, -5F, 1F, 1F, 5F)
				.texOffs(76, 97).addBox(0.5F, 2F, -5F, 1F, 1F, 5F),
			PartPose.offsetAndRotation(0F, 11.5F, 0F, -0.384F, 0F, 0F));
		return LayerDefinition.create(malla, 128, 128);
	}
}
