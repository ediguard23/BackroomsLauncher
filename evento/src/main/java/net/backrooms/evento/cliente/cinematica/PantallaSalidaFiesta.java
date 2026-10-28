package net.backrooms.evento.cliente.cinematica;

import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Lo que va encima del mundo en la cinematica de la salida de la Fiesta (SalidaFiestaCliente): las
 * franjas negras de cine, la luz que falla, el destello del golpe y el negro; y al despertar en la
 * Estacion, lo mismo que en el Nivel 0 (PantallaCinematica): la vista borrosa tras los parpados y el
 * titulo del nivel. Sin fondo, sin raton y sin Esc.
 */
public class PantallaSalidaFiesta extends Screen {
	public PantallaSalidaFiesta() {
		super(Component.literal("Backrooms"));
	}

	@Override
	protected void init() {
		GLFW.glfwSetInputMode(this.minecraft.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
	}

	/** El cursor vuelve si algo cierra la pantalla antes de tiempo (como en PantallaCinematica). */
	@Override
	public void removed() {
		long ventana = this.minecraft.getWindow().handle();
		if (GLFW.glfwGetInputMode(ventana, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN) {
			GLFW.glfwSetInputMode(ventana, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int x, int y, float parcial) {
		// nada: se ve el mundo
	}

	@Override
	public void render(GuiGraphics g, int x, int y, float parcial) {
		float p = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		int w = this.width;
		int h = this.height;
		if (SalidaFiestaCliente.despertando()) {
			if (SalidaFiestaCliente.desenfoque() > 0) {
				g.blurBeforeThisStratum();
			}
			PantallaCinematica.parpados(g, w, h, SalidaFiestaCliente.abertura(p), SalidaFiestaCliente.segundos(p));
			this.titulo(g, SalidaFiestaCliente.titulo(p));
		}
		int franja = Math.round(h * 0.11F * SalidaFiestaCliente.franjas(p));
		if (franja > 0) {
			g.fill(0, 0, w, franja, 0xFF000000);
			g.fill(0, h - franja, w, h, 0xFF000000);
		}
		int negro = Math.round(SalidaFiestaCliente.negro(p) * 255.0F);
		if (negro > 0) {
			g.fill(0, 0, w, h, negro << 24);
		}
		int blanco = Math.round(SalidaFiestaCliente.destello(p) * 255.0F);
		if (blanco > 0) {
			g.fill(0, 0, w, h, blanco << 24 | 0xFFFFFF);
		}
	}

	/** «NIVEL 3 / LA ESTACIÓN ELÉCTRICA», como el «NIVEL 0» del despertar del principio. */
	private void titulo(GuiGraphics g, float a) {
		if (a <= 0.01F) {
			return;
		}
		int alfa = Math.round(a * 230) << 24;
		float tam = this.height / 7.0F;
		String nivel = "NIVEL 3";
		float y = this.height * 0.62F;
		Texto.hud(g, nivel, this.width / 2.0F - Texto.anchoHud(nivel, tam, 0.18F) / 2.0F, y, tam, 0.18F, alfa | 0xE8D9A0);
		String sub = "LA ESTACIÓN ELÉCTRICA";
		float ts = tam * 0.28F;
		Texto.hud(g, sub, this.width / 2.0F - Texto.anchoHud(sub, ts, 0.3F) / 2.0F, y + tam * 1.05F, ts, 0.3F, alfa | 0xC8BC90);
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
