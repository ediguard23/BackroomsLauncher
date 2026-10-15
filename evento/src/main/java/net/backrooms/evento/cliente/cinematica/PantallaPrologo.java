package net.backrooms.evento.cliente.cinematica;

import java.util.Locale;
import java.util.Random;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

/**
 * El prologo del /start (LORE.md, 7: el jugador firma sin saber que firma), antes de la
 * cinematica del ascensor (CinematicaCliente, guion 0), que empieza justo donde acaba este:
 *
 *   0 - 5.5    la sala de reuniones: una mesa, la lampara, alguien con bata al otro lado
 *              (no se le ve la cara) que te acerca un papel. "Gracias por venir..."
 *   5.5 - 16   el contrato, de cerca: PROYECTO UMBRAL, CONSENTIMIENTO INFORMADO; la vista
 *              baja por la pagina, se marca la clausula de las consecuencias y firmas (tu nick)
 *  16 - 20     la sala otra vez: se lleva el papel. "Perfecto. Acompaneme."
 *  20 - 26     el pasillo hasta el ascensor (las luces del techo pasan), las puertas se abren
 *              y funde a blanco: ahi empieza la cinta del ascensor.
 *
 * Todo dibujado aqui, en 2D (sin modelos), con el sonido de cinematica.prologo
 * (tools/sonidos/prologo.js). Bloquea el control como la del ascensor.
 */
public class PantallaPrologo extends Screen {
	public static final float DURACION = 26.0F;
	private static final Identifier SONIDO = BackroomsEvento.id("cinematica.prologo");

	private static final int MADERA = 0xFF3B2618;
	private static final int MADERA_CLARA = 0xFF5A3A24;
	private static final int PARED = 0xFF2A3436;
	private static final int PAPEL = 0xFFEDE6D3;
	private static final int TINTA = 0xFF26231F;
	private static final int TINTA_FIRMA = 0xFF1C2E7A;

	private final long inicio = System.nanoTime();
	private final String nick;
	private final Runnable despues;
	private SoundInstance sonido;
	private boolean acabado;

	public PantallaPrologo(Runnable despues) {
		super(Component.literal("Proyecto Umbral"));
		Minecraft mc = Minecraft.getInstance();
		this.nick = mc.player != null ? mc.player.getGameProfile().name() : "Participante";
		this.despues = despues;
	}

	public static void empezar(Runnable despues) {
		Minecraft mc = Minecraft.getInstance();
		mc.getSoundManager().stop(net.backrooms.evento.Sonidos.MEGAFONIA_VESTIBULO.location(), null);
		mc.setScreen(new PantallaPrologo(despues));
	}

	private float t() {
		return (System.nanoTime() - this.inicio) / 1.0e9F;
	}

