package net.backrooms.evento;

import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.objetos.HerramientasServidor;
import net.backrooms.evento.red.AccionJugador;
import net.backrooms.evento.red.EstadoAmbiente;
import net.backrooms.evento.red.Herramientas;
import net.backrooms.evento.red.ViajeAscensor;
import net.backrooms.evento.vestibulo.GeneradorVestibulo;
import net.backrooms.evento.vestibulo.Vestibulo;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.red.SyncMisiones;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.backrooms.evento.objetos.Equipo;
import net.backrooms.evento.objetos.Objetos;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod del evento Backrooms (cliente y servidor): el vestibulo, las fases del
 * Nivel 0 (cada una su dimension), sus bloques, entidades, apagones y
 * alarmas, las misiones, la linterna y la camara, y la supervivencia.
 *
 * El servidor del evento usa el tipo de mundo backrooms_evento:nivel_0
 * (level-type en server.properties): el mundo principal ES el Nivel 0.
 */
public class BackroomsEvento implements ModInitializer {
	public static final String ID = "backrooms_evento";
	public static final Logger LOG = LoggerFactory.getLogger("backrooms-evento");

	public static Identifier id(String ruta) {
		return Identifier.fromNamespaceAndPath(ID, ruta);
	}

	@Override
	public void onInitialize() {
		Bloques.iniciar();
		Sonidos.iniciar();
		Objetos.iniciar();
		Equipo.registrar();
		Entidades.iniciar();
		PayloadTypeRegistry.playS2C().register(SyncMisiones.TYPE, SyncMisiones.CODEC);
		PayloadTypeRegistry.playS2C().register(net.backrooms.evento.red.IniciarCinematica.TYPE, net.backrooms.evento.red.IniciarCinematica.CODEC);
		PayloadTypeRegistry.playS2C().register(EstadoAmbiente.TYPE, EstadoAmbiente.CODEC);
		PayloadTypeRegistry.playS2C().register(Herramientas.TYPE, Herramientas.CODEC);
		PayloadTypeRegistry.playS2C().register(ViajeAscensor.TYPE, ViajeAscensor.CODEC);
		PayloadTypeRegistry.playS2C().register(net.backrooms.evento.red.EstadoJugador.TYPE, net.backrooms.evento.red.EstadoJugador.CODEC);
		PayloadTypeRegistry.playS2C().register(net.backrooms.evento.red.Susto.TYPE, net.backrooms.evento.red.Susto.CODEC);
		PayloadTypeRegistry.playS2C().register(net.backrooms.evento.red.Eliminado.TYPE, net.backrooms.evento.red.Eliminado.CODEC);
		PayloadTypeRegistry.playS2C().register(net.backrooms.evento.red.EstadoArrastre.TYPE, net.backrooms.evento.red.EstadoArrastre.CODEC);
		PayloadTypeRegistry.playS2C().register(net.backrooms.evento.red.Agarrado.TYPE, net.backrooms.evento.red.Agarrado.CODEC);
		PayloadTypeRegistry.playC2S().register(AccionJugador.TYPE, AccionJugador.CODEC);
		Misiones.registrar();
		net.backrooms.evento.expedicion.Expedicion.registrar();
		net.backrooms.evento.expedicion.Eliminacion.registrar();
		Ambiente.registrar();
		HerramientasServidor.registrar();
		Fases.registrar();
		Vestibulo.registrar();
		net.backrooms.evento.supervivencia.Supervivencia.registrar();
		net.backrooms.evento.supervivencia.Comida.registrar();
		net.backrooms.evento.entidad.Acechadores.registrar();
		net.backrooms.evento.entidad.Bacteria.registrar();
		net.backrooms.evento.escondite.Arrastre.registrar();
		net.backrooms.evento.escondite.Escondites.registrar();
		net.backrooms.evento.mision.Grabacion.registrar();
		net.backrooms.evento.acceso.Acceso.registrar();
		Comandos.registrar();
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("nivel_0"), GeneradorNivel0.CODEC);
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("vestibulo"), GeneradorVestibulo.CODEC);
	}
}
