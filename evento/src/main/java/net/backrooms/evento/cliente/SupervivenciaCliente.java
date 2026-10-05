package net.backrooms.evento.cliente;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.cliente.efectos.Cordura;
import net.backrooms.evento.cliente.efectos.Flash;
import net.backrooms.evento.red.EstadoJugador;
import net.backrooms.evento.red.Susto;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;

/**
 * Cordura y estamina en el cliente: el panel de abajo a la izquierda, las
 * alucinaciones y los sustos que manda el servidor.
 *
 * Alucinaciones (solo las oye y ve el propio jugador): por debajo del 50 %
 * de cordura, de vez en cuando suenan pasos a la espalda, golpes, susurros
 * o un grito lejano; por debajo del 30 % ademas late el corazon y hay
 * fogonazos de un instante; cuanto mas baja, mas a menudo.
 */
public final class SupervivenciaCliente {
	private static EstadoJugador estado = new EstadoJugador(100, 100, false, false);
	private static float estaminaSuave = 100;
	private static int proximaAlucinacion = 400;
	private static int latido;
	private static long cara = -1;
	private static final RandomSource AZAR = RandomSource.create();

	private SupervivenciaCliente() {
	}

	public static void recibir(EstadoJugador e) {
		estado = e;
		Cordura.poner(e.activa() ? e.cordura() : 100.0F);
	}

	public static void susto(Susto s) {
		Minecraft mc = Minecraft.getInstance();
		if (s.tipo() == Susto.FLASH) {
			Flash.disparar();
			mc.getSoundManager().play(SimpleSoundInstance.forUI(Sonidos.PITIDO, 1.0F, 0.9F));
		} else {
			cara = Util.getMillis();
			Cordura.asustar();
		}
	}

	public static boolean agotado() {
		return estado.activa() && estado.agotado();
	}

	public static void olvidar() {
		estado = new EstadoJugador(100, 100, false, false);
		estaminaSuave = 100;
		cara = -1;
	}

	/** Cada tick: las alucinaciones. */
	public static void tick(Minecraft mc) {
		estaminaSuave += (estado.estamina() - estaminaSuave) * 0.3F;
		Player j = mc.player;
		if (j == null || !estado.activa() || mc.isPaused()) {
			return;
		}
		float c = estado.cordura();
		if (c < 30 && --latido <= 0) {
			latido = c < 15 ? 16 : 24;
			mc.getSoundManager().play(SimpleSoundInstance.forUI(Sonidos.LATIDO, c < 15 ? 0.9F : 0.6F, 1.0F));
		}
		if (c >= 50 || --proximaAlucinacion > 0) {
			return;
		}
		// cuanto menos cordura, mas a menudo (de ~40 s a ~8 s)
		float k = Mth.clamp(c / 50.0F, 0.0F, 1.0F);
		proximaAlucinacion = (int) (160 + k * 640 + AZAR.nextInt(200));
		alucinar(mc, j, c);
	}

	private static void alucinar(Minecraft mc, Player j, float c) {
		// el sonido sale de algun sitio cerca, casi siempre detras
		double ang = Math.toRadians(j.getYRot() + 90 + (AZAR.nextDouble() - 0.5) * 140) + Math.PI;
		double d = 3 + AZAR.nextDouble() * 9;
		double x = j.getX() + Math.cos(ang) * d;
		double z = j.getZ() + Math.sin(ang) * d;
		SoundEvent s;
		int r = AZAR.nextInt(c < 30 ? 6 : 4);
		switch (r) {
			case 0 -> s = SoundEvents.WOOD_STEP;
			case 1 -> s = Sonidos.SUSURROS;
			case 2 -> s = SoundEvents.WOODEN_DOOR_CLOSE;
			case 3 -> s = Sonidos.BACTERIA_ACECHO;
			case 4 -> s = Sonidos.BACTERIA_GRITO;
			default -> s = Sonidos.SMILER_GRITO;
		}
		float vol = r >= 4 ? 0.35F : 0.8F;
		mc.getSoundManager().play(new SimpleSoundInstance(s.location(), SoundSource.AMBIENT, vol, 0.8F + AZAR.nextFloat() * 0.3F, AZAR,
			false, 0, SoundInstance.Attenuation.LINEAR, x, j.getEyeY(), z, false));
		if (s == SoundEvents.WOOD_STEP) {
			// pasos: tres o cuatro seguidos
			for (int i = 1; i < 4; i++) {
				mc.getSoundManager().playDelayed(new SimpleSoundInstance(s.location(), SoundSource.AMBIENT, 0.7F, 0.9F, AZAR,
					false, 0, SoundInstance.Attenuation.LINEAR, x, j.getEyeY(), z, false), i * 7);
			}
		}
		if (c < 30 && AZAR.nextInt(3) == 0) {
			Cordura.asustar();
		}
	}

