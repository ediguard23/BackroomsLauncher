package net.backrooms.evento.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import net.backrooms.menu.HojaMisiones;
import net.backrooms.menu.Tema;
import net.backrooms.menu.render.Texto;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

/**
 * Las misiones en una esquina de la pantalla, sin parar a abrir el inventario: cerradas son
 * un bollito de papel arrugado arriba a la izquierda; con la M el papel se estira y se ve la
 * hoja de MISIONES DEL EXPLORADOR (HojaMisiones) mientras se anda, corre o salta; otra vez
 * la M y se vuelve a arrugar. Solo durante la expedicion.
 */
public final class PapelMisiones {
	public static final KeyMapping TECLA = KeyBindingHelper.registerKeyBinding(
		new KeyMapping("key.backrooms_evento.misiones", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, HerramientasCliente.CATEGORIA));
	/** Lo que tarda en estirarse o arrugarse (ms). */
	private static final float DURA = 320.0F;
	private static final int MARGEN = 6;
	/** Radio del bollito (pixeles de la GUI). */
	private static final int BOLA = 15;

	private static boolean abierto;
	private static long cambio = Long.MIN_VALUE / 2;

	private PapelMisiones() {
	}

	/** Al arrancar el cliente: carga la clase para que la tecla quede registrada a tiempo. */
	public static void registrar() {
	}

	/** Cada tick: la M abre o arruga el papel. */
	public static void tick(Minecraft mc) {
		while (TECLA.consumeClick()) {
			alternar(mc);
		}
	}

