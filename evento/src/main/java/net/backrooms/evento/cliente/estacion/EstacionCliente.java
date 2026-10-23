package net.backrooms.evento.cliente.estacion;

import net.backrooms.evento.cliente.HerramientasCliente;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.red.EstadoEstacion;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * La Estacion Electrica en el cliente: el estado que manda el servidor (JuegoEstacion) y el
 * cuadrito de arriba a la derecha, como una placa de mantenimiento: el combustible del generador
 * (en rojo y parpadeando en la reserva), el piso donde estas, lo puesto en su central, las centrales
 * activas (un cuadrito por piso) y si hay
 * corriente. La barra del combustible baja suave, sin saltos.
 */
public final class EstacionCliente {
	private static final float RESERVA = 15.0F;
	private static EstadoEstacion estado = EstadoEstacion.FUERA;
	private static float combustibleVisto;
	private static long cambio;

	private EstacionCliente() {
	}

	public static void recibir(EstadoEstacion e) {
		if (estado.fuera() || e.fuera()) {
			combustibleVisto = e.combustible();
		}
		if (e.puestos() != estado.puestos() || e.centrales() != estado.centrales() || e.piso() != estado.piso()) {
			cambio = net.minecraft.util.Util.getMillis();
		}
		estado = e;
	}

	public static void olvidar() {
		estado = EstadoEstacion.FUERA;
	}

	public static EstadoEstacion estado() {
		return estado;
	}

	public static boolean dentro() {
		return !estado.fuera();
	}

	/** El marcador, un 18 % mas grande que como se dibuja (escalado desde la esquina de arriba a la izquierda). */
	private static final float ESCALA = 1.18F;

	public static void render(GuiGraphics g, DeltaTracker delta) {
		org.joml.Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.scale(ESCALA, ESCALA);
		dibujar(g, delta);
		p.popMatrix();
	}

	private static void dibujar(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (estado.fuera() || mc.player == null || mc.level == null || !net.backrooms.evento.estacion.Estacion.es(mc.level) || mc.options.hideGui || CinematicaCliente.activa() || HerramientasCliente.subida(1.0F) > 0.5F) {
			return;
		}
		combustibleVisto += (estado.combustible() - combustibleVisto) * 0.08F;
		long ms = net.minecraft.util.Util.getMillis();
		float brillo = Mth.clamp(1.0F - (ms - cambio) / 900.0F, 0.0F, 1.0F);
		boolean parpadeo = ms / 400 % 2 == 0;
		int ancho = 124;
		int alto = 33;
		int x1 = (int) (g.guiWidth() / ESCALA) - 6;
		int x0 = x1 - ancho;
		int y0 = 6;
		g.fill(x0, y0, x1, y0 + alto, 0xC0101511);
		g.renderOutline(x0, y0, ancho, alto, 0xCCE0B43A);
		// el combustible: el bidon y la barra con el porcentaje
		bidon(g, x0 + 4, y0 + 3);
		float k = Mth.clamp(combustibleVisto / 100.0F, 0.0F, 1.0F);
		boolean reserva = estado.combustible() < RESERVA;
		String pct = Math.round(k * 100) + "%";
		float anPct = Texto.anchoHud(pct, 6.5F, 0.06F);
		int bx0 = x0 + 15;
		int bx1 = x1 - 7 - (int) Math.ceil(anPct);
		int by = y0 + 6;
		g.fill(bx0 - 1, by - 1, bx1 + 1, by + 5, 0xFF000000);
		g.fill(bx0, by, bx1, by + 4, 0xFF242A22);
		int lleno = bx0 + Math.round((bx1 - bx0) * k);
		int col = reserva ? (parpadeo ? 0xFFE8382E : 0xFF8A1E18) : 0xFFE0A030;
		g.fill(bx0, by, lleno, by + 4, col);
		g.fill(bx0, by, lleno, by + 1, reserva ? 0xFFFF7A6A : 0xFFFFD27A);
		// marcas de cuarto
		for (int q = 1; q < 4; q++) {
			int mx = bx0 + (bx1 - bx0) * q / 4;
			g.fill(mx, by + 3, mx + 1, by + 4, 0x88000000);
		}
		Texto.hud(g, pct, x1 - 4 - anPct, by - 1, 6.5F, 0.06F, reserva ? 0xFFFF6A5A : 0xFFFFFFFF);
		// el piso, lo puesto en su central y las centrales activas (un cuadrito por piso)
		int blanco = mezcla(0xFFD8E0D0, 0xFFFFFFFF, brillo);
		int[] colores = {0xFFE0362C, 0xFFF2D03A, 0xFF3A9AE8};
		float an = Texto.hud(g, "PISO " + (estado.piso() + 1) + "  ", x0 + 5, y0 + 15, 6.5F, 0.06F, colores[Math.min(2, estado.piso())]);
		an += Texto.hud(g, "CENTRAL ", x0 + 5 + an, y0 + 15, 6.5F, 0.06F, 0xFF9FB09A);
		boolean llena = estado.necesarios() > 0 && estado.puestos() >= estado.necesarios();
		Texto.hud(g, estado.puestos() + "/" + estado.necesarios(), x0 + 5 + an, y0 + 15, 6.5F, 0.06F, llena ? 0xFF7AE07A : blanco);
		for (int i = 0; i < 3; i++) {
			int cx = x1 - 22 + i * 6;
			boolean activa = i < estado.centrales();
			g.fill(cx, y0 + 16, cx + 4, y0 + 21, activa ? colores[i] : 0xFF2A302A);
			g.renderOutline(cx, y0 + 16, 4, 5, activa ? 0xFFFFFFFF : 0xFF5A645A);
		}
		// la corriente
		String s;
		int c;
		if (estado.centrales() >= 3) {
			s = estado.cadena() ? "SALIDA ABIERTA" : "PASARELA · CADENA PUESTA";
			c = parpadeo ? 0xFF7AE07A : 0xFF4AB04A;
		} else if (!estado.corriente()) {
			s = "SIN CORRIENTE · GENERADOR";
			c = parpadeo ? 0xFFFF4A3A : 0xFF8A2A22;
		} else {
			s = "CORRIENTE OK";
			c = 0xFF9FD08A;
		}
		punto(g, x0 + 7, y0 + 27, c);
		Texto.hud(g, s, x0 + 12, y0 + 24, 6.0F, 0.06F, c);
	}

	/** Un bidon amarillo de 8 x 11. */
	private static void bidon(GuiGraphics g, int x, int y) {
		g.fill(x + 1, y + 1, x + 8, y + 11, 0xFFE0B43A);
		g.fill(x + 1, y + 1, x + 2, y + 11, 0xFFFFD86A);
		g.fill(x + 7, y + 1, x + 8, y + 11, 0xFF9A7418);
		g.fill(x + 1, y + 4, x + 8, y + 5, 0xFF9A7418);
		g.fill(x + 1, y + 8, x + 8, y + 9, 0xFF9A7418);
		g.fill(x + 5, y, x + 7, y + 1, 0xFF3A3A3A);
	}

	private static void punto(GuiGraphics g, int cx, int cy, int col) {
		g.fill(cx - 1, cy - 2, cx + 2, cy + 3, col);
		g.fill(cx - 2, cy - 1, cx + 3, cy + 2, col);
	}

	private static int mezcla(int a, int b, float k) {
		int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255);
		int gg = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255);
		int bb = (int) Mth.lerp(k, a & 255, b & 255);
		return 0xFF000000 | r << 16 | gg << 8 | bb;
	}
}
