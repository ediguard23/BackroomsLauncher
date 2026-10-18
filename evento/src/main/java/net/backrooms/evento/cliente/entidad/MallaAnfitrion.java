package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Malla de anfitrion. GENERADO por tools/texturas/entidades.js: no
 * editar a mano (se pierde al regenerar); cambiar alli y volver a generar.
 */
public final class MallaAnfitrion {
	private MallaAnfitrion() {
	}

	@SuppressWarnings("unused")
	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
		PartDefinition cadera = raiz.addOrReplaceChild("cadera", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5F, -12F, -2.5F, 9F, 12F, 5F)
				.texOffs(28, 0).addBox(-4F, -9F, -3.2F, 8F, 7F, 1F)
				.texOffs(46, 0).addBox(-2.5F, -12F, -3.4F, 5F, 2.5F, 1F)
				.texOffs(58, 0).addBox(-1F, -12.25F, -3.6F, 2F, 3F, 1F)
				.texOffs(64, 0).addBox(-1.5F, -13F, -1.5F, 3F, 1F, 3F),
			PartPose.offsetAndRotation(0F, 11F, 0F, 0F, 0F, 0F));
		PartDefinition cabeza = cadera.addOrReplaceChild("cabeza", CubeListBuilder.create()
				.texOffs(76, 0).addBox(-6F, -12F, -6F, 12F, 12F, 12F)
				.texOffs(0, 24).addBox(-5F, -13F, -5F, 10F, 14F, 10F)
				.texOffs(40, 24).addBox(-6.5F, -10F, -5F, 13F, 8F, 10F)
				.texOffs(86, 24).addBox(-1.5F, -6F, -7.5F, 3F, 3F, 1.5F),
			PartPose.offsetAndRotation(0F, -12F, 0F, 0F, 0F, 0F));
		PartDefinition gorro = cabeza.addOrReplaceChild("gorro", CubeListBuilder.create()
				.texOffs(96, 24).addBox(-2.5F, -3F, -2.5F, 5F, 3F, 5F)
				.texOffs(116, 24).addBox(-1.5F, -6F, -1.5F, 3F, 3F, 3F)
				.texOffs(0, 48).addBox(-0.5F, -8.5F, -0.5F, 1F, 2.5F, 1F)
				.texOffs(4, 48).addBox(-1F, -10F, -1F, 2F, 1.5F, 2F),
			PartPose.offsetAndRotation(1.5F, -12F, -0.5F, 0F, 0F, 0.244F));
		PartDefinition brazo_izq = cadera.addOrReplaceChild("brazo_izq", CubeListBuilder.create()
				.texOffs(12, 48).addBox(-1.25F, 0F, -1.25F, 2.5F, 14F, 2.5F)
				.texOffs(24, 48).addBox(-1.75F, 14F, -1.75F, 3.5F, 3.5F, 3.5F)
				.texOffs(40, 48).addBox(-2F, 13F, -2F, 4F, 1F, 4F),
			PartPose.offsetAndRotation(5.8F, -10F, 0F, 0F, 0F, -0.07F));
		PartDefinition brazo_der = cadera.addOrReplaceChild("brazo_der", CubeListBuilder.create()
				.texOffs(56, 48).addBox(-1.25F, 0F, -1.25F, 2.5F, 14F, 2.5F)
				.texOffs(68, 48).addBox(-1.75F, 14F, -1.75F, 3.5F, 3.5F, 3.5F)
				.texOffs(84, 48).addBox(-2F, 13F, -2F, 4F, 1F, 4F),
			PartPose.offsetAndRotation(-5.8F, -10F, 0F, 0F, 0F, 0.07F));
		PartDefinition pierna_izq = cadera.addOrReplaceChild("pierna_izq", CubeListBuilder.create()
				.texOffs(100, 48).addBox(-1.25F, 0F, -1.25F, 2.5F, 10F, 2.5F)
				.texOffs(0, 65).addBox(-2F, 10F, -5F, 4F, 3F, 7F),
			PartPose.offsetAndRotation(2.2F, 0F, 0F, 0F, 0F, 0F));
		PartDefinition pierna_der = cadera.addOrReplaceChild("pierna_der", CubeListBuilder.create()
				.texOffs(22, 65).addBox(-1.25F, 0F, -1.25F, 2.5F, 10F, 2.5F)
				.texOffs(34, 65).addBox(-2F, 10F, -5F, 4F, 3F, 7F),
			PartPose.offsetAndRotation(-2.2F, 0F, 0F, 0F, 0F, 0F));
		return LayerDefinition.create(malla, 128, 128);
	}
}
