package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.estacion.Amasijo;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Dibuja al Amasijo (ModeloAmasijo) y, encima, el brillo de sus ojillos y de los dientes: en un
 * pasillo sin corriente solo se ven cinco puntos amarillos y una sonrisa que se acerca.
 */
public class AmasijoRenderer extends MobRenderer<Amasijo, EstadoAmasijo, ModeloAmasijo> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(BackroomsEvento.id("amasijo"), "main");
	private static final Identifier TEXTURA = BackroomsEvento.id("textures/entity/amasijo.png");
	private static final RenderType BRILLO = RenderTypes.eyes(BackroomsEvento.id("textures/entity/amasijo_brillo.png"));

	/**
	 * En el final de la Estacion (SalidaEstacionCliente) solo se ve el Amasijo de la cinematica: los de
	 * verdad del piso 3 que anden cerca del ascensor se esconden para este jugador.
	 */
	@Override
	public boolean shouldRender(Amasijo a, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
		if (net.backrooms.evento.cliente.cinematica.SalidaEstacionCliente.activa() && !net.backrooms.evento.cliente.cinematica.SalidaEstacionCliente.esSuyo(a)) {
			return false;
		}
		return super.shouldRender(a, f, x, y, z);
	}

	public AmasijoRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ModeloAmasijo(ctx.bakeLayer(CAPA)), 0.7F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return BRILLO;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(EstadoAmasijo estado) {
		return TEXTURA;
	}

	@Override
	public EstadoAmasijo createRenderState() {
		return new EstadoAmasijo();
	}

	@Override
	public void extractRenderState(Amasijo a, EstadoAmasijo estado, float parcial) {
		super.extractRenderState(a, estado, parcial);
		estado.caza = a.caza(parcial);
		estado.agarrando = a.agarrando();
		estado.embiste = a.embisteCliente;
		estado.come = a.comeCliente;
		estado.alerta = a.alertaCliente;
		estado.gira = Mth.wrapDegrees(a.yBodyRot - a.yBodyRotO);
	}
}
