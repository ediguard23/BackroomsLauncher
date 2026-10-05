package net.backrooms.menu.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Rectangulo con degradado horizontal (GuiGraphics solo trae el vertical).
 * Es el "velo" que oscurece un lado de la pantalla para que se lea el texto.
 */
public record Degradado(Matrix3x2f pose, float x0, float y0, float x1, float y1, int izquierda, int derecha)
	implements GuiElementRenderState {

	public static void horizontal(GuiGraphics g, float x0, float y0, float x1, float y1, int izquierda, int derecha) {
		g.guiRenderState.submitGuiElement(new Degradado(new Matrix3x2f(g.pose()), x0, y0, x1, y1, izquierda, derecha));
	}

	@Override
	public void buildVertices(VertexConsumer v) {
		v.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.izquierda);
		v.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(this.izquierda);
		v.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.derecha);
		v.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(this.derecha);
	}

	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI;
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
		return new ScreenRectangle((int) this.x0, (int) this.y0, (int) Math.ceil(this.x1 - this.x0), (int) Math.ceil(this.y1 - this.y0))
			.transformMaxBounds(this.pose);
	}
}
