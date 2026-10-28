package net.backrooms.evento.cliente.esqueleto;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.esqueleto.Figura;
import net.backrooms.evento.esqueleto.Pose;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Dibuja una Figura con su esqueleto articulado (ModeloArticulado) en la pose de ese fotograma. */
public class FiguraRenderer extends EntityRenderer<Figura, FiguraRenderer.Estado> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(BackroomsEvento.id("figura"), "main");
	private static final Identifier SIN_PIEL = BackroomsEvento.id("textures/entity/superviviente.png");

	private final ModeloArticulado modelo;

	public FiguraRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.modelo = new ModeloArticulado(ctx.bakeLayer(CAPA));
		this.shadowRadius = 0.4F;
	}

	public static class Estado extends EntityRenderState {
		public @Nullable Pose pose;
		public Identifier piel = SIN_PIEL;
		public @Nullable Identifier encima;
		public int ocultos;
		public float giro;
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(Figura f, Estado e, float parcial) {
		super.extractRenderState(f, e, parcial);
		e.pose = f.pose(parcial);
		e.piel = f.piel != null ? f.piel : SIN_PIEL;
		e.encima = f.encima;
		e.ocultos = f.ocultos;
		e.giro = f.getYRot(parcial);
	}

	@Override
	protected AABB getBoundingBoxForCulling(Figura f) {
		// tumbada o con los brazos arriba se sale de su caja
		return f.getBoundingBox().inflate(1.5);
	}

	@Override
	public void submit(Estado e, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - e.giro));
		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.translate(0.0F, -1.501F, 0.0F);
		nodos.submitModel(this.modelo, e, pose, RenderTypes.entityCutoutNoCull(e.piel), e.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
		if (e.encima != null) {
			nodos.submitModel(this.modelo, e, pose, RenderTypes.entityTranslucent(e.encima), e.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
		}
		pose.popPose();
		super.submit(e, pose, nodos, camara);
	}
}
