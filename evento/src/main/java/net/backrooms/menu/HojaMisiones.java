package net.backrooms.menu;

import java.util.List;
import net.backrooms.evento.cliente.MisionesCliente;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.menu.render.Fondo;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

/**
 * Hoja de MISIONES DEL EXPLORADOR (papel rayado como el registro de
 * expedicion del menu): las tres misiones del jugador, la que va en curso con
 * su descripcion y, mientras busca casetes, el detector de senal.
 *
 * Se dibuja en pixeles de diseno, de (0, 0) a (ANCHO, ALTO); quien la usa
 * traslada y escala la pose. Va al lado del inventario (E).
 *
 * El detector es dificil a proposito: no da metros, solo la fuerza de la
 * senal, y la flecha tiembla mas cuanto mas lejos esta el casete. A mas de
 * Misiones.ALCANCE_SENAL bloques no capta nada.
 */
public final class HojaMisiones {
	public static final int ANCHO = 350;
	public static final int ALTO = 384;
	private static final int ROJO = 0xFFB0281D;

	private HojaMisiones() {
	}

	/** `mirando`: yaw del jugador, para que la flecha apunte respecto a su vista. */
	public static void dibujar(GuiGraphics g, Tema tema, float mirando) {
		int x1 = ANCHO;
		int y1 = ALTO;
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.rotateAbout((float) Math.toRadians(-0.6), ANCHO / 2.0F, 0);
		for (int i = 1; i <= 4; i++) {
			g.fill(i, i * 4, x1 + i, y1 + i * 4, 0x1E000000);
		}
		g.fill(0, 0, x1, y1, tema.papel);
		for (int y = 26; y < y1; y += 26) {
			g.fill(0, y, x1, y + 1, tema.papelRayas);
		}
		g.fill(34, 0, 36, y1, 0x59BE3228);
		Texto.maquina(g, "MISIONES DEL EXPLORADOR", 46, 12, 13, 0.18F, tema.papelTinta);
		g.fill(0, 34, x1, 35, MenuBackrooms.alfa(tema.papelTinta, 0.25F));

		List<TipoMision> lista = MisionesCliente.misiones();
		if (lista.isEmpty()) {
			Texto.parrafo(g, "Todavía no hay expedición. Espera a que el staff dé la salida.", 46, 50, 13, x1 - 62, 1.45F, tema.papelVacio);
			p.popMatrix();
			return;
		}
		int actual = MisionesCliente.actual();
		float y = 46;
		for (int i = 0; i < lista.size(); i++) {
			TipoMision m = lista.get(i);
			boolean hecha = i < actual;
			boolean enCurso = i == actual;
			int tinta = enCurso ? tema.papelTinta : hecha ? MenuBackrooms.alfa(tema.papelTinta, 0.55F) : tema.papelVacio;
			casilla(g, 12, (int) y + 2, tinta, hecha, enCurso);
			String titulo = (i + 1) + ". " + m.titulo;
			float alto = Texto.parrafo(g, titulo, 46, y, 15, x1 - 62, 1.3F, tinta);
			if (hecha) {
				g.fill(46, (int) (y + 9), 46 + (int) Texto.anchoMaquina(titulo, 15, 0), (int) (y + 10), 0xCCB0281D);
			}
			y += alto;
			if (enCurso) {
				y += Texto.parrafo(g, m.descripcion, 46, y + 2, 12.5F, x1 - 62, 1.45F, tema.papelTexto) + 4;
				if (m == TipoMision.CASETES) {
					Texto.hud(g, "CASETES " + MisionesCliente.casetes() + "/" + MisionesCliente.necesarios(), 46, y, 22, 0.08F, tema.papelFecha);
					y += 24;
				}
			}
			y += 12;
		}
		if (actual >= lista.size()) {
			Texto.parrafo(g, "Has completado tus misiones. Sigue con vida.", 46, y, 13, x1 - 62, 1.45F, tema.papelFecha);
		}
		detector(g, tema, MisionesCliente.distancia(), MisionesCliente.rumbo() - mirando, y1);
		p.popMatrix();
	}

