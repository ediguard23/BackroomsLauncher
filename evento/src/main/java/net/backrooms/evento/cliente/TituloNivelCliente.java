package net.backrooms.evento.cliente;

import net.backrooms.evento.red.TituloNivel;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * El rotulo de un nivel al llegar (TituloNivel): «NIVEL FUN» grande y debajo «FEEDING ROOMS», con la letra
 * del evento y los mismos colores y medidas que el «NIVEL 3» del final de la Fiesta (PantallaSalidaFiesta).
 * Entra parpadeando como un fluorescente, se queda y se va despacio. Si llega en plena cinematica (el
 * tobogan), espera a que acabe.
 */
public final class TituloNivelCliente {
	private static final float ENTRA = 0.9F;
	private static final float DURA = 5.2F;
	private static final float SALE = 1.2F;

	private static @Nullable TituloNivel titulo;
	/** Ticks desde que se empezo a ver (-1: esperando a que acabe la cinematica). */
	private static int ticks = -1;

	private TituloNivelCliente() {
	}

	public static void mostrar(TituloNivel t) {
		titulo = t;
		ticks = -1;
	}

	public static void tick(Minecraft mc) {
		if (titulo == null) {
			return;
		}
		if (ticks < 0) {
			if (!Cinematicas.alguna() && mc.player != null) {
				ticks = 0;
			}
			return;
		}
		if (++ticks > (DURA + SALE) * 20) {
			titulo = null;
			ticks = -1;
		}
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		TituloNivel t = titulo;
		if (t == null || ticks < 0) {
			return;
		}
		float s = (ticks + delta.getGameTimeDeltaPartialTick(false)) / 20.0F;
		float a = Mth.clamp((DURA + SALE - s) / SALE, 0.0F, 1.0F);
		if (s < ENTRA) {
			// el parpadeo del fluorescente al encenderse
			float r = Mth.frac(Mth.sin((float) Math.floor(s * 16.0F) * 12.9898F) * 43758.547F);
			a = r < 0.5F ? 0.15F : 1.0F;
		}
		if (a <= 0.01F) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		int alfa = Math.round(a * 230) << 24;
		float tam = h / 7.0F;
		float y = h * 0.62F;
		float ts = tam * 0.28F;
		float xn = w / 2.0F - Texto.anchoHud(t.nivel(), tam, 0.18F) / 2.0F;
		float xs = w / 2.0F - Texto.anchoHud(t.sub(), ts, 0.3F) / 2.0F;
		// una sombra oscura debajo: en las salas claras de la Fiesta, sin ella apenas se lee
		int sombra = Math.round(a * 150) << 24;
		float d = Math.max(1.0F, h / 270.0F);
		Texto.hud(g, t.nivel(), xn + d, y + d, tam, 0.18F, sombra | 0x1A1408);
		Texto.hud(g, t.sub(), xs + d * 0.6F, y + tam * 1.05F + d * 0.6F, ts, 0.3F, sombra | 0x1A1408);
		Texto.hud(g, t.nivel(), xn, y, tam, 0.18F, alfa | 0xE8D9A0);
		Texto.hud(g, t.sub(), xs, y + tam * 1.05F, ts, 0.3F, alfa | 0xC8BC90);
	}
}
