package net.backrooms.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Sonidos del menu. No se registran en el registro de sonidos del juego (el
 * mod es solo de cliente y no debe tocar registros que el servidor sincroniza):
 * se reproducen por su identificador, que resuelve assets/backrooms/sounds.json.
 *
 * El ambiente de las Piscinas y su musica son dos bucles sin costura que suenan
 * mientras no hay mundo cargado; la musica de menu de Minecraft queda anulada
 * (ver MinecraftMixin#getSituationalMusic).
 */
public final class Sonidos {
	public static final SoundEvent GOTA = evento("ui.gota");
	public static final SoundEvent TOQUE = evento("ui.toque");
	public static final SoundEvent INMERSION = evento("ui.inmersion");
	public static final SoundEvent ECO = evento("ui.eco");

	private static SoundInstance ambiente;
	private static SoundInstance musica;
	private static int ticksDesdeInicio;

	private Sonidos() {
	}

	private static Identifier id(String ruta) {
		return Identifier.fromNamespaceAndPath("backrooms", ruta);
	}

	private static SoundEvent evento(String ruta) {
		return SoundEvent.createVariableRangeEvent(id(ruta));
	}

	public static void ui(SoundEvent evento, float volumen) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(evento, 1.0F, volumen));
	}

	private static SoundInstance bucle(String ruta, SoundSource fuente, float volumen) {
		return new SimpleSoundInstance(
			id(ruta), fuente, volumen, 1.0F, SoundInstance.createUnseededRandom(), true, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true
		);
	}

	/** Cada tick del cliente: bucles encendidos en los menus, apagados dentro de un mundo. */
	public static void tick(Minecraft mc) {
		SoundManager sm = mc.getSoundManager();
		boolean enMenu = mc.level == null && mc.getOverlay() == null;
		if (!enMenu) {
			parar(sm);
			return;
		}
		ticksDesdeInicio++;
		// isActive tarda unos ticks en ser verdad tras play(): no se reintenta antes de 2 s
		if (ambiente == null || (ticksDesdeInicio > 40 && !sm.isActive(ambiente))) {
			ambiente = bucle("menu.ambiente", SoundSource.AMBIENT, 0.9F);
			sm.play(ambiente);
			ticksDesdeInicio = 0;
		}
		if (musica == null || (ticksDesdeInicio > 40 && !sm.isActive(musica))) {
			musica = bucle("menu.musica", SoundSource.MUSIC, 0.8F);
			sm.play(musica);
			ticksDesdeInicio = 0;
		}
	}

	private static void parar(SoundManager sm) {
		if (ambiente != null) {
			sm.stop(ambiente);
			ambiente = null;
		}
		if (musica != null) {
			sm.stop(musica);
			musica = null;
		}
	}
}
