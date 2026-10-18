package net.backrooms.evento.cliente.fiesta;

import java.util.HashSet;
import java.util.Set;
import net.backrooms.evento.cliente.HerramientasCliente;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.red.EstadoFiesta;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Lo de la Fiesta en el cliente: monedas, tickets y las fichas que ya cogio (no se dibujan:
 * FichaRenderer), y el marcador de arriba a la derecha (monedas, tickets / objetivo y una barra
 * hasta el Gran Premio). Los tickets suben contando, para que se note cada premio.
 */
public final class FiestaCliente {
	private static EstadoFiesta estado = EstadoFiesta.FUERA;
	private static final Set<Integer> COGIDAS = new HashSet<>();
	private static float ticketsVistos;
	private static long cambio;

	private FiestaCliente() {
	}

	public static void recibir(EstadoFiesta e) {
		if (estado.fuera() || e.fuera()) {
			ticketsVistos = e.tickets();
		}
		if (e.tickets() != estado.tickets() || e.monedas() != estado.monedas()) {
			cambio = net.minecraft.util.Util.getMillis();
		}
		estado = e;
		COGIDAS.clear();
		for (int id : e.cogidas()) {
			COGIDAS.add(id);
		}
	}

	public static void olvidar() {
		estado = EstadoFiesta.FUERA;
		COGIDAS.clear();
	}

	public static boolean cogida(int id) {
		return COGIDAS.contains(id);
	}

	public static boolean dentro() {
		return !estado.fuera();
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (estado.fuera() || mc.player == null || mc.options.hideGui || CinematicaCliente.activa() || HerramientasCliente.subida(1.0F) > 0.5F) {
			return;
		}
		ticketsVistos += (estado.tickets() - ticketsVistos) * 0.12F;
		if (Math.abs(estado.tickets() - ticketsVistos) < 0.5F) {
			ticketsVistos = estado.tickets();
		}
		int w = g.guiWidth();
		float tam = 11.0F;
		float x1 = w - 8;
		float y = 8;
		// destello al cambiar
		float brillo = Mth.clamp(1.0F - (net.minecraft.util.Util.getMillis() - cambio) / 900.0F, 0.0F, 1.0F);
		int ancho = 132;
		int x0 = (int) x1 - ancho;
		g.fill(x0 - 4, (int) y - 4, (int) x1 + 4, (int) y + 40, 0x88000000);
		g.renderOutline(x0 - 4, (int) y - 4, ancho + 8, 44, 0x55F2D46B);
		// la moneda: un circulo dorado de pixeles
		moneda(g, x0 + 5, (int) y + 5);
		Texto.hud(g, "MONEDAS " + estado.monedas(), x0 + 14, y, tam, 0.06F, mezcla(0xFFF2D46B, 0xFFFFFFFF, brillo));
		// el ticket: un rectangulo con muescas
		g.fill(x0, (int) y + 17, x0 + 10, (int) y + 24, 0xFFE8384A);
		g.fill(x0 + 4, (int) y + 17, x0 + 6, (int) y + 18, 0xFF000000);
		g.fill(x0 + 4, (int) y + 23, x0 + 6, (int) y + 24, 0xFF000000);
		int t = Math.round(ticketsVistos);
		Texto.hud(g, "TICKETS " + t + "/" + estado.objetivo(), x0 + 14, y + 14, tam, 0.06F, mezcla(0xFFFFE6EA, 0xFFFFFFFF, brillo));
		// la barra hasta el Gran Premio
		float k = estado.objetivo() <= 0 ? 0 : Mth.clamp(ticketsVistos / estado.objetivo(), 0.0F, 1.0F);
		g.fill(x0, (int) y + 30, (int) x1, (int) y + 34, 0xFF2A1F2B);
		g.fill(x0, (int) y + 30, x0 + Math.round(ancho * k), (int) y + 34, k >= 1.0F ? 0xFFF2D46B : 0xFFE8384A);
		if (estado.juego() == 1) {
			String s = SemaforoCliente.luz() == 2 ? "¡QUIETO!" : SemaforoCliente.luz() == 1 ? "¡CORRE!" : "SEMÁFORO";
			int col = SemaforoCliente.luz() == 2 ? 0xFFFF4A3A : 0xFF6BFF7A;
			Texto.hud(g, s, x1 - Texto.anchoHud(s, 16, 0.08F), y + 46, 16, 0.08F, col);
		}
	}

	private static void moneda(GuiGraphics g, int cx, int cy) {
		for (int dy = -4; dy <= 4; dy++) {
			for (int dx = -4; dx <= 4; dx++) {
				int d2 = dx * dx + dy * dy;
				if (d2 <= 17) {
					g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, d2 >= 10 ? 0xFFB8861C : (dx < 0 && dy < 0 ? 0xFFFFF1A8 : 0xFFF2C53A));
				}
			}
		}
	}

	private static int mezcla(int a, int b, float k) {
		int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255);
		int gg = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255);
		int bb = (int) Mth.lerp(k, a & 255, b & 255);
		return 0xFF000000 | r << 16 | gg << 8 | bb;
	}
}