	/** Abre o arruga el papel (la M; en pruebas, la orden "misiones"). */
	public static void alternar(Minecraft mc) {
		if (mc.player == null || !MisionesCliente.hayExpedicion()) {
			return;
		}
		// si estaba a medias, sigue desde donde iba
		float k = apertura();
		abierto = !abierto;
		cambio = net.minecraft.util.Util.getMillis() - (long) ((abierto ? k : 1.0F - k) * DURA);
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, abierto ? 1.15F : 0.8F, 0.9F));
	}

	public static void olvidar() {
		abierto = false;
		cambio = Long.MIN_VALUE / 2;
	}

	/** 0 arrugado, 1 estirado (con su suavizado). */
	private static float apertura() {
		float t = Mth.clamp((net.minecraft.util.Util.getMillis() - cambio) / DURA, 0.0F, 1.0F);
		float k = abierto ? t : 1.0F - t;
		return k * k * (3.0F - 2.0F * k);
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !MisionesCliente.hayExpedicion() || HerramientasCliente.subida(1.0F) > 0.5F) {
			return;
		}
		Tema tema = Tema.actual();
		float k = apertura();
		int alto = g.guiHeight();
		// la hoja ocupa algo menos de la mitad de alto de la pantalla
		float escala = Math.min(0.62F, alto * 0.48F / HojaMisiones.ALTO);
		float anchoHoja = HojaMisiones.ANCHO * escala;
		int altoDiseno = HojaMisiones.alto();
		float altoHoja = altoDiseno * escala;
		if (k < 0.02F) {
			bollito(g, tema, MARGEN + BOLA, MARGEN + BOLA, 1.0F);
			String m = "[" + TECLA.getTranslatedKeyMessage().getString().toUpperCase(java.util.Locale.ROOT) + "]";
			Texto.hud(g, m, MARGEN + BOLA - Texto.anchoHud(m, 9, 0.05F) / 2.0F, MARGEN + BOLA * 2 + 2, 9, 0.05F, 0xC8F2E6A0);
			return;
		}
		// estirandose: crece desde el bollito, primero a lo ancho y luego a lo alto, un poco torcido
		float kx = Mth.clamp(k * 1.35F, 0.0F, 1.0F);
		float ky = Mth.clamp((k - 0.15F) / 0.85F, 0.0F, 1.0F);
		float w = Mth.lerp(kx, BOLA * 2.0F, anchoHoja);
		float h = Mth.lerp(ky, BOLA * 2.0F, altoHoja);
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(MARGEN, MARGEN);
		p.rotateAbout((float) Math.toRadians((1.0F - k) * -9.0F), w / 2.0F, h / 2.0F);
		p.scale(w / HojaMisiones.ANCHO, h / altoDiseno);
		HojaMisiones.dibujar(g, tema, mc.player.getYRot());
		// las arrugas: se borran conforme se estira (y quedan unas pocas marcas del doblez)
		pliegues(g, Math.max(1.0F - k, 0.12F), altoDiseno);
		p.popMatrix();
		if (k < 0.5F) {
			bollito(g, tema, MARGEN + BOLA, MARGEN + BOLA, 1.0F - k * 2.0F);
		}
	}

	/** Marcas de arrugas sobre la hoja (en pixeles de diseno de HojaMisiones). */
	private static void pliegues(GuiGraphics g, float fuerza, int alto) {
		int a = Math.round(fuerza * 70);
		if (a <= 2) {
			return;
		}
		int oscuro = a << 24 | 0x3A2E18;
		int claro = (a / 2) << 24 | 0xFFFFFF;
		// unos dobleces quebrados que cruzan la hoja (siempre los mismos)
		for (int i = 0; i < 7; i++) {
			float x = 20 + h(i * 3.1F) * (HojaMisiones.ANCHO - 40);
			float y = 0;
			float dx = (h(i * 7.7F) - 0.5F) * 6.0F;
			while (y < alto) {
				float largo = 18 + h(i * 13.3F + y) * 30;
				float nx = Mth.clamp(x + dx * largo / 10.0F + (h(y * 0.37F + i) - 0.5F) * 14, 4, HojaMisiones.ANCHO - 6);
				linea(g, x, y, nx, Math.min(y + largo, alto - 1), oscuro, claro);
				x = nx;
				y += largo;
			}
		}
		for (int i = 0; i < 5; i++) {
			float y = 30 + h(i * 5.3F + 9) * Math.max(0, alto - 60);
			float x = 0;
			while (x < HojaMisiones.ANCHO) {
				float largo = 22 + h(i * 11.1F + x) * 40;
				float ny = y + (h(x * 0.21F + i * 3) - 0.5F) * 12;
				linea(g, x, y, x + largo, ny, oscuro, claro);
				x += largo;
				y = ny;
			}
		}
	}

	private static void linea(GuiGraphics g, float x0, float y0, float x1, float y1, int oscuro, int claro) {
		int n = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / 2.0F);
		for (int i = 0; i <= n; i++) {
			float u = n == 0 ? 0 : (float) i / n;
			int x = Math.round(Mth.lerp(u, x0, x1));
			int y = Math.round(Mth.lerp(u, y0, y1));
			g.fill(x, y, x + 2, y + 1, oscuro);
			g.fill(x, y + 1, x + 2, y + 2, claro);
		}
	}

	/**
	 * El papel hecho una bola: un contorno irregular con caras mas claras y mas oscuras
	 * (las arrugas) y aristas marcadas. `a`: opacidad.
	 */
	private static void bollito(GuiGraphics g, Tema tema, int cx, int cy, float a) {
		if (a <= 0.01F) {
			return;
		}
		int papel = tema.papel & 0xFFFFFF;
		// la sombra
		for (int y = -BOLA; y <= BOLA; y += 2) {
			for (int x = -BOLA; x <= BOLA; x += 2) {
				float d = Mth.sqrt(x * x + y * y);
				if (d < radio(Mth.atan2(y, x)) - 1) {
					g.fill(cx + x + 2, cy + y + 3, cx + x + 4, cy + y + 5, Math.round(a * 60) << 24);
				}
			}
		}
		for (int y = -BOLA; y <= BOLA; y += 2) {
			for (int x = -BOLA; x <= BOLA; x += 2) {
				float ang = (float) Mth.atan2(y, x);
				float d = Mth.sqrt(x * x + y * y);
				float r = radio(ang);
				if (d > r) {
					continue;
				}
				// cada cara de la bola tiene su tono: sectores y anillos movidos
				float sector = (ang + 3.1416F) / 6.2832F * 7.0F + d * 0.09F;
				int cara = (int) Math.floor(sector) + (d > r * 0.55F ? 11 : 0);
				float tono = 0.72F + 0.28F * h(cara * 1.7F);
				// luz desde arriba a la izquierda
				tono *= 0.85F + 0.15F * (-(x + y) / (BOLA * 2.0F) + 0.5F);
				boolean arista = sector - Math.floor(sector) < 0.12F || Math.abs(d - r * 0.55F) < 1.2F;
				if (arista) {
					tono *= 0.7F;
				}
				if (d > r - 1.6F) {
					tono *= 0.8F;
				}
				int col = mezcla(papel, tono);
				g.fill(cx + x, cy + y, cx + x + 2, cy + y + 2, Math.round(a * 255) << 24 | col);
			}
		}
	}

	/** El contorno de la bola en cada angulo: redondo pero con bultos. */
	private static float radio(double ang) {
		return BOLA * (0.84F + 0.1F * (float) Math.sin(ang * 3.0 + 1.3) + 0.06F * (float) Math.sin(ang * 7.0 + 0.4));
	}

	private static int mezcla(int rgb, float k) {
		int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k));
		int gg = Math.min(255, Math.round(((rgb >> 8) & 255) * k));
		int b = Math.min(255, Math.round((rgb & 255) * k));
		return r << 16 | gg << 8 | b;
	}

	private static float h(float n) {
		float s = Mth.sin(n * 12.9898F) * 43758.547F;
		return s - (float) Math.floor(s);
	}
}
