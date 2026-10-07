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
 * Las cinematicas del ascensor en el cliente: cuenta el tiempo, abre la
 * pantalla que las dibuja, pone la banda sonora y dice cuanto se oye el mundo
 * y cuanto desenfoque lleva la vista al despertar.
 *
 * Hay tres guiones (el shader los lee de la V de los vertices):
 *   0  el /start (abajo)
 *   2  la bajada a la fase 2: el ascensor de salida se atasca, lo arreglas a
 *      golpes, vuelve a fallar y cae; se abre a medias al sector B
 *   3  la bajada a la fase 3: algo cruza el pasillo, apagon y vision
 *      nocturna, garras por la junta, golpes en el techo y una sonrisa
 * En los de las fases no se despierta: la cinta se rompe, negro con el
 * nombre de la fase (el servidor te lleva en ese negro, Fases#bajada) y se
 * vuelve a ver el mundo.
 *
 * Guion del /start (segundos):
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
	private static int guion;
	private static boolean guiAntes;
	private static SoundInstance banda;
	/** Lo que suena despues del viaje: Minecraft corta todos los sonidos al cambiar de mundo. */
	private static SoundInstance despues;
	private static Object nivel;

	private CinematicaCliente() {
	}

	/** El guion en curso: 0 el /start, 2 o 3 la bajada a esa fase. */
	public static int guion() {
		return guion;
	}

	/** Hasta cuando se ve la cinta. */
	public static float finCinta() {
		return guion == 2 ? 27.8F : guion == 3 ? 25.4F : FIN_CINTA;
	}

	/** Cuando se despierta (solo el /start; en las fases, nunca). */
	public static float despertar() {
		return guion == 0 ? DESPERTAR : Float.MAX_VALUE;
	}

	/** Cuando se cierra la pantalla. */
	public static float fin() {
		return guion == 2 ? 33.0F : guion == 3 ? 30.6F : FIN;
	}

	public static void empezar() {
		empezar(0);
	}

	public static void empezar(int g) {
		Minecraft mc = Minecraft.getInstance();
		if (activa()) {
			terminar();
		}
		guion = g == 2 || g == 3 ? g : 0;
		inicio = System.nanoTime();
		// un aviso de la megafonia del vestibulo que estuviera sonando no sigue dentro del ascensor
		mc.getSoundManager().stop(net.backrooms.evento.Sonidos.MEGAFONIA_VESTIBULO.location(), null);
		guiAntes = mc.options.hideGui;
		mc.options.hideGui = true;
		banda = sonar(mc, bandaId());
		despues = null;
		nivel = mc.level;
		mc.setScreen(new PantallaCinematica());
	}

	public static boolean activa() {
		return inicio >= 0;
	}

	private static Identifier bandaId() {
		return guion == 0 ? SONIDO : BackroomsEvento.id("cinematica.fase" + guion);
	}

	private static SoundInstance sonar(Minecraft mc, Identifier id) {
		SoundInstance s = new SimpleSoundInstance(id, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
			SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
		mc.getSoundManager().play(s);
		return s;
	}

	/** Si ese sonido es la banda de la cinematica en curso (no se silencia). */
	public static boolean esBanda(Identifier id) {
		return activa() && (id.equals(bandaId()) || id.equals(bandaId().withSuffix(".despues")));
	}

	/**
	 * Cada tick: al cambiar de mundo (el viaje) Minecraft para todos los sonidos y con ellos
	 * la banda; lo que viene despues va en su propio archivo (tools/sonidos) y se pone aqui.
	 */
	public static void tick(Minecraft mc) {
		if (!activa() || mc.level == null || mc.level == nivel) {
			return;
		}
		nivel = mc.level;
		if (despues == null) {
			despues = sonar(mc, bandaId().withSuffix(".despues"));
		}
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
		if (despues != null) {
			mc.getSoundManager().stop(despues);
			despues = null;
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
			despues = null;
		}
	}

	/** Cuanto se oyen los demas sonidos: nada hasta despertar, y vuelven poco a poco. */
	public static float volumenMundo() {
		if (!activa()) {
			return 1.0F;
		}
		float t = segundos();
		if (guion != 0) {
			return Mth.clamp((t - (fin() - 2.5F)) / 2.5F, 0.0F, 1.0F);
		}
		return Mth.clamp((t - (DESPERTAR + 1.0F)) / 6.5F, 0.0F, 1.0F);
	}

	/** Radio del desenfoque de la vista al despertar, o -1 si no toca. */
	public static int desenfoque() {
		if (!activa()) {
			return -1;
		}
		float t = segundos();
		if (guion != 0 || t < DESPERTAR) {
			return -1;
		}
		float k = Mth.clamp((t - DESPERTAR) / (FIN - 1.0F - DESPERTAR), 0.0F, 1.0F);
		return Math.round(Mth.lerp(k * (2.0F - k), 22.0F, 0.0F));
	}
}