	@Override
	protected void init() {
		GLFW.glfwSetInputMode(this.minecraft.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
		if (this.sonido == null) {
			this.sonido = new SimpleSoundInstance(SONIDO, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
				SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
			this.minecraft.getSoundManager().play(this.sonido);
		}
	}

	@Override
	public void removed() {
		long ventana = this.minecraft.getWindow().handle();
		if (GLFW.glfwGetInputMode(ventana, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN) {
			GLFW.glfwSetInputMode(ventana, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		if (!this.acabado && this.t() >= DURACION) {
			this.acabado = true;
			this.despues.run();
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		float t = this.t();
		int w = this.width;
		int h = this.height;
		g.fill(0, 0, w, h, 0xFF000000);
		if (t < 5.5F) {
			this.sala(g, w, h, t, Mth.clamp((t - 1.0F) / 3.5F, 0.0F, 1.0F), false);
		} else if (t < 16.0F) {
			this.contrato(g, w, h, t - 5.5F);
		} else if (t < 20.0F) {
			this.sala(g, w, h, t, 1.0F - Mth.clamp((t - 16.6F) / 1.4F, 0.0F, 1.0F), true);
		} else {
			this.pasillo(g, w, h, t - 20.0F);
		}
		// fundidos entre escenas
		float negro = Math.max(Math.max(1.0F - t / 1.2F, bache(t, 5.5F)), Math.max(bache(t, 16.0F), bache(t, 20.0F)));
		if (negro > 0) {
			g.fill(0, 0, w, h, Math.round(Mth.clamp(negro, 0, 1) * 255) << 24);
		}
		// al final, la luz del ascensor lo llena todo
		float blanco = Mth.clamp((t - 24.6F) / 1.2F, 0.0F, 1.0F);
		if (blanco > 0) {
			g.fill(0, 0, w, h, (Math.round(blanco * 255) << 24) | 0xFFF2DA);
		}
		this.subtitulos(g, w, h, t);
		// las bandas de cine
		int banda = h / 11;
		g.fill(0, 0, w, banda, 0xFF000000);
		g.fill(0, h - banda, w, h, 0xFF000000);
	}

	/** Un fundido a negro de ida y vuelta centrado en `en` (0,5 s cada lado). */
	private static float bache(float t, float en) {
		float d = Math.abs(t - en);
		return d < 0.5F ? 1.0F - d / 0.5F : 0.0F;
	}

	/* -------------------------------------------------------------- dibujo */

	/** Relleno de un cuadrilatero convexo (a, b, c, d en orden) por lineas horizontales. */
	private static void cuad(GuiGraphics g, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int color) {
		float[] xs = {ax, bx, cx, dx};
		float[] ys = {ay, by, cy, dy};
		int y0 = (int) Math.floor(Math.min(Math.min(ay, by), Math.min(cy, dy)));
		int y1 = (int) Math.ceil(Math.max(Math.max(ay, by), Math.max(cy, dy)));
		for (int y = y0; y < y1; y++) {
			float yc = y + 0.5F;
			float min = Float.MAX_VALUE;
			float max = -Float.MAX_VALUE;
			for (int i = 0; i < 4; i++) {
				int j = (i + 1) % 4;
				float ya = ys[i];
				float yb = ys[j];
				if ((yc >= ya && yc < yb) || (yc >= yb && yc < ya)) {
					float x = xs[i] + (yc - ya) / (yb - ya) * (xs[j] - xs[i]);
					min = Math.min(min, x);
					max = Math.max(max, x);
				}
			}
			if (max > min) {
				g.fill(Math.round(min), y, Math.round(max), y + 1, color);
			}
		}
	}

	/** Un circulo de luz suave: elipses (aplastadas, sobre la mesa) con alfa decreciente. */
	private static void halo(GuiGraphics g, float cx, float cy, float r, int rgb, float fuerza) {
		for (int k = 12; k >= 1; k--) {
			float rr = r * k / 12.0F;
			float ry = rr * 0.55F;
			int a = Math.round(fuerza * 16 * (13 - k) / 12.0F);
			for (int y = Math.round(-ry); y <= Math.round(ry); y++) {
				float m = rr * (float) Math.sqrt(Math.max(0.0, 1.0 - (y / ry) * (y / ry)));
				g.fill(Math.round(cx - m), Math.round(cy + y), Math.round(cx + m), Math.round(cy + y) + 1, (a << 24) | (rgb & 0xFFFFFF));
			}
		}
	}

	private static int oscurecer(int color, float k) {
		int r = Math.round(((color >> 16) & 255) * k);
		int gg = Math.round(((color >> 8) & 255) * k);
		int b = Math.round((color & 255) * k);
		return (color & 0xFF000000) | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(gg, 0, 255) << 8) | Mth.clamp(b, 0, 255);
	}

	/**
	 * La sala de reuniones vista desde tu silla: la pared con el rotulo del proyecto, alguien
	 * con bata al otro lado (la cara fuera de cuadro), la mesa y la lampara. `papel` 0..1 es
	 * cuanto ha avanzado el contrato hacia ti; `seLoLleva` lo hace volver hacia el.
	 */
	private void sala(GuiGraphics g, int w, int h, float t, float papel, boolean seLoLleva) {
		float hor = h * 0.58F;
		// la pared, con un zocalo y un cuadro
		for (int y = 0; y < hor; y++) {
			float k = 0.75F + 0.35F * y / hor;
			g.fill(0, y, w, y + 1, oscurecer(PARED, k));
		}
		g.fill(0, Math.round(hor - h * 0.05F), w, Math.round(hor), 0xFF1D2426);
		g.fill(Math.round(w * 0.08F), Math.round(h * 0.16F), Math.round(w * 0.27F), Math.round(h * 0.33F), 0xFF141A1B);
		Texto.hud(g, "PROYECTO UMBRAL", w * 0.095F, h * 0.21F, h * 0.028F, 0.08F, 0xFF8FBDB6);
		Texto.hud(g, "SALA 3", w * 0.095F, h * 0.255F, h * 0.022F, 0.08F, 0xFF55706C);
		// el reloj de pared
		float rx = w * 0.82F;
		float ry = h * 0.22F;
		float rr = h * 0.06F;
		g.fill(Math.round(rx - rr), Math.round(ry - rr), Math.round(rx + rr), Math.round(ry + rr), 0xFF0E1213);
		g.fill(Math.round(rx - rr * 0.85F), Math.round(ry - rr * 0.85F), Math.round(rx + rr * 0.85F), Math.round(ry + rr * 0.85F), 0xFFD9D6CC);
		float seg = t * Mth.TWO_PI / 60.0F * 6.0F;
		cuad(g, rx - 1, ry, rx + 1, ry, rx + 1 + Mth.sin(seg) * rr * 0.75F, ry - Mth.cos(seg) * rr * 0.75F, rx - 1 + Mth.sin(seg) * rr * 0.75F, ry - Mth.cos(seg) * rr * 0.75F, 0xFF8A1C14);
		g.fill(Math.round(rx - 1), Math.round(ry - rr * 0.6F), Math.round(rx + 1), Math.round(ry), 0xFF151515);
		// la persona de la bata: hombros y torso (la cabeza queda por encima del encuadre de la mesa)
		float cx = w * 0.5F;
		float hombros = h * 0.18F;
		float cintura = hor + h * 0.02F;
		cuad(g, cx - w * 0.17F, hombros + h * 0.06F, cx + w * 0.17F, hombros + h * 0.06F, cx + w * 0.19F, cintura, cx - w * 0.19F, cintura, 0xFFDCDAD2);
		cuad(g, cx - w * 0.10F, hombros, cx + w * 0.10F, hombros, cx + w * 0.17F, hombros + h * 0.06F, cx - w * 0.17F, hombros + h * 0.06F, 0xFFE6E4DC);
		// las solapas de la bata
		cuad(g, cx - w * 0.035F, hombros, cx - w * 0.075F, hombros, cx - w * 0.03F, hombros + h * 0.2F, cx - w * 0.02F, hombros + h * 0.2F, 0xFFC4C2B9);
		cuad(g, cx + w * 0.035F, hombros, cx + w * 0.075F, hombros, cx + w * 0.03F, hombros + h * 0.2F, cx + w * 0.02F, hombros + h * 0.2F, 0xFFC4C2B9);
		// los brazos, mas oscuros a los lados
		cuad(g, cx - w * 0.17F, hombros + h * 0.06F, cx - w * 0.13F, hombros + h * 0.07F, cx - w * 0.15F, cintura, cx - w * 0.19F, cintura, 0xFFC8C6BE);
		cuad(g, cx + w * 0.17F, hombros + h * 0.06F, cx + w * 0.13F, hombros + h * 0.07F, cx + w * 0.15F, cintura, cx + w * 0.19F, cintura, 0xFFC8C6BE);
		// el cuello: la barbilla asoma en la sombra
		g.fill(Math.round(cx - w * 0.03F), Math.round(hombros - h * 0.04F), Math.round(cx + w * 0.03F), Math.round(hombros + h * 0.01F), 0xFF8E7462);
		// la camisa y la corbata, y la tarjeta colgada de la bata
		cuad(g, cx - w * 0.035F, hombros, cx + w * 0.035F, hombros, cx + w * 0.02F, cintura, cx - w * 0.02F, cintura, 0xFF2B3A40);
		cuad(g, cx - w * 0.008F, hombros + h * 0.01F, cx + w * 0.008F, hombros + h * 0.01F, cx + w * 0.012F, hombros + h * 0.25F, cx - w * 0.012F, hombros + h * 0.25F, 0xFF101315);
		g.fill(Math.round(cx + w * 0.07F), Math.round(hombros + h * 0.12F), Math.round(cx + w * 0.11F), Math.round(hombros + h * 0.18F), 0xFFF5F2E8);
		g.fill(Math.round(cx + w * 0.072F), Math.round(hombros + h * 0.125F), Math.round(cx + w * 0.108F), Math.round(hombros + h * 0.14F), 0xFF2E6E66);
		// la sombra de arriba: la cara nunca se ve
		for (int y = 0; y < Math.round(hombros + h * 0.05F); y++) {
			int a = Math.round(220 * (1.0F - y / (hombros + h * 0.05F)));
			g.fill(Math.round(cx - w * 0.2F), y, Math.round(cx + w * 0.2F), y + 1, (a << 24));
		}
		// la mesa: el canto y el tablero hacia ti
		cuad(g, -w * 0.1F, hor, w * 1.1F, hor, w * 1.3F, h, -w * 0.3F, h, MADERA);
		g.fill(0, Math.round(hor), w, Math.round(hor + h * 0.012F), MADERA_CLARA);
		for (int k = 0; k < 9; k++) {
			float yy = hor + (h - hor) * (k + 1) / 10.0F;
			g.fill(0, Math.round(yy), w, Math.round(yy) + 1, 0x18000000);
		}
		// sus manos sobre la mesa
		g.fill(Math.round(cx - w * 0.16F), Math.round(hor + h * 0.01F), Math.round(cx - w * 0.10F), Math.round(hor + h * 0.05F), 0xFFC9A98E);
		// la lampara de la derecha y su luz
		float lx = w * 0.84F;
		g.fill(Math.round(lx - w * 0.008F), Math.round(hor - h * 0.18F), Math.round(lx + w * 0.008F), Math.round(hor + h * 0.01F), 0xFF151515);
		cuad(g, lx - w * 0.06F, hor - h * 0.16F, lx + w * 0.02F, hor - h * 0.22F, lx + w * 0.05F, hor - h * 0.17F, lx - w * 0.03F, hor - h * 0.11F, 0xFF1F4A44);
		halo(g, lx - w * 0.02F, hor + h * 0.06F, w * 0.22F, 0xFFD9A0, 0.9F);
		// el contrato: avanza por la mesa hacia ti (o vuelve hacia el)
		float avance = seLoLleva ? 1.0F - papel : papel;
		float py = hor + h * 0.03F + avance * h * 0.22F;
		float pw = w * (0.09F + avance * 0.07F);
		float ph = h * (0.05F + avance * 0.08F);
		float px = cx + w * 0.02F - (seLoLleva ? 0 : (1 - avance) * w * 0.05F);
		cuad(g, px - pw, py, px + pw, py, px + pw * 1.12F, py + ph, px - pw * 1.12F, py + ph, PAPEL);
		for (int k = 1; k < 6; k++) {
			float yy = py + ph * k / 7.0F;
			g.fill(Math.round(px - pw * 0.8F), Math.round(yy), Math.round(px + pw * (k == 1 ? 0.2F : 0.8F)), Math.round(yy) + 1, 0x5526231F);
		}
		// el boligrafo, al lado del papel
		if (!seLoLleva) {
			cuad(g, px + pw * 1.3F, py + ph * 0.3F, px + pw * 1.36F, py + ph * 0.25F, px + pw * 1.9F, py + ph * 0.95F, px + pw * 1.84F, py + ph, 0xFF0F1A33);
		}
		// la vineta de la sala
		vineta(g, w, h, 0.55F);
	}

	private static void vineta(GuiGraphics g, int w, int h, float fuerza) {
		int pasos = 12;
		for (int i = 0; i < pasos; i++) {
			int a = Math.round(fuerza * 255 * (1 - i / (float) pasos) * (1 - i / (float) pasos) * 0.5F);
			int mx = Math.round(w * 0.012F * i);
			int my = Math.round(h * 0.012F * i);
			int gx = Math.max(1, Math.round(w * 0.012F));
			int gy = Math.max(1, Math.round(h * 0.012F));
			g.fill(mx, my, w - mx, my + gy, a << 24);
			g.fill(mx, h - my - gy, w - mx, h - my, a << 24);
			g.fill(mx, my, mx + gx, h - my, a << 24);
			g.fill(w - mx - gx, my, w - mx, h - my, a << 24);
		}
	}

	/**
	 * El contrato de cerca: la pagina sobre la mesa y la vista que baja por ella (u en
	 * segundos desde que empieza la escena): se lee, se marca la clausula y firmas.
	 */
	private void contrato(GuiGraphics g, int w, int h, float u) {
		// la mesa debajo, con la luz de la lampara
		g.fill(0, 0, w, h, MADERA);
		halo(g, w * 0.7F, h * 0.3F, w * 0.6F, 0xFFD9A0, 0.7F);
		float pw = Math.min(w * 0.46F, h * 0.95F);
		float x0 = w * 0.5F - pw / 2;
		float alto = pw * 1.45F;
		// la vista baja por la pagina: de la cabecera (0..3 s) a la firma (7..10,5 s)
		float bajada = Mth.clamp((u - 2.2F) / 4.8F, 0.0F, 1.0F);
		bajada = bajada * bajada * (3 - 2 * bajada);
		float y0 = h * 0.12F - bajada * (alto - h * 0.78F) + Mth.sin(u * 0.9F) * h * 0.004F;
		// sombra y papel
		g.fill(Math.round(x0 + pw * 0.015F), Math.round(y0 + pw * 0.02F), Math.round(x0 + pw * 1.015F), Math.round(y0 + alto + pw * 0.02F), 0x66000000);
		g.fill(Math.round(x0), Math.round(y0), Math.round(x0 + pw), Math.round(y0 + alto), PAPEL);
		float m = pw * 0.08F;
		float tx = x0 + m;
		float tw = pw - 2 * m;
		float tam = pw * 0.026F;
		float y = y0 + m;
		// membrete
		g.fill(Math.round(tx), Math.round(y), Math.round(tx + pw * 0.05F), Math.round(y + pw * 0.05F), 0xFF2E6E66);
		Texto.hud(g, "PROYECTO UMBRAL", tx + pw * 0.07F, y + pw * 0.004F, tam * 1.5F, 0.08F, 0xFF2E6E66);
		Texto.maquina(g, "Instituto de Estudios de Percepción", tx + pw * 0.07F, y + pw * 0.035F, tam * 0.85F, 0.0F, 0xFF55706C);
		y += pw * 0.1F;
		g.fill(Math.round(tx), Math.round(y), Math.round(tx + tw), Math.round(y) + 1, 0x9926231F);
		y += pw * 0.04F;
		Texto.maquina(g, "CONSENTIMIENTO INFORMADO", tx, y, tam * 1.35F, 0.05F, TINTA);
		y += tam * 2.6F;
		String participante = String.format(Locale.ROOT, "Participante: %s", this.nick);
		Texto.maquina(g, participante, tx, y, tam, 0.0F, TINTA);
		y += tam * 1.8F;
		y += Texto.parrafo(g, "Por la presente, el participante acepta formar parte de un estudio experimental "
			+ "relacionado con nuevas tecnologías de realidad y percepción, dirigido por el Proyecto Umbral.", tx, y, tam, tw, 1.45F, TINTA) + tam * 0.8F;
		y += Texto.parrafo(g, "El participante podrá abandonar el estudio al alcanzar la salida designada. "
			+ "Durante el estudio será observado y registrado en todo momento.", tx, y, tam, tw, 1.45F, TINTA) + tam * 0.8F;
		// la clausula, en letra pequena: se marca en amarillo mientras se lee
		float yClausula = y;
		String clausula = "Cláusula 14.c: el participante acepta cualquier consecuencia física, psicológica o perceptiva "
			+ "derivada del experimento. Al firmar, el participante deja de ser considerado ciudadano a efectos del estudio "
			+ "y pasa a ser considerado sujeto.";
		float altoClausula = Texto.parrafo(g, clausula, tx, y, tam * 0.78F, tw, 1.4F, 0xFF4A4640);
		float marca = Mth.clamp((u - 4.2F) / 2.0F, 0.0F, 1.0F);
		if (marca > 0) {
			g.fill(Math.round(tx - 2), Math.round(yClausula - 2), Math.round(tx - 2 + (tw + 4) * marca), Math.round(yClausula + altoClausula),
				0x40F2D23C);
		}
		y += altoClausula + tam * 1.6F;
		Texto.parrafo(g, "Lugar y fecha: ___________________     Expediente: S-" + Math.abs(this.nick.hashCode() % 1000), tx, y, tam * 0.9F, tw, 1.4F, TINTA);
		y += tam * 3.0F;
		// la firma: la linea y el nombre escrito a mano, letra a letra
		float yFirma = y + tam * 2.2F;
		g.fill(Math.round(tx), Math.round(yFirma), Math.round(tx + tw * 0.6F), Math.round(yFirma) + 1, TINTA);
		Texto.maquina(g, "Firma del participante", tx, yFirma + tam * 0.4F, tam * 0.8F, 0.0F, 0xFF4A4640);
		float firma = Mth.clamp((u - 7.2F) / 2.4F, 0.0F, 1.0F);
		float fin = this.firma(g, tx + tw * 0.04F, yFirma - tam * 1.9F, tam * 1.9F, firma);
		// el boligrafo, en la punta de lo escrito mientras firmas
		if (u > 6.8F && u < 10.2F) {
			float bx = fin;
			float by = yFirma - tam * 0.9F;
			cuad(g, bx, by, bx + pw * 0.012F, by - pw * 0.012F, bx + pw * 0.2F, by - pw * 0.24F, bx + pw * 0.186F, by - pw * 0.228F, 0xFF0F1A33);
			cuad(g, bx, by, bx + pw * 0.006F, by - pw * 0.006F, bx + pw * 0.03F, by - pw * 0.036F, bx + pw * 0.024F, by - pw * 0.03F, 0xFFB8B4AA);
		}
		vineta(g, w, h, 0.6F);
	}

	/** Escribe el nick como a mano (cada letra un poco torcida y a destiempo) hasta `k` 0..1; devuelve donde va la punta. */
	private float firma(GuiGraphics g, float x, float y, float tam, float k) {
		Random r = new Random(this.nick.hashCode());
		String s = this.nick;
		int letras = s.length();
		float hasta = k * letras;
		float cx = x;
		Matrix3x2fStack p = g.pose();
		for (int i = 0; i < letras && i < hasta; i++) {
			String c = s.substring(i, i + 1);
			float ancho = Texto.anchoMaquina(c, tam, 0.0F);
			float sube = (r.nextFloat() - 0.5F) * tam * 0.25F;
			float giro = (r.nextFloat() - 0.5F) * 0.35F - 0.12F;
			p.pushMatrix();
			p.translate(cx, y + sube);
			p.rotate(giro);
			// la letra que se esta escribiendo, aun a medias
			float parte = Mth.clamp(hasta - i, 0.0F, 1.0F);
			int alfa = Math.round(255 * parte);
			Texto.maquina(g, c, 0, 0, tam, 0.0F, (alfa << 24) | (TINTA_FIRMA & 0xFFFFFF));
			p.popMatrix();
			cx += ancho * 0.82F;
		}
		return cx;
	}

	/**
	 * El pasillo hasta el ascensor, andando (u en segundos): paredes blancas con la franja
	 * cian, paneles de luz que pasan por encima y, al fondo, las puertas que se acercan y se abren.
	 */
	private void pasillo(GuiGraphics g, int w, int h, float u) {
		float vx = w * 0.5F + Mth.sin(u * 4.2F) * w * 0.004F;
		float vy = h * 0.47F + Math.abs(Mth.sin(u * 4.2F)) * h * 0.006F;
		// cuanto se acerca la puerta del fondo
		float cerca = Mth.clamp(u / 4.2F, 0.0F, 1.0F);
		cerca = cerca * cerca;
		float pw = w * (0.06F + cerca * 0.3F);
		float ph = h * (0.12F + cerca * 0.62F);
		float fx0 = vx - pw;
		float fx1 = vx + pw;
		float fy0 = vy - ph * 0.55F;
		float fy1 = vy + ph * 0.45F;
		// techo, suelo y paredes como trapecios hasta el marco del fondo
		cuad(g, 0, 0, w, 0, fx1, fy0, fx0, fy0, 0xFFC9CCC8);
		cuad(g, 0, h, w, h, fx1, fy1, fx0, fy1, 0xFF6E7471);
		cuad(g, 0, 0, fx0, fy0, fx0, fy1, 0, h, 0xFFE2E2DC);
		cuad(g, w, 0, fx1, fy0, fx1, fy1, w, h, 0xFFD6D7D1);
		// la franja cian de las paredes
		for (int lado = 0; lado < 2; lado++) {
			float bx = lado == 0 ? 0 : w;
			float ex = lado == 0 ? fx0 : fx1;
			float ya = h * 0.62F;
			float yb = fy0 + (fy1 - fy0) * 0.62F;
			cuad(g, bx, ya, ex, yb, ex, yb + ph * 0.03F, bx, ya + h * 0.05F, 0xFF2E6E66);
		}
		// los paneles del techo pasan hacia ti
		for (int k = 0; k < 6; k++) {
			float z = ((k + u * 1.6F) % 6.0F) / 6.0F;
			float d = z * z;
			float py = fy0 * d;
			float half = (pw + (w * 0.5F - pw) * d) * 0.35F;
			float grueso = 2 + d * h * 0.05F;
			g.fill(Math.round(vx - half), Math.round(py), Math.round(vx + half), Math.round(py + grueso), 0xFFFFFDF2);
		}
		// las puertas del ascensor: acero; se abren al llegar
		float abre = Mth.clamp((u - 4.0F) / 1.2F, 0.0F, 1.0F);
		g.fill(Math.round(fx0), Math.round(fy0), Math.round(fx1), Math.round(fy1), 0xFF3E4442);
		float interior = pw * abre;
		if (interior > 0) {
			g.fill(Math.round(vx - interior), Math.round(fy0), Math.round(vx + interior), Math.round(fy1), 0xFFF0D9A8);
		}
		g.fill(Math.round(fx0), Math.round(fy0), Math.round(vx - interior), Math.round(fy1), 0xFF8E9592);
		g.fill(Math.round(vx + interior), Math.round(fy0), Math.round(fx1), Math.round(fy1), 0xFF9AA19E);
		g.fill(Math.round(vx - interior) - 1, Math.round(fy0), Math.round(vx - interior), Math.round(fy1), 0xFF2A2E2D);
		// el indicador encima de la puerta
		float iy = fy0 - ph * 0.12F;
		g.fill(Math.round(vx - pw * 0.25F), Math.round(iy), Math.round(vx + pw * 0.25F), Math.round(iy + ph * 0.08F), 0xFF0B0B0B);
		Texto.hud(g, "0", vx - Texto.anchoHud("0", ph * 0.07F, 0.0F) / 2, iy + ph * 0.004F, ph * 0.07F, 0.0F, 0xFFFF3A1C);
		vineta(g, w, h, 0.5F);
	}

	/** Lo que se dice (no se oyen voces: subtitulos, como una grabacion de vigilancia). */
	private void subtitulos(GuiGraphics g, int w, int h, float t) {
		String s = null;
		if (t > 1.6F && t < 5.2F) {
			s = "—Gracias por venir. Solo necesitamos su firma.";
		} else if (t > 7.4F && t < 11.0F) {
			s = "—Es un estudio sencillo. Léalo con calma.";
		} else if (t > 12.3F && t < 15.6F) {
			s = "—Aquí abajo, por favor.";
		} else if (t > 16.5F && t < 19.6F) {
			s = "—Perfecto. Acompáñeme: el ascensor le espera.";
		} else if (t > 21.0F && t < 24.0F) {
			s = "—Le veremos al otro lado.";
		}
		if (s == null) {
			return;
		}
		float tam = h / 26.0F;
		float ancho = Texto.anchoMaquina(s, tam, 0.0F);
		float x = w / 2.0F - ancho / 2.0F;
		float y = h - h / 11.0F - tam * 1.9F;
		g.fill(Math.round(x - tam * 0.5F), Math.round(y - tam * 0.25F), Math.round(x + ancho + tam * 0.5F), Math.round(y + tam * 1.15F), 0x99000000);
		Texto.maquina(g, s, x, y, tam, 0.0F, 0xFFF2EEE4);
	}
}
