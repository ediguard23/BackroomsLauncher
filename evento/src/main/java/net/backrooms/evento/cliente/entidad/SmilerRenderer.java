package net.backrooms.evento.cliente.entidad;

import com.mojang.blaze3d.vertex.PoseStack;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.cliente.efectos.EfectosMundo;
import net.backrooms.evento.entidad.Smiler;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * Dibuja el Smiler: el cuerpo negro y, encima, los ojos y los dientes con
 * brillo propio (capa de ojos, como los de la arana). Ademas avisa a
 * EfectosMundo de donde esta su cara para que se vea en pleno apagon.
 */
public class SmilerRenderer extends MobRenderer<Smiler, LivingEntityRenderState, ModeloSmiler> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(BackroomsEvento.id("smiler"), "main");
	private static final Identifier TEXTURA = BackroomsEvento.id("textures/entity/smiler.png");
	private static final RenderType BRILLO = RenderTypes.eyes(BackroomsEvento.id("textures/entity/smiler_brillo.png"));

	public SmilerRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ModeloSmiler(ctx.bakeLayer(CAPA)), 0.0F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return BRILLO;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState estado) {
		return TEXTURA;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public void submit(LivingEntityRenderState estado, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		super.submit(estado, pose, nodos, camara);
		EfectosMundo.brillo(estado.x, estado.y + 1.9, estado.z, 0.75F);
	}
}