	private static void casilla(GuiGraphics g, int cx, int cy, int tinta, boolean hecha, boolean enCurso) {
		g.renderOutline(cx, cy, 14, 14, tinta);
		if (hecha) {
			for (int k = 0; k < 10; k++) {
				g.fill(cx + 2 + k, cy + 2 + k, cx + 4 + k, cy + 4 + k, ROJO);
				g.fill(cx + 11 - k, cy + 2 + k, cx + 13 - k, cy + 4 + k, ROJO);
			}
		} else if (enCurso) {
			for (int k = 0; k < 5; k++) {
				g.fill(cx + 4 + k, cy + 2 + k, cx + 5 + k, cy + 12 - k, tinta);
			}
		}
	}

	/**
	 * Detector de casetes al pie de la hoja. distancia: metros (solo llega de
	 * cerca), -2 buscando sin senal, -1 la mision en curso no es de casetes.
	 */
	private static void detector(GuiGraphics g, Tema tema, int distancia, float grados, int y1) {
		if (distancia == -1) {
			return;
		}
		int cx = 70;
		int cy = y1 - 46;
		Texto.maquina(g, "SEÑAL DE CASETE", 106, y1 - 64, 12, 0.14F, tema.papelTexto);
		g.renderOutline(cx - 22, cy - 22, 44, 44, MenuBackrooms.alfa(tema.papelTinta, 0.4F));
		float t = Fondo.segundos();
		if (distancia < 0) {
			// sin senal: solo nieve en la pantallita
			for (int i = 0; i < 26; i++) {
				int h = (int) (Math.floor(t * 12) * 131 + i * 7919);
				int px = Math.floorMod(h * 37, 40) - 20;
				int py = Math.floorMod(h * 53, 40) - 20;
				g.fill(cx + px, cy + py, cx + px + 2, cy + py + 2, MenuBackrooms.alfa(tema.papelTinta, 0.35F));
			}
			Texto.hud(g, "SIN SEÑAL", 106, y1 - 46, 26, 0.06F, tema.papelVacio);
			return;
		}
		float cerca = 1.0F - Math.min(1.0F, distancia / 70.0F);
		int barras = cerca > 0.7F ? 3 : cerca > 0.35F ? 2 : 1;
		String fuerza = barras == 3 ? "FUERTE" : barras == 2 ? "MEDIA" : "DÉBIL";
		// la aguja tiembla mas cuanto mas debil es la senal
		float temblor = 8 + 42 * (1 - cerca);
		float ruido = (float) (Math.sin(t * 2.3) * 0.6 + Math.sin(t * 5.7 + 1.3) * 0.3 + Math.sin(t * 13.1) * 0.1);
		flecha(g, tema, cx, cy, grados + ruido * temblor);
		for (int i = 0; i < 3; i++) {
			int a = 7 + i * 6;
			g.fill(106 + i * 10, y1 - 22 - a, 113 + i * 10, y1 - 22, i < barras ? tema.papelFecha : MenuBackrooms.alfa(tema.papelTinta, 0.18F));
		}
		Texto.hud(g, fuerza, 146, y1 - 46, 26, 0.06F, tema.papelTinta);
	}

	/** Flecha hacia el casete: arriba es hacia donde mira el jugador. */
	private static void flecha(GuiGraphics g, Tema tema, int cx, int cy, float grados) {
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(cx, cy);
		p.rotate((float) Math.toRadians(grados));
		g.fill(-2, -4, 2, 16, tema.papelFecha);
		for (int k = 0; k < 8; k++) {
			g.fill(-k, -16 + k, k + 1, -15 + k, tema.papelFecha);
		}
		p.popMatrix();
	}
}
