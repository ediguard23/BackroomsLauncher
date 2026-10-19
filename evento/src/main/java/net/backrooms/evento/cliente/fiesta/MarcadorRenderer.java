package net.backrooms.evento.cliente.fiesta;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.fiesta.Marcador;
import net.backrooms.evento.fiesta.MarcadorEntidad;
import net.backrooms.evento.red.EstadoFiesta;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * El texto del marcador de la sala central, en LEDs rojos sobre el panel negro: cada uno ve lo suyo
 * (FiestaCliente). TIPO 0: "TICKETS 894/1500"; TIPO 1: el bote de la vuelta, que va bajando.
 */
public class MarcadorRenderer implements BlockEntityRenderer<MarcadorEntidad, MarcadorRenderer.Estado> {
	/** Brillo total: los LEDs no dependen de la luz de la sala. */
	private static final int BRILLO = 15728880;
	private static final float ESCALA = 0.026F;
	private final Font letra;

	public MarcadorRenderer(BlockEntityRendererProvider.Context ctx) {
		this.letra = ctx.font();
	}

	public static class Estado extends BlockEntityRenderState {
		public Direction mira = Direction.SOUTH;
		public String texto = "";
		public int color;
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(MarcadorEntidad m, Estado estado, float parcial, Vec3 camara, ModelFeatureRenderer.@Nullable CrumblingOverlay rotura) {
		BlockEntityRenderer.super.extractRenderState(m, estado, parcial, camara, rotura);
		estado.mira = m.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
		EstadoFiesta e = FiestaCliente.estado();
		if (m.getBlockState().getValue(Marcador.TIPO) == 0) {
			estado.texto = "TICKETS " + e.tickets() + "/" + e.objetivo();
			estado.color = e.tickets() >= e.objetivo() && e.objetivo() > 0 ? 0xFF6BFF7A : 0xFFFF3B2A;
		} else {
			long ahora = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
			int bote = e.boteAhora(ahora);
			estado.texto = bote <= 0 ? "BOTE COBRADO" : "BOTE " + bote;
			estado.color = 0xFFFFC93A;
		}
	}

	@Override
	public void submit(Estado estado, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		// el panel esta pegado a la pared de detras: el texto, justo delante, mirando hacia `mira`
		pose.mulPose(Axis.YP.rotationDegrees(-estado.mira.toYRot()));
		pose.translate(0.0F, 0.0F, -0.47F);
		pose.scale(ESCALA, -ESCALA, ESCALA);
		FormattedCharSequence s = Component.literal(estado.texto).getVisualOrderText();
		float x = -this.letra.width(s) / 2.0F;
		nodos.submitText(pose, x, -4.5F, s, false, Font.DisplayMode.POLYGON_OFFSET, BRILLO, estado.color, 0, 0);
		pose.popPose();
	}
}
