package net.backrooms.evento;

import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod del evento Backrooms (cliente y servidor): el Nivel 0 como mundo, sus
 * bloques y, mas adelante, sus entidades y la seguridad de acceso.
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
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("nivel_0"), GeneradorNivel0.CODEC);
	}
}
