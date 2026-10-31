package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Malla de amasijo. GENERADO por tools/texturas/entidades.js: no
 * editar a mano (se pierde al regenerar); cambiar alli y volver a generar.
 */
public final class MallaAmasijo {
	private MallaAmasijo() {
	}

	@SuppressWarnings("unused")
	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
		PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8F, -4F, -7F, 16F, 8F, 14F)
				.texOffs(60, 0).addBox(-6F, -8F, 6F, 12F, 10F, 4F),
			PartPose.offsetAndRotation(0F, 10F, 0F, 0F, 0F, 0F));
		PartDefinition torso = cuerpo.addOrReplaceChild("torso", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-9F, -12F, -13F, 18F, 16F, 16F)
				.texOffs(68, 22).addBox(-10.5F, -9F, -10F, 3F, 10F, 10F)
				.texOffs(94, 22).addBox(7.5F, -10F, -9F, 3F, 9F, 9F),
			PartPose.offsetAndRotation(0F, 0F, 5F, 0F, 0F, 0F));
		PartDefinition joroba = torso.addOrReplaceChild("joroba", CubeListBuilder.create()
				.texOffs(0, 54).addBox(-7F, -4F, -10F, 14F, 4F, 12F),
			PartPose.offsetAndRotation(0F, -12F, -1F, 0F, 0F, 0F));
		PartDefinition pata_0 = cuerpo.addOrReplaceChild("pata_0", CubeListBuilder.create()
				.texOffs(52, 54).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F)
				.texOffs(64, 54).addBox(-2F, 9.5F, -3F, 4F, 1.5F, 5F),
			PartPose.offsetAndRotation(-6F, 3F, -5F, -0.209F, 0F, 0F));
		PartDefinition pata_1 = cuerpo.addOrReplaceChild("pata_1", CubeListBuilder.create()
				.texOffs(52, 54).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F)
				.texOffs(64, 54).addBox(-2F, 9.5F, -3F, 4F, 1.5F, 5F),
			PartPose.offsetAndRotation(6F, 3F, -5F, -0.209F, 0F, 0F));
		PartDefinition pata_2 = cuerpo.addOrReplaceChild("pata_2", CubeListBuilder.create()
				.texOffs(52, 54).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F)
				.texOffs(64, 54).addBox(-2F, 9.5F, -3F, 4F, 1.5F, 5F),
			PartPose.offsetAndRotation(-7F, 3F, 1F, 0F, 0F, 0F));
		PartDefinition pata_3 = cuerpo.addOrReplaceChild("pata_3", CubeListBuilder.create()
				.texOffs(52, 54).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F)
				.texOffs(64, 54).addBox(-2F, 9.5F, -3F, 4F, 1.5F, 5F),
			PartPose.offsetAndRotation(7F, 3F, 1F, 0F, 0F, 0F));
		PartDefinition pata_4 = cuerpo.addOrReplaceChild("pata_4", CubeListBuilder.create()
				.texOffs(52, 54).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F)
				.texOffs(64, 54).addBox(-2F, 9.5F, -3F, 4F, 1.5F, 5F),
			PartPose.offsetAndRotation(-5F, 3F, 6F, 0.209F, 0F, 0F));
		PartDefinition pata_5 = cuerpo.addOrReplaceChild("pata_5", CubeListBuilder.create()
				.texOffs(52, 54).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F)
				.texOffs(64, 54).addBox(-2F, 9.5F, -3F, 4F, 1.5F, 5F),
			PartPose.offsetAndRotation(5F, 3F, 6F, 0.209F, 0F, 0F));
		PartDefinition brazo_0 = torso.addOrReplaceChild("brazo_0", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(-8F, -9F, -10F, -0.611F, 0.524F, 0F));
		PartDefinition ante_0 = brazo_0.addOrReplaceChild("ante_0", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		PartDefinition brazo_1 = torso.addOrReplaceChild("brazo_1", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(8F, -9F, -10F, -0.611F, -0.524F, 0F));
		PartDefinition ante_1 = brazo_1.addOrReplaceChild("ante_1", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		PartDefinition brazo_2 = torso.addOrReplaceChild("brazo_2", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(-9F, -2F, -11F, 0.175F, 0.785F, 0F));
		PartDefinition ante_2 = brazo_2.addOrReplaceChild("ante_2", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		PartDefinition brazo_3 = torso.addOrReplaceChild("brazo_3", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(9F, -2F, -11F, 0.175F, -0.785F, 0F));
		PartDefinition ante_3 = brazo_3.addOrReplaceChild("ante_3", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		PartDefinition brazo_4 = joroba.addOrReplaceChild("brazo_4", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(-4F, -2F, -7F, -0.785F, 0.262F, 0F));
		PartDefinition ante_4 = brazo_4.addOrReplaceChild("ante_4", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		PartDefinition brazo_5 = joroba.addOrReplaceChild("brazo_5", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(4F, -2F, -5F, -0.96F, -0.349F, 0F));
		PartDefinition ante_5 = brazo_5.addOrReplaceChild("ante_5", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		PartDefinition brazo_6 = torso.addOrReplaceChild("brazo_6", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(-9F, -6F, -2F, -0.349F, 1.92F, 0F));
		PartDefinition ante_6 = brazo_6.addOrReplaceChild("ante_6", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		PartDefinition brazo_7 = torso.addOrReplaceChild("brazo_7", CubeListBuilder.create()
				.texOffs(82, 54).addBox(-1.25F, -1.25F, -8F, 2.5F, 2.5F, 8F),
			PartPose.offsetAndRotation(9F, -6F, -2F, -0.349F, -1.92F, 0F));
		PartDefinition ante_7 = brazo_7.addOrReplaceChild("ante_7", CubeListBuilder.create()
				.texOffs(104, 54).addBox(-1F, -1F, -8F, 2F, 2F, 8F)
				.texOffs(0, 70).addBox(-2F, -0.75F, -12F, 4F, 1.5F, 4F)
				.texOffs(16, 70).addBox(-1.9F, -0.5F, -15.5F, 0.8F, 1F, 3.5F)
				.texOffs(16, 70).addBox(-0.9F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(0.1F, -0.5F, -16F, 0.8F, 1F, 4F)
				.texOffs(16, 70).addBox(1.1F, -0.5F, -15.2F, 0.8F, 1F, 3.2F)
				.texOffs(26, 70).addBox(2F, -0.5F, -11.5F, 1.6F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 0F, -8F, 0.524F, 0F, 0F));
		return LayerDefinition.create(malla, 128, 128);
	}
}
