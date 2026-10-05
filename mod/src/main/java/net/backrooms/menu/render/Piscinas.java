package net.backrooms.menu.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Fondo de los menus: el pasillo inundado de las Piscinas, dibujado en tiempo
 * real por el shader assets/backrooms/shaders/core/piscinas.fsh.
 *
 * El GuiRenderer de 1.21.11 no deja enlazar uniforms propios, asi que el
 * tiempo y la posicion de la camara viajan en las UV de los cuatro vertices
 * (iguales en todos, el shader los recibe como si fueran uniforms) y el
 * brillo en el canal rojo del color.
 */
public final class Piscinas {
	public static final RenderPipeline PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("backrooms", "pipeline/piscinas"))
			.withVertexShader(Identifier.fromNamespaceAndPath("backrooms", "core/piscinas"))
			.withFragmentShader(Identifier.fromNamespaceAndPath("backrooms", "core/piscinas"))
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.build()
	);

	private static final long INICIO = System.nanoTime();
	/** El bucle de la camara cierra cada 100 arcos (6.4 m): sin saltos visibles. */
	private static final float PERIODO_Z = 640.0F;
	private static final float VELOCIDAD = 0.45F;

	/** Brillo actual (1 = normal). Lo mueve el menu para los destellos de luz. */
	public static float luz = 1.0F;

	private Piscinas() {
	}

	public static float segundos() {
		return (System.nanoTime() - INICIO) / 1.0e9F;
	}

	public static void dibujar(GuiGraphics g, int ancho, int alto) {
		float t = segundos() % 3600.0F;
		float z = (t * VELOCIDAD) % PERIODO_Z;
		g.guiRenderState.submitGuiElement(new Estado(new Matrix3x2f(g.pose()), 0, 0, ancho, alto, t, z, luz));
	}

	private record Estado(Matrix3x2f pose, int x0, int y0, int x1, int y1, float t, float z, float brillo)
		implements GuiElementRenderState {

		@Override
		public void buildVertices(VertexConsumer v) {
			int rojo = Math.max(0, Math.min(255, Math.round(this.brillo * 0.75F * 255.0F)));
			int color = ARGB.color(255, rojo, 255, 255);
			v.addVertexWith2DPose(this.pose, this.x0, this.y0).setUv(this.t, this.z).setColor(color);
			v.addVertexWith2DPose(this.pose, this.x0, this.y1).setUv(this.t, this.z).setColor(color);
			v.addVertexWith2DPose(this.pose, this.x1, this.y1).setUv(this.t, this.z).setColor(color);
			v.addVertexWith2DPose(this.pose, this.x1, this.y0).setUv(this.t, this.z).setColor(color);
		}

		@Override
		public RenderPipeline pipeline() {
			return PIPELINE;
		}

		@Override
		public TextureSetup textureSetup() {
			return TextureSetup.noTexture();
		}

		@Override
		public @Nullable ScreenRectangle scissorArea() {
			return null;
		}

		@Override
		public @Nullable ScreenRectangle bounds() {
			return new ScreenRectangle(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0).transformMaxBounds(this.pose);
		}
	}
}
