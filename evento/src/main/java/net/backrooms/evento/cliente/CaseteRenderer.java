package net.backrooms.evento.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.mision.CaseteEntidad;
import net.backrooms.evento.objetos.Objetos;
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

/** El casete tumbado en la moqueta, con el giro que le toco al soltarlo. */
public class CaseteRenderer extends EntityRenderer<CaseteEntidad, CaseteRenderer.Estado> {
	private static final ItemStack CASETE = new ItemStack(Objetos.CASETE);
	private final ItemModelResolver modelos;

	public CaseteRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.modelos = ctx.getItemModelResolver();
		this.shadowRadius = 0.2F;
		this.shadowStrength = 0.6F;
	}

	public static class Estado extends EntityRenderState {
		public final ItemStackRenderState objeto = new ItemStackRenderState();
		public float giro;
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(CaseteEntidad casete, Estado estado, float parcial) {
		super.extractRenderState(casete, estado, parcial);
		this.modelos.updateForNonLiving(estado.objeto, CASETE, ItemDisplayContext.FIXED, casete);
		estado.giro = casete.getYRot();
	}

	@Override
	public void submit(Estado estado, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		pose.pushPose();
		pose.translate(0.0F, 0.02F, 0.0F);
		pose.mulPose(Axis.YP.rotationDegrees(estado.giro));
		pose.mulPose(Axis.XP.rotationDegrees(90.0F));
		pose.scale(0.55F, 0.55F, 0.55F);
		estado.objeto.submit(pose, nodos, estado.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
		super.submit(estado, pose, nodos, camara);
	}
}
