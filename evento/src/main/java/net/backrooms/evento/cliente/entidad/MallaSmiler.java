package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Malla de smiler. GENERADO por tools/texturas/entidades.js: no
 * editar a mano (se pierde al regenerar); cambiar alli y volver a generar.
 */
public final class MallaSmiler {
	private MallaSmiler() {
	}

	@SuppressWarnings("unused")
	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
		PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4F, -14F, -2F, 8F, 16F, 4F)
				.texOffs(24, 0).addBox(-3F, 2F, -1.5F, 6F, 6F, 3F)
				.texOffs(42, 0).addBox(-2F, 8F, -1F, 4F, 5F, 2F),
			PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
		PartDefinition cabeza = cuerpo.addOrReplaceChild("cabeza", CubeListBuilder.create()
				.texOffs(0, 20).addBox(-7F, -12F, -4F, 14F, 12F, 7F),
			PartPose.offsetAndRotation(0F, -14F, 0F, 0F, 0F, 0F));
		PartDefinition brazo_izq = cuerpo.addOrReplaceChild("brazo_izq", CubeListBuilder.create()
				.texOffs(42, 20).addBox(0F, 0F, -1F, 2F, 20F, 2F),
			PartPose.offsetAndRotation(4.5F, -13F, 0F, 0F, 0F, -0.209F));
		PartDefinition brazo_der = cuerpo.addOrReplaceChild("brazo_der", CubeListBuilder.create()
				.texOffs(50, 20).addBox(-2F, 0F, -1F, 2F, 20F, 2F),
			PartPose.offsetAndRotation(-4.5F, -13F, 0F, 0F, 0F, 0.209F));
		return LayerDefinition.create(malla, 64, 64);
	}
}
