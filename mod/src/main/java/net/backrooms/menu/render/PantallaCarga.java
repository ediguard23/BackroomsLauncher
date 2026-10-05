package net.backrooms.menu.render;

import java.time.LocalDateTime;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

/**
 * Lo que se ve mientras Minecraft carga, en vez de la pantalla roja de Mojang
 * Studios: una cinta VHS reproduciendose (PLAY, rayas de barrido, ruido), el
 * logo del evento con su tubo fluorescente, la barra de carga del launcher y
 * la firma de PeakMC Studio.
 *
 * Solo usa rectangulos y las texturas de Logos: en la primera carga todavia
 * no hay fuentes ni shaders del juego aparte de los de la GUI. Se dibuja en
 * pixeles reales de la ventana (no en unidades de GUI) para que las letras de
 * Osd queden nitidas a cualquier escala.
 */
public final class PantallaCarga {
	public static final int NEGRO = 0xFF0D0B05;
	private static final int TUBO = 0xFFF4C8;
	private static final String[] MESES = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"};
	private static final String[] CONSEJOS = {
		"NO OS SEPAREIS.",
		"SI LAS LUCES PARPADEAN, QUEDATE QUIETO.",
		"EL ZUMBIDO NUNCA SE APAGA. ACOSTUMBRATE.",
		"SI OYES TU NOMBRE, NO CONTESTES.",
		"EL AGUA DE ALMENDRAS ES TU MEJOR AMIGA.",
		"LA MOQUETA HUMEDA AMORTIGUA TUS PASOS. Y LOS SUYOS.",
		"NADIE HA CONTADO TODAS LAS HABITACIONES.",
		"NO HAGAS NOCLIP DONDE NO DEBES.",
		"SI ENCUENTRAS UNA SALIDA, DESCONFIA."
	};

	private static final long INICIO = System.currentTimeMillis();
	private static final Random AZAR = new Random();
	private static final int PRIMER_CONSEJO = AZAR.nextInt(CONSEJOS.length);
	private static long proximoParpadeo = INICIO + 2500;
	private static long parpadeoDesde = -1;

	private PantallaCarga() {
	}