	/* --------------------------------------------------------------- HUD */

	/** Cordura y estamina, abajo a la izquierda; y la cara del Smiler que te alcanza. */
	public static void render(GuiGraphics g, DeltaTracker tiempo) {
		Minecraft mc = Minecraft.getInstance();
		int w = g.guiWidth();
		int h = g.guiHeight();
		if (cara >= 0) {
			float t = (Util.getMillis() - cara) / 1000.0F;
			if (t < 0.9F) {
				caraSmiler(g, w, h, t);
			} else {
				cara = -1;
			}
		}
		if (!estado.activa() || mc.options.hideGui || mc.player == null || HerramientasCliente.subida(1.0F) > 0.5F) {
			return;
		}
		float x = 8;
		float y = h - 34;
		float c = Cordura.mostrada();
		int colorCordura = c > 50 ? 0xFFE8D9A0 : c > 25 ? 0xFFE0A040 : ((Util.getMillis() / 300) % 2 == 0 ? 0xFFE5281E : 0xFF7A1410);
		Texto.hud(g, "CORDURA " + Math.round(c) + "%", x, y, 10, 0.06F, colorCordura);
		barra(g, x, y + 11, 80, c / 100.0F, colorCordura);
		float e = estaminaSuave;
		if (e < 99.5F || estado.agotado()) {
			int colorEst = estado.agotado() ? 0xFFB04030 : 0xFFF2EEE4;
			Texto.hud(g, estado.agotado() ? "SIN ALIENTO" : "ESTAMINA", x, y - 16, 8, 0.06F, colorEst);
			barra(g, x, y - 7, 60, e / 100.0F, colorEst);
		}
	}

	private static void barra(GuiGraphics g, float x, float y, int ancho, float valor, int color) {
		int x0 = Math.round(x);
		int y0 = Math.round(y);
		g.fill(x0, y0, x0 + ancho, y0 + 4, 0x90000000);
		g.fill(x0 + 1, y0 + 1, x0 + 1 + Math.round((ancho - 2) * Mth.clamp(valor, 0, 1)), y0 + 3, color);
	}

	/** La sonrisa que se te echa encima: ojos y dientes blancos sobre negro, temblando. */
	private static void caraSmiler(GuiGraphics g, int w, int h, float t) {
		int a = Math.round(Mth.clamp(1.0F - (t - 0.6F) / 0.3F, 0, 1) * 255);
		g.fill(0, 0, w, h, (a << 24));
		float s = h / 14.0F * (1.0F + t * 0.6F);
		float cx = w / 2.0F + (AZAR.nextFloat() - 0.5F) * s * 0.6F;
		float cy = h / 2.0F + (AZAR.nextFloat() - 0.5F) * s * 0.6F;
		int blanco = (a << 24) | 0xF8F8F0;
		// ojos
		for (int lado = -1; lado <= 1; lado += 2) {
			float ex = cx + lado * s * 3.2F;
			g.fill(Math.round(ex - s * 1.4F), Math.round(cy - s * 3.6F), Math.round(ex + s * 1.4F), Math.round(cy - s * 2.4F), blanco);
		}
		// sonrisa en media luna, a franjas de dientes
		for (int i = -10; i <= 10; i++) {
			float k = i / 10.0F;
			float yy = cy + s * (1.4F * (1 - k * k));
			float x0 = cx + i * s * 0.6F;
			int col = i % 2 == 0 ? blanco : ((a << 24) | 0xB0AA96);
			g.fill(Math.round(x0), Math.round(yy), Math.round(x0 + s * 0.58F), Math.round(yy + s * 1.1F), col);
		}
	}
}
