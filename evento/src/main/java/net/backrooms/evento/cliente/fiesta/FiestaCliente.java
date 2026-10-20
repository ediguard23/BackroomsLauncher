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
		int ancho = 172;
		int alto = 66;
		int x1 = w - 10;
		int x0 = x1 - ancho;
		int y0 = 10;
		// el cartel: fondo oscuro con un poco de morado arriba, doble borde dorado y banderines colgando
		g.fill(x0, y0, x1, y0 + alto, 0xD0120C1A);
		g.fill(x0, y0, x1, y0 + 22, 0x3048266A);
		g.renderOutline(x0, y0, ancho, alto, 0xFFF2D46B);
		g.renderOutline(x0 + 2, y0 + 2, ancho - 4, alto - 4, 0x55F2D46B);
		int[] colores = {0xFFE8384A, 0xFFF6CE40, 0xFF3A96E6, 0xFF4AC46E};
		for (int i = 0, x = x0 + 4; x + 7 <= x1 - 4; i++, x += 9) {
			for (int f = 0; f < 5; f++) {
				g.fill(x + f, y0 + 3 + f, x + 8 - f, y0 + 4 + f, colores[i % 4]);
			}
		}
		// monedas: la moneda grande con su estrella, la etiqueta pequena y el numero grande
		moneda(g, x0 + 15, y0 + 26);
		Texto.hud(g, "MONEDAS", x0 + 27, y0 + 11, 7.0F, 0.08F, 0xFFBFA45A);
		Texto.hud(g, String.valueOf(estado.monedas()), x0 + 27, y0 + 18, 16.0F, 0.04F, mezcla(0xFFF2D46B, 0xFFFFFFFF, brillo));
		// tickets: el ticket con sus muescas y el numero grande con el objetivo pequeno al lado
		int tx = x0 + 74;
		ticket(g, tx, y0 + 21);
		int t = Math.round(ticketsVistos);
		Texto.hud(g, "TICKETS", tx + 20, y0 + 11, 7.0F, 0.08F, 0xFFD98A96);
		float an = Texto.hud(g, String.valueOf(t), tx + 20, y0 + 18, 16.0F, 0.04F, mezcla(0xFFFFE6EA, 0xFFFFFFFF, brillo));
		Texto.hud(g, "/" + estado.objetivo(), tx + 20 + an + 2, y0 + 25, 8.0F, 0.06F, 0xFFB7A3B0);
		// la barra hasta la puerta del final: rayas que se mueven, marcas cada 10 % y el porcentaje
		float k = estado.objetivo() <= 0 ? 0 : Mth.clamp(ticketsVistos / estado.objetivo(), 0.0F, 1.0F);
		int bx0 = x0 + 8;
		int bx1 = x1 - 8;
		int by = y0 + 40;
		g.fill(bx0 - 1, by - 1, bx1 + 1, by + 9, 0xFF000000);
		g.fill(bx0, by, bx1, by + 8, 0xFF2A1F2B);
		int lleno = bx0 + Math.round((bx1 - bx0) * k);
		boolean completo = k >= 1.0F;
		int desplaza = (int) (ms / 60 % 8);
		for (int x = bx0; x < lleno; x++) {
			boolean raya = (x - bx0 + desplaza) % 8 < 4;
			int col = completo ? (raya ? 0xFFF2D46B : 0xFFD9B347) : (raya ? 0xFFE8384A : 0xFFC22A3C);
			g.fill(x, by, x + 1, by + 8, col);
		}
		g.fill(bx0, by, lleno, by + 2, 0x44FFFFFF);
		for (int i = 1; i < 10; i++) {
			int x = bx0 + (bx1 - bx0) * i / 10;
			g.fill(x, by + 6, x + 1, by + 8, 0x66000000);
		}
		String pct = Math.round(k * 100) + "%";
		Texto.hud(g, pct, bx1 - 2 - Texto.anchoHud(pct, 7.0F, 0.06F), by + 1, 7.0F, 0.06F, 0xFFFFFFFF);
		// el bote de la vuelta (lo que daria el tobogan del castillo ahora mismo)
		long ahora = mc.level == null ? 0L : mc.level.getGameTime();
		int bote = estado.boteAhora(ahora);
		String sb = bote <= 0 ? "BOTE COBRADO · SALTA A UN NOCLIP" : "BOTE DE LA VUELTA  " + bote;
		Texto.hud(g, sb, x0 + 8, y0 + 53, 7.5F, 0.06F, bote <= 0 ? 0xFF8C8C8C : 0xFFFFC93A);
		if (estado.juego() == 1) {
			String s = SemaforoCliente.luz() == 2 ? "¡QUIETO!" : SemaforoCliente.luz() == 1 ? "¡CORRE!" : "SEMÁFORO";
			int col = SemaforoCliente.luz() == 2 ? 0xFFFF4A3A : 0xFF6BFF7A;
			Texto.hud(g, s, x1 - Texto.anchoHud(s, 16, 0.08F), y0 + alto + 6, 16, 0.08F, col);
		}
	}

	/** La moneda del marcador: canto oscuro, cara dorada con brillo y una estrella. */
	private static void moneda(GuiGraphics g, int cx, int cy) {
		for (int dy = -7; dy <= 7; dy++) {
			for (int dx = -7; dx <= 7; dx++) {
				int d2 = dx * dx + dy * dy;
				if (d2 > 52) {
					continue;
				}
				int col = d2 >= 40 ? 0xFF9A6A12 : d2 >= 30 ? (dx + dy < 0 ? 0xFFFFE27A : 0xFFD49A2A) : (dx < -1 && dy < -1 ? 0xFFFFE89A : 0xFFF2C53A);
				g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, col);
			}
		}
		// la estrella
		int[][] e = {{0, -4}, {0, -3}, {-1, -2}, {0, -2}, {1, -2}, {-4, -1}, {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1}, {4, -1},
			{-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {-2, 1}, {-1, 1}, {1, 1}, {2, 1}, {-3, 2}, {-2, 2}, {2, 2}, {3, 2}};
		for (int[] p : e) {
			g.fill(cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, 0xFFFFF4C0);
		}
	}

	/** El ticket del marcador: rojo, con las muescas de los lados y la linea de puntos del talon. */
	private static void ticket(GuiGraphics g, int x, int y) {
		g.fill(x, y - 6, x + 16, y + 6, 0xFFE8384A);
		g.fill(x, y - 6, x + 16, y - 5, 0xFFFF6E7E);
		g.fill(x, y - 1, x + 1, y + 1, 0xFF120C1A);
		g.fill(x + 15, y - 1, x + 16, y + 1, 0xFF120C1A);
		for (int yy = y - 5; yy < y + 6; yy += 2) {
			g.fill(x + 11, yy, x + 12, yy + 1, 0xFFFFE6EA);
		}
		g.fill(x + 3, y - 2, x + 9, y - 1, 0xFFFFE6EA);
		g.fill(x + 3, y + 1, x + 8, y + 2, 0xFFFFE6EA);
	}

	private static int mezcla(int a, int b, float k) {
		int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255);
		int gg = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255);
		int bb = (int) Mth.lerp(k, a & 255, b & 255);
		return 0xFF000000 | r << 16 | gg << 8 | bb;
	}
}
