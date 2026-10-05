package net.backrooms.evento.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.supervivencia.ComidaEntidad;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** La comida en el suelo: la botella de agua de pie; galletas y pizza tumbadas. */
public class ComidaRenderer extends EntityRenderer<ComidaEntidad, ComidaRenderer.Estado> {
	private final ItemModelResolver modelos;

	public ComidaRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.modelos = ctx.getItemModelResolver();
		this.shadowRadius = 0.2F;
		this.shadowStrength = 0.5F;
	}

	public static class Estado extends EntityRenderState {
		public final ItemStackRenderState objeto = new ItemStackRenderState();
		public float giro;
		public boolean dePie;
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(ComidaEntidad c, Estado e, float parcial) {
		super.extractRenderState(c, e, parcial);
		this.modelos.updateForNonLiving(e.objeto, new ItemStack(c.objeto()), ItemDisplayContext.FIXED, c);
		e.giro = c.getYRot();
		e.dePie = c.tipo() == 1;
	}

	@Override
	public void submit(Estado e, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(e.giro));
		if (e.dePie) {
			pose.translate(0.0F, 0.28F, 0.0F);
			pose.scale(0.55F, 0.55F, 0.55F);
		} else {
			pose.translate(0.0F, 0.02F, 0.0F);
			pose.mulPose(Axis.XP.rotationDegrees(90.0F));
			pose.scale(0.5F, 0.5F, 0.5F);
		}
		e.objeto.submit(pose, nodos, e.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
		super.submit(e, pose, nodos, camara);
	}
}
