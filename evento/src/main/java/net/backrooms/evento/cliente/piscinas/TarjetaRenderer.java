package net.backrooms.evento.cliente.piscinas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.objetos.Objetos;
import net.backrooms.evento.piscinas.TarjetaEntidad;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Una tarjeta tirada: de pie, flotando un palmo, girando despacio y con luz propia (se ve en
 * una sala a oscuras y en el fondo de la piscina, pero no a traves de las paredes).
 */
public class TarjetaRenderer extends EntityRenderer<TarjetaEntidad, TarjetaRenderer.Estado> {
	private final ItemModelResolver modelos;

	public TarjetaRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.modelos = ctx.getItemModelResolver();
		this.shadowRadius = 0.15F;
		this.shadowStrength = 0.4F;
	}

	public static class Estado extends EntityRenderState {
		public final ItemStackRenderState objeto = new ItemStackRenderState();
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(TarjetaEntidad t, Estado estado, float parcial) {
		super.extractRenderState(t, estado, parcial);
		int c = Mth.clamp(t.color(), 0, 2);
		this.modelos.updateForNonLiving(estado.objeto, new ItemStack(Objetos.TARJETAS[c]), ItemDisplayContext.GROUND, t);
	}

	@Override
	public void submit(Estado estado, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		pose.pushPose();
		float t = estado.ageInTicks;
		pose.translate(0.0F, 0.3F + Mth.sin(t * 0.08F) * 0.06F, 0.0F);
		pose.mulPose(Axis.YP.rotationDegrees(t * 2.5F));
		pose.scale(0.9F, 0.9F, 0.9F);
		estado.objeto.submit(pose, nodos, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
		super.submit(estado, pose, nodos, camara);
	}
}
