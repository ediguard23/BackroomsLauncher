package net.backrooms.evento.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import net.backrooms.evento.objetos.Objetos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * La acreditacion del Proyecto Umbral en el pecho del traje: una tarjetita blanca con una
 * franja cian y el nick de quien lo lleva. Sustituye al nombre flotante (que se quita a quien
 * lleva la chaqueta del traje: NombreTrajeMixin). Sin traje, en la recepcion, el nombre de
 * siempre.
 */
public class TarjetaPecho extends RenderLayer<AvatarRenderState, PlayerModel> {
	/** Un pixel del modelo, en bloques. */
	private static final float PX = 1.0F / 16.0F;
	private static final int PAPEL = 0xFFECEBE4;
	private static final int TINTA = 0xFF1E2224;
	private static final int FRANJA = 0xFF1F7F86;

	public TarjetaPecho(RenderLayerParent<AvatarRenderState, PlayerModel> padre) {
		super(padre);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector cola, int luz, AvatarRenderState s, float yaw, float pitch) {
		if (s.isInvisible || !s.chestEquipment.is(Objetos.TRAJE_CHAQUETA)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Entity e = mc.level == null ? null : mc.level.getEntity(s.id);
		if (!(e instanceof Player jugador)) {
			return;
		}
		Font font = mc.font;
		FormattedCharSequence nick = Component.literal(jugador.getGameProfile().name()).getVisualOrderText();
		FormattedCharSequence cabecera = Component.literal("UMBRAL").getVisualOrderText();
		int ancho = Math.max(font.width(nick), 1);
		pose.pushPose();
		this.getParentModel().body.translateAndRotate(pose);
		// en medio del pecho, bajo el cuello, sobre la tela de la chaqueta (que va inflada un pixel)
		pose.translate(0.0F, 3.4F * PX, -3.15F * PX);
		// el nick mide 1,7 pixeles de alto y cabe en 7,4 de ancho (los largos, mas pequenos)
		float esc = Math.min(PX * 1.7F / 8.0F, PX * 7.4F / ancho);
		float escCab = PX * 0.8F / 8.0F / esc;
		int anchoCab = font.width(cabecera);
		// dos pasadas: primero los fondos (la tarjeta blanca y la franja cian) y luego las letras un
		// pelo por delante, para que el fondo no las tape (estan en el mismo plano)
		for (int pasada = 0; pasada < 2; pasada++) {
			pose.pushPose();
			if (pasada == 1) {
				pose.translate(0.0F, 0.0F, -0.06F * PX);
			}
			pose.scale(esc, esc, esc);
			cola.submitText(pose, -ancho / 2.0F, 0, nick, false, Font.DisplayMode.POLYGON_OFFSET, luz,
				pasada == 0 ? 0x00000000 : TINTA, pasada == 0 ? PAPEL : 0, 0);
			// encima, la franja cian con el nombre del proyecto
			pose.scale(escCab, escCab, escCab);
			cola.submitText(pose, -anchoCab / 2.0F, -9.5F, cabecera, false, Font.DisplayMode.POLYGON_OFFSET, luz,
				pasada == 0 ? 0x00000000 : 0xFFE6F4F2, pasada == 0 ? FRANJA : 0, 0);
			pose.popPose();
		}
		pose.popPose();
	}
}