	private static int alfa(int rgb, float a) {
		return (Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static String dos(long n) {
		return n < 10 ? "0" + n : Long.toString(n);
	}

	/** Brillo del tubo: casi siempre 1; cada pocos segundos, un parpadeo como el del launcher. */
	private static float tubo(long ahora) {
		if (ahora >= proximoParpadeo) {
			parpadeoDesde = ahora;
			proximoParpadeo = ahora + 3500 + AZAR.nextInt(5000);
		}
		long t = ahora - parpadeoDesde;
		if (parpadeoDesde < 0 || t > 260) {
			return 1.0F;
		}
		return t < 70 ? 0.25F : t < 150 ? 1.0F : t < 210 ? 0.4F : 1.0F;
	}

	/**
	 * @param fondo    opacidad del fondo (0-1): las cargas de recursos en
	 *                 partida entran y salen fundiendo sobre la pantalla
	 * @param contenido opacidad del logo, textos y rayas
	 * @param barra    opacidad de la barra (se va antes que el resto)
	 * @param progreso 0-1
	 */
	public static void dibujar(GuiGraphics g, float fondo, float contenido, float barra, float progreso) {
		Minecraft mc = Minecraft.getInstance();
		int escala = mc.getWindow().getGuiScale();
		int w = g.guiWidth() * escala;
		int h = g.guiHeight() * escala;
		long ahora = System.currentTimeMillis();

		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(1.0F / escala, 1.0F / escala);

		g.fill(0, 0, w, h, alfa(NEGRO, fondo));
		if (contenido <= 0.01F) {
			pose.popMatrix();
			return;
		}

		// pixel de las letras: 2 a 720p, 3 a 1080p...
		int p = Math.max(1, Math.round(h / 360.0F));

		// rayas de barrido y la banda de tracking que sube
		for (int y = 0; y < h; y += 3) {
			g.fill(0, y, w, y + 1, alfa(0x000000, 0.22F * contenido));
		}
		float banda = 1.15F - ((ahora - INICIO) % 9000L) / 9000.0F * 1.3F;
		int by = Math.round(banda * h);
		int ba = Math.round(h * 0.06F);
		g.fillGradient(0, by - ba, w, by, alfa(0xFFFAE6, 0.0F), alfa(0xFFFAE6, 0.07F * contenido));
		g.fillGradient(0, by, w, by + ba, alfa(0xFFFAE6, 0.07F * contenido), alfa(0xFFFAE6, 0.0F));
		// cortes de ruido sueltos
		for (int i = 0; i < 26; i++) {
			int ry = AZAR.nextInt(h);
			int rx = AZAR.nextInt(w);
			g.fill(rx, ry, rx + p * (4 + AZAR.nextInt(40)), ry + 1, alfa(0xFFFFFF, (0.05F + AZAR.nextFloat() * 0.12F) * contenido));
		}
		// vineta arriba y abajo
		int v = Math.round(h * 0.18F);
		g.fillGradient(0, 0, w, v, alfa(0x000000, 0.55F * contenido), alfa(0x000000, 0.0F));
		g.fillGradient(0, h - v, w, h, alfa(0x000000, 0.0F), alfa(0x000000, 0.45F * contenido));

		// logo, con su corrimiento de color y el parpadeo del tubo
		float luz = tubo(ahora);
		int altoLogo = Math.round(Math.min(h * 0.44F, w * 0.62F / Math.max(0.1F, Logos.BACKROOMS.proporcion())));
		int anchoLogo = Math.round(altoLogo * Logos.BACKROOMS.proporcion());
		int lx = (w - anchoLogo) / 2;
		int ly = Math.round(h * 0.40F) - altoLogo / 2;
		int d = Math.max(2, p);
		Logos.BACKROOMS.dibujar(g, lx + d, ly, altoLogo, 1.0F, alfa(0xFF2846, 0.35F * contenido * luz));
		Logos.BACKROOMS.dibujar(g, lx - d, ly, altoLogo, 1.0F, alfa(0x00D2FF, 0.3F * contenido * luz));
		Logos.BACKROOMS.dibujar(g, lx, ly, altoLogo, 1.0F, alfa(0xFFFFFF, contenido * (0.55F + 0.45F * luz)));

		// barra de carga del launcher: borde fino y relleno a tramos
		if (barra > 0.01F) {
			int bw = Math.round(Math.min(w * 0.5F, Math.max(anchoLogo, h * 0.7F)));
			int bx = (w - bw) / 2;
			int bh = 5 * p;
			int byb = ly + altoLogo + 10 * p + Osd.alto(p);
			float a = contenido * barra;
			int pct = Math.round(progreso * 100);
			String etiqueta = "CARGANDO EL NIVEL" + ".".repeat((int) ((ahora / 400) % 4));
			Osd.texto(g, etiqueta, bx, byb - Osd.alto(p) - 3 * p, p, alfa(TUBO, 0.9F * a));
			String num = pct + "%";
			Osd.texto(g, num, bx + bw - Osd.ancho(num, p), byb - Osd.alto(p) - 3 * p, p, alfa(TUBO, 0.9F * a));
			int borde = alfa(TUBO, 0.6F * a);
			g.fill(bx, byb, bx + bw, byb + 1, borde);
			g.fill(bx, byb + bh - 1, bx + bw, byb + bh, borde);
			g.fill(bx, byb, bx + 1, byb + bh, borde);
			g.fill(bx + bw - 1, byb, bx + bw, byb + bh, borde);
			int lleno = Math.round((bw - 2 * p) * progreso);
			int tramo = 4 * p;
			for (int x = 0; x < lleno; x += tramo + p) {
				g.fill(bx + p + x, byb + p, bx + p + Math.min(lleno, x + tramo), byb + bh - p, alfa(TUBO, a));
			}
			// consejo del explorador, cambia cada 5 s
			String consejo = CONSEJOS[(int) ((PRIMER_CONSEJO + (ahora - INICIO) / 5000) % CONSEJOS.length)];
			Osd.texto(g, consejo, (w - Osd.ancho(consejo, p)) / 2, byb + bh + 9 * p, p, alfa(0xF2E9C4, 0.62F * a));
		}

		// HUD de la camara
		int margen = 12 * p;
		int alfaHud = Math.round(255 * contenido);
		Osd.textoCinta(g, "> PLAY", margen, margen, 2 * p, alfaHud);
		long s = (ahora - INICIO) / 1000;
		String tc = "SP " + dos(s / 3600) + ":" + dos(s / 60 % 60) + ":" + dos(s % 60);
		Osd.texto(g, tc, w - margen - Osd.ancho(tc, p), margen + p * 3, p, alfa(TUBO, 0.85F * contenido));
		LocalDateTime f = LocalDateTime.now();
		String fecha = MESES[f.getMonthValue() - 1] + ". " + dos(f.getDayOfMonth()) + " " + f.getYear() + "  " + dos(f.getHour()) + ":" + dos(f.getMinute());
		Osd.texto(g, fecha, w - margen - Osd.ancho(fecha, p), h - margen - Osd.alto(p), p, alfa(TUBO, 0.85F * contenido));

		// firma del estudio
		int altoFirma = Math.max(16, Math.round(h * 0.075F));
		int anchoFirma = Math.round(altoFirma * Logos.PEAKMC_STUDIO.proporcion());
		String por = "DESARROLLADO POR";
		int total = Osd.ancho(por, p) + 4 * p + anchoFirma;
		int fx = (w - total) / 2;
		int fy = h - margen - altoFirma;
		Osd.texto(g, por, fx, fy + (altoFirma - Osd.alto(p)) / 2, p, alfa(0xF2E9C4, 0.75F * contenido));
		Logos.PEAKMC_STUDIO.dibujar(g, fx + Osd.ancho(por, p) + 4 * p, fy, altoFirma, 1.0F, alfa(0xFFFFFF, contenido));
		pose.popMatrix();
	}
}
