package net.backrooms.evento.cliente;

import java.util.Locale;
import net.backrooms.evento.cliente.efectos.Cordura;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Lo que se ve por el visor de la camara (C): REC, contador, bateria, las
 * esquinas del encuadre y, si la mision en curso es de grabar, cuanto lleva
 * grabado. En el apagon pasa a NIGHT SHOT (la vision nocturna la pinta
 * EfectosMundo). Mismo estilo que el HUD de la cinematica del ascensor.
 */
public final class CamaraHud {
	private static long levantada = -1;

	private CamaraHud() {
	}

	public static void render(GuiGraphics g, DeltaTracker tiempo) {
		Minecraft mc = Minecraft.getInstance();
		float subida = HerramientasCliente.subida(tiempo.getGameTimeDeltaPartialTick(false));
		if (mc.player == null || mc.options.hideGui) {
			return;
		}
		if (!HerramientasCliente.camara()) {
			levantada = -1;
		} else if (levantada < 0) {
			levantada = Util.getMillis();
		}
		if (subida < 0.05F) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		int a = Math.round(Mth.clamp(subida, 0, 1) * 255) << 24;
		int blanco = a | 0xF2EEE4;
		float tam = Math.max(8.0F, h / 18.0F);
		float m = h / 14.0F;
		float t = levantada < 0 ? 0 : (Util.getMillis() - levantada) / 1000.0F;

		// esquinas del encuadre
		int largo = Math.round(h / 9.0F);
		int grueso = Math.max(1, Math.round(h / 240.0F));
		int x0 = Math.round(m * 0.6F);
		int y0 = Math.round(m * 0.6F);
		int x1 = w - x0;
		int y1 = h - y0;
		esquina(g, x0, y0, largo, grueso, 1, 1, blanco);
		esquina(g, x1, y0, largo, grueso, -1, 1, blanco);
		esquina(g, x0, y1, largo, grueso, 1, -1, blanco);
		esquina(g, x1, y1, largo, grueso, -1, -1, blanco);
		// cruz del centro
		int cx = w / 2;
		int cy = h / 2;
		int c = Math.round(h / 60.0F);
		g.fill(cx - c, cy, cx + c + 1, cy + 1, a | 0xC8C4BA);
		g.fill(cx, cy - c, cx + 1, cy + c + 1, a | 0xC8C4BA);

		// REC y contador
		if ((int) (t * 1.6F) % 2 == 0) {
			g.fill(Math.round(m), Math.round(m + tam * 0.22F), Math.round(m + tam * 0.55F), Math.round(m + tam * 0.77F), a | 0xE5281E);
		}
		Texto.hud(g, "REC", m + tam * 0.8F, m, tam, 0.08F, blanco);
		int s = (int) t;
		String contador = String.format(Locale.ROOT, "0:%02d:%02d", s / 60, s % 60);
		Texto.hud(g, contador, w - m - Texto.anchoHud(contador, tam, 0.08F), m, tam, 0.08F, blanco);
		// bateria
		float bx = w - m - tam * 1.6F;
		float by = m + tam * 1.3F;
		g.renderOutline(Math.round(bx), Math.round(by), Math.round(tam * 1.3F), Math.round(tam * 0.6F), blanco);
		for (int i = 0; i < 3; i++) {
			float r = bx + tam * (0.12F + i * 0.39F);
			g.fill(Math.round(r), Math.round(by + tam * 0.12F), Math.round(r + tam * 0.3F), Math.round(by + tam * 0.48F), blanco);
		}
		if (AmbienteCliente.oscuridad() > 0.5F) {
			Texto.hud(g, "NIGHT SHOT", m, m + tam * 1.3F, tam * 0.8F, 0.08F, a | 0x7CFF8A);
		}
		if (Cordura.efecto() > 0.6F && (int) (t * 7) % 9 == 0) {
			Texto.hud(g, "ERROR DE CINTA", w / 2.0F - Texto.anchoHud("ERROR DE CINTA", tam, 0.08F) / 2.0F, h * 0.3F, tam, 0.08F, a | 0xE5281E);
		}

		// lo que lleva grabado de la mision
		float progreso = MisionesCliente.grabado();
		String objetivo = MisionesCliente.objetivoGrabacion();
		if (objetivo != null) {
			float ancho = w * 0.32F;
			float px = w / 2.0F - ancho / 2.0F;
			float py = h - m - tam * 1.1F;
			Texto.hud(g, objetivo, w / 2.0F - Texto.anchoHud(objetivo, tam * 0.75F, 0.08F) / 2.0F, py - tam * 1.0F, tam * 0.75F, 0.08F,
				MisionesCliente.grabando() ? (a | 0xFFE36B) : blanco);
			g.renderOutline(Math.round(px), Math.round(py), Math.round(ancho), Math.round(tam * 0.45F), blanco);
			g.fill(Math.round(px + 2), Math.round(py + 2), Math.round(px + 2 + (ancho - 4) * Mth.clamp(progreso, 0, 1)), Math.round(py + tam * 0.45F - 2),
				a | 0xE5281E);
		}
		Texto.hud(g, "SP", w - m - Texto.anchoHud("SP", tam, 0.08F), h - m - tam, tam, 0.08F, blanco);
	}

	private static void esquina(GuiGraphics g, int x, int y, int largo, int grueso, int sx, int sy, int color) {
		int xa = Math.min(x, x + sx * largo);
		int xb = Math.max(x, x + sx * largo);
		int ya = Math.min(y, y + sy * largo);
		int yb = Math.max(y, y + sy * largo);
		g.fill(xa, sy > 0 ? y : y - grueso, xb, sy > 0 ? y + grueso : y, color);
		g.fill(sx > 0 ? x : x - grueso, ya, sx > 0 ? x + grueso : x, yb, color);
	}
}
