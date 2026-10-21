package net.backrooms.evento.cliente.cinematica;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Lo que va encima del mundo en la cinematica del tobogan (ToboganCliente): las franjas negras de
 * cine y el fundido a negro. Sin fondo (el mundo se ve detras), sin raton y sin Esc: se va sola.
 */
public class PantallaTobogan extends Screen {
	public PantallaTobogan() {
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
		int franja = Math.round(this.height * 0.11F * ToboganCliente.franjas(p));
		if (franja > 0) {
			g.fill(0, 0, this.width, franja, 0xFF000000);
			g.fill(0, this.height - franja, this.width, this.height, 0xFF000000);
		}
		int negro = Math.round(ToboganCliente.negro(p) * 255.0F);
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
