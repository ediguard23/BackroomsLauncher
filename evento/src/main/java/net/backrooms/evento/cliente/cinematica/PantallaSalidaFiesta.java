package net.backrooms.evento.cliente.cinematica;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Lo que va encima del mundo en la cinematica de la salida de la Fiesta (SalidaFiestaCliente): las
 * franjas negras de cine, el destello del golpe y el negro. Sin fondo, sin raton y sin Esc.
 */
public class PantallaSalidaFiesta extends Screen {
	public PantallaSalidaFiesta() {
		super(Component.literal("Backrooms"));
	}

	@Override
	protected void init() {
		GLFW.glfwSetInputMode(this.minecraft.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
	}

	@Override
	public void renderBackground(GuiGraphics g, int x, int y, float parcial) {
		// nada: se ve el mundo
	}

	@Override
	public void render(GuiGraphics g, int x, int y, float parcial) {
		float p = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		int franja = Math.round(this.height * 0.11F * SalidaFiestaCliente.franjas(p));
		if (franja > 0) {
			g.fill(0, 0, this.width, franja, 0xFF000000);
			g.fill(0, this.height - franja, this.width, this.height, 0xFF000000);
		}
		int negro = Math.round(SalidaFiestaCliente.negro(p) * 255.0F);
		if (negro > 0) {
			g.fill(0, 0, this.width, this.height, negro << 24);
		}
		int blanco = Math.round(SalidaFiestaCliente.destello(p) * 255.0F);
		if (blanco > 0) {
			g.fill(0, 0, this.width, this.height, blanco << 24 | 0xFFFFFF);
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
