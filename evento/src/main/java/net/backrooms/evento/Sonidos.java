package net.backrooms.evento;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Sonidos del evento registrados en el juego. Hacen falta en el registro (y no
 * solo en sounds.json) para que los datos del mundo los puedan nombrar: el
 * bioma del Nivel 0 usa el zumbido como ambiente en bucle.
 */
public final class Sonidos {
	public static final SoundEvent ZUMBIDO = registrar("nivel0.zumbido");

	private Sonidos() {
	}

	private static SoundEvent registrar(String ruta) {
		Identifier id = BackroomsEvento.id(ruta);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void iniciar() {
		BackroomsEvento.LOG.info("Sonidos registrados ({})", ZUMBIDO.location());
	}
}
