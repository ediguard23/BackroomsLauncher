package net.backrooms.evento.cliente.cinematica;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Lo que va encima del mundo en el final de la Estacion (SalidaEstacionCliente): las franjas de cine y el
 * negro del corte. Sin fondo, sin raton y sin Esc.
 */
public class PantallaSalidaEstacion extends Screen {
	public PantallaSalidaEstacion() {
		super(Component.literal("Backrooms"));
	}

	@Override
	protected void init() {
		GLFW.glfwSetInputMode(this.minecraft.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
	}

	@Override
	public void removed() {
		long ventana = this.minecraft.getWindow().handle();
		if (GLFW.glfwGetInputMode(ventana, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN) {
			GLFW.glfwSetInputMode(ventana, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int x, int y, float parcial) {
	}

	@Override
	public void render(GuiGraphics g, int x, int y, float parcial) {
		float p = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		int franja = Math.round(this.height * 0.11F * SalidaEstacionCliente.franjas(p));
		if (franja > 0) {
			g.fill(0, 0, this.width, franja, 0xFF000000);
			g.fill(0, this.height - franja, this.width, this.height, 0xFF000000);
		}
		int negro = Math.round(SalidaEstacionCliente.negro(p) * 255.0F);
		if (negro > 0) {
			g.fill(0, 0, this.width, this.height, negro << 24);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
