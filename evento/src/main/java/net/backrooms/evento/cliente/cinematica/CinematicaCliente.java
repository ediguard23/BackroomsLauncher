package net.backrooms.evento.cliente.cinematica;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * La cinematica del /start en el cliente: cuenta el tiempo, abre la pantalla
 * que la dibuja, pone la banda sonora y dice cuanto se oye el mundo y cuanto
 * desenfoque lleva la vista al despertar.
 *
 * Guion (segundos):
 *   0 - 3.6    la videocamara se enciende; las puertas del ascensor se cierran
 *   3.6 - 20   baja con calma (musica de ascensor), a los 18 la luz falla
 *  20 - 26.5   frenazo: atascado, crujidos, alarma, se escurre dos veces
 *  26.5 - 39   se parte el cable: caida libre, chispas, fuego, revientan las puertas
 *  39 - 45     golpe; negro, pitido y latidos (el servidor te lleva al Nivel 0 a los 41)
 *  45 - 55     abres los ojos: parpadeos, todo borroso, el oido vuelve poco a poco
 */
public final class CinematicaCliente {
	public static final float FIN_CINTA = 39.4F;
	public static final float DESPERTAR = 45.0F;
	public static final float FIN = 55.0F;
	public static final Identifier SONIDO = BackroomsEvento.id("cinematica");

	private static long inicio = -1;
	private static boolean guiAntes;
	private static SoundInstance banda;

	private CinematicaCliente() {
	}

	public static void empezar() {
		Minecraft mc = Minecraft.getInstance();
		if (activa()) {
			terminar();
		}
		inicio = System.nanoTime();
		// un aviso de la megafonia del vestibulo que estuviera sonando no sigue dentro del ascensor
		mc.getSoundManager().stop(net.backrooms.evento.Sonidos.MEGAFONIA_VESTIBULO.location(), null);
		guiAntes = mc.options.hideGui;
		mc.options.hideGui = true;
		banda = new SimpleSoundInstance(SONIDO, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
			SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
		mc.getSoundManager().play(banda);
		mc.setScreen(new PantallaCinematica());
	}

	public static boolean activa() {
		return inicio >= 0;
	}

	/** Segundos desde que empezo (o -1). */
	public static float segundos() {
		return inicio < 0 ? -1.0F : (System.nanoTime() - inicio) / 1.0e9F;
	}

	/** Si algo cierra la pantalla (la de cargar al cambiar de mundo), vuelve. */
	public static Screen pantalla() {
		return new PantallaCinematica();
	}

	static void terminar() {
		Minecraft mc = Minecraft.getInstance();
		inicio = -1;
		mc.options.hideGui = guiAntes;
		if (banda != null) {
			mc.getSoundManager().stop(banda);
			banda = null;
		}
		if (mc.screen instanceof PantallaCinematica) {
			mc.setScreen(null);
		}
	}

	/** Al desconectarse no queda nada a medias. */
	public static void olvidar() {
		if (activa()) {
			inicio = -1;
			Minecraft.getInstance().options.hideGui = guiAntes;
			banda = null;
		}
	}

	/** Cuanto se oyen los demas sonidos: nada hasta despertar, y vuelven poco a poco. */
	public static float volumenMundo() {
		if (!activa()) {
			return 1.0F;
		}
		float t = segundos();
		return Mth.clamp((t - (DESPERTAR + 1.0F)) / 6.5F, 0.0F, 1.0F);
	}

	/** Radio del desenfoque de la vista al despertar, o -1 si no toca. */
	public static int desenfoque() {
		if (!activa()) {
			return -1;
		}
		float t = segundos();
		if (t < DESPERTAR) {
			return -1;
		}
		float k = Mth.clamp((t - DESPERTAR) / (FIN - 1.0F - DESPERTAR), 0.0F, 1.0F);
		return Math.round(Mth.lerp(k * (2.0F - k), 22.0F, 0.0F));
	}
}
