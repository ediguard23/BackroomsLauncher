package net.backrooms.evento.cliente;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.red.ViajeAscensor;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * El viaje en el ascensor de salida, en pantalla: las puertas se cierran
 * (dos hojas de acero desde los lados), negro mientras baja, y al llegar se
 * abren sobre la fase nueva con su nombre y dificultad. Si era la ultima,
 * "HAS ESCAPADO". El sonido (puertas, motor, campanilla) va en ascensor.ogg.
 */
public final class AscensorCliente {
	private static final float LLEGADA = Fases.BAJADA / 20.0F;
	private static final float FIN = LLEGADA + 5.5F;
	private static long inicio = -1;
	private static ViajeAscensor viaje;

	private AscensorCliente() {
	}

	public static void empezar(ViajeAscensor v) {
		// a las fases 2 y 3 se baja con su cinematica (CinematicaCliente, guiones 2 y 3);
		// a la 4 y a la salida, con este fundido
		if (v.fase() == 2 || v.fase() == 3) {
			olvidar();
			net.backrooms.evento.cliente.cinematica.CinematicaCliente.empezar(v.fase());
			return;
		}
		viaje = v;
		inicio = Util.getMillis();
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Sonidos.ASCENSOR, 1.0F, 1.0F));
	}

	public static void olvidar() {
		inicio = -1;
		viaje = null;
	}

	public static boolean activo() {
		return inicio >= 0 && segundos() < FIN;
	}

	private static float segundos() {
		return (Util.getMillis() - inicio) / 1000.0F;
	}

	public static void render(GuiGraphics g, DeltaTracker tiempo) {
		if (!activo() || viaje == null) {
			return;
		}
		float t = segundos();
		int w = g.guiWidth();
		int h = g.guiHeight();
		// cuanto cubren las puertas: se cierran en 1,2 s y se abren al llegar
		float cierre = t < LLEGADA ? Mth.clamp(t / 1.2F, 0, 1) : 1.0F - Mth.clamp((t - LLEGADA - 0.6F) / 1.3F, 0, 1);
		cierre = cierre * cierre * (3 - 2 * cierre);
		int hoja = Math.round(w / 2.0F * cierre);
		int temblor = t > 1.4F && t < LLEGADA - 0.3F ? Math.round((float) Math.sin(t * 61.0) * 1.5F) : 0;
		if (hoja > 0) {
			g.fill(0, temblor, hoja, h + temblor, 0xFF2A2C2E);
			g.fill(w - hoja, temblor, w, h + temblor, 0xFF2A2C2E);
			// brillo del acero cepillado y la junta del medio
			g.fill(hoja - 3, temblor, hoja, h + temblor, 0xFF55595D);
			g.fill(w - hoja, temblor, w - hoja + 3, h + temblor, 0xFF55595D);
		}
		if (cierre > 0.98F && t > 1.6F && t < LLEGADA) {
			// el piso que baja
			int planta = Math.max(1, (int) (9 - (t - 1.6F) * 1.8F));
			String s = "▼ " + planta;
			float tam = h / 10.0F;
			Texto.hud(g, s, w / 2.0F - Texto.anchoHud(s, tam, 0.1F) / 2.0F, h * 0.12F, tam, 0.1F, 0xCCFF5040);
		}
		// rotulo de llegada
		float a = Mth.clamp((t - LLEGADA - 0.8F) / 0.6F, 0, 1) * Mth.clamp((FIN - t) / 1.0F, 0, 1);
		if (a > 0.01F) {
			int alfa = Math.round(a * 235) << 24;
			float tam = h / 8.0F;
			String titulo = viaje.fase() == 0 ? "HAS ESCAPADO" : "FASE " + viaje.fase();
			float y = h * 0.34F;
			Texto.hud(g, titulo, w / 2.0F - Texto.anchoHud(titulo, tam, 0.16F) / 2.0F, y, tam, 0.16F, alfa | 0xE8D9A0);
			String sub = viaje.fase() == 0 ? "SALISTE DE LOS BACKROOMS" : viaje.nombre() + " · DIFICULTAD " + viaje.dificultad();
			float ts = tam * 0.32F;
			Texto.hud(g, sub, w / 2.0F - Texto.anchoHud(sub, ts, 0.25F) / 2.0F, y + tam * 1.1F, ts, 0.25F, alfa | 0xC8BC90);
		}
	}
}
