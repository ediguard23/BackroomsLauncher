package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.fiesta.Anfitrion;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Dibuja al Anfitrion con su modelo (ModeloAnfitrion) y, encima, el brillo de los ojos de boton
 * (capa de ojos): en el apagon solo se ven dos puntitos blancos acercandose.
 */
public class AnfitrionRenderer extends MobRenderer<Anfitrion, EstadoAnfitrion, ModeloAnfitrion> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(BackroomsEvento.id("anfitrion"), "main");
	private static final Identifier TEXTURA = BackroomsEvento.id("textures/entity/anfitrion.png");
	private static final RenderType BRILLO = RenderTypes.eyes(BackroomsEvento.id("textures/entity/anfitrion_brillo.png"));

	public AnfitrionRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ModeloAnfitrion(ctx.bakeLayer(CAPA)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return BRILLO;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(EstadoAnfitrion estado) {
		return TEXTURA;
	}

	@Override
	public EstadoAnfitrion createRenderState() {
		return new EstadoAnfitrion();
	}

	@Override
	public void extractRenderState(Anfitrion a, EstadoAnfitrion estado, float parcial) {
		super.extractRenderState(a, estado, parcial);
		estado.caza = a.caza(parcial);
		estado.abrazando = a.abrazando();
	}
}
