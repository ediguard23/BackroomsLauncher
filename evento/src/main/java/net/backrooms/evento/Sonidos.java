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
	public static final SoundEvent BACTERIA_CAZA = registrar("bacteria.caza");
	public static final SoundEvent SMILER_FLASH = registrar("smiler.flash");
	public static final SoundEvent SMILER_GRITO = registrar("smiler.grito");
	public static final SoundEvent PITIDO = registrar("pitido");
	public static final SoundEvent SUSURROS = registrar("susurros");
	public static final SoundEvent LATIDO = registrar("latido");
	public static final SoundEvent MEGAFONIA_VESTIBULO = registrar("megafonia.vestibulo");
	public static final SoundEvent MEGAFONIA_ALERTA = registrar("megafonia.alerta");
	// acciones del jugador (tools/sonidos/acciones.js): ninguna con el sonido generico de Minecraft
	public static final SoundEvent CASETE_COGER = registrar("casete.coger");
	public static final SoundEvent AGUA_COGER = registrar("agua.coger");
	public static final SoundEvent COMIDA_COGER = registrar("comida.coger");
	public static final SoundEvent AGUA_BEBER = registrar("agua.beber");
	public static final SoundEvent AGUA_SUSPIRO = registrar("agua.suspiro");
	public static final SoundEvent NOTA_LEER = registrar("nota.leer");
	public static final SoundEvent BUTACA = registrar("butaca");
	public static final SoundEvent BACTERIA_GOLPE = registrar("bacteria.golpe");
	// el agarre: te agarra, te levanta, muerde tres veces y te devora (tools/sonidos/bacteria.js)
	public static final SoundEvent BACTERIA_AGARRE = registrar("bacteria.agarre");
	public static final SoundEvent BACTERIA_LEVANTA = registrar("bacteria.levanta");
	public static final SoundEvent BACTERIA_MORDISCO = registrar("bacteria.mordisco");
	public static final SoundEvent BACTERIA_DEVORA = registrar("bacteria.devora");
	/** Te ha perdido o te has escondido: grunido de rabia y se va. */
	public static final SoundEvent BACTERIA_RENUNCIA = registrar("bacteria.renuncia");
	/** El grito ahogado de quien acaba de agarrar. */
	public static final SoundEvent VICTIMA_GRITO = registrar("victima.grito");
	/** Meterse o salir de un hueco de la pared: pladur que cruje y cascotes. */
	public static final SoundEvent HUECO = registrar("hueco");
	public static final SoundEvent ASCENSOR_DENEGADO = registrar("ascensor.denegado");
	public static final SoundEvent ASCENSOR_PANEL = registrar("ascensor.panel");
	public static final SoundEvent MISION_COMPLETA = registrar("mision.completa");
	public static final SoundEvent ESCAPADO = registrar("escapado");
	public static final SoundEvent ELIMINADO = registrar("eliminado");
	public static final SoundEvent MOQUETA_PASO = registrar("moqueta.paso");
	public static final SoundEvent MOQUETA_MOJADA_PASO = registrar("moqueta_mojada.paso");

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
