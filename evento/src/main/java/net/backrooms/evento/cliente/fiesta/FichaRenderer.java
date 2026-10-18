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
 * Una ficha de la Fiesta: la moneda o la tira de tickets flotando a media altura, girando y
 * subiendo y bajando. Quien ya la cogio no la ve.
 */
public class FichaRenderer extends EntityRenderer<FichaEntidad, FichaRenderer.Estado> {
	private final ItemModelResolver modelos;

	public FichaRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.modelos = ctx.getItemModelResolver();
		this.shadowRadius = 0.2F;
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
		e.tiempo = f.tickCount + parcial + f.id() * 13.0F;
	}

	@Override
	public void submit(Estado e, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		if (e.oculta) {
			return;
		}
		pose.pushPose();
		pose.translate(0.0F, 0.45F + (float) Math.sin(e.tiempo * 0.09F) * 0.08F, 0.0F);
		pose.mulPose(Axis.YP.rotationDegrees(e.tiempo * 4.0F));
		pose.scale(1.3F, 1.3F, 1.3F);
		e.objeto.submit(pose, nodos, 15728880, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
		super.submit(e, pose, nodos, camara);
	}
}
