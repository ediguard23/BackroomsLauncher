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
 * hasta la puerta del final). Los tickets suben contando, para que se note cada premio.
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

	public static EstadoFiesta estado() {
		return estado;
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
		long ms = net.minecraft.util.Util.getMillis();
		// destello al cambiar
		float brillo = Mth.clamp(1.0F - (ms - cambio) / 900.0F, 0.0F, 1.0F);
		// un marcador pequeno: una fila con las monedas y los tickets, la barra fina y el bote debajo
		int ancho = 124;
		int alto = 33;
		int x1 = w - 6;
		int x0 = x1 - ancho;
		int y0 = 6;
		g.fill(x0, y0, x1, y0 + alto, 0xC0120C1A);
		g.renderOutline(x0, y0, ancho, alto, 0xCCF2D46B);
		// monedas
		moneda(g, x0 + 9, y0 + 9);
		Texto.hud(g, String.valueOf(estado.monedas()), x0 + 17, y0 + 4, 11.0F, 0.04F, mezcla(0xFFF2D46B, 0xFFFFFFFF, brillo));
		// tickets, con el objetivo en pequeno
		int tx = x0 + 44;
		ticket(g, tx, y0 + 9);
		int t = Math.round(ticketsVistos);
		float an = Texto.hud(g, String.valueOf(t), tx + 14, y0 + 4, 11.0F, 0.04F, mezcla(0xFFFFE6EA, 0xFFFFFFFF, brillo));
		Texto.hud(g, "/" + estado.objetivo(), tx + 14 + an + 1, y0 + 8, 6.5F, 0.06F, 0xFFB7A3B0);
		// la barra hasta la puerta del final, fina, con el porcentaje a la derecha
		float k = estado.objetivo() <= 0 ? 0 : Mth.clamp(ticketsVistos / estado.objetivo(), 0.0F, 1.0F);
		String pct = Math.round(k * 100) + "%";
		float anPct = Texto.anchoHud(pct, 6.5F, 0.06F);
		int bx0 = x0 + 5;
		int bx1 = x1 - 7 - (int) Math.ceil(anPct);
		int by = y0 + 18;
		g.fill(bx0 - 1, by - 1, bx1 + 1, by + 4, 0xFF000000);
		g.fill(bx0, by, bx1, by + 3, 0xFF2A1F2B);
		int lleno = bx0 + Math.round((bx1 - bx0) * k);
		boolean completo = k >= 1.0F;
		int desplaza = (int) (ms / 60 % 6);
		for (int x = bx0; x < lleno; x++) {
			boolean raya = (x - bx0 + desplaza) % 6 < 3;
			int col = completo ? (raya ? 0xFFF2D46B : 0xFFD9B347) : (raya ? 0xFFE8384A : 0xFFC22A3C);
			g.fill(x, by, x + 1, by + 3, col);
		}
		Texto.hud(g, pct, x1 - 4 - anPct, by - 1, 6.5F, 0.06F, 0xFFFFFFFF);
		// el bote de la vuelta (lo que daria el tobogan del castillo ahora mismo)
		long ahora = mc.level == null ? 0L : mc.level.getGameTime();
		int bote = estado.boteAhora(ahora);
		String sb = bote <= 0 ? "BOTE COBRADO · A UN NOCLIP" : "BOTE DE LA VUELTA  " + bote;
		Texto.hud(g, sb, x0 + 5, y0 + 24, 6.0F, 0.06F, bote <= 0 ? 0xFF8C8C8C : 0xFFFFC93A);
		if (estado.juego() == 1) {
			String s = SemaforoCliente.luz() == 2 ? "¡QUIETO!" : SemaforoCliente.luz() == 1 ? "¡CORRE!" : "SEMÁFORO";
			int col = SemaforoCliente.luz() == 2 ? 0xFFFF4A3A : 0xFF6BFF7A;
			Texto.hud(g, s, x1 - Texto.anchoHud(s, 14, 0.08F), y0 + alto + 4, 14, 0.08F, col);
		}
	}

	/** La moneda del marcador (11 px): canto oscuro, cara dorada con brillo. */
	private static void moneda(GuiGraphics g, int cx, int cy) {
		for (int dy = -5; dy <= 5; dy++) {
			for (int dx = -5; dx <= 5; dx++) {
				int d2 = dx * dx + dy * dy;
				if (d2 > 28) {
					continue;
				}
				int col = d2 >= 20 ? 0xFF9A6A12 : d2 >= 13 ? (dx + dy < 0 ? 0xFFFFE27A : 0xFFD49A2A) : (dx < 0 && dy < 0 ? 0xFFFFE89A : 0xFFF2C53A);
				g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, col);
			}
		}
		// una estrellita de cuatro puntas en el centro
		g.fill(cx, cy - 2, cx + 1, cy + 3, 0xFFFFF4C0);
		g.fill(cx - 2, cy, cx + 3, cy + 1, 0xFFFFF4C0);
	}

	/** El ticket del marcador (12 x 8): rojo, con las muescas de los lados y la linea de puntos del talon. */
	private static void ticket(GuiGraphics g, int x, int y) {
		g.fill(x, y - 4, x + 12, y + 4, 0xFFE8384A);
		g.fill(x, y - 4, x + 12, y - 3, 0xFFFF6E7E);
		g.fill(x, y - 1, x + 1, y + 1, 0xFF120C1A);
		g.fill(x + 11, y - 1, x + 12, y + 1, 0xFF120C1A);
		for (int yy = y - 3; yy < y + 4; yy += 2) {
			g.fill(x + 8, yy, x + 9, yy + 1, 0xFFFFE6EA);
		}
		g.fill(x + 2, y - 1, x + 7, y, 0xFFFFE6EA);
		g.fill(x + 2, y + 1, x + 6, y + 2, 0xFFFFE6EA);
	}

	private static int mezcla(int a, int b, float k) {
		int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255);
		int gg = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255);
		int bb = (int) Mth.lerp(k, a & 255, b & 255);
		return 0xFF000000 | r << 16 | gg << 8 | bb;
	}
}
