package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Malla de serpiente. GENERADO por tools/texturas/entidades.js: no
 * editar a mano (se pierde al regenerar); cambiar alli y volver a generar.
 */
public final class MallaSerpiente {
	private MallaSerpiente() {
	}

	@SuppressWarnings("unused")
	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
		PartDefinition base = raiz.addOrReplaceChild("base", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8F, -7F, -10F, 16F, 14F, 12F),
			PartPose.offsetAndRotation(0F, 17F, 54F, 0F, 0F, 0F));
		PartDefinition tronco1 = base.addOrReplaceChild("tronco1", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8F, -7F, -12F, 16F, 14F, 12F)
				.texOffs(56, 0).addBox(-8F, -7F, -7F, 16F, 14F, 14F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -10F, -7F, 2F, 3F, 2F),
			PartPose.offsetAndRotation(0F, 0F, -10F, -0.436F, 0F, 0F));
		PartDefinition tronco2 = tronco1.addOrReplaceChild("tronco2", CubeListBuilder.create()
				.texOffs(124, 0).addBox(-7.5F, -7F, -12F, 15F, 14F, 12F)
				.texOffs(178, 0).addBox(-7.5F, -7F, -7F, 15F, 14F, 14F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -10F, -7F, 2F, 3F, 2F)
				.texOffs(0, 28).addBox(-7.5F, -7F, -7.5F, 15F, 14F, 3F, new CubeDeformation(0.35F)),
			PartPose.offsetAndRotation(0F, 0F, -12F, -0.524F, 0F, 0F));
		PartDefinition tronco3 = tronco2.addOrReplaceChild("tronco3", CubeListBuilder.create()
				.texOffs(36, 28).addBox(-7F, -6.5F, -12F, 14F, 13F, 12F)
				.texOffs(88, 28).addBox(-7F, -6.5F, -6.5F, 14F, 13F, 13F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -9.5F, -7F, 2F, 3F, 2F),
			PartPose.offsetAndRotation(0F, 0F, -12F, -0.349F, 0F, 0F));
		PartDefinition cuello1 = tronco3.addOrReplaceChild("cuello1", CubeListBuilder.create()
				.texOffs(142, 28).addBox(-7F, -6.5F, -11F, 14F, 13F, 11F)
				.texOffs(88, 28).addBox(-7F, -6.5F, -6.5F, 14F, 13F, 13F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -9.5F, -6.5F, 2F, 3F, 2F)
				.texOffs(192, 28).addBox(-7F, -6.5F, -7F, 14F, 13F, 3F, new CubeDeformation(0.35F)),
			PartPose.offsetAndRotation(0F, 0F, -12F, 0.262F, 0F, 0F));
		PartDefinition cuello2 = cuello1.addOrReplaceChild("cuello2", CubeListBuilder.create()
				.texOffs(0, 54).addBox(-6.5F, -6F, -10F, 13F, 12F, 10F)
				.texOffs(46, 54).addBox(-6.5F, -6F, -6F, 13F, 12F, 12F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -9F, -6F, 2F, 3F, 2F),
			PartPose.offsetAndRotation(0F, 0F, -11F, 0.524F, 0F, 0F));
		PartDefinition cuello3 = cuello2.addOrReplaceChild("cuello3", CubeListBuilder.create()
				.texOffs(96, 54).addBox(-6F, -5.5F, -8F, 12F, 11F, 8F)
				.texOffs(136, 54).addBox(-6F, -5.5F, -5.5F, 12F, 11F, 11F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -8.5F, -5F, 2F, 3F, 2F),
			PartPose.offsetAndRotation(0F, 0F, -10F, 0.524F, 0F, 0F));
		PartDefinition costillas2 = cuello2.addOrReplaceChild("costillas2", CubeListBuilder.create()
				.texOffs(182, 54).addBox(-7.5F, 0F, -2F, 1F, 4F, 1F)
				.texOffs(182, 54).addBox(6.5F, 0F, -2F, 1F, 4F, 1F)
				.texOffs(186, 54).addBox(-7.5F, 3.5F, -2F, 15F, 1F, 1F)
				.texOffs(182, 54).addBox(-7.5F, 0F, -5F, 1F, 4F, 1F)
				.texOffs(182, 54).addBox(6.5F, 0F, -5F, 1F, 4F, 1F)
				.texOffs(186, 54).addBox(-7.5F, 3.5F, -5F, 15F, 1F, 1F)
				.texOffs(182, 54).addBox(-7.5F, 0F, -8F, 1F, 4F, 1F)
				.texOffs(182, 54).addBox(6.5F, 0F, -8F, 1F, 4F, 1F)
				.texOffs(186, 54).addBox(-7.5F, 3.5F, -8F, 15F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 4.5F, 0F, 0F, 0F, 0F));
		PartDefinition costillas3 = cuello3.addOrReplaceChild("costillas3", CubeListBuilder.create()
				.texOffs(182, 54).addBox(-7F, 0F, -2F, 1F, 4F, 1F)
				.texOffs(182, 54).addBox(6F, 0F, -2F, 1F, 4F, 1F)
				.texOffs(218, 54).addBox(-7F, 3.5F, -2F, 14F, 1F, 1F)
				.texOffs(182, 54).addBox(-7F, 0F, -5F, 1F, 4F, 1F)
				.texOffs(182, 54).addBox(6F, 0F, -5F, 1F, 4F, 1F)
				.texOffs(218, 54).addBox(-7F, 3.5F, -5F, 14F, 1F, 1F),
			PartPose.offsetAndRotation(0F, 4F, 0F, 0F, 0F, 0F));
		PartDefinition craneo = cuello3.addOrReplaceChild("craneo", CubeListBuilder.create()
				.texOffs(0, 78).addBox(-12F, -14F, -20F, 24F, 16F, 20F)
				.texOffs(88, 78).addBox(-9F, -15.5F, -17F, 18F, 2F, 14F)
				.texOffs(152, 78).addBox(-9F, 2F, -19F, 18F, 4F, 14F)
				.texOffs(216, 78).addBox(-13F, -6F, -17F, 2F, 6F, 10F)
				.texOffs(216, 78).addBox(11F, -6F, -17F, 2F, 6F, 10F)
				.texOffs(0, 114).addBox(-8F, 6F, -18.5F, 16F, 2F, 1F)
				.texOffs(34, 114).addBox(-8.5F, 6F, -18F, 1F, 2F, 11F)
				.texOffs(34, 114).addBox(7.5F, 6F, -18F, 1F, 2F, 11F)
				.texOffs(58, 114).addBox(-12F, -14F, -21.5F, 24F, 3.5F, 1.5F)
				.texOffs(110, 114).addBox(-12F, -4F, -21F, 10F, 2F, 1F)
				.texOffs(110, 114).addBox(2F, -4F, -21F, 10F, 2F, 1F)
				.texOffs(132, 114).addBox(-2F, -10.5F, -21F, 4F, 6.5F, 1F)
				.texOffs(142, 114).addBox(-12F, -10.5F, -21F, 3F, 6.5F, 1F)
				.texOffs(142, 114).addBox(9F, -10.5F, -21F, 3F, 6.5F, 1F)
				.texOffs(150, 114).addBox(-6.5F, 6F, -18.9F, 1F, 4F, 1F)
				.texOffs(154, 114).addBox(-3F, 6F, -18.9F, 1F, 3F, 1F)
				.texOffs(154, 114).addBox(2F, 6F, -18.9F, 1F, 3F, 1F)
				.texOffs(150, 114).addBox(5.5F, 6F, -18.9F, 1F, 4F, 1F),
			PartPose.offsetAndRotation(0F, -1F, -6F, 0.349F, 0F, 0F));
		PartDefinition mandibula = craneo.addOrReplaceChild("mandibula", CubeListBuilder.create()
				.texOffs(158, 114).addBox(-8F, 0F, -15F, 16F, 3F, 15F)
				.texOffs(220, 114).addBox(-7.5F, -2F, -14.5F, 15F, 2F, 1F)
				.texOffs(0, 132).addBox(-7.5F, -2F, -13.5F, 1F, 2F, 10F)
				.texOffs(0, 132).addBox(6.5F, -2F, -13.5F, 1F, 2F, 10F)
				.texOffs(154, 114).addBox(-5.5F, -4F, -14.9F, 1F, 3F, 1F)
				.texOffs(22, 132).addBox(-2F, -4F, -14.9F, 1F, 2F, 1F)
				.texOffs(22, 132).addBox(1F, -4F, -14.9F, 1F, 2F, 1F)
				.texOffs(154, 114).addBox(4.5F, -4F, -14.9F, 1F, 3F, 1F),
			PartPose.offsetAndRotation(0F, 5F, -4F, 0.384F, 0F, 0F));
		PartDefinition humero_izq_cuello1 = cuello1.addOrReplaceChild("humero_izq_cuello1", CubeListBuilder.create()
				.texOffs(26, 132).addBox(-1F, 0F, -1F, 2F, 19F, 2F)
				.texOffs(34, 132).addBox(-1.5F, -1F, -1.5F, 3F, 3F, 3F),
			PartPose.offsetAndRotation(7F, 3F, -5F, -0.175F, 0F, -0.524F));
		PartDefinition cubito_izq_cuello1 = humero_izq_cuello1.addOrReplaceChild("cubito_izq_cuello1", CubeListBuilder.create()
				.texOffs(46, 132).addBox(-1.5F, 0F, -0.5F, 1F, 19F, 1F)
				.texOffs(46, 132).addBox(0.5F, 0F, -0.5F, 1F, 19F, 1F)
				.texOffs(50, 132).addBox(-1.5F, -1F, -1F, 3F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 19F, 0F, -0.733F, 0F, 0F));
		PartDefinition mano_izq_cuello1 = cubito_izq_cuello1.addOrReplaceChild("mano_izq_cuello1", CubeListBuilder.create()
				.texOffs(60, 132).addBox(-2F, 0F, -1F, 4F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 19F, 0F, 0.593F, 0F, 0F));
		PartDefinition falange_izq_cuello1_0 = mano_izq_cuello1.addOrReplaceChild("falange_izq_cuello1_0", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-1.5F, 1.5F, 0F, 0.489F, 0F, -0.367F));
		PartDefinition falange_izq_cuello1_1 = mano_izq_cuello1.addOrReplaceChild("falange_izq_cuello1_1", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-0.5F, 1.5F, 0F, 0.489F, 0F, -0.122F));
		PartDefinition falange_izq_cuello1_2 = mano_izq_cuello1.addOrReplaceChild("falange_izq_cuello1_2", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(0.5F, 1.5F, 0F, 0.489F, 0F, 0.122F));
		PartDefinition falange_izq_cuello1_3 = mano_izq_cuello1.addOrReplaceChild("falange_izq_cuello1_3", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(1.5F, 1.5F, 0F, 0.489F, 0F, 0.367F));
		PartDefinition humero_der_cuello1 = cuello1.addOrReplaceChild("humero_der_cuello1", CubeListBuilder.create()
				.texOffs(26, 132).addBox(-1F, 0F, -1F, 2F, 19F, 2F)
				.texOffs(34, 132).addBox(-1.5F, -1F, -1.5F, 3F, 3F, 3F),
			PartPose.offsetAndRotation(-7F, 3F, -5F, 0.105F, 0F, 0.559F));
		PartDefinition cubito_der_cuello1 = humero_der_cuello1.addOrReplaceChild("cubito_der_cuello1", CubeListBuilder.create()
				.texOffs(46, 132).addBox(-1.5F, 0F, -0.5F, 1F, 19F, 1F)
				.texOffs(46, 132).addBox(0.5F, 0F, -0.5F, 1F, 19F, 1F)
				.texOffs(50, 132).addBox(-1.5F, -1F, -1F, 3F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 19F, 0F, -0.524F, 0F, 0F));
		PartDefinition mano_der_cuello1 = cubito_der_cuello1.addOrReplaceChild("mano_der_cuello1", CubeListBuilder.create()
				.texOffs(60, 132).addBox(-2F, 0F, -1F, 4F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 19F, 0F, 0.698F, 0F, 0F));
		PartDefinition falange_der_cuello1_0 = mano_der_cuello1.addOrReplaceChild("falange_der_cuello1_0", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-1.5F, 1.5F, 0F, 0.489F, 0F, 0.367F));
		PartDefinition falange_der_cuello1_1 = mano_der_cuello1.addOrReplaceChild("falange_der_cuello1_1", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-0.5F, 1.5F, 0F, 0.489F, 0F, 0.122F));
		PartDefinition falange_der_cuello1_2 = mano_der_cuello1.addOrReplaceChild("falange_der_cuello1_2", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(0.5F, 1.5F, 0F, 0.489F, 0F, -0.122F));
		PartDefinition falange_der_cuello1_3 = mano_der_cuello1.addOrReplaceChild("falange_der_cuello1_3", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(1.5F, 1.5F, 0F, 0.489F, 0F, -0.367F));
		PartDefinition humero_izq_tronco1 = tronco1.addOrReplaceChild("humero_izq_tronco1", CubeListBuilder.create()
				.texOffs(80, 132).addBox(-1F, 0F, -1F, 2F, 13F, 2F)
				.texOffs(34, 132).addBox(-1.5F, -1F, -1.5F, 3F, 3F, 3F),
			PartPose.offsetAndRotation(8F, 4F, -6F, 0.698F, 0F, -0.593F));
		PartDefinition cubito_izq_tronco1 = humero_izq_tronco1.addOrReplaceChild("cubito_izq_tronco1", CubeListBuilder.create()
				.texOffs(88, 132).addBox(-1.5F, 0F, -0.5F, 1F, 13F, 1F)
				.texOffs(88, 132).addBox(0.5F, 0F, -0.5F, 1F, 13F, 1F)
				.texOffs(50, 132).addBox(-1.5F, -1F, -1F, 3F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 13F, 0F, -0.489F, 0F, 0F));
		PartDefinition mano_izq_tronco1 = cubito_izq_tronco1.addOrReplaceChild("mano_izq_tronco1", CubeListBuilder.create()
				.texOffs(60, 132).addBox(-2F, 0F, -1F, 4F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 13F, 0F, 0.524F, 0F, 0F));
		PartDefinition falange_izq_tronco1_0 = mano_izq_tronco1.addOrReplaceChild("falange_izq_tronco1_0", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-1.5F, 1.5F, 0F, 0.489F, 0F, -0.367F));
		PartDefinition falange_izq_tronco1_1 = mano_izq_tronco1.addOrReplaceChild("falange_izq_tronco1_1", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-0.5F, 1.5F, 0F, 0.489F, 0F, -0.122F));
		PartDefinition falange_izq_tronco1_2 = mano_izq_tronco1.addOrReplaceChild("falange_izq_tronco1_2", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(0.5F, 1.5F, 0F, 0.489F, 0F, 0.122F));
		PartDefinition falange_izq_tronco1_3 = mano_izq_tronco1.addOrReplaceChild("falange_izq_tronco1_3", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(1.5F, 1.5F, 0F, 0.489F, 0F, 0.367F));
		PartDefinition humero_der_tronco1 = tronco1.addOrReplaceChild("humero_der_tronco1", CubeListBuilder.create()
				.texOffs(80, 132).addBox(-1F, 0F, -1F, 2F, 13F, 2F)
				.texOffs(34, 132).addBox(-1.5F, -1F, -1.5F, 3F, 3F, 3F),
			PartPose.offsetAndRotation(-8F, 4F, -6F, 0.698F, 0F, 0.593F));
		PartDefinition cubito_der_tronco1 = humero_der_tronco1.addOrReplaceChild("cubito_der_tronco1", CubeListBuilder.create()
				.texOffs(88, 132).addBox(-1.5F, 0F, -0.5F, 1F, 13F, 1F)
				.texOffs(88, 132).addBox(0.5F, 0F, -0.5F, 1F, 13F, 1F)
				.texOffs(50, 132).addBox(-1.5F, -1F, -1F, 3F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 13F, 0F, -0.489F, 0F, 0F));
		PartDefinition mano_der_tronco1 = cubito_der_tronco1.addOrReplaceChild("mano_der_tronco1", CubeListBuilder.create()
				.texOffs(60, 132).addBox(-2F, 0F, -1F, 4F, 2F, 2F),
			PartPose.offsetAndRotation(0F, 13F, 0F, 0.524F, 0F, 0F));
		PartDefinition falange_der_tronco1_0 = mano_der_tronco1.addOrReplaceChild("falange_der_tronco1_0", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-1.5F, 1.5F, 0F, 0.489F, 0F, 0.367F));
		PartDefinition falange_der_tronco1_1 = mano_der_tronco1.addOrReplaceChild("falange_der_tronco1_1", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(-0.5F, 1.5F, 0F, 0.489F, 0F, 0.122F));
		PartDefinition falange_der_tronco1_2 = mano_der_tronco1.addOrReplaceChild("falange_der_tronco1_2", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(0.5F, 1.5F, 0F, 0.489F, 0F, -0.122F));
		PartDefinition falange_der_tronco1_3 = mano_der_tronco1.addOrReplaceChild("falange_der_tronco1_3", CubeListBuilder.create()
				.texOffs(72, 132).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F)
				.texOffs(76, 132).addBox(-0.5F, 4.5F, -1.5F, 1F, 6F, 1F),
			PartPose.offsetAndRotation(1.5F, 1.5F, 0F, 0.489F, 0F, -0.367F));
		PartDefinition cola1 = base.addOrReplaceChild("cola1", CubeListBuilder.create()
				.texOffs(92, 132).addBox(-7.5F, -6F, 0F, 15F, 13F, 12F)
				.texOffs(146, 132).addBox(-7.5F, -6F, -7.5F, 15F, 13F, 15F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -8.5F, 5F, 2F, 2.5F, 2F),
			PartPose.offsetAndRotation(0F, 0F, 2F, 0F, 0.436F, 0F));
		PartDefinition cola2 = cola1.addOrReplaceChild("cola2", CubeListBuilder.create()
				.texOffs(0, 160).addBox(-7F, -5F, 0F, 14F, 12F, 12F)
				.texOffs(52, 160).addBox(-7F, -5F, -7F, 14F, 12F, 14F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -7.5F, 5F, 2F, 2.5F, 2F)
				.texOffs(108, 160).addBox(-7F, -5F, 4.5F, 14F, 12F, 3F, new CubeDeformation(0.35F)),
			PartPose.offsetAndRotation(0F, 0F, 12F, 0F, 0.436F, 0F));
		PartDefinition cola3 = cola2.addOrReplaceChild("cola3", CubeListBuilder.create()
				.texOffs(142, 160).addBox(-6.5F, -4F, 0F, 13F, 11F, 12F)
				.texOffs(192, 160).addBox(-6.5F, -4F, -6.5F, 13F, 11F, 13F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -6.5F, 5F, 2F, 2.5F, 2F),
			PartPose.offsetAndRotation(0F, 0F, 12F, 0F, -0.698F, 0F));
		PartDefinition cola4 = cola3.addOrReplaceChild("cola4", CubeListBuilder.create()
				.texOffs(0, 186).addBox(-6F, -3F, 0F, 12F, 10F, 12F)
				.texOffs(0, 186).addBox(-6F, -3F, -6F, 12F, 10F, 12F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -5.5F, 5F, 2F, 2.5F, 2F),
			PartPose.offsetAndRotation(0F, 0F, 12F, 0F, -0.785F, 0F));
		PartDefinition cola5 = cola4.addOrReplaceChild("cola5", CubeListBuilder.create()
				.texOffs(48, 186).addBox(-5.5F, -2F, 0F, 11F, 9F, 11F)
				.texOffs(48, 186).addBox(-5.5F, -2F, -5.5F, 11F, 9F, 11F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -4.5F, 4.5F, 2F, 2.5F, 2F)
				.texOffs(92, 186).addBox(-5.5F, -2F, 4F, 11F, 9F, 3F, new CubeDeformation(0.35F)),
			PartPose.offsetAndRotation(0F, 0F, 12F, 0F, -0.698F, 0F));
		PartDefinition cola6 = cola5.addOrReplaceChild("cola6", CubeListBuilder.create()
				.texOffs(120, 186).addBox(-5F, -1F, 0F, 10F, 8F, 11F)
				.texOffs(162, 186).addBox(-5F, -1F, -5F, 10F, 8F, 10F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -3.5F, 4.5F, 2F, 2.5F, 2F),
			PartPose.offsetAndRotation(0F, 0F, 11F, 0F, 0.611F, 0F));
		PartDefinition cola7 = cola6.addOrReplaceChild("cola7", CubeListBuilder.create()
				.texOffs(202, 186).addBox(-4.5F, 0F, 0F, 9F, 7F, 10F)
				.texOffs(0, 208).addBox(-4.5F, 0F, -4.5F, 9F, 7F, 9F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -2.5F, 4F, 2F, 2.5F, 2F),
			PartPose.offsetAndRotation(0F, 0F, 11F, 0F, 0.785F, 0F));
		PartDefinition cola8 = cola7.addOrReplaceChild("cola8", CubeListBuilder.create()
				.texOffs(36, 208).addBox(-4F, 1F, 0F, 8F, 6F, 10F)
				.texOffs(72, 208).addBox(-4F, 1F, -4F, 8F, 6F, 8F, new CubeDeformation(-0.3F))
				.texOffs(116, 0).addBox(-1F, -1.5F, 4F, 2F, 2.5F, 2F)
				.texOffs(104, 208).addBox(-4F, 1F, 3.5F, 8F, 6F, 3F, new CubeDeformation(0.35F)),
			PartPose.offsetAndRotation(0F, 0F, 10F, 0F, 0.785F, 0F));
		PartDefinition cola9 = cola8.addOrReplaceChild("cola9", CubeListBuilder.create()
				.texOffs(126, 208).addBox(-3F, 2F, 0F, 6F, 5F, 9F)
				.texOffs(156, 208).addBox(-3F, 2F, -3F, 6F, 5F, 6F, new CubeDeformation(-0.3F)),
			PartPose.offsetAndRotation(0F, 0F, 10F, 0F, 0.524F, 0F));
		PartDefinition cola10 = cola9.addOrReplaceChild("cola10", CubeListBuilder.create()
				.texOffs(180, 208).addBox(-2.5F, 3F, 0F, 5F, 4F, 9F)
				.texOffs(208, 208).addBox(-2.5F, 3F, -2.5F, 5F, 4F, 5F, new CubeDeformation(-0.3F)),
			PartPose.offsetAndRotation(0F, 0F, 9F, 0F, 0.349F, 0F));
		return LayerDefinition.create(malla, 256, 256);
	}
}
