package net.backrooms.evento.cliente.entidad;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.piscinas.CadaverEntidad;
import net.backrooms.evento.piscinas.PlanoPiscinas;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Los cuerpos de las Piscinas: un modelo de jugador con una de las pieles de
 * tools/texturas/cadaveres.js, tumbado, sentado o colgado (con su cuerda) segun la postura.
 */
public class CadaverRenderer extends EntityRenderer<CadaverEntidad, CadaverRenderer.Estado> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(BackroomsEvento.id("cadaver"), "main");
	private static final Identifier[] PIELES = new Identifier[PlanoPiscinas.PIELES];

	static {
		for (int i = 0; i < PIELES.length; i++) {
			PIELES[i] = BackroomsEvento.id("textures/entity/cadaver_" + i + ".png");
		}
	}

	private final ModeloCadaver modelo;

	public CadaverRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.modelo = new ModeloCadaver(ctx.bakeLayer(CAPA));
		this.shadowRadius = 0.0F;
	}

	public static class Estado extends EntityRenderState {
		public int piel;
		public int postura;
		public int mutilado;
		public float cuerda;
		public float giro;
		/** 0..1 fijo por cuerpo: que no esten todos en la misma postura exacta. */
		public float azar;
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(CadaverEntidad c, Estado e, float parcial) {
		super.extractRenderState(c, e, parcial);
		e.piel = Mth.clamp(c.piel(), 0, PIELES.length - 1);
		e.postura = c.postura();
		e.mutilado = c.mutilado();
		e.cuerda = c.cuerda();
		e.giro = c.getYRot();
		e.azar = ((Mth.floor(c.getX()) * 73856093 ^ Mth.floor(c.getZ()) * 83492791) & 1023) / 1023.0F;
	}

	@Override
	protected AABB getBoundingBoxForCulling(CadaverEntidad c) {
		// los colgados ocupan desde el techo hasta casi el suelo
		return c.getBoundingBox().inflate(1.5, 1.5, 1.5).expandTowards(0, -(c.cuerda() + 2.5), 0);
	}

	@Override
	public void submit(Estado e, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - e.giro));
		switch (e.postura) {
			case PlanoPiscinas.BOCA_ARRIBA -> {
				pose.translate(0.0F, 0.14F, 0.0F);
				pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
			}
			case PlanoPiscinas.BOCA_ABAJO -> {
				pose.translate(0.0F, 0.14F, 0.0F);
				pose.mulPose(Axis.XP.rotationDegrees(90.0F));
			}
			case PlanoPiscinas.SENTADO -> pose.translate(0.0F, -0.62F, 0.0F);
			case PlanoPiscinas.COLGADO_PIES -> {
				// la cuerda baja del techo hasta los pies; el cuerpo, del reves
				pose.translate(0.0F, -e.cuerda, 0.0F);
				pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
			}
			case PlanoPiscinas.COLGADO_CUELLO -> pose.translate(0.0F, -e.cuerda - 2.0F, 0.0F);
			default -> {
			}
		}
		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.translate(0.0F, -1.501F, 0.0F);
		nodos.submitModel(this.modelo, e, pose, RenderTypes.entityCutoutNoCull(PIELES[e.piel]), e.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
		pose.popPose();
		super.submit(e, pose, nodos, camara);
	}
}
