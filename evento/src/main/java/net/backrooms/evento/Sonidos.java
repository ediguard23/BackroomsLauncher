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
	public static final SoundEvent LINTERNA = registrar("linterna");
	public static final SoundEvent CAMARA = registrar("camara");
	public static final SoundEvent APAGON = registrar("apagon");
	public static final SoundEvent LUZ_VUELVE = registrar("luz_vuelve");
	public static final SoundEvent ALARMA = registrar("alarma");
	public static final SoundEvent ASCENSOR = registrar("ascensor");
	public static final SoundEvent VESTIBULO = registrar("vestibulo.ambiente");
	public static final SoundEvent BACTERIA_GRITO = registrar("bacteria.grito");
	public static final SoundEvent BACTERIA_ACECHO = registrar("bacteria.acecho");
	public static final SoundEvent BACTERIA_PASOS = registrar("bacteria.pasos");
	public static final SoundEvent SMILER_FLASH = registrar("smiler.flash");
	public static final SoundEvent SMILER_GRITO = registrar("smiler.grito");
	public static final SoundEvent PITIDO = registrar("pitido");
	public static final SoundEvent SUSURROS = registrar("susurros");
	public static final SoundEvent LATIDO = registrar("latido");
	public static final SoundEvent MEGAFONIA_VESTIBULO = registrar("megafonia.vestibulo");
	public static final SoundEvent MEGAFONIA_ALERTA = registrar("megafonia.alerta");
	public static final SoundEvent ELIMINADO = registrar("eliminado");

	private Sonidos() {
	}

	private static SoundEvent registrar(String ruta) {
		Identifier id = BackroomsEvento.id(ruta);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Un sonido solo para este jugador, sin posicion (megafonia, avisos). */
	public static void aJugador(net.minecraft.server.level.ServerPlayer j, SoundEvent sonido, float volumen) {
		j.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
			net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sonido), net.minecraft.sounds.SoundSource.AMBIENT,
			j.getX(), j.getEyeY(), j.getZ(), volumen, 1.0F, j.getRandom().nextLong()));
	}

	public static void iniciar() {
		BackroomsEvento.LOG.info("Sonidos registrados ({})", ZUMBIDO.location());
	}
}
