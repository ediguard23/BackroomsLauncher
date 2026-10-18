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
	// las Piscinas (tools/sonidos/nivel37.js): nada en bucle; lo suelto lo pone AmbientePiscinas
	public static final SoundEvent PISCINAS_GOTA = registrar("piscinas.gota");
	public static final SoundEvent PISCINAS_AGUA = registrar("piscinas.agua");
	public static final SoundEvent PISCINAS_ORILLA = registrar("piscinas.orilla");
	public static final SoundEvent PISCINAS_CHAPOTEO = registrar("piscinas.chapoteo");
	public static final SoundEvent PISCINAS_TUBERIA = registrar("piscinas.tuberia");
	public static final SoundEvent PISCINAS_GRAVE = registrar("piscinas.grave");
	public static final SoundEvent PISCINAS_VOZ = registrar("piscinas.voz");
	public static final SoundEvent PISCINAS_PASO = registrar("piscinas.paso");
	public static final SoundEvent PISCINAS_PASO_AGUA = registrar("piscinas.paso_agua");
	public static final SoundEvent PISCINAS_NADO = registrar("piscinas.nado");
	public static final SoundEvent PISCINAS_ZAMBULLIDA = registrar("piscinas.zambullida");
	public static final SoundEvent PISCINAS_CHISPA = registrar("piscinas.chispa");
	/** El susurro con el nombre: se sintetiza en el cliente (SusurroNombre); en sounds.json solo hay un hueco. */
	public static final SoundEvent PISCINAS_SUSURRO = registrar("piscinas.susurro");
	public static final SoundEvent SERPIENTE_GRUNIDO = registrar("serpiente.grunido");
	public static final SoundEvent SERPIENTE_RESPIRA = registrar("serpiente.respira");
	public static final SoundEvent SERPIENTE_PASO = registrar("serpiente.paso");
	public static final SoundEvent SERPIENTE_AGUA = registrar("serpiente.agua");
	public static final SoundEvent SERPIENTE_GRITO = registrar("serpiente.grito");
	public static final SoundEvent SERPIENTE_CAZA = registrar("serpiente.caza");
	public static final SoundEvent SERPIENTE_ATAQUE = registrar("serpiente.ataque");
	public static final SoundEvent SERPIENTE_MORDISCO = registrar("serpiente.mordisco");
	/* las misiones de las Piscinas */
	public static final SoundEvent VALVULA_GIRO = registrar("valvula.giro");
	public static final SoundEvent VALVULA_CIERRE = registrar("valvula.cierre");
	public static final SoundEvent VALVULA_MAQUINARIA = registrar("valvula.maquinaria");
	public static final SoundEvent TARJETA_COGER = registrar("tarjeta.coger");
	public static final SoundEvent LECTOR_ACEPTA = registrar("lector.acepta");
	public static final SoundEvent PUERTA_SALIDA = registrar("puerta.salida");
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
	/** Los avisos de la recepcion, por turnos (Vestibulo#megafonia): el de la linterna y los de espera. */
	public static final SoundEvent[] MEGAFONIA_RECEPCION = {MEGAFONIA_VESTIBULO, registrar("megafonia.recepcion_1"),
		registrar("megafonia.recepcion_2"), registrar("megafonia.recepcion_3")};
	public static final SoundEvent MEGAFONIA_ALERTA = registrar("megafonia.alerta");
	// la inundacion de las Piscinas: "fallo en el sistema de drenaje" y "averia contenida"
	public static final SoundEvent MEGAFONIA_INUNDACION = registrar("megafonia.inundacion");
	public static final SoundEvent MEGAFONIA_DRENAJE = registrar("megafonia.drenaje");
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

	/* la Fiesta (Nivel Fun): tools/sonidos/fiesta.js y las voces de tools/sonidos/voces.js */
	public static final SoundEvent FIESTA_BIENVENIDA = registrar("fiesta.bienvenida");
	public static final SoundEvent FIESTA_APAGON = registrar("fiesta.apagon");
	public static final SoundEvent FIESTA_LUZ = registrar("fiesta.luz");
	public static final SoundEvent FIESTA_PREMIO = registrar("fiesta.premio");
	/** Los anuncios sueltos de la megafonia de la Fiesta (AmbienteFiesta, por turnos). */
	public static final SoundEvent[] FIESTA_ANUNCIOS = {registrar("fiesta.anuncio_tickets"), registrar("fiesta.anuncio_semaforo"),
		registrar("fiesta.anuncio_comida"), registrar("fiesta.anuncio_sonrian"), registrar("fiesta.anuncio_cortado")};
	public static final SoundEvent FIESTA_MONEDA = registrar("fiesta.moneda");
	public static final SoundEvent FIESTA_TICKETS = registrar("fiesta.tickets");
	public static final SoundEvent FIESTA_TORNO = registrar("fiesta.torno");
	public static final SoundEvent FIESTA_TORNO_NO = registrar("fiesta.torno_no");
	public static final SoundEvent FIESTA_OBJETIVO = registrar("fiesta.objetivo");
	public static final SoundEvent FIESTA_REGALO = registrar("fiesta.regalo");
	public static final SoundEvent FIESTA_AMBIENTE = registrar("fiesta.ambiente");
	public static final SoundEvent FIESTA_MUSICA = registrar("fiesta.musica");
	public static final SoundEvent FIESTA_GLOBO = registrar("fiesta.globo");
	public static final SoundEvent FIESTA_JUGUETE = registrar("fiesta.juguete");
	public static final SoundEvent FIESTA_NINO = registrar("fiesta.nino");
	public static final SoundEvent SEMAFORO_VERDE = registrar("semaforo.verde");
	public static final SoundEvent SEMAFORO_ROJA = registrar("semaforo.roja");
	public static final SoundEvent SEMAFORO_PILLADO = registrar("semaforo.pillado");
	public static final SoundEvent SEMAFORO_GANA = registrar("semaforo.gana");
	public static final SoundEvent SEMAFORO_MUSICA = registrar("semaforo.musica");
	public static final SoundEvent ANFITRION_PASO = registrar("anfitrion.paso");
	public static final SoundEvent ANFITRION_DETECTA = registrar("anfitrion.detecta");
	public static final SoundEvent ANFITRION_RESPIRA = registrar("anfitrion.respira");
	public static final SoundEvent ANFITRION_PIERDE = registrar("anfitrion.pierde");
	public static final SoundEvent ANFITRION_ABRAZO = registrar("anfitrion.abrazo");
	public static final SoundEvent ANFITRION_VOZ = registrar("anfitrion.voz");
	public static final SoundEvent ANFITRION_RISA = registrar("anfitrion.risa");

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
