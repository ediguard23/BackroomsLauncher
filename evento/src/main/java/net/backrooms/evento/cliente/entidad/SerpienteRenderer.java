package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.entidad.Serpiente;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Dibuja la Serpiente con su modelo animado (ModeloSerpiente) y, encima, los ojos cian con
 * brillo propio (capa de ojos, como el Smiler): se ven aunque la sala este a oscuras. Sin
 * sombra: el cuerpo es largo y suele estar en el agua.
 */
public class SerpienteRenderer extends MobRenderer<Serpiente, EstadoSerpiente, ModeloSerpiente> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(BackroomsEvento.id("serpiente"), "main");
	private static final Identifier TEXTURA = BackroomsEvento.id("textures/entity/serpiente.png");
	private static final RenderType BRILLO = RenderTypes.eyes(BackroomsEvento.id("textures/entity/serpiente_brillo.png"));

	public SerpienteRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ModeloSerpiente(ctx.bakeLayer(CAPA)), 0.0F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return BRILLO;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(EstadoSerpiente estado) {
		return TEXTURA;
	}

	@Override
	public EstadoSerpiente createRenderState() {
		return new EstadoSerpiente();
	}

	@Override
	public void extractRenderState(Serpiente s, EstadoSerpiente estado, float parcial) {
		super.extractRenderState(s, estado, parcial);
		estado.caza = s.caza(parcial);
		estado.ataque = s.ataque(parcial);
		estado.agua = s.isInWater();
	}

	/** Mide unos 11 bloques de la cabeza a la cola (que va detras): que no desaparezca solo porque su caja quede fuera. */
	@Override
	protected AABB getBoundingBoxForCulling(Serpiente s) {
		return s.getBoundingBox().inflate(12.0, 1.5, 12.0);
	}
}
