package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.entidad.Bacteria;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Dibuja la Bacteria con su modelo animado. */
public class BacteriaRenderer extends MobRenderer<Bacteria, EstadoBacteria, ModeloBacteria> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(BackroomsEvento.id("bacteria"), "main");
	private static final Identifier TEXTURA = BackroomsEvento.id("textures/entity/bacteria.png");

	public BacteriaRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ModeloBacteria(ctx.bakeLayer(CAPA)), 0.6F);
		this.shadowStrength = 0.8F;
	}

	@Override
	public Identifier getTextureLocation(EstadoBacteria estado) {
		return TEXTURA;
	}

	@Override
	public EstadoBacteria createRenderState() {
		return new EstadoBacteria();
	}

	@Override
	public void extractRenderState(Bacteria b, EstadoBacteria estado, float parcial) {
		super.extractRenderState(b, estado, parcial);
		estado.caza = b.caza(parcial);
		estado.agarre = b.agarre(parcial);
	}
}
