package net.backrooms.evento.cliente.fiesta;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.fiesta.FichaEntidad;
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

/**
 * Una moneda de la Fiesta: tumbada encima de la mesa, cada una girada a su manera, con la luz del
 * sitio. Quien ya la cogio no la ve.
 */
public class FichaRenderer extends EntityRenderer<FichaEntidad, FichaRenderer.Estado> {
	private final ItemModelResolver modelos;

	public FichaRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.modelos = ctx.getItemModelResolver();
		this.shadowRadius = 0.0F;
		this.shadowStrength = 0.4F;
	}

	public static class Estado extends EntityRenderState {
		public final ItemStackRenderState objeto = new ItemStackRenderState();
		public boolean oculta;
		public float tiempo;
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(FichaEntidad f, Estado e, float parcial) {
		super.extractRenderState(f, e, parcial);
		this.modelos.updateForNonLiving(e.objeto, new ItemStack(f.objeto()), ItemDisplayContext.GROUND, f);
		e.oculta = FiestaCliente.cogida(f.id());
		e.tiempo = f.id() * 47.0F;
	}

	@Override
	public void submit(Estado e, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		if (e.oculta) {
			return;
		}
		pose.pushPose();
		pose.translate(0.0F, 0.02F, 0.0F);
		pose.mulPose(Axis.YP.rotationDegrees(e.tiempo));
		pose.mulPose(Axis.XP.rotationDegrees(90.0F));
		pose.scale(0.7F, 0.7F, 0.7F);
		e.objeto.submit(pose, nodos, e.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
		super.submit(e, pose, nodos, camara);
	}
}
