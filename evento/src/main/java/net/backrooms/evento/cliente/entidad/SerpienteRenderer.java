package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.entidad.Serpiente;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Dibuja la Serpiente: el cuerpo de azulejos y, encima, los ojos cian con brillo
 * propio (capa de ojos, como el Smiler): se ven aunque la sala este a oscuras.
 * Sin sombra: el cuerpo es largo y suele estar en el agua.
 */
public class SerpienteRenderer extends MobRenderer<Serpiente, LivingEntityRenderState, ModeloSerpiente> {
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
	public Identifier getTextureLocation(LivingEntityRenderState estado) {
		return TEXTURA;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	/** El modelo mide unos 11 bloques de la cabeza a la cola: que no desaparezca solo porque su caja quede fuera de la vista. */
	@Override
	protected AABB getBoundingBoxForCulling(Serpiente s) {
		return s.getBoundingBox().inflate(9.0, 1.0, 9.0);
	}
}
